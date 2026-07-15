package dev.hellgates.retekeyime;

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
    /** Shortest the keyboard may be shrunk to, as a fraction of its base height. */
    static final float MIN_SCALE = 0.65f;
    /** Tallest the keyboard may be grown to, as a fraction of its base height. */
    static final float MAX_SCALE = 1.75f;
    /** Scale used before the user has ever adjusted the height. */
    static final float DEFAULT_SCALE = 1.0f;

    private KeyboardHeightScale() {
    }

    /** The stored or requested scale, forced into the supported range (NaN falls back to default). */
    static float clamp(float scale) {
        if (Float.isNaN(scale)) {
            return DEFAULT_SCALE;
        }
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
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
