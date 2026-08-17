package com.retekey;

/**
 * Which editors want a keypad rather than letters.
 *
 * <p>A field that takes a phone number, a PIN, an amount or a date has no use for a Hangul layout,
 * and other keyboards answer it with a number pad. ReteKey's answer is the 12-key Keypad layout: it
 * is the pad this keyboard already has, in the places the user's thumbs already know.
 *
 * <p>This only chooses what is shown <em>first</em>. The layout key still walks the user's own list
 * from there, and nothing is stored — the next ordinary field opens on whatever they were using.
 *
 * <p>Android-free: the caller passes the input type it was handed, and the constants are the
 * platform's own frozen values, so this can be tested without a device.
 */
final class NumericFieldPolicy {
    /** {@code InputType.TYPE_MASK_CLASS}. */
    static final int MASK_CLASS = 0x0000000f;
    /** {@code InputType.TYPE_CLASS_NUMBER}. */
    static final int CLASS_NUMBER = 0x00000002;
    /** {@code InputType.TYPE_CLASS_PHONE}. */
    static final int CLASS_PHONE = 0x00000003;
    /** {@code InputType.TYPE_CLASS_DATETIME}. */
    static final int CLASS_DATETIME = 0x00000004;

    private NumericFieldPolicy() {
    }

    /**
     * Whether this editor should open on the keypad.
     *
     * @param inputType the editor's {@code inputType}, or 0 where there is no editor at all
     */
    static boolean wantsKeypad(int inputType) {
        int inputClass = inputType & MASK_CLASS;
        return inputClass == CLASS_NUMBER
            || inputClass == CLASS_PHONE
            || inputClass == CLASS_DATETIME;
    }
}
