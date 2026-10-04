/*
  Simple DirectMedia Layer
  Copyright (C) 1997-2026 Sam Lantinga <slouken@libsdl.org>

  This software is provided 'as-is', without any express or implied
  warranty.  In no event will the authors be held liable for any damages
  arising from the use of this software.

  Permission is granted to anyone to use this software for any purpose,
  including commercial applications, and to alter it and redistribute it
  freely, subject to the following restrictions:

  1. The origin of this software must not be misrepresented; you must not
     claim that you wrote the original software. If you use this software
     in a product, an acknowledgment in the product documentation would be
     appreciated but is not required.
  2. Altered source versions must be plainly marked as such, and must not be
     misrepresented as being the original software.
  3. This notice may not be removed or altered from any source distribution.
*/
#include "SDL_internal.h"

#ifdef SDL_VIDEO_DRIVER_ANDROID

#include "../SDL_sysvideo.h"
#include "../../events/SDL_keyboard_c.h"
#include "../../events/SDL_mouse_c.h"
#include "../../events/SDL_windowevents_c.h"
#include "../../core/android/SDL_android.h"

#include "SDL_androidvideo.h"
#include "SDL_androidevents.h"
#include "SDL_androidwindow.h"


// Currently only one window
SDL_Window *Android_Window = NULL;

static bool Android_CreateOffscreenWindow(SDL_VideoDevice *_this, SDL_Window *window, SDL_PropertiesID create_props)
{
    window->x = 0;
    window->y = 0;
    window->w = Android_SurfaceWidth;
    window->h = Android_SurfaceHeight;

    SDL_WindowData *data = (SDL_WindowData *)SDL_calloc(1, sizeof(*data));
    if (!data) {
        return false;
    }
    // Only primary window has ANativeWindow
    data->native_window = NULL;
    SDL_SetAtomicInt(&data->surface_mode, SDL_ANDROID_WINDOW_PLATFORM);

#ifdef SDL_VIDEO_OPENGL_EGL
    if (window->flags & SDL_WINDOW_OPENGL) {
        data->egl_surface = SDL_EGL_CreateOffscreenSurface(_this, window->w, window->h);
        if (data->egl_surface == EGL_NO_SURFACE) {
            SDL_free(data);
            return false;
        }
    }
    SDL_SetPointerProperty(SDL_GetWindowProperties(window), SDL_PROP_WINDOW_ANDROID_SURFACE_POINTER, data->egl_surface);
#endif
    window->internal = data;
    return true;
}

bool Android_CreateWindow(SDL_VideoDevice *_this, SDL_Window *window, SDL_PropertiesID create_props)
{
    bool result = true;

    if (!Android_WaitActiveAndLockActivity()) {
        return false;
    }

    if(window->flags & SDL_WINDOW_HIDDEN) {
        result = Android_CreateOffscreenWindow(_this, window, create_props);
        goto endfunction;
    }

    if(Android_Window && window->flags & SDL_WINDOW_VULKAN) {
        result = SDL_SetError("Android does not support creating multiple Vulkan windows");
        goto endfunction;
    }

    if(Android_Window) {
        SDL_Log("Creating offscreen window as Android does not support multiple visible windows");
        // We only store a single owner window, other ones are offscreen
        result = Android_CreateOffscreenWindow(_this, window, create_props);
        goto endfunction;
    }

    ANativeWindow *anw = Android_JNI_GetNativeWindow();

    SDL_WindowData *data;

    // Set orientation
    Android_JNI_SetOrientation(window->w, window->h, window->flags & SDL_WINDOW_RESIZABLE, SDL_GetHint(SDL_HINT_ORIENTATIONS));

    // Adjust the window data to match the screen
    window->x = 0;
    window->y = 0;
    window->w = Android_SurfaceWidth;
    window->h = Android_SurfaceHeight;

    // One window, it always has focus
    SDL_SetMouseFocus(window);
    SDL_SetKeyboardFocus(window);

    data = (SDL_WindowData *)SDL_calloc(1, sizeof(*data));
    if (!data) {
        result = false;
        goto endfunction;
    }

    data->native_window = anw;

    // CreateOffscreenWindow() is for additional windows, here we still create it as the main one
    SDL_SetAtomicInt(&data->surface_mode, anw ? SDL_ANDROID_WINDOW_PLATFORM : SDL_ANDROID_WINDOW_OFFSCREEN);

    SDL_SetPointerProperty(SDL_GetWindowProperties(window), SDL_PROP_WINDOW_ANDROID_WINDOW_POINTER, data->native_window);

    /* Do not create EGLSurface for Vulkan window since it will then make the window
       incompatible with vkCreateAndroidSurfaceKHR */
#ifdef SDL_VIDEO_OPENGL_EGL
    if (window->flags & SDL_WINDOW_OPENGL && anw) {
        data->egl_surface = SDL_EGL_CreateSurface(_this, window, (NativeWindowType)data->native_window);

        if (data->egl_surface == EGL_NO_SURFACE) {
            ANativeWindow_release(data->native_window);
            SDL_free(data);
            result = false;
            goto endfunction;
        }
    }
    SDL_SetPointerProperty(SDL_GetWindowProperties(window), SDL_PROP_WINDOW_ANDROID_SURFACE_POINTER, data->egl_surface);
#endif

    SDL_SetWindowSafeAreaInsets(window, Android_SafeInsetLeft, Android_SafeInsetRight, Android_SafeInsetTop, Android_SafeInsetBottom);

    window->internal = data;
    Android_Window = window;

endfunction:

    Android_UnlockActivityMutex();

    return result;
}

void Android_SetWindowTitle(SDL_VideoDevice *_this, SDL_Window *window)
{
    if(window != Android_Window) {
        return;
    }
    Android_JNI_SetActivityTitle(window->title);
}

SDL_FullscreenResult Android_SetWindowFullscreen(SDL_VideoDevice *_this, SDL_Window *window, SDL_VideoDisplay *display, SDL_FullscreenOp fullscreen)
{
    Android_LockActivityMutex();

    if (window == Android_Window) {
        SDL_WindowData *data;
        int old_w, old_h, new_w, new_h;

        // If the window is being destroyed don't change visible state
        if (!window->is_destroying) {
            Android_JNI_SetWindowStyle(fullscreen);
        }

        /* Ensure our size matches reality after we've executed the window style change.
         *
         * It is possible that we've set width and height to the full-size display, but on
         * Samsung DeX or Chromebooks or other windowed Android environments, our window may
         * still not be the full display size.
         */
        if (!SDL_IsDeXMode() && !SDL_IsChromebook()) {
            goto endfunction;
        }

        data = window->internal;
        if (!data || !data->native_window) {
            if (data && !data->native_window) {
                SDL_SetError("Missing native window");
            }
            goto endfunction;
        }

        old_w = window->w;
        old_h = window->h;

        new_w = ANativeWindow_getWidth(data->native_window);
        new_h = ANativeWindow_getHeight(data->native_window);

        if (new_w < 0 || new_h < 0) {
            SDL_SetError("ANativeWindow_getWidth/Height() fails");
        }

        if (old_w != new_w || old_h != new_h) {
            SDL_SendWindowEvent(window, SDL_EVENT_WINDOW_RESIZED, new_w, new_h);
        }
    }

endfunction:

    Android_UnlockActivityMutex();

    return SDL_FULLSCREEN_SUCCEEDED;
}

void Android_MinimizeWindow(SDL_VideoDevice *_this, SDL_Window *window)
{
    if(window != Android_Window) {
        return;
    }
    Android_JNI_MinimizeWindow();
}

void Android_SetWindowResizable(SDL_VideoDevice *_this, SDL_Window *window, bool resizable)
{
    if(window != Android_Window) {
        return;
    }
    // Set orientation
    Android_JNI_SetOrientation(window->w, window->h, window->flags & SDL_WINDOW_RESIZABLE, SDL_GetHint(SDL_HINT_ORIENTATIONS));
}

void Android_ManageSurface(SDL_VideoDevice *_this, SDL_Window *window) {
    if(!window) {
        return;
    }
#ifdef SDL_VIDEO_OPENGL_EGL
    if(!(window->flags & SDL_WINDOW_OPENGL)) {
        SDL_Log("Sorry, we don't support managing EGL surfaces for non-OpenGL windows!");
        return;
    }
    SDL_WindowData *data = window->internal;
    if(!data) {
        return;
    }
    SDL_EGL_MakeCurrent(_this, NULL, NULL);
    // First: we destroy already existing surface
    if(data->egl_surface != EGL_NO_SURFACE) {
        SDL_EGL_DestroySurface(_this, data->egl_surface);
    }
    // Second: we decide, what surface we should create
    create_surface:
    switch(SDL_GetAtomicInt(&data->surface_mode)) {
        case SDL_ANDROID_WINDOW_OFFSCREEN:
            data->egl_surface = SDL_EGL_CreateOffscreenSurface(_this, Android_SurfaceWidth, Android_SurfaceHeight);
            if(!data->egl_surface) {
                // There's nothing we can do if even offscreen surface has failed
                SDL_Log("Unable to create an offscreen EGL surface: %s", SDL_GetError());
                return;
            }
            SDL_Log("Window %s is now offscreen!", window->title);
            break;
        case SDL_ANDROID_WINDOW_PLATFORM:
            // For platform surface we need to fetch an ANativeWindow
            // The method below will block this thread till the ANativeWindow arrives
            data->native_window = Android_JNI_GetNativeWindow();
            if(!data->native_window) {
                SDL_Log("Unable to fetch ANativeWindow, switching to offscreen");
                SDL_SetAtomicInt(&data->surface_mode, SDL_ANDROID_WINDOW_OFFSCREEN);
                goto create_surface;
            }
            SDL_SetPointerProperty(SDL_GetWindowProperties(Android_Window), SDL_PROP_WINDOW_ANDROID_WINDOW_POINTER, data->native_window);
            data->egl_surface = SDL_EGL_CreateSurface(_this, window, data->native_window);
            if(!data->egl_surface) {
                // Read the comment above
                SDL_Log("Unable to create a platform EGL surface: %s. Switching to offscreen", SDL_GetError());
                SDL_SetAtomicInt(&data->surface_mode, SDL_ANDROID_WINDOW_OFFSCREEN);
                goto create_surface;
            }
            SDL_Log("Window %s is now being rendered on-screen! Contact developers if it isn't for whatever reasons", window->title);
    }
    SDL_EGL_MakeCurrent(_this, data->egl_surface, SDL_GL_GetCurrentContext());
#endif
}

void Android_ShowWindow(SDL_VideoDevice *_this, SDL_Window *window)
{
    if(!window) {
        return; // Yes, this happens. Sometimes
    }
    SDL_WindowData *data = window->internal;
    if(!data) {
        return;
    }

    SDL_SetMouseFocus(window);
    SDL_SetKeyboardFocus(window);

    Android_Window = window;

    // See Android_GLES_SwapWindow(). This will automatically fetch ANativeWindow and create a platform surface
    SDL_SetAtomicInt(&data->surface_mode, SDL_ANDROID_WINDOW_PLATFORM);
    SDL_SetAtomicInt(&data->surface_changed, true);
}

void Android_HideWindow(SDL_VideoDevice *_this, SDL_Window *window)
{
    if(!window) {
        return;
    }
    SDL_WindowData *data = window->internal;
    if(!data) {
        return;
    }
    data->native_window = NULL;
    SDL_SetAtomicInt(&data->surface_mode, SDL_ANDROID_WINDOW_OFFSCREEN);
    SDL_SetAtomicInt(&data->surface_changed, true);
}

static void Android_DestroyOffscreenWindow(SDL_VideoDevice *_this, SDL_Window *window)
{
    if (window->internal) {
        SDL_WindowData *data = window->internal;

#ifdef SDL_VIDEO_OPENGL_EGL
        if (data->egl_surface != EGL_NO_SURFACE) {
            SDL_EGL_DestroySurface(_this, data->egl_surface);
        }
#endif
        SDL_free(window->internal);
        window->internal = NULL;
    }
}

void Android_DestroyWindow(SDL_VideoDevice *_this, SDL_Window *window)
{
    Android_LockActivityMutex();

    if (window == Android_Window) {
        Android_Window = NULL;

        if (window->internal) {
            SDL_WindowData *data = window->internal;

#ifdef SDL_VIDEO_OPENGL_EGL
            if (data->egl_surface != EGL_NO_SURFACE) {
                SDL_EGL_DestroySurface(_this, data->egl_surface);
            }
#endif

            if (data->native_window) {
                ANativeWindow_release(data->native_window);
            }
            SDL_free(window->internal);
            window->internal = NULL;
        }
    }
    else {
        Android_DestroyOffscreenWindow(_this, window);
    }

    Android_UnlockActivityMutex();
}

#endif // SDL_VIDEO_DRIVER_ANDROID
