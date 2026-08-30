package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.Collections;
import org.junit.Test;

/**
 * Ctrl, Meta and Alt hold the same three states Shift does. What a chord carries, and what
 * survives it, is the whole of the difference between armed and locked.
 */
public final class ModifierLatchesTest {
    @Test
    public void nothingIsCarriedUntilOneIsPressed() {
        assertEquals(Collections.emptySet(), new ModifierLatches().active());
    }

    @Test
    public void aTapArmsExactlyOneKey() {
        ModifierLatches latches = new ModifierLatches();
        latches.tap(ControlKey.CTRL);

        assertEquals(EnumSet.of(KeyModifier.CTRL), latches.active());
        assertTrue(latches.consumeOneShots());
        assertEquals(Collections.emptySet(), latches.active());
    }

    @Test
    public void aHoldKeepsItForEveryKeyUntilItIsHeldAgain() {
        ModifierLatches latches = new ModifierLatches();
        latches.hold(ControlKey.ALT);

        for (int key = 0; key < 5; key++) {
            assertEquals("key " + key, EnumSet.of(KeyModifier.ALT), latches.active());
            assertFalse("a locked modifier is never spent", latches.consumeOneShots());
        }

        latches.hold(ControlKey.ALT);
        assertEquals(Collections.emptySet(), latches.active());
    }

    @Test
    public void aTapOnALockedModifierClearsIt() {
        ModifierLatches latches = new ModifierLatches();
        latches.hold(ControlKey.META);
        latches.tap(ControlKey.META);

        assertFalse(latches.isActive(ControlKey.META));
        assertFalse(latches.isLocked(ControlKey.META));
    }

    @Test
    public void aSecondTapCancelsAnArmingWithoutTypingAnything() {
        ModifierLatches latches = new ModifierLatches();
        latches.tap(ControlKey.CTRL);
        latches.tap(ControlKey.CTRL);

        assertEquals(Collections.emptySet(), latches.active());
    }

    @Test
    public void armedAndLockedAreBothCarriedButOnlyTheArmedIsSpent() {
        ModifierLatches latches = new ModifierLatches();
        latches.hold(ControlKey.CTRL);
        latches.tap(ControlKey.ALT);

        assertEquals(EnumSet.of(KeyModifier.CTRL, KeyModifier.ALT), latches.active());

        assertTrue(latches.consumeOneShots());
        assertEquals(EnumSet.of(KeyModifier.CTRL), latches.active());
    }

    @Test
    public void lockedAndArmedAreToldApart() {
        ModifierLatches latches = new ModifierLatches();
        latches.tap(ControlKey.CTRL);
        latches.hold(ControlKey.ALT);

        assertTrue(latches.isActive(ControlKey.CTRL));
        assertFalse("an armed modifier is not a held one", latches.isLocked(ControlKey.CTRL));
        assertTrue(latches.isLocked(ControlKey.ALT));
    }

    @Test
    public void clearDropsEverything() {
        ModifierLatches latches = new ModifierLatches();
        latches.hold(ControlKey.CTRL);
        latches.tap(ControlKey.ALT);
        latches.clear();

        assertEquals(Collections.emptySet(), latches.active());
    }

    @Test
    public void theDrawingSignatureSeparatesOffArmedAndLocked() {
        ModifierLatches off = new ModifierLatches();
        ModifierLatches armed = new ModifierLatches();
        armed.tap(ControlKey.CTRL);
        ModifierLatches locked = new ModifierLatches();
        locked.hold(ControlKey.CTRL);

        assertNotEquals(off.signature(), armed.signature());
        assertNotEquals(armed.signature(), locked.signature());
        assertNotEquals(off.signature(), locked.signature());
    }

    @Test
    public void onlyTheThreeModifiersAreHandled() {
        assertTrue(ModifierLatches.handles(ControlKey.CTRL));
        assertTrue(ModifierLatches.handles(ControlKey.META));
        assertTrue(ModifierLatches.handles(ControlKey.ALT));
        assertFalse("shift travels with the layout, not the chord",
            ModifierLatches.handles(ControlKey.SHIFT));
        assertFalse(ModifierLatches.handles(ControlKey.TAB_HOLD));
        assertFalse(ModifierLatches.handles(null));
    }

    /** RSh(오른쪽 시프트)는 코드용 시프트 래치다 — 탭=일회성, 홀드=잠금, 소진 규칙 동일. */
    @org.junit.Test
    public void rightShiftLatchesAndMapsToTheShiftModifier() {
        ModifierLatches latches = new ModifierLatches();
        latches.tap(ControlKey.RSHIFT);
        org.junit.Assert.assertTrue(latches.active().contains(KeyModifier.SHIFT));
        latches.consumeOneShots();
        org.junit.Assert.assertFalse(latches.active().contains(KeyModifier.SHIFT));
        latches.hold(ControlKey.RSHIFT);
        org.junit.Assert.assertTrue(latches.isLocked(ControlKey.RSHIFT));
        latches.consumeOneShots();
        org.junit.Assert.assertTrue("잠금은 소진되지 않는다",
            latches.active().contains(KeyModifier.SHIFT));
        latches.hold(ControlKey.RSHIFT);
        org.junit.Assert.assertFalse(latches.active().contains(KeyModifier.SHIFT));
    }
}