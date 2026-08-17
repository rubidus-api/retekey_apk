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
     * <p>Automatic asks two questions rather than one. The tappable-element inset says how much of
     * the bottom the system takes touches in, and the navigation bar's says how much furniture is
     * actually down there; the band is the smaller of the two, and nothing at all where the
     * navigation bar is not showing. A phone that draws no keyboard buttons over the IME — several
     * Samsung ROMs let you turn them off — therefore gives up no keyboard height, while the
     * three-button device from issue #1 still gets its band.
     *
     * @param sdkInt the running platform's API level
     * @param tappableBottom the tappable-element bottom inset, or 0 where the platform has none
     * @param navigationBottom the navigation-bar bottom inset
     * @param navigationVisible whether the navigation bar is showing at all
     * @param systemWindowBottom the system-window bottom inset, the only answer below API 29
     */
    static int bandPx(int sdkInt, int tappableBottom, int navigationBottom,
            boolean navigationVisible, int systemWindowBottom, Mode mode) {
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
        return Math.max(0, Math.min(tappableBottom, navigationBottom));
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
