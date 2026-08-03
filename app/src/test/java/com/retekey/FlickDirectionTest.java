package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.retekey.CheonjiinInterpreter.Flick;
import org.junit.Test;

/** Which way a finger went, and when it has not gone anywhere yet. */
public final class FlickDirectionTest {
    private static final int THRESHOLD = 20;

    @Test
    public void aPressThatBarelyMovesIsNoDrag() {
        assertNull(FlickDirection.of(0, 0, THRESHOLD));
        assertNull(FlickDirection.of(19, 19, THRESHOLD));
        assertNull(FlickDirection.of(-19, 19, THRESHOLD));
    }

    @Test
    public void theThresholdItselfCounts() {
        assertEquals(Flick.RIGHT, FlickDirection.of(20, 0, THRESHOLD));
        assertEquals(Flick.DOWN, FlickDirection.of(0, 20, THRESHOLD));
    }

    @Test
    public void eachDirectionIsTheOneItLooksLike() {
        assertEquals(Flick.LEFT, FlickDirection.of(-40, 0, THRESHOLD));
        assertEquals(Flick.RIGHT, FlickDirection.of(40, 0, THRESHOLD));
        // Screen coordinates: y grows downwards, so a negative dy is upwards.
        assertEquals(Flick.UP, FlickDirection.of(0, -40, THRESHOLD));
        assertEquals(Flick.DOWN, FlickDirection.of(0, 40, THRESHOLD));
    }

    @Test
    public void theLongerAxisWinsSoAWanderingDragStillMeansOneThing() {
        assertEquals(Flick.RIGHT, FlickDirection.of(40, 25, THRESHOLD));
        assertEquals(Flick.DOWN, FlickDirection.of(25, 40, THRESHOLD));
        // A dead-even diagonal is horizontal, arbitrarily but predictably.
        assertEquals(Flick.LEFT, FlickDirection.of(-30, 30, THRESHOLD));
    }

    @Test
    public void oneAxisPastTheThresholdIsEnough() {
        assertEquals(Flick.RIGHT, FlickDirection.of(40, 5, THRESHOLD));
        assertEquals(Flick.UP, FlickDirection.of(5, -40, THRESHOLD));
    }
}
