package com.retekey;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The hysteresis that decides when a moving finger has changed keys. */
public class TouchTargetingTest {
    private static final int LEFT = 100;
    private static final int TOP = 200;
    private static final int RIGHT = 200;
    private static final int BOTTOM = 300;
    private static final int SLOP = 24;

    @Test
    public void insideTheKeyIsNotAnEscape() {
        assertFalse(TouchTargeting.escaped(150, 250, LEFT, TOP, RIGHT, BOTTOM, SLOP));
    }

    @Test
    public void aFingerThatRollsOverTheEdgeKeepsItsKey() {
        // Just past the boundary in each direction: this is the tap that used to be lost.
        assertFalse(TouchTargeting.escaped(LEFT - 1, 250, LEFT, TOP, RIGHT, BOTTOM, SLOP));
        assertFalse(TouchTargeting.escaped(RIGHT + 1, 250, LEFT, TOP, RIGHT, BOTTOM, SLOP));
        assertFalse(TouchTargeting.escaped(150, TOP - 1, LEFT, TOP, RIGHT, BOTTOM, SLOP));
        assertFalse(TouchTargeting.escaped(150, BOTTOM + 1, LEFT, TOP, RIGHT, BOTTOM, SLOP));
    }

    @Test
    public void theWholeSlopStillBelongsToTheKey() {
        assertFalse(TouchTargeting.escaped(LEFT - SLOP, 250, LEFT, TOP, RIGHT, BOTTOM, SLOP));
        assertFalse(TouchTargeting.escaped(150, BOTTOM + SLOP, LEFT, TOP, RIGHT, BOTTOM, SLOP));
    }

    @Test
    public void beyondTheSlopTheFingerHasGoneSomewhereElse() {
        assertTrue(TouchTargeting.escaped(LEFT - SLOP - 1, 250, LEFT, TOP, RIGHT, BOTTOM, SLOP));
        assertTrue(TouchTargeting.escaped(RIGHT + SLOP + 1, 250, LEFT, TOP, RIGHT, BOTTOM, SLOP));
        assertTrue(TouchTargeting.escaped(150, TOP - SLOP - 1, LEFT, TOP, RIGHT, BOTTOM, SLOP));
        assertTrue(TouchTargeting.escaped(150, BOTTOM + SLOP + 1, LEFT, TOP, RIGHT, BOTTOM, SLOP));
    }

    @Test
    public void aDiagonalEscapeCountsOnEitherAxis() {
        assertTrue(TouchTargeting.escaped(
            LEFT - SLOP - 1, TOP - SLOP - 1, LEFT, TOP, RIGHT, BOTTOM, SLOP));
    }
}
