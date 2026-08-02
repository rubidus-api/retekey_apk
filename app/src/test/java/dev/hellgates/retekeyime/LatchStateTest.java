package dev.hellgates.retekeyime;

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
}
