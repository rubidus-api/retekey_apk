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

    /** Passed as {@code overlapPx} when the window has not been laid out and cannot be measured. */
    static final int OVERLAP_UNKNOWN = -1;

    private SystemBarInsets() {
    }

    /** What the user asked for: work it out, always keep a band, or never keep one. */
    enum Mode {
        AUTOMATIC,
        ALWAYS,
        NEVER;

        static final String PREF_KEY = "system_band_mode";

        String stored() {
            return name().toLowerCase(java.util.Locale.US);
        }

        static Mode parse(String value) {
            for (Mode mode : values()) {
                if (mode.stored().equals(value)) {
                    return mode;
                }
            }
            return AUTOMATIC;
        }
    }

    /**
     * The band to reserve.
     *
     * <p>Automatic asks three questions, and the third is the one that matters. The insets say how
     * much of the bottom the system takes touches in and how tall its furniture is — but they say
     * that about the screen, not about this window. Whether any of it is over <em>us</em> is a
     * matter of geometry: this window's own bottom against the top of the navigation bar.
     *
     * <p>That is what separates the two phones this has been wrong on. On the reported device the
     * IME window reaches the physical bottom of the screen and the system's keyboard buttons sit on
     * our keys, so the overlap is the bar's height. On a phone where the framework has already put
     * the window above the bar, the overlap is zero, and reserving anything there gives up a strip
     * of keyboard for furniture that is not over it. Read from the insets alone the two look
     * identical; measured, they do not.
     *
     * @param sdkInt the running platform's API level
     * @param tappableBottom the tappable-element bottom inset, or 0 where the platform has none
     * @param navigationBottom the navigation-bar bottom inset
     * @param navigationVisible whether the navigation bar is showing at all
     * @param systemWindowBottom the system-window bottom inset, the only answer below API 29
     * @param overlapPx how far this window reaches into the navigation bar, or
     *     {@link #OVERLAP_UNKNOWN} before it has been laid out and can be measured
     */
    static int bandPx(int sdkInt, int tappableBottom, int navigationBottom,
            boolean navigationVisible, int systemWindowBottom, int overlapPx, Mode mode) {
        if (sdkInt < ANY_INSETS_SDK || mode == Mode.NEVER) {
            return 0;
        }
        if (sdkInt < TAPPABLE_INSETS_SDK) {
            // Before tappable-element insets there is one number and no way to tell what is in it.
            return Math.max(0, systemWindowBottom);
        }
        if (mode == Mode.ALWAYS) {
            return Math.max(0, Math.max(tappableBottom, navigationBottom));
        }
        if (!navigationVisible) {
            return 0;
        }
        int band = Math.min(tappableBottom, navigationBottom);
        if (overlapPx != OVERLAP_UNKNOWN) {
            // Only the part actually over this window is ours to give up.
            band = Math.min(band, overlapPx);
        }
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
