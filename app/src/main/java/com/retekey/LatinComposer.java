package com.retekey;

/**
 * A composer for a Latin-typed script: Vietnamese Telex, romaji-to-kana. The processor gives it
 * every key press it {@link #accepts}; everything else flushes it. One instance is one composing
 * word, Android-free, exactly the contract {@link HangulComposer} has for Hangul.
 */
interface LatinComposer {
    /** What a key did: text to commit now, and the preedit that stays composing. */
    final class Result {
        final String commit;
        final String preedit;

        Result(String commit, String preedit) {
            this.commit = commit;
            this.preedit = preedit;
        }
    }

    /** Whether this key press is the composer's. */
    boolean accepts(String text);

    Result input(String text);

    /** Takes back one keystroke, or returns null when nothing is composing. */
    Result backspace();

    /** The word so far, committed; the composer is empty afterwards. */
    String flush();

    void reset();

    boolean isComposing();

    String preeditText();
}
