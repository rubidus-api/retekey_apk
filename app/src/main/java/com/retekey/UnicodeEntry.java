package com.retekey;

/**
 * Typing a character by its code point: U+ followed by hex digits, the way the Jamotong project's
 * code-input window does it. Useful for the characters no keyboard has a key for — a rare Hanja,
 * a box-drawing glyph, an arrow — and for anyone who knows the number and does not want to go
 * hunting through a symbol picker.
 *
 * <p>State is the digits typed so far and nothing else, so the whole thing is a small immutable
 * value: Android-free, unit-tested, and safe to keep across whatever the editor does meanwhile.
 */
public final class UnicodeEntry {
    /** The longest code point is six hex digits (U+10FFFF). */
    public static final int MAX_DIGITS = 6;

    private final String digits;

    private UnicodeEntry(String digits) {
        this.digits = digits;
    }

    /** An empty entry, waiting for its first digit. */
    public static UnicodeEntry empty() {
        return new UnicodeEntry("");
    }

    /** Whether this character can be typed into a code point: 0-9, a-f, A-F. */
    public static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    /**
     * The entry with one more digit. A character that is not a hex digit, or a digit past the
     * sixth, leaves the entry as it was: the caller can offer every keystroke without filtering.
     */
    public UnicodeEntry append(char c) {
        if (!isHexDigit(c) || digits.length() >= MAX_DIGITS) {
            return this;
        }
        return new UnicodeEntry(digits + Character.toUpperCase(c));
    }

    /** The entry with its last digit removed; an empty entry stays empty. */
    public UnicodeEntry backspace() {
        return digits.isEmpty() ? this : new UnicodeEntry(digits.substring(0, digits.length() - 1));
    }

    public boolean isEmpty() {
        return digits.isEmpty();
    }

    /** The digits typed so far, upper case, without the U+. */
    public String digits() {
        return digits;
    }

    /** What the entry shows while it is being typed: {@code U+} and the digits. */
    public String display() {
        return "U+" + digits;
    }

    /** The code point typed, or -1 when the digits do not name one that can be typed. */
    public int codePoint() {
        if (digits.isEmpty()) {
            return -1;
        }
        int value;
        try {
            value = Integer.parseInt(digits, 16);
        } catch (NumberFormatException notANumber) {
            return -1;
        }
        return isTypable(value) ? value : -1;
    }

    /** The character the code point names, or null when there is not one yet. */
    public String character() {
        int codePoint = codePoint();
        return codePoint < 0 ? null : new String(Character.toChars(codePoint));
    }

    /**
     * Whether a code point can be put into a document: inside Unicode's range, and not one of the
     * surrogate halves, which are an encoding detail rather than characters anyone can type.
     */
    public static boolean isTypable(int codePoint) {
        return codePoint >= 0
            && codePoint <= 0x10FFFF
            && !(codePoint >= 0xD800 && codePoint <= 0xDFFF);
    }

    /** {@code U+XXXX} for a code point, in the four-or-more-digit form the standard uses. */
    public static String label(int codePoint) {
        String hex = Integer.toHexString(codePoint).toUpperCase(java.util.Locale.ROOT);
        while (hex.length() < 4) {
            hex = "0" + hex;
        }
        return "U+" + hex;
    }

    /** {@code U+XXXX} for the first character of a string, for the gloss beside a candidate. */
    public static String labelOf(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return label(text.codePointAt(0));
    }
}
