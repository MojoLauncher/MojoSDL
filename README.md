# MojoSDL

MojoSDL is a fork of SDL3 adapted for running desktop applications on Android. Unlike the upstream SDL it uses custom JNI bindings derived from `android-project` example app adding stuff required for PC apps such as mouse cursor warp callbacks and easier mouse click handling.

Changes compared to upstream:
* (Almost) proper implementation of offscreen multi-windowing enough to run Minecraft 26.3+ on OpenGL.
* MojoExec integration including forced GLES contexts (needed by LTW) and Vulkan/EGL handle discovery through the library.
* Launcher callbacks required for the platform/gamepad routing.
* Random compatibility patches with Minecraft mods such as Controlify or lwjgl3ify (GT:NH).

### why not upstream

Two reasons: 
* Hacks are non-upstreamable, but they're required sometimes.
* bytehook/shadowhook is fragile. Nah, we're not doing this, tomi.

### TODO:
- [ ] Real multi-windowing. Current one works only for offscreen windows, only a single window can own ANativeWindow for obvious reasons.
- [ ] Drop OpenGL ES context hacks
- [ ] Add support for OpenGL/ANGLE in UIKit backend.
- [ ] In general research what's required for the iOS port (mostly input).

# SDL
Simple DirectMedia Layer (SDL for short) is a cross-platform library
designed to make it easy to write multi-media software, such as games
and emulators.

You can find the latest release and additional information at:
https://www.libsdl.org/

Installation instructions and a quick introduction is available in
[INSTALL.md](INSTALL.md)

This library is distributed under the terms of the zlib license,
available in [LICENSE.txt](LICENSE.txt).

Enjoy!

Sam Lantinga (slouken@libsdl.org)
