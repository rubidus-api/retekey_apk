package com.retekey;

/**
 * Which key a character is typed on, on a US keyboard.
 *
 * <p>An IME normally hands text to the editor and never mentions keys. Some apps only take key
 * events — remote-desktop clients forward them to the far side, and terminals and games read them
 * directly — and for those, "type an a" has to become "press the A key" instead
 * (see RFC-0010).
 *
 * <p>The map is deliberately the US layout and nothing else: it is what a physical keyboard's
 * key codes mean, and the far side is doing its own layout on top. Anything not on it — Hangul
 * above all — has no key to be pressed on and is not this class's business; the caller falls back
 * to sending text, as it always did.
 */
final class AsciiKeyStrokes {
    /** A key and whether Shift is held with it. */
    static final class Stroke {
        final RawKey key;
        final boolean shifted;

        Stroke(RawKey key, boolean shifted) {
            this.key = key;
            this.shifted = shifted;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Stroke)) {
                return false;
            }
            Stroke stroke = (Stroke) other;
            return key == stroke.key && shifted == stroke.shifted;
        }

        @Override
        public int hashCode() {
            return key.hashCode() * 31 + (shifted ? 1 : 0);
        }

        @Override
        public String toString() {
            return (shifted ? "Shift+" : "") + key;
        }
    }

    /** The unshifted character on each punctuation key, and the shifted one above it. */
    private static final String PUNCTUATION_PLAIN = "-=[]\\;'`,./";
    private static final String PUNCTUATION_SHIFTED = "_+{}|:\"~<>?";
    private static final RawKey[] PUNCTUATION_KEYS = {
        RawKey.MINUS, RawKey.EQUALS, RawKey.LEFT_BRACKET, RawKey.RIGHT_BRACKET, RawKey.BACKSLASH,
        RawKey.SEMICOLON, RawKey.APOSTROPHE, RawKey.GRAVE, RawKey.COMMA, RawKey.PERIOD, RawKey.SLASH
    };
    /** The characters above the digit row, in 1..9 then 0 order. */
    private static final String SHIFTED_DIGITS = "!@#$%^&*()";

    private AsciiKeyStrokes() {
    }

    /** The stroke that types {@code c}, or null where no key on a US keyboard does. */
    static Stroke of(char c) {
        if (c >= 'a' && c <= 'z') {
            return new Stroke(letter(c - 'a'), false);
        }
        if (c >= 'A' && c <= 'Z') {
            return new Stroke(letter(c - 'A'), true);
        }
        if (c >= '0' && c <= '9') {
            return new Stroke(digit(c - '0'), false);
        }
        int shiftedDigit = SHIFTED_DIGITS.indexOf(c);
        if (shiftedDigit >= 0) {
            // "!" is Shift+1 … ")" is Shift+0.
            return new Stroke(digit((shiftedDigit + 1) % 10), true);
        }
        if (c == ' ') {
            return new Stroke(RawKey.SPACE, false);
        }
        if (c == '\n') {
            return new Stroke(RawKey.ENTER, false);
        }
        if (c == '\t') {
            return new Stroke(RawKey.TAB, false);
        }
        int plain = PUNCTUATION_PLAIN.indexOf(c);
        if (plain >= 0) {
            return new Stroke(PUNCTUATION_KEYS[plain], false);
        }
        int shifted = PUNCTUATION_SHIFTED.indexOf(c);
        if (shifted >= 0) {
            return new Stroke(PUNCTUATION_KEYS[shifted], true);
        }
        return null;
    }

    /** The stroke for a whole string, or null unless it is exactly one typeable character. */
    static Stroke ofText(String text) {
        return text != null && text.length() == 1 ? of(text.charAt(0)) : null;
    }

    private static RawKey letter(int offset) {
        return RawKey.values()[RawKey.A.ordinal() + offset];
    }

    private static RawKey digit(int value) {
        return RawKey.values()[RawKey.DIGIT_0.ordinal() + value];
    }
}
