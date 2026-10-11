package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The action bar's own height, as a percentage of the 40 dp it has always been. */
public final class ActionBarHeightSettingsTest {
    @Test
    public void theDefaultIsTheHeightTheBarAlwaysHad() {
        for (float density : new float[] {0.75f, 1.0f, 1.5f, 2.0f, 2.625f, 3.0f, 3.5f}) {
            assertEquals(Math.round(40 * density),
                ActionBarHeightSettings.heightPx(ActionBarHeightSettings.DEFAULT_PERCENT, density));
        }
    }

    @Test
    public void thePercentageScalesIt() {
        assertEquals(40, ActionBarHeightSettings.heightPx(50, 2.0f));
        assertEquals(120, ActionBarHeightSettings.heightPx(150, 2.0f));
        assertEquals(160, ActionBarHeightSettings.heightPx(200, 2.0f));
    }

    @Test
    public void aStoredValueOutsideTheRangeIsBroughtIntoIt() {
        assertEquals(ActionBarHeightSettings.MIN_PERCENT, ActionBarHeightSettings.clamp(0));
        assertEquals(ActionBarHeightSettings.MAX_PERCENT, ActionBarHeightSettings.clamp(999));
    }

    @Test
    public void aShortBarShrinksItsLabelsToFit() {
        // Half height at density 2: a 40 px bar less its margins is a 28 px face; a 30 px label
        // would be cut, so it comes down to seven tenths of the face.
        assertEquals(19.6f, ActionBarHeightSettings.labelPx(50, 30.0f, 28), 0.01f);
        // Already small enough: left alone.
        assertEquals(12.0f, ActionBarHeightSettings.labelPx(50, 12.0f, 28), 0.01f);
    }

    @Test
    public void atTheDefaultAndAboveTheLabelIsWhatItWas() {
        assertEquals(30.0f, ActionBarHeightSettings.labelPx(100, 30.0f, 28), 0.0f);
        assertEquals(30.0f, ActionBarHeightSettings.labelPx(200, 30.0f, 148), 0.0f);
    }

    @Test
    public void itsKeyIsRecognisedInEitherOrientation() {
        assertTrue(ActionBarHeightSettings.isKey(
            ScreenOrientation.PORTRAIT.key(ActionBarHeightSettings.KEY_PERCENT)));
        assertTrue(ActionBarHeightSettings.isKey(
            ScreenOrientation.LANDSCAPE.key(ActionBarHeightSettings.KEY_PERCENT)));
        assertFalse(ActionBarHeightSettings.isKey(null));
        assertFalse(ActionBarHeightSettings.isKey(
            ScreenOrientation.PORTRAIT.key(KeyGapSettings.KEY_PERCENT)));
    }
}
