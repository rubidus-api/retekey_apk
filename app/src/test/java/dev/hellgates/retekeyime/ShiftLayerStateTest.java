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
        assertFalse(shift.isSticky());
    }

    @Test
    public void tapsCycleOffOneShotStickyOff() {
        ShiftLayerState shift = new ShiftLayerState();
        shift.advance();
        assertEquals(ShiftLayerState.State.ONE_SHOT, shift.state());
        assertTrue(shift.isActive());
        assertFalse(shift.isSticky());

        shift.advance();
        assertEquals(ShiftLayerState.State.STICKY, shift.state());
        assertTrue(shift.isSticky());

        shift.advance();
        assertEquals(ShiftLayerState.State.OFF, shift.state());
        assertFalse(shift.isActive());
    }

    @Test
    public void oneShotIsConsumedByASingleKey() {
        ShiftLayerState shift = new ShiftLayerState();
        shift.advance();
        assertTrue(shift.consumeOneShot());
        assertEquals(ShiftLayerState.State.OFF, shift.state());
        assertFalse(shift.consumeOneShot());
    }

    @Test
    public void stickyShiftSurvivesKeyPresses() {
        ShiftLayerState shift = new ShiftLayerState();
        shift.advance();
        shift.advance();
        assertFalse(shift.consumeOneShot());
        assertTrue(shift.isSticky());
        assertFalse(shift.consumeOneShot());
        assertTrue(shift.isActive());
    }

    @Test
    public void clearDropsEveryLayer() {
        ShiftLayerState shift = new ShiftLayerState();
        shift.advance();
        shift.advance();
        shift.clear();
        assertEquals(ShiftLayerState.State.OFF, shift.state());
        assertFalse(shift.isActive());
    }
}
