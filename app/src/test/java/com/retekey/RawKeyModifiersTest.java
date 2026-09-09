package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import org.junit.Test;

/** What an arrow, Home or Page Down is sent with. */
public final class RawKeyModifiersTest {

    @Test
    public void aLockedShiftReachesAKeyThatHasNoShiftedForm() {
        // Shift+arrow selects text. Before this, the Shift key only ever chose a letter layer, so
        // on the arrows it did nothing at all.
        assertEquals(EnumSet.of(KeyModifier.SHIFT),
            RawKeyModifiers.of(EnumSet.noneOf(KeyModifier.class), true));
    }

    @Test
    public void theOtherModifiersComeWithIt() {
        Set<KeyModifier> mods = RawKeyModifiers.of(
            EnumSet.of(KeyModifier.CTRL, KeyModifier.ALT), true);
        assertEquals(EnumSet.of(KeyModifier.CTRL, KeyModifier.ALT, KeyModifier.SHIFT), mods);
    }

    @Test
    public void nothingHeldIsNoChord() {
        assertTrue(RawKeyModifiers.of(EnumSet.noneOf(KeyModifier.class), false).isEmpty());
        assertTrue(RawKeyModifiers.of(null, false).isEmpty());
    }

    @Test
    public void shiftAlreadyInTheLatchesIsNotDoubled() {
        // The right-hand Shift is one of the latches; the layout's is not. Both mean SHIFT.
        assertEquals(EnumSet.of(KeyModifier.SHIFT),
            RawKeyModifiers.of(EnumSet.of(KeyModifier.SHIFT), true));
    }

    @Test
    public void theCallerSSetIsNotModified() {
        Set<KeyModifier> latched = EnumSet.of(KeyModifier.CTRL);
        RawKeyModifiers.of(latched, true);
        assertEquals(EnumSet.of(KeyModifier.CTRL), latched);
    }
}
