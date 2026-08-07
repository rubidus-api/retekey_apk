package com.retekey;

/**
 * The keyboard height, as a percentage of the screen's height.
 *
 * <p>A percentage is the one unit the user can see the meaning of: 25 means the keyboard covers a
 * quarter of the screen, and the number on the slider is the number on the screen. What came
 * before was a level from 1 to 50 standing for a multiple of a nominal row height in dp — a unit
 * that means nothing without knowing the row height, and that describes a different fraction of
 * the screen on every device.
 *
 * <p>Percentages are of the height of the screen <em>in the orientation being set</em>, which is
 * why the same keyboard is a different percentage upright and sideways. Android-free, so the
 * arithmetic is unit-tested on the JVM.
 */
final class KeyboardHeightPercent {
    /** Smallest keyboard the setting allows: a sliver, but never nothing. */
    static final int MIN_PERCENT = 1;
    /** Largest: half the screen. Beyond that the keyboard is the app. */
    static final int MAX_PERCENT = 50;
    /**
     * The share of the screen a keyboard takes before anyone sets it — a quarter of the display's
     * <em>long</em> edge, in both orientations. The long edge on purpose: a quarter of the short
     * edge would make the landscape keyboard a strip, and the keyboard wants to be about the same
     * size in the hand whichever way the device is held. Upright that is a quarter of the screen;
     * sideways it works out taller in proportion, which is what {@link #defaultPercent} computes.
     */
    static final float DEFAULT_LONG_EDGE_FRACTION = 0.25f;
    /** What one press of the menu's size− / size+ keys moves, in percentage points. */
    static final int STEP_PERCENT = 2;

    /** The nominal row height the old level-based setting was built on; kept for migration only. */
    private static final float LEGACY_BASE_ROW_DP = 58.0f;
    /** Every page of the keyboard is four rows; the old height was rows × row height × scale. */
    private static final int LEGACY_ROWS = 4;

    private KeyboardHeightPercent() {
    }

    /** A percentage forced into the supported range. */
    static int clamp(int percent) {
        return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, percent));
    }

    /** The keyboard's height in pixels: that percentage of the screen it is shown on. */
    static int heightPx(int percent, int screenHeightPx) {
        if (screenHeightPx <= 0) {
            return 0;
        }
        return Math.round(screenHeightPx * clamp(percent) / 100.0f);
    }

    /** What percentage of that screen a keyboard of this many pixels is. */
    static int percentForHeight(int heightPx, int screenHeightPx) {
        if (screenHeightPx <= 0) {
            return 25;
        }
        return clamp(Math.round(heightPx * 100.0f / screenHeightPx));
    }

    /**
     * The percentage to start at when an orientation has never been set: whatever makes the
     * keyboard a quarter of the display's long edge. Upright that is 25; sideways, where the
     * screen is shorter than its long edge, it is proportionally more.
     *
     * @param screenHeightPx the height of the screen in the orientation being set
     * @param longestScreenPx the display's long edge, the same in both orientations
     */
    static int defaultPercent(int screenHeightPx, int longestScreenPx) {
        if (screenHeightPx <= 0 || longestScreenPx <= 0) {
            return 25;
        }
        return clamp(Math.round(
            DEFAULT_LONG_EDGE_FRACTION * longestScreenPx * 100.0f / screenHeightPx));
    }

    /**
     * The percentage a keyboard set under the old scale-of-a-nominal-row-height setting was
     * actually occupying, so an existing user's keyboard keeps the size it had when the unit
     * changed under it. Returns the same number the screen was already showing them.
     */
    static int fromLegacyScale(float scale, float density, int screenHeightPx) {
        if (Float.isNaN(scale) || density <= 0.0f || screenHeightPx <= 0) {
            return 25;
        }
        int baseHeightPx = Math.round(LEGACY_BASE_ROW_DP * density) * LEGACY_ROWS;
        return percentForHeight(Math.round(baseHeightPx * scale), screenHeightPx);
    }
}
