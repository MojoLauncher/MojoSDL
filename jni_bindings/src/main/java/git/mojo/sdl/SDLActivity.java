package git.mojo.sdl;

import static android.content.Context.UI_MODE_SERVICE;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SDLActivity {

    private static List<GrabListener> grabListeners = new ArrayList<>();
    private static Map<Integer, SDLCursor> customCursors = new HashMap<>();
    private static SDLCursor.CursorChangeCallback cursorCallback;
    private static SDLKeyboardCaller keyboardCaller;
    private static int lastCursorId = 0;
    private static Runnable initCallback;

    protected static Surface mSurface;
    protected static Activity mContext;

    private static SDLClipboard mClipboard;

    public static void initialize() {
        mSurface = null;
    }

    public static void setClipboard(SDLClipboard clipboard){
        mClipboard = clipboard;
    }
    public static void setCursorCallback(SDLCursor.CursorChangeCallback callback){
        cursorCallback = callback;
    }
    public static void setKeyboardCaller(SDLKeyboardCaller caller){
        keyboardCaller = caller;
    }

    public static void setNativeSurface(Surface surface){
        SDLActivity.mSurface = surface;
    }
    public static void setInitCallback(Runnable callback){
        initCallback = callback;
    }

    // Native method declarations
    public static native String nativeGetVersion();
    public static native void nativeSetupJNI();
    public static native int nativeGetCompiledSubsystems();
    public static native boolean nativeIsHIDAPIEnabled();
    public static native void nativeInitMainThread();
    public static native void nativeCleanupMainThread();
    public static native int nativeRunMain(String library, String function, Object arguments);
    public static native void nativeLowMemory();
    public static native void nativeSendQuit();
    public static native void nativeQuit();
    public static native void nativePause();
    public static native void nativeResume();
    public static native void nativeFocusChanged(boolean hasFocus);
    public static native void onNativeDropFile(String filename);
    public static native void nativeSetScreenResolution(int surfaceWidth, int surfaceHeight, int deviceWidth, int deviceHeight, float density, float rate);
    public static native void onNativeResize();
    public static native void onNativeKeyDown(int keycode);
    public static native void onNativeKeyUp(int keycode);
    public static native boolean onNativeSoftReturnKey();
    public static native void onNativeKeyboardFocusLost();
    public static native void onNativeMouse(int button, int action, float x, float y, boolean relative);
    public static native void onNativeMouseButton(int button, int action, float x, float y, boolean relative);
    public static native void onNativeTouch(int touchDevId, int pointerFingerId,
                                            int action, float x,
                                            float y, float p);
    public static native void onNativePen(int penId, int device_type, int button, int action, float x, float y, float p);
    public static native void onNativeClipboardChanged();
    public static native void onNativeSurfaceCreated();
    public static native void onNativeSurfaceChanged();
    public static native void onNativeSurfaceDestroyed();
    public static native void onNativeScreenKeyboardShown();
    public static native void onNativeScreenKeyboardHidden();
    public static native String nativeGetHint(String name);
    public static native boolean nativeGetHintBoolean(String name, boolean default_value);
    public static native void nativeSetenv(String name, String value);
    public static native void nativeSetNaturalOrientation(int orientation);
    public static native void onNativeRotationChanged(int rotation);
    public static native void onNativeInsetsChanged(int left, int right, int top, int bottom);
    public static native void nativeAddTouch(int touchId, String name);
    public static native void nativePermissionResult(int requestCode, boolean result);
    public static native void onNativeLocaleChanged();
    public static native void onNativeDarkModeChanged(boolean enabled);
    public static native boolean nativeAllowRecreateActivity();
    public static native int nativeCheckSDLThreadCounter();
    public static native void onNativeFileDialog(int requestCode, String[] filelist, int filter);
    public static native void onNativePinchStart(float span_x, float span_y, float focus_x, float focus_y);
    public static native void onNativePinchUpdate(float scale, float span_x, float span_y, float focus_x, float focus_y);
    public static native void onNativePinchEnd();
    public static void onSDLInit(){
        Log.i("SDLInit", "SDL init called!");
        if(initCallback != null) initCallback.run();
    }

    public static void addGrabListener(GrabListener grabListener){
        SDLActivity.grabListeners.add(grabListener);
    }

    public static Surface getNativeSurface() {
        return mSurface;
    }

    public static Activity getContext() {
        return SDL.getContext();
    }

    public static void manualBackButton() {
        // Unsupported
    }

    public static void setOrientation(int w, int h, boolean resizable, String hint) {
        // Unsupported
    }

    public static boolean shouldMinimizeOnFocusLoss() {
        return false;
    }

    public static boolean supportsRelativeMouse() {
        return true;
    }

    public static boolean setRelativeMouseEnabled(boolean enabled) {
        for(GrabListener grabListener : grabListeners){
            grabListener.onGrabState(enabled);
        }
        return true;
    }

    public static void initTouch() {
        // TODO
    }

    public static boolean clipboardHasText() {
        return mClipboard != null && mClipboard.getClipboardString() != null;
    }

    public static String clipboardGetText() {
        return mClipboard != null ? mClipboard.getClipboardString() : "";
    }

    public static void clipboardSetText(String string) {
        if(mClipboard != null) mClipboard.setClipboardString(string);
    }

    public static int createCustomCursor(int[] colors, int width, int height, int hotSpotX, int hotSpotY) {
        Bitmap bitmap = Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888);
        SDLCursor cursor = new SDLCursor(width, height, hotSpotX, hotSpotY, bitmap);
        lastCursorId++;
        customCursors.put(lastCursorId, cursor);
        return lastCursorId;
    }

    public static void destroyCustomCursor(int cursorID) {
        customCursors.remove(cursorID);
    }

    public static boolean setCustomCursor(int cursorID) {
        if(!customCursors.containsKey(cursorID)) return false;
        cursorCallback.onCursorChange(customCursors.get(cursorID));
        return true;
    }

    public static boolean setSystemCursor(int cursorID) {
        // TODO: implement system cursors (point,loading, etc) on Mojo side
        cursorCallback.onCursorChange(null);
        return true;
    }

    public static void requestPermission(String permission, int requestCode) {
        // TODO: maybe implement?
    }

    public static boolean openURL(String url)
    {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));

            int flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                | Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
            i.addFlags(flags);

            mContext.startActivity(i);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    static String getDeviceFormFactor()
    {
        // TODO: WearOS
        if (isAndroidTV()) {
            return "tv";
        } else if (isVRHeadset()) {
            return "headset";
        } else if (isTablet()) {
            return "tablet";
            //} else if (isAndroidAutomotive()) {
            //    return "car";
        } else {
            return "phone";
        }
    }

    public static boolean getManifestEnvironmentVariables() {
        try {
            if (getContext() == null) {
                return false;
            }

            ApplicationInfo applicationInfo = getContext().getPackageManager().getApplicationInfo(getContext().getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = applicationInfo.metaData;
            if (bundle == null) {
                return false;
            }
            String prefix = "SDL_ENV.";
            final int trimLength = prefix.length();
            for (String key : bundle.keySet()) {
                if (key.startsWith(prefix)) {
                    String name = key.substring(trimLength);
                    String value = bundle.get(key).toString();
                    nativeSetenv(name, value);
                }
            }
            /* environment variables set! */
            return true;
        } catch (Exception e) {
            Log.v("SDL", "Manifest env exception " + e.toString());
        }
        return false;
    }
    public static boolean isAndroidTV() {
        UiModeManager uiModeManager = (UiModeManager) getContext().getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            return true;
        }
        if (Build.MANUFACTURER.equals("MINIX") && Build.MODEL.equals("NEO-U1")) {
            return true;
        }
        if (Build.MANUFACTURER.equals("Amlogic") &&
            (Build.MODEL.startsWith("TV") ||
                Build.MODEL.equals("X96-W") ||
                Build.MODEL.equals("A95X-R1"))) {
            return true;
        }
        return false;
    }

    public static boolean isVRHeadset() {
        if (Build.MANUFACTURER.equals("Oculus") && Build.MODEL.startsWith("Quest")) {
            return true;
        }
        if (Build.MANUFACTURER.equals("Pico")) {
            return true;
        }
        return false;
    }

    public static boolean isChromebook() {
        // https://stackoverflow.com/questions/39784415/how-to-detect-programmatically-if-android-app-is-running-in-chrome-book-or-in
        if (getContext() != null) {
            if (getContext().getPackageManager().hasSystemFeature("org.chromium.arc")
                || getContext().getPackageManager().hasSystemFeature("org.chromium.arc.device_management")) {
                return true;
            }
        }

        // Running on AVD emulator
        return (Build.MODEL != null && Build.MODEL.startsWith("sdk_gpc_"));
    }
    public static boolean isDeXMode() {
        if (Build.VERSION.SDK_INT < 24 /* Android 7.0 (N) */) {
            return false;
        }
        try {
            final Configuration config = getContext().getResources().getConfiguration();
            final Class<?> configClass = config.getClass();
            return configClass.getField("SEM_DESKTOP_MODE_ENABLED").getInt(configClass)
                == configClass.getField("semDesktopModeEnabled").getInt(config);
        } catch(Exception ignored) {
            return false;
        }
    }
    public static double getDiagonal()
    {
        DisplayMetrics metrics = new DisplayMetrics();
        Activity activity = getContext();
        if (activity == null) {
            return 0.0;
        }
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        double dWidthInches = metrics.widthPixels / (double)metrics.xdpi;
        double dHeightInches = metrics.heightPixels / (double)metrics.ydpi;

        return Math.sqrt((dWidthInches * dWidthInches) + (dHeightInches * dHeightInches));
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static boolean isTablet() {
        // If our diagonal size is seven inches or greater, we consider ourselves a tablet.
        return (getDiagonal() >= 7.0);
    }
    public static boolean sendMessage(int what, int arg) {
        return false;
    }
    public static void minimizeWindow() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(startMain);
    }

    public static boolean setActivityTitle(String title) {
        return true;
    }

    public static void setWindowStyle(boolean fullscreen) {
    }

    public static boolean showTextInput(int x, int y, int w, int h, int flags) {
        keyboardCaller.acceptKeyboard(x, y);
        return true;
    }

    public static boolean showToast(String message, int duration, int gravity, int xOffset, int yOffset)
    {
        try
        {
            class OneShotTask implements Runnable {
                private final String mMessage;
                private final int mDuration;
                private final int mGravity;
                private final int mXOffset;
                private final int mYOffset;

                OneShotTask(String message, int duration, int gravity, int xOffset, int yOffset) {
                    mMessage  = message;
                    mDuration = duration;
                    mGravity  = gravity;
                    mXOffset  = xOffset;
                    mYOffset  = yOffset;
                }

                public void run() {
                    try
                    {
                        Toast toast = Toast.makeText(mContext, mMessage, mDuration);
                        if (mGravity >= 0) {
                            toast.setGravity(mGravity, mXOffset, mYOffset);
                        }
                        toast.show();
                    } catch(Exception ex) {
                        Log.e("SDL", "Failed to spawn toast: " + ex.getMessage());
                    }
                }
            }
            mContext.runOnUiThread(new OneShotTask(message, duration, gravity, xOffset, yOffset));
        } catch(Exception ex) {
            return false;
        }
        return true;
    }

    public static int openFileDescriptor(String uri, String mode) {
        try(ParcelFileDescriptor fileDescriptor = mContext.getContentResolver().openFileDescriptor(Uri.parse(uri), mode);) {
            if(fileDescriptor == null) return -1;
            return fileDescriptor.detachFd();
        } catch (IOException e) {
            Log.e("SDL", "Unable to open FD at " + uri);
            return -1;
        }
    }

    public static boolean showFileDialog(String[] filters, boolean allowMultiple, int type, String initialPath, int requestCode) {
        // Unsupported
        return false;
    }

    public static String getPreferredLocales() {
        StringBuilder result = new StringBuilder();
        if (Build.VERSION.SDK_INT >= 24 /* Android 7 (N) */) {
            LocaleList locales = LocaleList.getAdjustedDefault();
            for (int i = 0; i < locales.size(); i++) {
                if (i != 0) result.append(",");
                result.append(formatLocale(locales.get(i)));
            }
        }
        return result.toString();
    }

    public static String formatLocale(Locale locale) {
        String result = "";
        String lang = "";
        if (locale.getLanguage().equals("in")) {
            // Indonesian is "id" according to ISO 639.2, but on Android is "in" because of Java backwards compatibility
            lang = "id";
        } else if (locale.getLanguage().isEmpty()) {
            // Make sure language is never empty
            lang = "und";
        } else {
            lang = locale.getLanguage();
        }

        if (locale.getCountry() == "") {
            result = lang;
        } else {
            result = lang + "_" + locale.getCountry();
        }
        return result;
    }
}
