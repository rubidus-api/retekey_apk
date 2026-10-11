package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The action bar's own height, as a percentage of the screen's height. */
public final class ActionBarHeightSettingsTest {
    @Test
    public void untilItIsSetTheBarIsTheHeightItAlwaysWas() {
        for (float density : new float[] {0.75f, 1.0f, 1.5f, 2.0f, 2.625f, 3.0f, 3.5f}) {
            assertEquals(Math.round(40 * density), ActionBarHeightSettings.usualPx(
                ActionBarHeightSettings.LEGACY_DEFAULT_PERCENT, density));
        }
    }

    @Test
    public void aHeightSetUnderTheOldUnitIsKeptAsItWas() {
        // 0.2.2 stored a percentage of the 40 dp bar.
        assertEquals(40, ActionBarHeightSettings.usualPx(50, 2.0f));
        assertEquals(120, ActionBarHeightSettings.usualPx(150, 2.0f));
        assertEquals(160, ActionBarHeightSettings.usualPx(999, 2.0f));
        assertEquals(40, ActionBarHeightSettings.usualPx(0, 2.0f));
    }

    @Test
    public void thePercentageIsOfTheScreensHeight() {
        assertEquals(80, ActionBarHeightSettings.heightPx(10, 800));
        assertEquals(24, ActionBarHeightSettings.heightPx(5, 480));
        assertEquals(0, ActionBarHeightSettings.heightPx(10, 0));
    }

    @Test
    public void aStoredValueOutsideTheRangeIsBroughtIntoIt() {
        assertEquals(ActionBarHeightSettings.MIN_PERCENT, ActionBarHeightSettings.clamp(0));
        assertEquals(ActionBarHeightSettings.MAX_PERCENT, ActionBarHeightSettings.clamp(999));
        assertEquals(ActionBarHeightSettings.heightPx(ActionBarHeightSettings.MAX_PERCENT, 800),
            ActionBarHeightSettings.heightPx(999, 800));
    }

    @Test
    public void theSliderStartsWhereTheBarAlreadyIs() {
        // The 60 px bar of an 800 px screen is 7.5% of it; of the same screen sideways, 12.5%.
        assertEquals(8, ActionBarHeightSettings.percentFor(60, 800));
        assertEquals(13, ActionBarHeightSettings.percentFor(60, 480));
        assertEquals(ActionBarHeightSettings.MIN_PERCENT, ActionBarHeightSettings.percentFor(60, 0));
    }

    @Test
    public void theBarLeavesTheKeysHalfTheKeyboard() {
        // The bar is inside the keyboard's height. However tall it is set, the keys keep half.
        assertEquals(60, ActionBarHeightSettings.within(60, 200));
        assertEquals(100, ActionBarHeightSettings.within(160, 200));
        assertEquals(0, ActionBarHeightSettings.within(60, 0));
    }

    @Test
    public void aShortBarShrinksItsLabelsToFit() {
        // A 40 px bar where 80 px is usual: less its margins it is a 28 px face; a 30 px label
        // would be cut, so it comes down to seven tenths of the face.
        assertEquals(19.6f, ActionBarHeightSettings.labelPx(40, 80, 30.0f, 28), 0.01f);
        // Already small enough: left alone.
        assertEquals(12.0f, ActionBarHeightSettings.labelPx(40, 80, 12.0f, 28), 0.01f);
    }

    @Test
    public void atItsUsualHeightAndAboveTheLabelIsWhatItWas() {
        assertEquals(30.0f, ActionBarHeightSettings.labelPx(80, 80, 30.0f, 28), 0.0f);
        assertEquals(30.0f, ActionBarHeightSettings.labelPx(160, 80, 30.0f, 148), 0.0f);
    }

    @Test
    public void itsKeysAreRecognisedInEitherOrientation() {
        assertTrue(ActionBarHeightSettings.isKey(
            ScreenOrientation.PORTRAIT.key(ActionBarHeightSettings.KEY_SCREEN_PERCENT)));
        assertTrue(ActionBarHeightSettings.isKey(
            ScreenOrientation.LANDSCAPE.key(ActionBarHeightSettings.LEGACY_KEY_PERCENT)));
        assertFalse(ActionBarHeightSettings.isKey(null));
        assertFalse(ActionBarHeightSettings.isKey(
            ScreenOrientation.PORTRAIT.key(KeyGapSettings.KEY_CELL_PERCENT)));
    }
}
