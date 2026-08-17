package com.retekey;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Which fields open on the keypad instead of on letters. */
public final class NumericFieldPolicyTest {
    /** The platform's own values, spelled out here so the test does not need a device. */
    private static final int CLASS_TEXT = 0x00000001;
    private static final int CLASS_NUMBER = 0x00000002;
    private static final int CLASS_PHONE = 0x00000003;
    private static final int CLASS_DATETIME = 0x00000004;
    private static final int VARIATION_PASSWORD = 0x00000080;
    private static final int FLAG_MULTI_LINE = 0x00020000;

    @Test
    public void numbersPhonesAndDatesOpenOnTheKeypad() {
        assertTrue(NumericFieldPolicy.wantsKeypad(CLASS_NUMBER));
        assertTrue(NumericFieldPolicy.wantsKeypad(CLASS_PHONE));
        assertTrue(NumericFieldPolicy.wantsKeypad(CLASS_DATETIME));
    }

    @Test
    public void theVariationDoesNotChangeTheAnswer() {
        // A numeric PIN field is still a numeric field.
        assertTrue(NumericFieldPolicy.wantsKeypad(CLASS_NUMBER | VARIATION_PASSWORD));
    }

    @Test
    public void ordinaryTextDoesNot() {
        assertFalse(NumericFieldPolicy.wantsKeypad(CLASS_TEXT));
        assertFalse(NumericFieldPolicy.wantsKeypad(CLASS_TEXT | FLAG_MULTI_LINE));
    }

    @Test
    public void anEditorThatSaysNothingIsNotANumberField() {
        // TYPE_NULL: terminals and other editors that take key events. Those must keep letters.
        assertFalse(NumericFieldPolicy.wantsKeypad(0));
    }
}
