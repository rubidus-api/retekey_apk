package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.view.KeyEvent;
import java.util.EnumSet;
import org.junit.Test;

/** Action-bar keys chord with whatever modifiers are down, wherever they are held. */
public final class BarKeyChordTest {
    private static final EnumSet<KeyModifier> CTRL = EnumSet.of(KeyModifier.CTRL);

    @Test
    public void aLetterSlotChordsWithCtrl() {
        assertEquals(RawKey.C, BarKeyChord.chordKey("c", CTRL));
        assertEquals(RawKey.V, BarKeyChord.chordKey("V", EnumSet.of(KeyModifier.META)));
        assertEquals(RawKey.DIGIT_5, BarKeyChord.chordKey("5", EnumSet.of(KeyModifier.ALT)));
    }

    @Test
    public void withoutAChordModifierTheTextIsTyped() {
        assertNull(BarKeyChord.chordKey("c", EnumSet.noneOf(KeyModifier.class)));
        assertNull(BarKeyChord.chordKey("c", EnumSet.of(KeyModifier.SHIFT)));
        assertNull("longer text is text", BarKeyChord.chordKey("hello", CTRL));
        assertNull("no key for it", BarKeyChord.chordKey("ㄱ", CTRL));
        assertEquals("C", BarKeyChord.typed("c", EnumSet.of(KeyModifier.SHIFT)));
        assertEquals("hello", BarKeyChord.typed("hello", EnumSet.of(KeyModifier.SHIFT)));
        assertEquals("c", BarKeyChord.typed("c", EnumSet.noneOf(KeyModifier.class)));
    }

    @Test
    public void everySourceCounts() {
        assertEquals(EnumSet.of(KeyModifier.CTRL, KeyModifier.SHIFT),
            BarKeyChord.union(CTRL, EnumSet.of(KeyModifier.SHIFT)));
    }

    @Test
    public void aPhysicalModifierIsHeldUntilItsOwnKeyComesUp() {
        HeldHardwareModifiers held = new HeldHardwareModifiers();
        held.onKey(1, KeyEvent.KEYCODE_SHIFT_LEFT, true);
        held.onKey(1, KeyEvent.KEYCODE_SHIFT_RIGHT, true);
        held.onKey(1, KeyEvent.KEYCODE_CTRL_LEFT, true);
        held.onKey(1, KeyEvent.KEYCODE_SHIFT_LEFT, false);
        assertEquals("the right Shift is still down",
            EnumSet.of(KeyModifier.SHIFT, KeyModifier.CTRL), held.held());
        held.onKey(1, KeyEvent.KEYCODE_A, true);
        held.onKey(1, KeyEvent.KEYCODE_SHIFT_RIGHT, false);
        held.onKey(1, KeyEvent.KEYCODE_CTRL_LEFT, false);
        assertEquals(EnumSet.noneOf(KeyModifier.class), held.held());
    }
}
