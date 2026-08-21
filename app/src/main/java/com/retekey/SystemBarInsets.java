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

    /** Passed as {@code liftPx} when the window has not been laid out and cannot be measured. */
    static final int LIFT_UNKNOWN = -1;

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
     * How far the system's bottom furniture reaches up from the physical bottom of the screen —
     * the height the keyboard's keys have to clear.
     *
     * <p>Two insets describe it and neither is enough on its own. In gesture navigation on a Galaxy
     * A56 the tappable-element inset is 42px (the gesture bar's own box) and the navigation-bar inset
     * is 135px (the whole zone the hide-keyboard and switch-keyboard buttons live in); on a phone
     * with those buttons on but gesture bar hidden the two can come the other way round. The
     * furniture is whichever reaches higher. This is the number the reporter of issue #1 called A,
     * and it is what the Always mode has reserved all along — which is why Always was right on every
     * phone and setting he tried.
     */
    static int furniturePx(int tappableBottom, int navigationBottom) {
        return Math.max(0, Math.max(tappableBottom, navigationBottom));
    }

    /**
     * The band to reserve under the keys.
     *
     * <p>Automatic is one subtraction: the furniture's height, minus how far the framework has already
     * lifted this window off the physical bottom. On a phone where the window sits at the very bottom
     * the lift is zero and the whole furniture height is ours to reserve; on a phone where the
     * framework has already placed the window above the bar the lift eats some or all of it and there
     * is little or nothing left to add. The reporter of issue #1 wrote the rule down as
     * {@code A - B} after reading the numbers off the settings screen of two phones, and he was
     * right: the earlier versions measured the lift and then used it as a cap on the navigation bar
     * alone, which changes nothing on a phone whose window is at the bottom — exactly the phones that
     * were reporting the bug.
     *
     * @param sdkInt the running platform's API level
     * @param tappableBottom the tappable-element bottom inset, or 0 where the platform has none
     * @param navigationBottom the navigation-bar bottom inset
     * @param navigationVisible whether the navigation bar is showing at all
     * @param systemWindowBottom the system-window bottom inset, the only answer below API 29
     * @param liftPx how far this window's bottom edge sits above the physical bottom of the screen,
     *     or {@link #LIFT_UNKNOWN} before it has been laid out and can be measured
     */
    static int bandPx(int sdkInt, int tappableBottom, int navigationBottom,
            boolean navigationVisible, int systemWindowBottom, int liftPx, Mode mode) {
        if (sdkInt < ANY_INSETS_SDK || mode == Mode.NEVER) {
            return 0;
        }
        if (sdkInt < TAPPABLE_INSETS_SDK) {
            // Before tappable-element insets there is one number and no way to tell what is in it.
            return Math.max(0, systemWindowBottom);
        }
        int furniture = furniturePx(tappableBottom, navigationBottom);
        if (mode == Mode.ALWAYS) {
            return furniture;
        }
        if (!navigationVisible && tappableBottom <= 0) {
            // Nothing is showing down there and nothing takes a tap: there is nothing to clear.
            return 0;
        }
        if (liftPx == LIFT_UNKNOWN) {
            // The window has no geometry yet. Reserve the whole furniture for now — the same answer
            // as Always — and the frame re-asks once it has been laid out.
            return furniture;
        }
        return Math.max(0, furniture - Math.max(0, liftPx));
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
