package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** A US physical keyboard typing Persian, on the Windows Persian layout's positions. */
public final class PersianHardwareMapperTest {
    private static final PersianHardwareMapper MAPPER = PersianHardwareMapper.INSTANCE;

    @Test
    public void theLetterRowsSitOnTheirWindowsPositions() {
        assertEquals("ض", MAPPER.map("hardware.key.q", false).text());
        assertEquals("ش", MAPPER.map("hardware.key.a", false).text());
        assertEquals("ا", MAPPER.map("hardware.key.h", false).text());
        assertEquals("م", MAPPER.map("hardware.key.l", false).text());
        assertEquals("و", MAPPER.map("hardware.keycode.55", false).text());
        assertEquals("ک", MAPPER.map("hardware.keycode.74", false).text());
        assertEquals("پ", MAPPER.map("hardware.keycode.73", false).text());
    }

    @Test
    public void theShiftLayerCarriesTheRareTwinsAndMarks() {
        assertEquals("ژ", MAPPER.map("hardware.key.c", true).text());
        assertEquals("آ", MAPPER.map("hardware.key.h", true).text());
        assertEquals("ء", MAPPER.map("hardware.key.m", true).text());
        assertEquals("،", MAPPER.map("hardware.key.t", true).text());
        assertEquals("«", MAPPER.map("hardware.key.k", true).text());
        assertEquals("؟", MAPPER.map("hardware.keycode.76", true).text());
    }

    @Test
    public void shiftSpaceIsTheHalfSpace() {
        assertEquals("‌", MAPPER.map("hardware.keycode.62", true).text());
        assertNull("a plain space is the editor's own", MAPPER.map("hardware.keycode.62", false));
    }

    @Test
    public void unmappedKeysAreLeftToTheEditor() {
        assertNull(MAPPER.map("hardware.key.q", false) == null ? "x" : null); // q maps
        assertNull(MAPPER.map("hardware.keycode.7", false));
        assertNull(MAPPER.map(null, false));
    }
}
