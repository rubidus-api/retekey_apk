package com.retekey;

/**
 * Where the floating keyboard is allowed to be, and how big it is allowed to get.
 *
 * <p>The rules come from the product decision that a floating keyboard must never cover more than
 * its own half of the screen, so the other half stays usable for the app being typed into:
 *
 * <ul>
 *   <li>the panel's width never exceeds half the screen width;</li>
 *   <li>the panel stays inside the half it currently occupies — dragging cannot carry it across
 *       the middle;</li>
 *   <li>crossing sides is an explicit act: {@link #mirrored()} reflects the panel about the
 *       screen's vertical centre line, which is what the {@code <} / {@code >} key does;</li>
 *   <li>height is bounded so the panel cannot become unusably small or taller than the screen.</li>
 * </ul>
 *
 * <p>Android-free so the geometry is unit-tested directly; the view supplies screen size and touch
 * deltas and asks for a new instance.
 */
public final class FloatingKeyboardBounds {
    /** Which half of the screen the panel lives in. */
    public enum Side {
        LEFT,
        RIGHT
    }

    /** Smallest usable panel, as a fraction of the half it lives in. */
    public static final float MIN_WIDTH_FRACTION = 0.45f;
    /** Smallest usable panel height, as a fraction of the screen height. */
    public static final float MIN_HEIGHT_FRACTION = 0.18f;
    /** Tallest the panel may get, as a fraction of the screen height. */
    public static final float MAX_HEIGHT_FRACTION = 0.75f;

    public static final float DEFAULT_WIDTH_FRACTION = 0.92f;
    public static final float DEFAULT_HEIGHT_FRACTION = 0.42f;

    private final int screenWidth;
    private final int screenHeight;
    private final Side side;
    private final int left;
    private final int top;
    private final int width;
    private final int height;

    private FloatingKeyboardBounds(
        int screenWidth,
        int screenHeight,
        Side side,
        int left,
        int top,
        int width,
        int height
    ) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.side = side;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
    }

    /** A panel of the given size placed at the given corner, corrected to obey every rule. */
    public static FloatingKeyboardBounds of(
        int screenWidth,
        int screenHeight,
        Side side,
        int left,
        int top,
        int width,
        int height
    ) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            throw new IllegalArgumentException("screen must have a positive size");
        }
        if (side == null) {
            throw new IllegalArgumentException("side must not be null");
        }
        int half = halfWidth(screenWidth);
        int clampedWidth = clamp(width, Math.max(1, Math.round(half * MIN_WIDTH_FRACTION)), half);
        int clampedHeight = clamp(
            height,
            Math.max(1, Math.round(screenHeight * MIN_HEIGHT_FRACTION)),
            Math.max(1, Math.round(screenHeight * MAX_HEIGHT_FRACTION))
        );
        int minLeft = side == Side.LEFT ? 0 : half;
        int maxLeft = (side == Side.LEFT ? half : screenWidth) - clampedWidth;
        return new FloatingKeyboardBounds(
            screenWidth,
            screenHeight,
            side,
            clamp(left, minLeft, Math.max(minLeft, maxLeft)),
            clamp(top, 0, Math.max(0, screenHeight - clampedHeight)),
            clampedWidth,
            clampedHeight
        );
    }

    /** The panel a first-time user gets: bottom of the screen, on the given side. */
    public static FloatingKeyboardBounds initial(int screenWidth, int screenHeight, Side side) {
        int width = Math.round(halfWidth(screenWidth) * DEFAULT_WIDTH_FRACTION);
        int height = Math.round(screenHeight * DEFAULT_HEIGHT_FRACTION);
        int left = side == Side.LEFT
            ? (halfWidth(screenWidth) - width) / 2
            : halfWidth(screenWidth) + (halfWidth(screenWidth) - width) / 2;
        return of(screenWidth, screenHeight, side, left, screenHeight - height, width, height);
    }

    /** The same panel moved by a drag, still inside its own half. */
    public FloatingKeyboardBounds movedBy(int dx, int dy) {
        return of(screenWidth, screenHeight, side, left + dx, top + dy, width, height);
    }

    /** The same panel resized by a drag on the resize handle, anchored at its top-left corner. */
    public FloatingKeyboardBounds resizedBy(int dx, int dy) {
        return of(screenWidth, screenHeight, side, left, top, width + dx, height + dy);
    }

    /**
     * The panel reflected onto the other half, about the screen's vertical centre. A panel hugging
     * the outer edge of the left half comes back hugging the outer edge of the right half, so the
     * move reads as a mirror rather than a jump.
     */
    public FloatingKeyboardBounds mirrored() {
        return of(
            screenWidth,
            screenHeight,
            side == Side.LEFT ? Side.RIGHT : Side.LEFT,
            screenWidth - left - width,
            top,
            width,
            height
        );
    }

    /** The same panel on a screen of a different size, e.g. after a rotation. */
    public FloatingKeyboardBounds onScreen(int newScreenWidth, int newScreenHeight) {
        if (newScreenWidth == screenWidth && newScreenHeight == screenHeight) {
            return this;
        }
        float widthFraction = (float) width / halfWidth(screenWidth);
        float heightFraction = (float) height / screenHeight;
        float leftFraction = (float) left / screenWidth;
        float topFraction = (float) top / screenHeight;
        return of(
            newScreenWidth,
            newScreenHeight,
            side,
            Math.round(newScreenWidth * leftFraction),
            Math.round(newScreenHeight * topFraction),
            Math.round(halfWidth(newScreenWidth) * widthFraction),
            Math.round(newScreenHeight * heightFraction)
        );
    }

    /** True when the panel sits in the left half, so the cross-over key should read {@code >}. */
    public boolean isLeft() {
        return side == Side.LEFT;
    }

    public Side side() {
        return side;
    }

    public int left() {
        return left;
    }

    public int top() {
        return top;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int right() {
        return left + width;
    }

    public int bottom() {
        return top + height;
    }

    public int screenWidth() {
        return screenWidth;
    }

    public int screenHeight() {
        return screenHeight;
    }

    private static int halfWidth(int screenWidth) {
        return screenWidth / 2;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public String toString() {
        return "FloatingKeyboardBounds{" + side + " " + left + "," + top
            + " " + width + "x" + height + "}";
    }
}
