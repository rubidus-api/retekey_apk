package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The colour of choosing (owner's request, 2026-09-18): a hold's strip and the echo of what it
 * types must not be the same colour as a pressed key, or a hold landing on the wrong alternate
 * looks exactly like a key going down.
 */
public final class ChoiceHueTest {
    private static final int LIGHT_ACCENT = 0xFF1C5AA8;  // the light theme's held blue
    private static final int DARK_ACCENT = 0xFF5696E0;   // the dark theme's held blue

    @Test
    public void theChoosingColourIsADifferentHueNotADifferentShade() {
        for (int accent : new int[] {LIGHT_ACCENT, DARK_ACCENT}) {
            int choice = ChoiceHue.of(accent);
            assertTrue("a hue away from the accent", hueDistance(accent, choice) > 90.0f);
            assertTrue("and about as bright, so it reads in the same theme",
                Math.abs(value(accent) - value(choice)) < 0.2f);
            assertEquals("alpha is carried through", 0xFF, (choice >>> 24) & 0xFF);
        }
    }

    @Test
    public void aGreyThemeStillGetsAColour() {
        // A greyscale accent has no hue to turn, so the turn's own hue is used rather than
        // answering with the same grey the keys are already painted in.
        int grey = 0xFF808080;
        int choice = ChoiceHue.of(grey);
        assertTrue("not a grey any more",
            Math.abs(((choice >> 16) & 0xFF) - (choice & 0xFF)) > 20);
    }

    @Test
    public void theSoftVersionStaysCloseToTheFace() {
        int face = 0xFFFCFDFF;
        int soft = ChoiceHue.softOf(LIGHT_ACCENT, face);
        assertTrue("tinted, not replaced", value(soft) > 0.7f);
        assertTrue("but tinted towards the choosing hue, not the accent",
            hueDistance(soft, ChoiceHue.of(LIGHT_ACCENT)) < hueDistance(soft, LIGHT_ACCENT));
    }

    private static float value(int colour) {
        int r = (colour >> 16) & 0xFF;
        int g = (colour >> 8) & 0xFF;
        int b = colour & 0xFF;
        return Math.max(r, Math.max(g, b)) / 255.0f;
    }

    private static float hue(int colour) {
        float r = ((colour >> 16) & 0xFF) / 255.0f;
        float g = ((colour >> 8) & 0xFF) / 255.0f;
        float b = (colour & 0xFF) / 255.0f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        if (delta == 0.0f) {
            return 0.0f;
        }
        float hue;
        if (max == r) {
            hue = 60.0f * (((g - b) / delta) % 6.0f);
        } else if (max == g) {
            hue = 60.0f * (((b - r) / delta) + 2.0f);
        } else {
            hue = 60.0f * (((r - g) / delta) + 4.0f);
        }
        return hue < 0.0f ? hue + 360.0f : hue;
    }

    private static float hueDistance(int one, int other) {
        float difference = Math.abs(hue(one) - hue(other)) % 360.0f;
        return difference > 180.0f ? 360.0f - difference : difference;
    }
}
