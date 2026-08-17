package com.retekey;

/**
 * What a 12-key pad cell types when it is held.
 *
 * <p>The pads used to hold the phone keypad's own digit — 1 under the first cell, 0 under the tenth
 * — which was true to a phone keypad and useless in practice: the {@code 123} overlay already types
 * every one of them with a tap, and the Keypad layout is nothing but digits.
 *
 * <p>What a hold is good for is the character you want <em>without leaving the letters</em>, and the
 * set the owner chose is a calculator's: <code>+ ( ) - = % / e ^ ! $ @</code>, laid over the keypad
 * in its own order. The {@code e} is the letter, for exponents — the one entry that is not
 * punctuation, and confirmed rather than guessed.
 *
 * <p>The table is by pad cell, not by layout, so 천지인, 나랏글 and the Keypad layout all hold the
 * same character in the same place.
 */
final class PadHolds {
    /** Pad order: three cells per row, top to bottom — 1 2 3 / 4 5 6 / 7 8 9 / * 0 #. */
    private static final String[] SYMBOLS = {
        "+", "(", ")",
        "-", "=", "%",
        "/", "e", "^",
        "!", "$", "@"
    };

    /** How many cells a 12-key pad has. */
    static final int CELLS = 12;

    private PadHolds() {
    }

    /** The character held under pad cell {@code index}, counting from the top left. */
    static String symbol(int index) {
        if (index < 0 || index >= SYMBOLS.length) {
            throw new IndexOutOfBoundsException("pad cell " + index);
        }
        return SYMBOLS[index];
    }

    /** The digit the same cell carries on the keypad — what it types with a tap on the Keypad layout. */
    static String digit(int index) {
        String[] digits = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};
        if (index < 0 || index >= digits.length) {
            throw new IndexOutOfBoundsException("pad cell " + index);
        }
        return digits[index];
    }
}
