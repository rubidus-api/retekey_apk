package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class LatchStateTest {
    @Test
    public void startsOff() {
        LatchState latch = new LatchState();
        assertEquals(LatchState.State.OFF, latch.state());
        assertFalse(latch.isActive());
        assertFalse(latch.isLocked());
    }

    @Test
    public void aTapArmsOneShotAndAnotherTapCancels() {
        LatchState latch = new LatchState();
        latch.tap();
        assertEquals(LatchState.State.ONE_SHOT, latch.state());
        assertTrue(latch.isActive());
        assertFalse(latch.isLocked());

        latch.tap();
        assertEquals(LatchState.State.OFF, latch.state());
    }

    @Test
    public void oneShotIsConsumedByASingleKey() {
        LatchState latch = new LatchState();
        latch.tap();
        assertTrue(latch.consumeOneShot());
        assertEquals(LatchState.State.OFF, latch.state());
        assertFalse(latch.consumeOneShot());
    }

    @Test
    public void aHoldTogglesTheLockOnAndOff() {
        LatchState latch = new LatchState();
        latch.toggleLock();
        assertEquals(LatchState.State.LOCKED, latch.state());
        assertTrue(latch.isLocked());
        assertTrue(latch.isActive());

        latch.toggleLock();
        assertEquals(LatchState.State.OFF, latch.state());
    }

    @Test
    public void aLockedLatchSurvivesKeyPresses() {
        LatchState latch = new LatchState();
        latch.toggleLock();
        assertFalse(latch.consumeOneShot());
        assertTrue(latch.isLocked());
        assertFalse(latch.consumeOneShot());
        assertTrue(latch.isActive());
    }

    @Test
    public void aTapClearsALock() {
        LatchState latch = new LatchState();
        latch.toggleLock();
        latch.tap();
        assertEquals(LatchState.State.OFF, latch.state());
    }

    @Test
    public void clearDropsEveryState() {
        LatchState latch = new LatchState();
        latch.toggleLock();
        latch.clear();
        assertEquals(LatchState.State.OFF, latch.state());
        assertFalse(latch.isActive());
    }

    /**
     * A press that is canceled — the finger slid off the keyboard, the view was rebuilt under it —
     * leaves the latch as it was before it. A second tap would not do: it arms what was off and
     * clears what was locked (review finding R11).
     */
    @org.junit.Test
    public void aCanceledPressLeavesTheLatchAsItWas() {
        LatchState latch = new LatchState();
        LatchState.State before = latch.state();
        latch.tap();
        latch.restore(before);
        org.junit.Assert.assertEquals(LatchState.State.OFF, latch.state());

        latch.toggleLock();
        before = latch.state();
        latch.tap();
        org.junit.Assert.assertEquals(LatchState.State.OFF, latch.state());
        latch.restore(before);
        org.junit.Assert.assertEquals(LatchState.State.LOCKED, latch.state());

        latch.restore(null);
        org.junit.Assert.assertEquals(LatchState.State.OFF, latch.state());
    }
}
