package com.retekey;

/**
 * The action bar's own height, as a percentage of the 40 dp it has always been — kept per screen
 * orientation, beside the keyboard's height.
 *
 * <p>The bar is part of the keyboard's height, not added on top of it ({@link ActionBarFrame}):
 * a taller bar takes its extra from the keys, and a shorter one gives it back. 100 is the bar as
 * it was, to the pixel. Android-free.
 */
final class ActionBarHeightSettings {
    static final String KEY_PERCENT = "bar_height_percent";
    /** Half height: about as short as a finger can still find. */
    static final int MIN_PERCENT = 50;
    /** Double: taller than a key row on most keyboards. */
    static final int MAX_PERCENT = 200;
    static final int DEFAULT_PERCENT = 100;

    /** How tall the strip is at 100%. Shorter than a key row: a shelf, not another row of keys. */
    private static final int BASE_DP = 40;
    /** The most of a slot's face its label may fill once the bar is shorter than it was. */
    private static final float LABEL_SHARE = 0.7f;

    private ActionBarHeightSettings() {
    }

    static int clamp(int percent) {
        return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, percent));
    }

    /** The bar's height in pixels. */
    static int heightPx(int percent, float density) {
        return Math.round(BASE_DP * density * clamp(percent) / 100.0f);
    }

    /**
     * The label size for a slot whose face is {@code facePx} tall. A bar at its usual height or
     * taller keeps the label it always had; a shorter one brings the label down to fit its face.
     */
    static float labelPx(int percent, float usualPx, int facePx) {
        if (clamp(percent) >= DEFAULT_PERCENT) {
            return usualPx;
        }
        return Math.min(usualPx, facePx * LABEL_SHARE);
    }

    /** Whether a changed preference is this setting, for either orientation. */
    static boolean isKey(String changed) {
        return changed != null && changed.startsWith(KEY_PERCENT + ".");
    }
}
