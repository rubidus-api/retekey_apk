package com.retekey;

/**
 * The gap drawn round each key, as a percentage of the 4 dp the keyboard has always drawn — kept
 * per screen orientation, because the same gap is a larger share of a short landscape row than of
 * a tall portrait one.
 *
 * <p>100 is the gap as it was, to the pixel, so nobody's keyboard changes until they move the
 * slider. The gap is drawing only: every pixel of the keyboard still belongs to a key, and a wider
 * gap makes a key's face smaller without making the key harder to hit. Android-free.
 */
final class KeyGapSettings {
    static final String KEY_PERCENT = "key_gap_percent";
    /** No gap: faces meet edge to edge. */
    static final int MIN_PERCENT = 0;
    /** Three times the usual gap. Beyond that the faces are islands. */
    static final int MAX_PERCENT = 300;
    static final int DEFAULT_PERCENT = 100;

    /** The gap the keyboard draws at 100%. */
    private static final float BASE_DP = 4.0f;

    private KeyGapSettings() {
    }

    static int clamp(int percent) {
        return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, percent));
    }

    /** The gap on each side of a key face, in pixels. */
    static int gapPx(int percent, float density) {
        return Math.round(BASE_DP * density * clamp(percent) / 100.0f);
    }

    /**
     * The gap one cell can afford. A widened gap takes at most a quarter of the cell's smaller
     * side from each edge, so the face keeps half the cell and is never turned inside out on a
     * short row. The gap the keyboard always had ({@code basePx}) and anything narrower is never
     * cut: those draw exactly what they drew before this setting existed.
     */
    static int fit(int gapPx, int basePx, int cellWidthPx, int cellHeightPx) {
        int quarter = Math.min(cellWidthPx, cellHeightPx) / 4;
        return Math.min(gapPx, Math.max(basePx, quarter));
    }
}
