package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The 1-50 height the user sets, and the scale the keyboard measures with. */
public final class KeyboardHeightLevelTest {
    @Test
    public void theRangeIsOneToFifty() {
        assertEquals(1, KeyboardHeightScale.MIN_LEVEL);
        assertEquals(50, KeyboardHeightScale.MAX_LEVEL);
        assertEquals(1, KeyboardHeightScale.clampLevel(0));
        assertEquals(1, KeyboardHeightScale.clampLevel(-9));
        assertEquals(50, KeyboardHeightScale.clampLevel(51));
    }

    @Test
    public void theMiddleOfTheRangeIsTheSizeItAlwaysWas() {
        assertEquals(25, KeyboardHeightScale.DEFAULT_LEVEL);
        assertEquals(1.0f, KeyboardHeightScale.scaleForLevel(25), 0.0001f);
        assertEquals(1.0f, KeyboardHeightScale.DEFAULT_SCALE, 0.0001f);
        // And the ends are what they say: fifty is twice the middle.
        assertEquals(2.0f, KeyboardHeightScale.scaleForLevel(50), 0.0001f);
        assertEquals(0.04f, KeyboardHeightScale.scaleForLevel(1), 0.0001f);
    }

    @Test
    public void everyLevelSurvivesTheRoundTrip() {
        for (int level = KeyboardHeightScale.MIN_LEVEL;
                level <= KeyboardHeightScale.MAX_LEVEL; level++) {
            assertEquals(level,
                KeyboardHeightScale.levelForScale(KeyboardHeightScale.scaleForLevel(level)));
        }
    }

    @Test
    public void aScaleFromBeforeTheLevelsLandsOnTheNearestOne() {
        // Heights stored as free-floating scales still show a sensible number on the slider.
        assertEquals(25, KeyboardHeightScale.levelForScale(1.0f));
        assertEquals(16, KeyboardHeightScale.levelForScale(0.65f));
        assertEquals(44, KeyboardHeightScale.levelForScale(1.75f));
        assertEquals(25, KeyboardHeightScale.levelForScale(Float.NaN));
    }

    @Test
    public void aTallerLevelIsATallerKeyboard() {
        int base = KeyboardHeightScale.baseHeightPx(4, 2.0f);
        int previous = 0;
        for (int level = KeyboardHeightScale.MIN_LEVEL;
                level <= KeyboardHeightScale.MAX_LEVEL; level++) {
            int height = KeyboardHeightScale.heightForScale(
                KeyboardHeightScale.scaleForLevel(level), base);
            assertTrue("level " + level, height > previous);
            previous = height;
        }
    }
}
