package com.retekey;

/**
 * What the twelve cells of a 12-key page show instead of Hangul: the phone keypad's digits, or
 * the cursor cluster. Toggled by the two keys in the second column — 123 on the top row, 이동 on
 * the row below — and toggled back by the same key, so the overlay is a mode of the page rather
 * than a page of its own: the frame around the pad (modifiers, backspace, space, enter, 한자,
 * the page keys) stays exactly where the fingers know it.
 */
public enum PhoneOverlay {
    NONE,
    DIGITS,
    NAV
}
