package com.retekey;

/**
 * Where the word around the cursor starts and ends.
 *
 * <p>Selecting a word is the one bar action that cannot be handed straight to the editor: there is
 * no context-menu id for it, so the keyboard has to read the text either side of the cursor and
 * work the boundary out. It is given what {@code getTextBeforeCursor} and {@code getTextAfterCursor}
 * returned and answers in characters — how far back the word reaches, and how far forward — which
 * the caller turns into an absolute selection.
 *
 * <p>A word is a run of letters, digits, marks and underscores. Hangul, Latin and CJK are all
 * letters as far as this is concerned, so it works the same in Korean as in English. Anything else
 * — space, punctuation, a bracket — is a boundary.
 *
 * <p>When the cursor has no word touching it, the answer is nothing rather than a guess: a bar
 * button that selects the space you are sitting in would be worse than one that does nothing
 * visible.
 */
final class WordBoundary {
    /** How many characters before the cursor belong to the word. */
    final int before;
    /** How many characters after it do. */
    final int after;

    private WordBoundary(int before, int after) {
        this.before = before;
        this.after = after;
    }

    /** Whether there is a word here at all. */
    boolean isEmpty() {
        return before == 0 && after == 0;
    }

    /**
     * The word around a cursor sitting between {@code textBefore} and {@code textAfter}. Either may
     * be null or empty — at the start of a field, at its end, or where the editor answered nothing.
     */
    static WordBoundary of(CharSequence textBefore, CharSequence textAfter) {
        int before = 0;
        if (textBefore != null) {
            for (int i = textBefore.length() - 1; i >= 0 && isWordChar(textBefore.charAt(i)); i--) {
                before++;
            }
        }
        int after = 0;
        if (textAfter != null) {
            for (int i = 0; i < textAfter.length() && isWordChar(textAfter.charAt(i)); i++) {
                after++;
            }
        }
        return new WordBoundary(before, after);
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || isCombining(c);
    }

    /**
     * A combining mark belongs to the letter it sits on. {@code Character.isLetterOrDigit} says no
     * for them, and a selection that stopped short of one would cut a letter in half.
     */
    private static boolean isCombining(char c) {
        int type = Character.getType(c);
        return type == Character.NON_SPACING_MARK
            || type == Character.COMBINING_SPACING_MARK
            || type == Character.ENCLOSING_MARK;
    }
}
