package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.retekey.FloatingKeyboardBounds.Side;
import org.junit.Test;

public final class FloatingKeyboardBoundsTest {
    private static final int W = 1600;
    private static final int H = 1000;

    @Test
    public void widthNeverExceedsHalfTheScreen() {
        FloatingKeyboardBounds greedy =
            FloatingKeyboardBounds.of(W, H, Side.LEFT, 0, 0, W, 400);

        assertEquals(W / 2, greedy.width());
        assertTrue(greedy.right() <= W / 2);
    }

    @Test
    public void aLeftPanelCannotBeDraggedPastTheMiddle() {
        FloatingKeyboardBounds panel =
            FloatingKeyboardBounds.of(W, H, Side.LEFT, 0, 500, 600, 400);

        FloatingKeyboardBounds shoved = panel.movedBy(5000, 0);

        assertEquals(Side.LEFT, shoved.side());
        assertEquals(W / 2 - 600, shoved.left());
        assertEquals(W / 2, shoved.right());
    }

    @Test
    public void aRightPanelCannotBeDraggedPastTheMiddle() {
        FloatingKeyboardBounds panel =
            FloatingKeyboardBounds.of(W, H, Side.RIGHT, W / 2, 500, 600, 400);

        FloatingKeyboardBounds shoved = panel.movedBy(-5000, 0);

        assertEquals(Side.RIGHT, shoved.side());
        assertEquals(W / 2, shoved.left());
    }

    @Test
    public void draggingStaysInsideTheScreenVertically() {
        FloatingKeyboardBounds panel =
            FloatingKeyboardBounds.of(W, H, Side.LEFT, 0, 500, 600, 400);

        assertEquals(0, panel.movedBy(0, -5000).top());
        assertEquals(H - 400, panel.movedBy(0, 5000).top());
    }

    @Test
    public void crossingOverReflectsAboutTheCentreLine() {
        // Hugging the left edge; the mirror image hugs the right edge.
        FloatingKeyboardBounds panel =
            FloatingKeyboardBounds.of(W, H, Side.LEFT, 0, 300, 600, 400);

        FloatingKeyboardBounds crossed = panel.mirrored();

        assertEquals(Side.RIGHT, crossed.side());
        assertEquals(W - 600, crossed.left());
        assertEquals(W, crossed.right());
        assertEquals(300, crossed.top());
        assertEquals(600, crossed.width());
        assertEquals(400, crossed.height());
    }

    @Test
    public void crossingOverTwiceReturnsToTheStart() {
        FloatingKeyboardBounds panel =
            FloatingKeyboardBounds.of(W, H, Side.LEFT, 120, 300, 600, 400);

        FloatingKeyboardBounds roundTrip = panel.mirrored().mirrored();

        assertEquals(panel.side(), roundTrip.side());
        assertEquals(panel.left(), roundTrip.left());
        assertEquals(panel.top(), roundTrip.top());
    }

    @Test
    public void sideDecidesWhichArrowTheCrossOverKeyShows() {
        assertTrue(FloatingKeyboardBounds.initial(W, H, Side.LEFT).isLeft());
        assertFalse(FloatingKeyboardBounds.initial(W, H, Side.RIGHT).isLeft());
    }

    @Test
    public void resizingIsBoundedAtBothEnds() {
        FloatingKeyboardBounds panel =
            FloatingKeyboardBounds.of(W, H, Side.LEFT, 0, 200, 600, 400);

        FloatingKeyboardBounds huge = panel.resizedBy(5000, 5000);
        assertEquals(W / 2, huge.width());
        assertEquals(Math.round(H * FloatingKeyboardBounds.MAX_HEIGHT_FRACTION), huge.height());

        FloatingKeyboardBounds tiny = panel.resizedBy(-5000, -5000);
        assertEquals(
            Math.round((W / 2) * FloatingKeyboardBounds.MIN_WIDTH_FRACTION), tiny.width());
        assertEquals(Math.round(H * FloatingKeyboardBounds.MIN_HEIGHT_FRACTION), tiny.height());
    }

    @Test
    public void growingAtTheInnerEdgeStaysInsideItsOwnHalf() {
        // A left panel already touching the middle may grow, but only away from the centre line.
        FloatingKeyboardBounds panel =
            FloatingKeyboardBounds.of(W, H, Side.LEFT, W / 2 - 500, 200, 500, 400);

        FloatingKeyboardBounds grown = panel.resizedBy(200, 0);

        assertEquals(700, grown.width());
        assertTrue(grown.right() <= W / 2);
    }

    @Test
    public void theInitialPanelSitsAtTheBottomOfItsHalf() {
        FloatingKeyboardBounds left = FloatingKeyboardBounds.initial(W, H, Side.LEFT);

        assertEquals(H, left.bottom());
        assertTrue(left.right() <= W / 2);
        assertTrue(left.left() >= 0);

        FloatingKeyboardBounds right = FloatingKeyboardBounds.initial(W, H, Side.RIGHT);

        assertEquals(H, right.bottom());
        assertTrue(right.left() >= W / 2);
        assertTrue(right.right() <= W);
    }

    @Test
    public void aRotationRescalesTheePanelInsteadOfLosingIt() {
        FloatingKeyboardBounds portrait =
            FloatingKeyboardBounds.of(1000, 1600, Side.RIGHT, 500, 1000, 480, 500);

        FloatingKeyboardBounds landscape = portrait.onScreen(1600, 1000);

        assertEquals(Side.RIGHT, landscape.side());
        assertTrue(landscape.left() >= 800);
        assertTrue(landscape.right() <= 1600);
        assertTrue(landscape.bottom() <= 1000);
    }

    @Test
    public void aScreenWithNoSizeIsRejected() {
        try {
            FloatingKeyboardBounds.of(0, 100, Side.LEFT, 0, 0, 10, 10);
            org.junit.Assert.fail("expected rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("positive"));
        }
    }
}
