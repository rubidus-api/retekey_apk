package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * The physical-keyboard tables for Dvorak and Colemak, checked against this app's own soft
 * layouts for those two.
 *
 * <p>A hardware table is a list of US key positions and what the layout types there. Written by
 * hand it is a transcription, and a transcription of a keyboard layout is exactly the sort of
 * thing that is wrong in one cell and looks right. The soft pages already carry each layout's
 * letters in each layout's own row order, so they are the reference: the letters the table
 * produces, read row by row, must be the letters the soft page shows.
 */
public final class HardwareLatinLayoutTest {

    /** US positions, top row then home row then bottom row, as the tables address them. */
    private static final String[] US_TOP = {
        "hardware.key.q", "hardware.key.w", "hardware.key.e", "hardware.key.r", "hardware.key.t",
        "hardware.key.y", "hardware.key.u", "hardware.key.i", "hardware.key.o", "hardware.key.p"
    };
    private static final String[] US_HOME = {
        "hardware.key.a", "hardware.key.s", "hardware.key.d", "hardware.key.f", "hardware.key.g",
        "hardware.key.h", "hardware.key.j", "hardware.key.k", "hardware.key.l",
        "hardware.keycode.74"
    };
    private static final String[] US_BOTTOM = {
        "hardware.key.z", "hardware.key.x", "hardware.key.c", "hardware.key.v", "hardware.key.b",
        "hardware.key.n", "hardware.key.m", "hardware.keycode.55", "hardware.keycode.56",
        "hardware.keycode.76"
    };

    @Test
    public void dvorakTypesTheLettersItsOwnSoftPageShows() {
        // The soft page is 7/10/9: the top row gives its first three cells to Enter, backspace
        // and the period, which on a physical keyboard are ' , . — so the letters start at the
        // fourth US key. The bottom row drops the semicolon Dvorak has at its left end.
        assertEquals(
            softLetters(KeyboardLayoutId.EN_DVORAK, 0),
            typed(KeyboardLayoutId.EN_DVORAK, US_TOP, 3, 10));
        assertEquals(
            softLetters(KeyboardLayoutId.EN_DVORAK, 1),
            typed(KeyboardLayoutId.EN_DVORAK, US_HOME, 0, 10));
        assertEquals(
            softLetters(KeyboardLayoutId.EN_DVORAK, 2),
            typed(KeyboardLayoutId.EN_DVORAK, US_BOTTOM, 1, 10));
    }

    @Test
    public void colemakTypesTheLettersItsOwnSoftPageShows() {
        // Colemak's soft page is 9/10/7: the top row ends with backspace where the semicolon is,
        // and the bottom row stops at m, leaving , . / where QWERTY has them.
        assertEquals(
            softLetters(KeyboardLayoutId.EN_COLEMAK, 0),
            typed(KeyboardLayoutId.EN_COLEMAK, US_TOP, 0, 9));
        assertEquals(
            softLetters(KeyboardLayoutId.EN_COLEMAK, 1),
            typed(KeyboardLayoutId.EN_COLEMAK, US_HOME, 0, 10));
        assertEquals(
            softLetters(KeyboardLayoutId.EN_COLEMAK, 2),
            typed(KeyboardLayoutId.EN_COLEMAK, US_BOTTOM, 0, 7));
    }

    @Test
    public void shiftGivesTheCapitalOfWhateverTheKeyTypes() {
        HardwareSemanticMapper dvorak = HardwareLayoutTables.of(KeyboardLayoutId.EN_DVORAK);
        assertNotNull(dvorak);
        // US k is Dvorak's t.
        assertEquals("t", dvorak.map("hardware.key.k", false).text());
        assertEquals("T", dvorak.map("hardware.key.k", true).text());
        // US q is Dvorak's apostrophe, whose shift is the quote — not a capital letter.
        assertEquals("'", dvorak.map("hardware.key.q", false).text());
        assertEquals("\"", dvorak.map("hardware.key.q", true).text());
    }

    @Test
    public void aKeyTheTableDoesNotNameIsLeftAlone() {
        // Colemak moves no digit and no bracket, so those keys keep doing what they say.
        HardwareSemanticMapper colemak = HardwareLayoutTables.of(KeyboardLayoutId.EN_COLEMAK);
        assertNotNull(colemak);
        assertEquals(null, colemak.map("hardware.keycode.71", false));
        assertEquals(null, colemak.map("hardware.keycode.8", false));
    }

    /** What the table types across a run of US positions, as one string. */
    private static String typed(KeyboardLayoutId id, String[] positions, int from, int to) {
        HardwareSemanticMapper mapper = HardwareLayoutTables.of(id);
        assertNotNull("no table for " + id, mapper);
        StringBuilder typed = new StringBuilder();
        for (int index = from; index < to; index++) {
            SemanticInput input = mapper.map(positions[index], false);
            assertNotNull(positions[index] + " types nothing under " + id, input);
            typed.append(input.text());
        }
        return typed.toString();
    }

    /** The letters one row of a layout's soft page shows, in order. */
    private static String softLetters(KeyboardLayoutId id, int rowIndex) {
        KeyboardLayout page = KeyboardLayouts.of(id, false);
        List<String> letters = new ArrayList<>();
        for (SoftwareKeySpec key : page.rows().get(rowIndex)) {
            String label = key.label();
            if (label != null && label.length() == 1
                    && label.charAt(0) >= 'a' && label.charAt(0) <= 'z') {
                letters.add(label);
            }
        }
        StringBuilder joined = new StringBuilder();
        for (String letter : letters) {
            joined.append(letter);
        }
        return joined.toString();
    }
}
