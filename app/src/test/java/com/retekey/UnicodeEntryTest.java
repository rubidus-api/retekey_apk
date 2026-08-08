package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Typing a character by its code point: what the digits mean, and what they never mean. */
public final class UnicodeEntryTest {

    @Test
    public void digitsBuildACodePoint() {
        UnicodeEntry entry = UnicodeEntry.empty().append('a').append('c').append('0').append('0');
        assertEquals("AC00", entry.digits());
        assertEquals("U+AC00", entry.display());
        assertEquals(0xAC00, entry.codePoint());
        assertEquals("가", entry.character());
    }

    @Test
    public void anythingThatIsNotAHexDigitIsIgnored() {
        UnicodeEntry entry = UnicodeEntry.empty().append('4').append('한').append('!').append('1');
        assertEquals("41", entry.digits());
        assertEquals("A", entry.character());
    }

    @Test
    public void sixDigitsIsTheLimit() {
        UnicodeEntry entry = UnicodeEntry.empty();
        for (char c : "10FFFF9".toCharArray()) {
            entry = entry.append(c);
        }
        assertEquals("10FFFF", entry.digits());
        assertEquals(0x10FFFF, entry.codePoint());
    }

    @Test
    public void backspaceTakesOneBackAndStopsAtEmpty() {
        UnicodeEntry entry = UnicodeEntry.empty().append('2').append('6').backspace();
        assertEquals("2", entry.digits());
        assertTrue(entry.backspace().isEmpty());
        assertTrue(entry.backspace().backspace().isEmpty());
    }

    @Test
    public void anEmptyEntryNamesNothing() {
        UnicodeEntry entry = UnicodeEntry.empty();
        assertTrue(entry.isEmpty());
        assertEquals(-1, entry.codePoint());
        assertNull(entry.character());
        assertEquals("U+", entry.display());
    }

    /** Surrogate halves are an encoding detail, not characters anyone can type. */
    @Test
    public void surrogatesAndOutOfRangeValuesAreRefused() {
        assertFalse(UnicodeEntry.isTypable(0xD800));
        assertFalse(UnicodeEntry.isTypable(0xDFFF));
        assertFalse(UnicodeEntry.isTypable(0x110000));
        assertFalse(UnicodeEntry.isTypable(-1));
        assertTrue(UnicodeEntry.isTypable(0x10FFFF));
        assertTrue(UnicodeEntry.isTypable(0x41));

        UnicodeEntry surrogate = UnicodeEntry.empty().append('d').append('8').append('0').append('0');
        assertEquals(-1, surrogate.codePoint());
        assertNull(surrogate.character());
    }

    @Test
    public void aCodePointIsLabelledTheWayTheStandardWritesIt() {
        assertEquals("U+0041", UnicodeEntry.label(0x41));
        assertEquals("U+AC00", UnicodeEntry.label(0xAC00));
        assertEquals("U+1F600", UnicodeEntry.label(0x1F600));
        assertEquals("U+2605", UnicodeEntry.labelOf("★"));
        // A character outside the basic plane is labelled by its whole code point, not its halves.
        assertEquals("U+1F600", UnicodeEntry.labelOf("😀"));
        assertEquals("", UnicodeEntry.labelOf(""));
        assertEquals("", UnicodeEntry.labelOf(null));
    }

    @Test
    public void hexDigitsAreTheOnlyDigits() {
        assertTrue(UnicodeEntry.isHexDigit('0'));
        assertTrue(UnicodeEntry.isHexDigit('9'));
        assertTrue(UnicodeEntry.isHexDigit('a'));
        assertTrue(UnicodeEntry.isHexDigit('F'));
        assertFalse(UnicodeEntry.isHexDigit('g'));
        assertFalse(UnicodeEntry.isHexDigit(' '));
        assertFalse(UnicodeEntry.isHexDigit('ㄱ'));
    }
}
