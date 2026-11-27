package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.EnumSet;
import org.junit.Test;
import com.retekey.core.ControlKey;
import com.retekey.core.KeyModifier;
import com.retekey.core.LatchState;

/** Unit contract for {@link ModifierCoordinator}: chords, lock/unlock holds, and multi-finger unwind. */
public final class ModifierCoordinatorTest {

    @Test
    public void rawKeyConsumptionSpendsOneShotCtrlAndShiftWhilePreservingLocks() {
        ModifierCoordinator mods = new ModifierCoordinator();
        mods.tap(ControlKey.CTRL);
        mods.hold(ControlKey.ALT, null);
        mods.tap(ControlKey.SHIFT);

        assertEquals(EnumSet.of(KeyModifier.CTRL, KeyModifier.ALT, KeyModifier.SHIFT),
            mods.rawKeyModifiers());

        assertTrue(mods.consumeOneShots(true, Collections.emptySet()));
        assertFalse(mods.modifierActive(ControlKey.CTRL));
        assertFalse(mods.shiftActive());
        assertTrue(mods.modifierLocked(ControlKey.ALT));
        assertEquals(EnumSet.of(KeyModifier.ALT), mods.rawKeyModifiers());
    }

    @Test
    public void chordModifiersForLetterIncludesShiftWithLatchesButNotShiftAlone() {
        ModifierCoordinator mods = new ModifierCoordinator();
        mods.tap(ControlKey.SHIFT);
        assertEquals(Collections.emptySet(), mods.chordModifiersForLetter());

        mods.tap(ControlKey.CTRL);
        assertEquals(EnumSet.of(KeyModifier.CTRL, KeyModifier.SHIFT),
            mods.chordModifiersForLetter());
    }

    @Test
    public void holdingLockedModifierUnlocksToOffInsteadOfReLocking() {
        ModifierCoordinator mods = new ModifierCoordinator();
        // Hold from OFF locks Shift.
        LatchState.State beforeOff = mods.press(ControlKey.SHIFT);
        mods.hold(ControlKey.SHIFT, beforeOff);
        assertTrue(mods.shiftLocked());

        // Holding Shift while already LOCKED restores LOCKED then toggles to OFF.
        LatchState.State beforeLocked = mods.press(ControlKey.SHIFT);
        assertEquals(LatchState.State.LOCKED, beforeLocked);
        mods.hold(ControlKey.SHIFT, beforeLocked);
        assertFalse(mods.shiftLocked());
        assertFalse(mods.shiftActive());

        // Same contract for Ctrl latch.
        LatchState.State ctrlBefore = mods.press(ControlKey.CTRL);
        mods.hold(ControlKey.CTRL, ctrlBefore);
        assertTrue(mods.modifierLocked(ControlKey.CTRL));
        LatchState.State ctrlLockedBefore = mods.press(ControlKey.CTRL);
        mods.hold(ControlKey.CTRL, ctrlLockedBefore);
        assertFalse(mods.modifierLocked(ControlKey.CTRL));
        assertFalse(mods.modifierActive(ControlKey.CTRL));
    }

    @Test
    public void unwindRestoresUnusedPressWithoutCorruptingOtherHeldModifiers() {
        ModifierCoordinator mods = new ModifierCoordinator();
        mods.press(ControlKey.ALT);
        LatchState.State ctrlBefore = mods.press(ControlKey.CTRL);

        // Sliding off Ctrl without typing restores Ctrl to OFF while leaving Alt armed.
        assertTrue(mods.unwind(ControlKey.CTRL, ctrlBefore, false, false));
        assertFalse(mods.modifierActive(ControlKey.CTRL));
        assertTrue(mods.modifierActive(ControlKey.ALT));
    }

    @Test
    public void perModifierFingerTrackingConsumesUnheldOneShotOnly() {
        ModifierCoordinator mods = new ModifierCoordinator();
        mods.press(ControlKey.ALT);
        mods.tap(ControlKey.CTRL);

        // Finger is holding ALT while CTRL is armed: consuming unheld latches drops CTRL, keeps ALT.
        mods.consumeOneShots(true, EnumSet.of(ControlKey.ALT));
        assertFalse(mods.modifierActive(ControlKey.CTRL));
        assertTrue(mods.modifierActive(ControlKey.ALT));

        // Releasing ALT after a key was typed consumes ONLY ALT, leaving another one-shot intact.
        mods.tap(ControlKey.SHIFT);
        assertTrue(mods.releaseAfterTyped(ControlKey.ALT, false));
        assertFalse(mods.modifierActive(ControlKey.ALT));
        assertTrue(mods.shiftActive());
    }
}
