package com.retekey.core;

/**
 * Cuts text by whole characters. Java counts a string in UTF-16 units, and the characters outside
 * the basic plane — an emoji, a rarer Hanja — take two of them; cutting between the two leaves
 * half a character, which is not text any more. Every length limit in the keyboard (the clip and
 * stash histories, a user layout's name and key cap) and the phonetic search's backspace go
 * through here (review finding R08).
 */
public final class Scalars {
    private Scalars() {
    }

    /** {@code text} cut to at most {@code maxUtf16Units}, never through a character. */
    public static String truncate(String text, int maxUtf16Units) {
        if (text == null || text.length() <= maxUtf16Units) {
            return text;
        }
        if (maxUtf16Units <= 0) {
            return "";
        }
        int end = maxUtf16Units;
        if (Character.isHighSurrogate(text.charAt(end - 1))) {
            // The pair starts inside the limit and ends outside it: leave it out whole. A lone
            // high surrogate in the text itself is dropped the same way rather than carried over.
            end--;
        }
        return text.substring(0, end);
    }

    /** {@code text} without its last character — a whole one, both units of a pair. */
    public static String withoutLastScalar(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? null : "";
        }
        return text.substring(0, text.offsetByCodePoints(text.length(), -1));
    }

    /**
     * Replaces any unpaired UTF-16 surrogates with U+FFFD while keeping valid surrogate pairs
     * intact, so text from external clipboards or notes never crashes {@link SemanticInput#text}.
     */
    public static String sanitizeWellFormed(String text) {
        if (text == null || UnicodeScalar.isWellFormed(text)) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (i + 1 < text.length() && Character.isLowSurrogate(text.charAt(i + 1))) {
                    out.append(c).append(text.charAt(i + 1));
                    i++;
                } else {
                    out.append('\uFFFD');
                }
            } else if (Character.isLowSurrogate(c)) {
                out.append('\uFFFD');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
