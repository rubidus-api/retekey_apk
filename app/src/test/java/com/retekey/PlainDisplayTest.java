package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Issue #15: a keyboard for an E-Ink screen — one colour, and nothing that moves. */
public final class PlainDisplayTest {
    private static int r(int c) {
        return (c >> 16) & 0xFF;
    }

    private static int g(int c) {
        return (c >> 8) & 0xFF;
    }

    private static int b(int c) {
        return c & 0xFF;
    }

    private static double luminance(int c) {
        return KeyLabelContrast.relativeLuminance(r(c), g(c), b(c));
    }

    private static double contrast(int a, int b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    @Test
    public void everyColourIsAGrey() {
        for (boolean night : new boolean[] {false, true}) {
            for (int colour : PlainDisplay.monochrome(night)) {
                assertEquals(0xFF, (colour >>> 24) & 0xFF);
                assertEquals(r(colour), g(colour));
                assertEquals(g(colour), b(colour));
            }
        }
    }

    @Test
    public void labelsAndOutlinesAreFullContrast() {
        for (boolean night : new boolean[] {false, true}) {
            int[] c = PlainDisplay.monochrome(night);
            assertEquals(21.0, contrast(c[PlainDisplay.KEY_TEXT], c[PlainDisplay.KEY_FACE]), 0.01);
            assertEquals(21.0, contrast(c[PlainDisplay.EDGE], c[PlainDisplay.KEY_FACE]), 0.01);
            // A held key is the face turned over, and its label the face's own colour.
            assertEquals(c[PlainDisplay.KEY_TEXT], c[PlainDisplay.HELD]);
        }
    }

    @Test
    public void armedAndHeldCanBeToldApart() {
        for (boolean night : new boolean[] {false, true}) {
            int[] c = PlainDisplay.monochrome(night);
            assertNotEquals(c[PlainDisplay.HELD], c[PlainDisplay.ARMED]);
            assertNotEquals(c[PlainDisplay.KEY_FACE], c[PlainDisplay.ARMED]);
            assertNotEquals(c[PlainDisplay.CHOICE], c[PlainDisplay.HELD]);
        }
    }

    @Test
    public void lightIsWhiteKeysWithBlackInk() {
        int[] c = PlainDisplay.monochrome(false);
        assertEquals(0xFFFFFFFF, c[PlainDisplay.KEY_FACE]);
        assertEquals(0xFFFFFFFF, c[PlainDisplay.BACKGROUND]);
        assertEquals(0xFF000000, c[PlainDisplay.KEY_TEXT]);
    }

    @Test
    public void stillWhenAskedOrWhenAndroidRemovesAnimations() {
        assertFalse(PlainDisplay.still(false, 1.0f));
        assertTrue(PlainDisplay.still(true, 1.0f));
        assertTrue(PlainDisplay.still(false, 0.0f));
        assertFalse(PlainDisplay.still(false, 0.5f));
    }
}
