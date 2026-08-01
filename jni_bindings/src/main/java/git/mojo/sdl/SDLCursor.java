package git.mojo.sdl;

import android.graphics.Bitmap;

public class SDLCursor {
    public interface CursorChangeCallback {
        void onCursorChange(SDLCursor cursor);
    }
    private final int width;
    private final int height;
    private final int xhot;
    private final int yhot;
    private final Bitmap bitmap;


    public SDLCursor(int width, int height, int xhot, int yhot, Bitmap bitmap) {
        this.width = width;
        this.height = height;
        this.xhot = xhot;
        this.yhot = yhot;
        this.bitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public int getYhot() {
        return yhot;
    }

    public int getXhot() {
        return xhot;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }
}
