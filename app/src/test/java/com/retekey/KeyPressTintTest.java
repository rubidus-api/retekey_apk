package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The pressed key's face. It has to change visibly in both themes: brighter where the face has
 * room to be brighter, and tinted where it is already white and has none.
 */
public final class KeyPressTintTest {
    private static final int ACCENT = 0xFF3F51B5;

    private static int red(int c) { return (c >> 16) & 0xFF; }
    private static int green(int c) { return (c >> 8) & 0xFF; }
    private static int blue(int c) { return c & 0xFF; }
    private static int luma(int c) { return red(c) * 299 + green(c) * 587 + blue(c) * 114; }

    @Test
    public void aDarkFaceGetsBrighter() {
        int dark = 0xFF202124;
        int pressed = KeyPressTint.pressed(dark, ACCENT);
        assertTrue("pressed should be lighter than the face", luma(pressed) > luma(dark));
    }

    @Test
    public void aWhiteFaceStillChanges() {
        int white = 0xFFFFFFFF;
        int pressed = KeyPressTint.pressed(white, ACCENT);
        assertTrue("a white face cannot brighten, so it must tint", pressed != white);
        assertTrue("and it tints toward the accent", blue(pressed) > red(pressed));
    }

    @Test
    public void alphaSurvives() {
        assertEquals(0xFF, (KeyPressTint.pressed(0xFF102030, ACCENT) >>> 24) & 0xFF);
        assertEquals(0x80, (KeyPressTint.pressed(0x80102030, ACCENT) >>> 24) & 0xFF);
    }

    @Test
    public void mixingIsProportionalAndClamped() {
        assertEquals(0xFF000000, KeyPressTint.mix(0xFF000000, 0xFFFFFFFF, 0.0f));
        assertEquals(0xFFFFFFFF, KeyPressTint.mix(0xFF000000, 0xFFFFFFFF, 1.0f));
        assertEquals(0xFF808080, KeyPressTint.mix(0xFF000000, 0xFFFFFFFF, 0.502f));
        // Out-of-range amounts are clamped rather than overshooting the endpoints.
        assertEquals(0xFF000000, KeyPressTint.mix(0xFF000000, 0xFFFFFFFF, -1.0f));
        assertEquals(0xFFFFFFFF, KeyPressTint.mix(0xFF000000, 0xFFFFFFFF, 2.0f));
    }
}
