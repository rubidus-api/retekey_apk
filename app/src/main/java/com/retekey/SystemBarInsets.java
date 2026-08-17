package com.retekey;

/**
 * How much room at the bottom of the screen belongs to the system rather than to the keyboard.
 *
 * <p>Most ROMs put two buttons under an open keyboard — hide the keyboard, switch keyboard — and
 * they take the taps in that band. A keyboard that draws its bottom row there does not merely look
 * cramped: those keys cannot be pressed at all, because the system takes the touch first. That was
 * issue #1, reported on One UI: the {@code !#} and layout keys were unreachable.
 *
 * <p>The band to reserve is the <em>tappable element</em> inset, not the navigation bar's. Under
 * gesture navigation the navigation bar still reports a height for its handle, while the tappable
 * inset is zero — nothing there takes a tap, so nothing needs reserving and no height is wasted.
 * Under three-button navigation the two agree, and that is the case that was broken.
 *
 * <p>Android-free by design: this is the rule, and {@link SystemBandFrame} is the one place that
 * applies it.
 */
final class SystemBarInsets {
    /** Tappable-element insets arrived in API 29; before that, the system-window inset is all there is. */
    static final int TAPPABLE_INSETS_SDK = 29;
    /** Window insets are dispatched to views from API 20. Below it there is nothing to ask. */
    static final int ANY_INSETS_SDK = 20;

    /**
     * No more than this share of the keyboard may be given away. An inset is normally a few dozen
     * pixels; a wrong one — a full-screen inset arriving while the window is being resized — would
     * otherwise swallow the keys, and a keyboard with no keys is worse than one with a covered row.
     */
    private static final int MAX_SHARE_DENOMINATOR = 4;

    private SystemBarInsets() {
    }

    /**
     * The band to reserve.
     *
     * @param sdkInt the running platform's API level
     * @param tappableBottom the tappable-element bottom inset, or 0 where the platform has none
     * @param systemWindowBottom the system-window bottom inset
     */
    static int bandPx(int sdkInt, int tappableBottom, int systemWindowBottom) {
        if (sdkInt < ANY_INSETS_SDK) {
            return 0;
        }
        int band = sdkInt >= TAPPABLE_INSETS_SDK ? tappableBottom : systemWindowBottom;
        return Math.max(0, band);
    }

    /**
     * The band, held to a share of the space the keyboard has. A non-positive height means the view
     * has not been measured yet, and then the band is taken as given — the clamp arrives with the
     * next layout pass.
     */
    static int clampToHeight(int bandPx, int viewHeightPx) {
        int band = Math.max(0, bandPx);
        if (viewHeightPx <= 0) {
            return band;
        }
        return Math.min(band, viewHeightPx / MAX_SHARE_DENOMINATOR);
    }
}
