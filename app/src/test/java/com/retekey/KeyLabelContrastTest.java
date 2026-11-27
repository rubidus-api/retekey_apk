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

    @Test
    public void inverseInkHasOppositePolarityFromOrdinaryText() {
        // Light theme: ordinary text is dark (28,30,34), so inverse must be light (false).
        assertFalse(KeyLabelContrast.inversePrefersDarkInk(28, 30, 34));
        // Dark theme: ordinary text is light (230,233,239), so inverse must be dark (true).
        assertTrue(KeyLabelContrast.inversePrefersDarkInk(230, 233, 239));

        // Light theme on deep blue fill (28,90,168): needs light inverse ink.
        assertTrue(KeyLabelContrast.useInverseInk(28, 90, 168, 28, 30, 34));
        // Light theme on pale armed blue (138,180,232): ordinary dark ink stays readable.
        assertFalse(KeyLabelContrast.useInverseInk(138, 180, 232, 28, 30, 34));

        // Dark theme on vivid blue fill (86,150,224): needs dark inverse ink.
        assertTrue(KeyLabelContrast.useInverseInk(86, 150, 224, 230, 233, 239));
        // Dark theme on deep armed blue (48,78,116): ordinary light ink stays readable.
        assertFalse(KeyLabelContrast.useInverseInk(48, 78, 116, 230, 233, 239));
    }
}
