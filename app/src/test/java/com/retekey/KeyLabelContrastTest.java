package com.retekey;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * A held key is painted in the strongest colour the theme has, and "strongest" is dark in one
 * theme and vivid in the other. The label has to follow the fill, or it disappears into it.
 */
public final class KeyLabelContrastTest {
    @Test
    public void aPaleFillTakesDarkInk() {
        assertTrue(KeyLabelContrast.prefersDarkInk(255, 255, 255));
        assertTrue("the light theme's armed blue", KeyLabelContrast.prefersDarkInk(138, 180, 232));
    }

    @Test
    public void aDeepFillTakesLightInk() {
        assertFalse(KeyLabelContrast.prefersDarkInk(0, 0, 0));
        assertFalse("the light theme's held blue", KeyLabelContrast.prefersDarkInk(28, 90, 168));
        assertFalse("the dark theme's armed blue", KeyLabelContrast.prefersDarkInk(48, 78, 116));
    }

    @Test
    public void theDarkThemesHeldBlueIsLightEnoughToNeedDarkInk() {
        assertTrue(KeyLabelContrast.prefersDarkInk(86, 150, 224));
    }

    @Test
    public void greenWeighsMostAndBlueLeast() {
        double green = KeyLabelContrast.relativeLuminance(0, 200, 0);
        double red = KeyLabelContrast.relativeLuminance(200, 0, 0);
        double blue = KeyLabelContrast.relativeLuminance(0, 0, 200);
        assertTrue(green > red);
        assertTrue(red > blue);
    }

    @Test
    public void theTwoStatesOfALatchFallOnOppositeSidesInEachTheme() {
        // Light theme: armed is light, held is deep — so they cannot be confused for each other.
        assertTrue(KeyLabelContrast.relativeLuminance(138, 180, 232)
            > KeyLabelContrast.relativeLuminance(28, 90, 168) + 0.3);
        // Dark theme: held is the vivid one.
        assertTrue(KeyLabelContrast.relativeLuminance(86, 150, 224)
            > KeyLabelContrast.relativeLuminance(48, 78, 116) + 0.15);
    }

    @Test(expected = IllegalArgumentException.class)
    public void aChannelOutsideAByteIsRejected() {
        KeyLabelContrast.relativeLuminance(0, 256, 0);
    }
}
