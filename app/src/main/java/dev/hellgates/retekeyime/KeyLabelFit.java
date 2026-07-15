package dev.hellgates.retekeyime;

/**
 * Pure geometry for auto-fitting a key label inside its cell.
 *
 * <p>The renderer measures a label at the height cap and asks for the largest text size that still
 * fits the usable width, never larger than the cap and never smaller than a legibility floor. Kept
 * free of Android types so the arithmetic is unit-tested on the JVM.
 */
final class KeyLabelFit {
    /** Fraction of a cell's height used as the upper bound on the label's text size. */
    static final float HEIGHT_RATIO = 0.40f;
    /** Fraction of a cell's width the label is allowed to occupy before it shrinks. */
    static final float WIDTH_RATIO = 0.86f;

    private KeyLabelFit() {
    }

    /**
     * The largest text size that fits the cell.
     *
     * @param measuredWidth width the label paints at {@code capSize} (as reported by the text engine)
     * @param capSize       the height-derived upper bound on text size
     * @param allowedWidth  the usable width the label must fit within
     * @param minSize       the legibility floor
     * @return a size in the same unit as {@code capSize}, clamped to {@code [minSize, capSize]}
     */
    static float fitSize(float measuredWidth, float capSize, float allowedWidth, float minSize) {
        float size = capSize;
        if (measuredWidth > allowedWidth && measuredWidth > 0.0f) {
            // The label is too wide at the cap, so scale the size down in proportion to the overflow.
            size = capSize * allowedWidth / measuredWidth;
        }
        return Math.max(minSize, size);
    }
}
