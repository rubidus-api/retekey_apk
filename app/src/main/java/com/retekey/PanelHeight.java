package com.retekey;

/**
 * How tall a panel over the keyboard may be (issue #8).
 *
 * <p>The notepad and the clipboard fill the window above the keys, and the window used to be
 * measured at the whole screen. An IME window is anchored to the bottom of the screen, so a window
 * as tall as the screen reaches up behind the status bar — the reporter's screenshots show the top
 * of the panel underneath it — and asking for more room than the system means to give an input
 * method is also how an app ends up hiding the keyboard the moment the panel opens.
 *
 * <p>So the panel is measured against what is actually there: the screen, less the band at the top
 * the system draws in, less whatever the keyboard already reserves at the bottom. Pure arithmetic,
 * so the rule is unit tested rather than eyeballed on one phone.
 */
final class PanelHeight {

    /**
     * The most of the screen a panel's window may take, leaving a strip of the app behind it.
     *
     * An app whose own area is squeezed to nothing by a full-height input window can decide the
     * keyboard is in the way and put it down — which is what the reporter saw in a terminal and in
     * a chat app, where the panel opened and closed again at once (issue #8). Leaving a fifth of
     * the screen to the app costs the panel a little room and keeps the app on its feet.
     */
    static final int MOST_OF_THE_SCREEN_PERCENT = 80;

    private PanelHeight() {
    }

    /**
     * The height to measure a panel frame at.
     *
     * @param screenHeight   the display's height in pixels
     * @param topInset       the system's band at the top, or a negative number when it is unknown
     * @param bottomReserved what the keyboard keeps clear at the bottom for the system's own keys
     */
    static int forPanel(int screenHeight, int topInset, int bottomReserved) {
        int height = screenHeight;
        if (topInset > 0 && topInset < screenHeight) {
            height -= topInset;
        }
        if (bottomReserved > 0) {
            height -= bottomReserved;
        }
        int most = screenHeight * MOST_OF_THE_SCREEN_PERCENT / 100;
        if (height > most) {
            height = most;
        }
        return Math.max(0, height);
    }

    /**
     * The band at the top of the screen, asked of the window first and of the platform's own
     * {@code status_bar_height} when the window cannot answer — which is every device below
     * Android 6, and any moment before the view has a window.
     */
    static int topInset(android.view.View view) {
        int fromWindow = -1;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
            fromWindow = WindowInsetsWatcher.currentTop(view);
        }
        if (fromWindow >= 0) {
            return fromWindow;
        }
        android.content.res.Resources resources = view.getResources();
        int id = resources.getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? resources.getDimensionPixelSize(id) : 0;
    }
}
