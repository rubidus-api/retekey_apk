package com.retekey;

/**
 * Pure arithmetic for the adjustable keyboard height.
 *
 * <p>The keyboard's height is {@code baseHeight * scale}, where the base is a per-row density value
 * times the row count and the scale is a user preference clamped to a comfortable range. Isolated
 * from Android so the measure and drag-resize math is unit-tested on the JVM.
 */
final class KeyboardHeightScale {
    /** Default nominal height of one key row, in density-independent pixels. */
    static final float BASE_ROW_DP = 58.0f;
    /**
     * The height is set as a level from 1 to 50, and each level is 4% of the keyboard's base
     * height — so level 25 is the size it has always been, 50 is twice that, and 1 is a sliver.
     * A number the user can name beats a percentage of something they cannot see.
     */
    static final int MIN_LEVEL = 1;
    static final int MAX_LEVEL = 50;
    static final int DEFAULT_LEVEL = 25;
    static final float SCALE_PER_LEVEL = 0.04f;

    /** Shortest the keyboard may be shrunk to, as a fraction of its base height. */
    static final float MIN_SCALE = MIN_LEVEL * SCALE_PER_LEVEL;
    /** Tallest the keyboard may be grown to, as a fraction of its base height. */
    static final float MAX_SCALE = MAX_LEVEL * SCALE_PER_LEVEL;
    /** Scale used before the user has ever adjusted the height, on a screen of unknown size. */
    static final float DEFAULT_SCALE = DEFAULT_LEVEL * SCALE_PER_LEVEL;

    /**
     * How much of the screen the keyboard takes before anyone adjusts it: a quarter of the display's
     * long edge. The long edge in both orientations on purpose — a quarter of the short edge would
     * make the landscape keyboard a strip, and the keyboard wants to be about the same size in the
     * hand whichever way the phone is held.
     */
    static final float DEFAULT_SCREEN_FRACTION = 0.25f;

    private KeyboardHeightScale() {
    }

    /** The stored or requested scale, forced into the supported range (NaN falls back to default). */
    static float clamp(float scale) {
        if (Float.isNaN(scale)) {
            return DEFAULT_SCALE;
        }
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    /** A level forced into 1-50. */
    static int clampLevel(int level) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
    }

    /** The scale a height level means. */
    static float scaleForLevel(int level) {
        return clampLevel(level) * SCALE_PER_LEVEL;
    }

    /** The level a scale amounts to, for showing a stored setting on the slider. */
    static int levelForScale(float scale) {
        if (Float.isNaN(scale)) {
            return DEFAULT_LEVEL;
        }
        return clampLevel(Math.round(scale / SCALE_PER_LEVEL));
    }

    /**
     * The scale to start at on a screen whose long edge is {@code longestScreenPx}: whatever makes
     * the keyboard {@link #DEFAULT_SCREEN_FRACTION} of it. Falls back to {@link #DEFAULT_SCALE}
     * when the screen size is not known yet.
     */
    static float defaultScaleForScreen(int baseHeightPx, int longestScreenPx) {
        if (baseHeightPx <= 0 || longestScreenPx <= 0) {
            return DEFAULT_SCALE;
        }
        return clamp(DEFAULT_SCREEN_FRACTION * longestScreenPx / baseHeightPx);
    }

    /** The base (scale-1.0) keyboard height in pixels for the given rows and display density. */
    static int baseHeightPx(int rows, float density) {
        int safeRows = Math.max(1, rows);
        return Math.round(BASE_ROW_DP * density) * safeRows;
    }

    /** The measured keyboard height in pixels for a scale, clamped first. */
    static int heightForScale(float scale, int baseHeightPx) {
        return Math.round(baseHeightPx * clamp(scale));
    }

    /** The scale that yields the given pixel height against a base, clamped to the range. */
    static float scaleForHeight(int heightPx, int baseHeightPx) {
        if (baseHeightPx <= 0) {
            return DEFAULT_SCALE;
        }
        return clamp((float) heightPx / (float) baseHeightPx);
    }
}
