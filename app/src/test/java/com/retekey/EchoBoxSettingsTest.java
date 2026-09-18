package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The echo box's own settings: whether it is drawn, and how much shows through it. */
public final class EchoBoxSettingsTest {
    @Test
    public void opacityStaysBetweenAGhostAndSolid() {
        assertEquals(EchoBoxSettings.MIN_OPACITY, EchoBoxSettings.clampOpacity(0));
        assertEquals(EchoBoxSettings.MIN_OPACITY, EchoBoxSettings.clampOpacity(-40));
        assertEquals(EchoBoxSettings.MAX_OPACITY, EchoBoxSettings.clampOpacity(140));
        assertEquals(60, EchoBoxSettings.clampOpacity(60));
    }

    @Test
    public void theAlphaIsThePercentage() {
        assertEquals(255, EchoBoxSettings.alphaOf(100));
        assertEquals(128, EchoBoxSettings.alphaOf(50));
        assertEquals(EchoBoxSettings.alphaOf(EchoBoxSettings.MIN_OPACITY),
            EchoBoxSettings.alphaOf(5));
    }

    @Test
    public void theColourKeepsItsHueAndTakesTheAlpha() {
        int painted = EchoBoxSettings.withOpacity(0xFF1C5AA8, 50);
        assertEquals(0x1C5AA8, painted & 0x00FFFFFF);
        assertEquals(128, (painted >>> 24) & 0xFF);
        assertTrue("the keys read through it", ((painted >>> 24) & 0xFF) < 255);
    }

    @Test
    public void theCharacterStaysReadableHoweverFaintTheBoxIs() {
        // A see-through box is for reading the keys under it; the box is for reading the
        // character. At 35 % both faded together and neither could be read (emulator, 2026-09-18).
        assertEquals(EchoBoxSettings.MIN_INK_OPACITY, EchoBoxSettings.inkOpacity(20));
        assertEquals(EchoBoxSettings.MIN_INK_OPACITY, EchoBoxSettings.inkOpacity(35));
        assertEquals(100, EchoBoxSettings.inkOpacity(100));
        assertTrue("the ink is never fainter than the box",
            EchoBoxSettings.inkOpacity(35) > EchoBoxSettings.clampOpacity(35));
    }

    @Test
    public void theBoxIsOnUntilSomebodyTurnsItOff() {
        assertTrue(EchoBoxSettings.DEFAULT_ENABLED);
        assertEquals(EchoBoxSettings.DEFAULT_OPACITY,
            EchoBoxSettings.clampOpacity(EchoBoxSettings.DEFAULT_OPACITY));
    }
}
