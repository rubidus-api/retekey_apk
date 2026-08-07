package com.retekey;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * The keyboard height, in percent of the screen — the unit the user sets and the one the slider
 * shows. The old setting was a level standing for a multiple of a nominal row height, which named
 * a different share of the screen on every device; these tests pin the meaning of the new one.
 */
public final class KeyboardHeightPercentTest {

    @Test
    public void theRangeIsOneToFiftyPercent() {
        assertEquals(1, KeyboardHeightPercent.MIN_PERCENT);
        assertEquals(50, KeyboardHeightPercent.MAX_PERCENT);
        assertEquals(1, KeyboardHeightPercent.clamp(0));
        assertEquals(1, KeyboardHeightPercent.clamp(-7));
        assertEquals(50, KeyboardHeightPercent.clamp(51));
        assertEquals(30, KeyboardHeightPercent.clamp(30));
    }

    @Test
    public void thePercentageIsOfTheScreenHeight() {
        assertEquals(500, KeyboardHeightPercent.heightPx(25, 2000));
        assertEquals(1000, KeyboardHeightPercent.heightPx(50, 2000));
        assertEquals(20, KeyboardHeightPercent.heightPx(1, 2000));
        // Out-of-range percentages are clamped before they measure anything.
        assertEquals(1000, KeyboardHeightPercent.heightPx(90, 2000));
        // An unknown screen measures nothing rather than dividing by it.
        assertEquals(0, KeyboardHeightPercent.heightPx(25, 0));
    }

    @Test
    public void aHeightReadsBackAsThePercentageItWasSetTo() {
        for (int percent = 1; percent <= 50; percent++) {
            int height = KeyboardHeightPercent.heightPx(percent, 2340);
            assertEquals(percent, KeyboardHeightPercent.percentForHeight(height, 2340));
        }
    }

    /** Upright, the default is a quarter of the screen, because the screen is the long edge. */
    @Test
    public void theDefaultUprightIsAQuarterOfTheScreen() {
        assertEquals(25, KeyboardHeightPercent.defaultPercent(1920, 1920));
        assertEquals(25, KeyboardHeightPercent.defaultPercent(2340, 2340));
    }

    /**
     * Sideways the screen is the short edge, so the same keyboard is a larger share of it — the
     * point being that it stays the same size in the hand.
     */
    @Test
    public void theDefaultSidewaysIsTheSameKeyboardOnAShorterScreen() {
        assertEquals(44, KeyboardHeightPercent.defaultPercent(1080, 1920));
        // On a very long screen a quarter of the long edge would be more than half the short one,
        // and the range stops it at half.
        assertEquals(50, KeyboardHeightPercent.defaultPercent(800, 2400));
    }

    @Test
    public void anUnknownScreenFallsBackToAQuarter() {
        assertEquals(25, KeyboardHeightPercent.defaultPercent(0, 1920));
        assertEquals(25, KeyboardHeightPercent.defaultPercent(1920, 0));
        assertEquals(25, KeyboardHeightPercent.percentForHeight(400, 0));
    }

    /**
     * A keyboard set under the old unit keeps the size it had. The old height was four rows of a
     * nominal 58dp times the stored scale, so at density 2 the default scale of 1.0 was 464px —
     * which on a 1920px screen is the 24% it was already occupying.
     */
    @Test
    public void anOldSettingConvertsToTheShareItWasAlreadyTaking() {
        assertEquals(24, KeyboardHeightPercent.fromLegacyScale(1.0f, 2.0f, 1920));
        assertEquals(48, KeyboardHeightPercent.fromLegacyScale(2.0f, 2.0f, 1920));
        assertEquals(12, KeyboardHeightPercent.fromLegacyScale(0.5f, 2.0f, 1920));
        // Nothing stored, or a screen we cannot measure: the ordinary quarter.
        assertEquals(25, KeyboardHeightPercent.fromLegacyScale(Float.NaN, 2.0f, 1920));
        assertEquals(25, KeyboardHeightPercent.fromLegacyScale(1.0f, 0.0f, 1920));
        assertEquals(25, KeyboardHeightPercent.fromLegacyScale(1.0f, 2.0f, 0));
    }

    @Test
    public void theMenuSizeKeysMoveTwoPointsAtATime() {
        assertEquals(2, KeyboardHeightPercent.STEP_PERCENT);
    }
}
