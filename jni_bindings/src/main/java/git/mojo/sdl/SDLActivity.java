package git.mojo.sdl;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Surface;

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
        // TODO
    }

    public static boolean openURL(String url) {
        // TODO
        return false;
    }

    public static String getDeviceFormFactor() {
        // TODO
        return "";
    }

    public static boolean getManifestEnvironmentVariables() {
        // TODO
        return false;
    }

    public static boolean isAndroidTV() {
        // TODO
        return false;
    }

    public static boolean isChromebook() {
        // TODO
        return false;
    }

    public static boolean isDeXMode() {
        // TODO
        return false;
    }

    public static boolean isTablet() {
        // TODO
        return false;
    }

    public static boolean sendMessage(int what, int arg) {
        // TODO
        return false;
    }

    public static void minimizeWindow() {
        // TODO
    }

    public static boolean setActivityTitle(String title) {
        // TODO
        return true;
    }

    public static void setWindowStyle(boolean fullscreen) {
        // TODO
    }

    public static boolean showTextInput(int x, int y, int w, int h, int flags) {
        // TODO
        return false;
    }

    public static boolean showToast(String message, int duration, int gravity, int xOffset, int yOffset) {
        // TODO
        return false;
    }

    public static int openFileDescriptor(String uri, String mode) {
        // TODO
        return -1;
    }

    public static boolean showFileDialog(String[] filters, boolean allowMultiple, int type, String initialPath, int requestCode) {
        // TODO
        return false;
    }

    public static String getPreferredLocales() {
        // TODO
        return "";
    }

    public static String formatLocale(Locale locale) {
        // TODO
        return locale.toString();
    }
}
