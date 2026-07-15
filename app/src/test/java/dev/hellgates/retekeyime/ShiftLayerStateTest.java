package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ShiftLayerStateTest {
    @Test
    public void startsOff() {
        ShiftLayerState shift = new ShiftLayerState();
        assertEquals(ShiftLayerState.State.OFF, shift.state());
        assertFalse(shift.isActive());
        assertFalse(shift.isLocked());
    }

    @Test
    public void aTapArmsOneShotAndAnotherTapCancels() {
        ShiftLayerState shift = new ShiftLayerState();
        shift.tap();
        assertEquals(ShiftLayerState.State.ONE_SHOT, shift.state());
        assertTrue(shift.isActive());
        assertFalse(shift.isLocked());

        shift.tap();
        assertEquals(ShiftLayerState.State.OFF, shift.state());
    }

    @Test
    public void oneShotIsConsumedByASingleKey() {
        ShiftLayerState shift = new ShiftLayerState();
        shift.tap();
        assertTrue(shift.consumeOneShot());
        assertEquals(ShiftLayerState.State.OFF, shift.state());
        assertFalse(shift.consumeOneShot());
    }

    @Test
    public void aHoldTogglesTheLockOnAndOff() {
        ShiftLayerState shift = new ShiftLayerState();
        shift.toggleLock();
        assertEquals(ShiftLayerState.State.LOCKED, shift.state());
        assertTrue(shift.isLocked());
        assertTrue(shift.isActive());

        shift.toggleLock();
        assertEquals(ShiftLayerState.State.OFF, shift.state());
    }

    @Test
    public void aLockedShiftSurvivesKeyPresses() {
        ShiftLayerState shift = new ShiftLayerState();
        shift.toggleLock();
        assertFalse(shift.consumeOneShot());
        assertTrue(shift.isLocked());
        assertFalse(shift.consumeOneShot());
        assertTrue(shift.isActive());
    }

    @Test
    public void aTapClearsALock() {
        ShiftLayerState shift = new ShiftLayerState();
        shift.toggleLock();
        shift.tap();
        assertEquals(ShiftLayerState.State.OFF, shift.state());
    }

    @Test
    public void clearDropsEveryState() {
        ShiftLayerState shift = new ShiftLayerState();
        shift.toggleLock();
        shift.clear();
        assertEquals(ShiftLayerState.State.OFF, shift.state());
        assertFalse(shift.isActive());
    }
}
