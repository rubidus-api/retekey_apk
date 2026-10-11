package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The gap between keys, as a percentage of one cell's width. */
public final class KeyGapSettingsTest {
    @Test
    public void untilItIsSetTheGapIsTheOneTheKeyboardAlwaysHad() {
        // 4 dp a side, rounded the way the view rounded it, at the densities phones actually have.
        for (float density : new float[] {0.75f, 1.0f, 1.5f, 2.0f, 2.625f, 3.0f, 3.5f}) {
            assertEquals(Math.round(4.0f * density),
                KeyGapSettings.usualPx(KeyGapSettings.LEGACY_DEFAULT_PERCENT, density));
        }
    }

    @Test
    public void aGapSetUnderTheOldUnitIsKeptAsItWas() {
        // 0.2.2 stored a percentage of the 4 dp gap. That keyboard keeps the gap it was given
        // until its owner moves the new slider.
        assertEquals(0, KeyGapSettings.usualPx(0, 2.0f));
        assertEquals(4, KeyGapSettings.usualPx(50, 2.0f));
        assertEquals(24, KeyGapSettings.usualPx(300, 2.0f));
        assertEquals(24, KeyGapSettings.usualPx(10_000, 2.0f));
    }

    @Test
    public void thePercentageIsOfOneCellAndIsTheWholeSpaceBetweenTwoFaces() {
        // 20% of a 100 px cell is 20 px between two faces: 10 px taken from each side of a key.
        assertEquals(10, KeyGapSettings.sidePx(20, 100));
        assertEquals(0, KeyGapSettings.sidePx(0, 100));
        // A key two cells wide is not the measure: the cell is.
        assertEquals(5, KeyGapSettings.sidePx(10, 96));
    }

    @Test
    public void aStoredValueOutsideTheRangeIsBroughtIntoIt() {
        assertEquals(KeyGapSettings.MIN_PERCENT, KeyGapSettings.clamp(-5));
        assertEquals(KeyGapSettings.MAX_PERCENT, KeyGapSettings.clamp(10_000));
        assertEquals(13, KeyGapSettings.clamp(13));
        assertEquals(KeyGapSettings.sidePx(KeyGapSettings.MAX_PERCENT, 100),
            KeyGapSettings.sidePx(10_000, 100));
    }

    @Test
    public void theSliderStartsWhereTheGapAlreadyIs() {
        // A 6 px side on a 48 px cell is 12 px between faces: a quarter of the cell.
        assertEquals(25, KeyGapSettings.percentFor(6, 48));
        assertEquals(KeyGapSettings.MAX_PERCENT, KeyGapSettings.percentFor(60, 48));
        assertEquals(0, KeyGapSettings.percentFor(6, 0));
    }

    @Test
    public void aGapNeverTakesMoreThanHalfAKey() {
        // A short row: the face keeps half the cell, so it is never turned inside out, its label
        // still has somewhere to be, and there is still something to touch.
        int gap = KeyGapSettings.fit(24, 120, 40);
        assertEquals(10, gap);
        assertTrue(40 - 2 * gap >= 20);
        assertEquals(24, KeyGapSettings.fit(24, 120, 200));
        assertEquals(3, KeyGapSettings.fit(8, 20, 12));
    }

    @Test
    public void itsKeysAreRecognisedInEitherOrientation() {
        assertTrue(KeyGapSettings.isKey(
            ScreenOrientation.PORTRAIT.key(KeyGapSettings.KEY_CELL_PERCENT)));
        assertTrue(KeyGapSettings.isKey(
            ScreenOrientation.LANDSCAPE.key(KeyGapSettings.LEGACY_KEY_PERCENT)));
        assertFalse(KeyGapSettings.isKey(null));
        assertFalse(KeyGapSettings.isKey("height_percent.portrait"));
    }
}
