package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * What the keystroke echo box shows. It exists to answer "what did I just type" when a finger is
 * covering the key, so it only speaks for keys that typed something.
 */
public final class EchoLabelTest {
    @Test
    public void aTypedAlternateWinsOverTheKeysOwnLabel() {
        SoftwareKeySpec period = KeyboardLayouts
            .of(KeyboardLayoutId.EN_QWERTY, false)
            .findById("touch.text.period");

        assertEquals(",", ReteKeyboardView.echoLabel(period, ","));
    }

    @Test
    public void aLetterEchoesItsLabel() {
        KeyboardLayout en = KeyboardLayouts.of(KeyboardLayoutId.EN_QWERTY, false);
        assertEquals("q", ReteKeyboardView.echoLabel(en.rows().get(0).get(0), null));

        KeyboardLayout ko = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);
        assertEquals("ㅂ", ReteKeyboardView.echoLabel(ko.rows().get(0).get(0), null));
    }

    @Test
    public void shiftedLettersEchoTheirUppercase() {
        KeyboardLayout shifted = KeyboardLayouts.of(KeyboardLayoutId.EN_QWERTY, true);
        assertEquals("Q", ReteKeyboardView.echoLabel(shifted.rows().get(0).get(0), null));
    }

    @Test
    public void keysThatTypeNothingEchoNothing() {
        KeyboardLayout en = KeyboardLayouts.of(KeyboardLayoutId.EN_QWERTY, false);
        // Shift, backspace, enter and the layer keys have nothing to blow up to syllable size.
        assertNull(ReteKeyboardView.echoLabel(en.rows().get(2).get(0), null));
        assertNull(ReteKeyboardView.echoLabel(en.rows().get(1).get(9), null));
        assertNull(ReteKeyboardView.echoLabel(en.rows().get(2).get(9), null));
        assertNull(ReteKeyboardView.echoLabel(en.findById("touch.layout.toggle"), null));
        assertNull(ReteKeyboardView.echoLabel(null, null));
    }

    @Test
    public void aRawKeyEchoesNothing() {
        SoftwareKeySpec escape = KeyboardLayouts
            .specialKeys(NumpadMode.NUMBERS)
            .findById("touch.key.esc");

        assertNull(ReteKeyboardView.echoLabel(escape, null));
    }
}
