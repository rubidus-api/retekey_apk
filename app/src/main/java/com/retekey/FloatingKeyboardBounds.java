package com.retekey;

/**
 * Where the floating keyboard is allowed to be, and how big it is allowed to get.
 *
 * <p>The rules come from the product decision that a floating keyboard must never cover more than
 * its own half of the screen, so the other half stays usable for the app being typed into. Which
 * half that is follows the shape of the screen: a screen wider than it is tall splits left and
 * right, and a screen taller than it is wide splits top and bottom. Halving a tall screen down the
 * middle would leave two columns too narrow to type in, and halving a wide one across would leave
 * two strips too short to read — so the panel always takes the half that is worth having.
 *
 * <ul>
 *   <li>the panel never exceeds half the screen along the splitting axis;</li>
 *   <li>the panel stays inside the half it currently occupies — dragging cannot carry it across
 *       the middle;</li>
 *   <li>crossing sides is an explicit act: {@link #mirrored()} reflects the panel about the
 *       screen's centre line, which is what the {@code <} / {@code >} key does;</li>
 *   <li>the other dimension is bounded so the panel cannot become unusably small or fill the
 *       screen.</li>
 * </ul>
 *
 * <p>Android-free so the geometry is unit-tested directly; the view supplies screen size and touch
 * deltas and asks for a new instance.
 */
public final class FloatingKeyboardBounds {
    /**
     * Which half of the screen the panel lives in. The names are the wide-screen case, which is
     * where the feature started; on a tall screen {@code LEFT} is the top half and {@code RIGHT}
     * the bottom, so a setting stored before the split became shape-aware still means something.
     */
    public enum Side {
        LEFT,
        RIGHT
    }

    /** Whether this screen splits left/right or top/bottom. */
    public static boolean splitsHorizontally(int screenWidth, int screenHeight) {
        return screenWidth >= screenHeight;
    }

    /** Smallest usable panel, as a fraction of the half it lives in. */
    public static final float MIN_WIDTH_FRACTION = 0.45f;
    /** Smallest usable panel height, as a fraction of the screen height. */
    public static final float MIN_HEIGHT_FRACTION = 0.18f;
    /** Tallest the panel may get, as a fraction of the screen height. */
    public static final float MAX_HEIGHT_FRACTION = 0.75f;

    public static final float DEFAULT_WIDTH_FRACTION = 0.92f;
    public static final float DEFAULT_HEIGHT_FRACTION = 0.42f;
    /**
     * On a tall screen the panel is wide and not very deep — a keyboard shape — so it fills nearly
     * the whole width and takes a comfortable share of its half's height rather than most of it.
     */
    public static final float TALL_DEFAULT_ACROSS_FRACTION = 0.94f;
    public static final float TALL_DEFAULT_ALONG_FRACTION = 0.60f;
    /** Across a tall screen the panel may reach the edges; across a wide one it may not. */
    public static final float TALL_MAX_ACROSS_FRACTION = 1.00f;

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
        boolean horizontal = splitsHorizontally(screenWidth, screenHeight);
        // Along the splitting axis the panel is capped at the half and confined to its own half;
        // across it, the panel is merely kept usable and on screen.
        int halfAlong = (horizontal ? screenWidth : screenHeight) / 2;
        int along = horizontal ? width : height;
        int across = horizontal ? height : width;
        int screenAcross = horizontal ? screenHeight : screenWidth;
        int clampedAlong =
            clamp(along, Math.max(1, Math.round(halfAlong * MIN_WIDTH_FRACTION)), halfAlong);
        float maxAcrossFraction = horizontal ? MAX_HEIGHT_FRACTION : TALL_MAX_ACROSS_FRACTION;
        int clampedAcross = clamp(
            across,
            Math.max(1, Math.round(screenAcross * MIN_HEIGHT_FRACTION)),
            Math.max(1, Math.round(screenAcross * maxAcrossFraction))
        );
        int clampedWidth = horizontal ? clampedAlong : clampedAcross;
        int clampedHeight = horizontal ? clampedAcross : clampedAlong;

        int startAlong = horizontal ? left : top;
        int minAlong = side == Side.LEFT ? 0 : halfAlong;
        int maxAlong = (side == Side.LEFT ? halfAlong : (horizontal ? screenWidth : screenHeight))
            - clampedAlong;
        int placedAlong = clamp(startAlong, minAlong, Math.max(minAlong, maxAlong));
        int placedAcross = clamp(
            horizontal ? top : left, 0, Math.max(0, screenAcross - clampedAcross));

        return new FloatingKeyboardBounds(
            screenWidth,
            screenHeight,
            side,
            horizontal ? placedAlong : placedAcross,
            horizontal ? placedAcross : placedAlong,
            clampedWidth,
            clampedHeight
        );
    }

    /** The panel a first-time user gets: at the far edge of its own half, centred across it. */
    public static FloatingKeyboardBounds initial(int screenWidth, int screenHeight, Side side) {
        boolean horizontal = splitsHorizontally(screenWidth, screenHeight);
        int halfAlong = (horizontal ? screenWidth : screenHeight) / 2;
        int screenAcross = horizontal ? screenHeight : screenWidth;
        int along = Math.round(halfAlong
            * (horizontal ? DEFAULT_WIDTH_FRACTION : TALL_DEFAULT_ALONG_FRACTION));
        int across = Math.round(screenAcross
            * (horizontal ? DEFAULT_HEIGHT_FRACTION : TALL_DEFAULT_ACROSS_FRACTION));
        int placedAlong = side == Side.LEFT
            ? (halfAlong - along) / 2
            : halfAlong + (halfAlong - along) / 2;
        // Across the axis the panel starts where a keyboard belongs: at the bottom on a wide
        // screen, and against the near edge on a tall one.
        int placedAcross = horizontal ? screenAcross - across : (screenAcross - across) / 2;
        return of(
            screenWidth,
            screenHeight,
            side,
            horizontal ? placedAlong : placedAcross,
            horizontal ? placedAcross : placedAlong,
            horizontal ? along : across,
            horizontal ? across : along
        );
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
        boolean horizontal = splitsHorizontally(screenWidth, screenHeight);
        return of(
            screenWidth,
            screenHeight,
            side == Side.LEFT ? Side.RIGHT : Side.LEFT,
            horizontal ? screenWidth - left - width : left,
            horizontal ? top : screenHeight - top - height,
            width,
            height
        );
    }

    /** The same panel on a screen of a different size, e.g. after a rotation. */
    public FloatingKeyboardBounds onScreen(int newScreenWidth, int newScreenHeight) {
        if (newScreenWidth == screenWidth && newScreenHeight == screenHeight) {
            return this;
        }
        // A rotation can change which axis the screen splits on, so the panel is re-derived from
        // fractions of the screen rather than carried over as pixels.
        float widthFraction = (float) width / screenWidth;
        float heightFraction = (float) height / screenHeight;
        float leftFraction = (float) left / screenWidth;
        float topFraction = (float) top / screenHeight;
        return of(
            newScreenWidth,
            newScreenHeight,
            side,
            Math.round(newScreenWidth * leftFraction),
            Math.round(newScreenHeight * topFraction),
            Math.round(newScreenWidth * widthFraction),
            Math.round(newScreenHeight * heightFraction)
        );
    }

    /** True when the panel sits in the first half — left on a wide screen, top on a tall one. */
    public boolean isLeft() {
        return side == Side.LEFT;
    }

    /** Whether this panel's screen splits left/right rather than top/bottom. */
    public boolean splitsHorizontally() {
        return splitsHorizontally(screenWidth, screenHeight);
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public String toString() {
        return "FloatingKeyboardBounds{" + side + " " + left + "," + top
            + " " + width + "x" + height + "}";
    }
}
