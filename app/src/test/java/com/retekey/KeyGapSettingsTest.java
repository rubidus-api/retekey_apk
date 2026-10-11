package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The gap drawn round each key, as a percentage of the gap the keyboard has always drawn. */
public final class KeyGapSettingsTest {
    @Test
    public void theDefaultIsTheGapTheKeyboardAlwaysHad() {
        // 4 dp, rounded the way the view rounded it, at the densities phones actually have.
        for (float density : new float[] {0.75f, 1.0f, 1.5f, 2.0f, 2.625f, 3.0f, 3.5f}) {
            assertEquals(Math.round(4.0f * density),
                KeyGapSettings.gapPx(KeyGapSettings.DEFAULT_PERCENT, density));
        }
    }

    @Test
    public void thePercentageScalesIt() {
        assertEquals(0, KeyGapSettings.gapPx(0, 2.0f));
        assertEquals(4, KeyGapSettings.gapPx(50, 2.0f));
        assertEquals(16, KeyGapSettings.gapPx(200, 2.0f));
        assertEquals(24, KeyGapSettings.gapPx(300, 2.0f));
    }

    @Test
    public void aStoredValueOutsideTheRangeIsBroughtIntoIt() {
        assertEquals(KeyGapSettings.MIN_PERCENT, KeyGapSettings.clamp(-5));
        assertEquals(KeyGapSettings.MAX_PERCENT, KeyGapSettings.clamp(10_000));
        assertEquals(130, KeyGapSettings.clamp(130));
    }

    @Test
    public void aWideGapNeverTakesMoreThanHalfASmallKey() {
        // A 300% gap on a short row: the face keeps half the cell, so it is never turned inside
        // out and its label still has somewhere to be.
        int gap = KeyGapSettings.fit(24, 8, 120, 40);
        assertEquals(10, gap);
        assertTrue(40 - 2 * gap >= 20);
        assertEquals(24, KeyGapSettings.fit(24, 8, 120, 200));
    }

    @Test
    public void theGapTheKeyboardAlwaysHadIsNeverCut() {
        // At the default and below, a tiny keyboard draws exactly what it drew before this
        // setting existed, however small its cells.
        assertEquals(8, KeyGapSettings.fit(8, 8, 20, 12));
        assertEquals(4, KeyGapSettings.fit(4, 8, 20, 12));
    }
}
