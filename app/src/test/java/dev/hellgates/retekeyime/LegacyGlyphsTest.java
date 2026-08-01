package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** A key that draws nothing is worse than a key that draws a word. */
public final class LegacyGlyphsTest {
    @Test
    public void aModernDeviceKeepsTheGlyph() {
        assertEquals("☰", LegacyGlyphs.label("☰", LegacyGlyphs.GLYPHS_FROM));
        assertEquals("☰", LegacyGlyphs.label("☰", 33));
        assertEquals("⌫", LegacyGlyphs.label("⌫", 28));
    }

    @Test
    public void anOldDeviceGetsAWordInstead() {
        assertEquals("Menu", LegacyGlyphs.label("☰", LegacyGlyphs.GLYPHS_FROM - 1));
        assertEquals("Copy", LegacyGlyphs.label("⧉", 19));
        assertEquals("Bksp", LegacyGlyphs.label("⌫", 14));
        assertEquals("Shift", LegacyGlyphs.label("⇧", 14));
    }

    @Test
    public void aLabelWithNoSubstituteIsLeftAlone() {
        // Hangul and Latin are in every font the app will ever meet.
        assertEquals("ㅂ", LegacyGlyphs.label("ㅂ", 14));
        assertEquals("space", LegacyGlyphs.label("space", 14));
        assertEquals("ㆍ", LegacyGlyphs.label("ㆍ", 14));
        assertNull(LegacyGlyphs.label(null, 14));
    }
}
