package com.retekey;

/**
 * The action bar's own height, as a percentage of the screen's height — kept per screen
 * orientation, beside the keyboard's height and in the same unit, so the two numbers can be read
 * against each other: a keyboard at 30 with a bar at 8 leaves the keys 22.
 *
 * <p>The bar is part of the keyboard's height, not added on top of it ({@link ActionBarFrame}):
 * a taller bar takes its extra from the keys, and a shorter one gives it back. Until the setting
 * is touched the bar is the 40 dp it has always been. Android-free.
 */
final class ActionBarHeightSettings {
    static final String KEY_SCREEN_PERCENT = "bar_height_screen_percent";
    /** What an orientation nobody has set reads as: follow the height the bar always had. */
    static final int UNSET = -1;
    /** About as short as a finger can still find on a tall screen. */
    static final int MIN_PERCENT = 2;
    /** A quarter of the screen: half of the tallest keyboard the height setting allows. */
    static final int MAX_PERCENT = 25;

    /**
     * The key 0.2.2 wrote: a percentage of the 40 dp bar. Still read where the new one is unset,
     * so a bar set under the old unit stays exactly as it was set.
     */
    static final String LEGACY_KEY_PERCENT = "bar_height_percent";
    static final int LEGACY_DEFAULT_PERCENT = 100;
    private static final int LEGACY_MIN_PERCENT = 50;
    private static final int LEGACY_MAX_PERCENT = 200;

    /** How tall the strip has always been. Shorter than a key row: a shelf, not a row of keys. */
    private static final int USUAL_DP = 40;
    /** The most of a slot's face its label may fill once the bar is shorter than it was. */
    private static final float LABEL_SHARE = 0.7f;

    private ActionBarHeightSettings() {
    }

    static int clamp(int percent) {
        return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, percent));
    }

    /** The bar's height while the setting is untouched, in pixels. */
    static int usualPx(int legacyPercent, float density) {
        int percent = Math.max(LEGACY_MIN_PERCENT, Math.min(LEGACY_MAX_PERCENT, legacyPercent));
        return Math.round(USUAL_DP * density * percent / 100.0f);
    }

    /** The bar's height in pixels: that percentage of the screen it is shown on. */
    static int heightPx(int percent, int screenHeightPx) {
        if (screenHeightPx <= 0) {
            return 0;
        }
        return Math.round(screenHeightPx * clamp(percent) / 100.0f);
    }

    /** What percentage of that screen a bar of this many pixels is. */
    static int percentFor(int heightPx, int screenHeightPx) {
        if (screenHeightPx <= 0) {
            return MIN_PERCENT;
        }
        return clamp(Math.round(heightPx * 100.0f / screenHeightPx));
    }

    /**
     * The bar's share of a keyboard {@code keyboardHeightPx} tall, bar included. However tall the
     * bar is set, the keys keep half: the bar is the shelf above them, and a slider must not be
     * able to leave a keyboard with nothing to type on.
     */
    static int within(int barPx, int keyboardHeightPx) {
        return Math.max(0, Math.min(barPx, keyboardHeightPx / 2));
    }

    /**
     * The label size for a slot whose face is {@code facePx} tall. A bar at its usual height or
     * taller keeps the label it always had; a shorter one brings the label down to fit its face.
     */
    static float labelPx(int barPx, int usualBarPx, float usualLabelPx, int facePx) {
        if (barPx >= usualBarPx) {
            return usualLabelPx;
        }
        return Math.min(usualLabelPx, facePx * LABEL_SHARE);
    }

    /** Whether a changed preference is this setting, for either orientation, in either unit. */
    static boolean isKey(String changed) {
        return changed != null && (changed.startsWith(KEY_SCREEN_PERCENT + ".")
            || changed.startsWith(LEGACY_KEY_PERCENT + "."));
    }
}
