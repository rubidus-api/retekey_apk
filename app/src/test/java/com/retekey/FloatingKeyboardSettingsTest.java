package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The floating panel's opacity: a percentage the user sets, an alpha the panel paints with. */
public final class FloatingKeyboardSettingsTest {
    @Test
    public void opacityIsHeldAboveThePointItStopsBeingReadable() {
        assertEquals(FloatingKeyboardSettings.MIN_OPACITY_PERCENT,
            FloatingKeyboardSettings.clampOpacity(0));
        assertEquals(FloatingKeyboardSettings.MIN_OPACITY_PERCENT,
            FloatingKeyboardSettings.clampOpacity(-40));
        assertEquals(FloatingKeyboardSettings.MAX_OPACITY_PERCENT,
            FloatingKeyboardSettings.clampOpacity(150));
        assertEquals(60, FloatingKeyboardSettings.clampOpacity(60));
    }

    @Test
    public void aPercentageBecomesAnAlpha() {
        assertEquals(255, FloatingKeyboardSettings.alphaOf(100));
        assertEquals(128, FloatingKeyboardSettings.alphaOf(50));
        // Out-of-range values are clamped before they are converted, never wrapped.
        assertEquals(FloatingKeyboardSettings.alphaOf(FloatingKeyboardSettings.MIN_OPACITY_PERCENT),
            FloatingKeyboardSettings.alphaOf(0));
    }

    @Test
    public void theDefaultIsClearlySeeThroughAndTheSameInBothOrientations() {
        assertEquals("the owner's default", 40, FloatingKeyboardSettings.DEFAULT_OPACITY_PERCENT);
        int alpha = FloatingKeyboardSettings.alphaOf(FloatingKeyboardSettings.DEFAULT_OPACITY_PERCENT);
        assertTrue("plainly lets the app through", alpha < 128);
        assertTrue("not so faint the keys vanish", alpha > 64);
        // One constant feeds both orientations, so neither can drift from the other.
        for (ScreenOrientation orientation : ScreenOrientation.values()) {
            assertEquals(orientation.toString(),
                FloatingKeyboardSettings.DEFAULT_OPACITY_PERCENT,
                FloatingKeyboardSettings.opacityPercent(null, orientation));
        }
    }

    @Test
    public void thePadFloatsWithItsOwnPlaceButTheSameSeeThroughness() {
        // Geometry is per-panel: the code-point pad is a different size and sits elsewhere.
        assertTrue("the pad's own slot", FloatingKeyboardSettings.UNICODE_PREFIX.length() > 0);
        assertEquals(null, FloatingKeyboardSettings.load(null,
            FloatingKeyboardSettings.UNICODE_PREFIX));
        // Opacity is not: one preference answers how see-through a floating panel is.
        assertTrue("opacity is unprefixed",
            !FloatingKeyboardSettings.KEY_OPACITY.startsWith(
                FloatingKeyboardSettings.UNICODE_PREFIX));
    }

    @Test
    public void portraitAndLandscapeBoundsAreStoredSeparately() {
        LockedPreferences prefs = new LockedPreferences();
        FloatingKeyboardBounds portrait = FloatingKeyboardBounds.of(
            1080, 2400, FloatingKeyboardBounds.Side.RIGHT, 40, 1400, 1000, 700);
        FloatingKeyboardBounds landscape = FloatingKeyboardBounds.of(
            2400, 1080, FloatingKeyboardBounds.Side.LEFT, 20, 400, 900, 500);

        FloatingKeyboardSettings.store(prefs, ScreenOrientation.PORTRAIT, portrait);
        FloatingKeyboardSettings.store(prefs, ScreenOrientation.LANDSCAPE, landscape);

        FloatingKeyboardBounds loadedPortrait =
            FloatingKeyboardSettings.load(prefs, ScreenOrientation.PORTRAIT);
        FloatingKeyboardBounds loadedLandscape =
            FloatingKeyboardSettings.load(prefs, ScreenOrientation.LANDSCAPE);
        assertEquals(1000, loadedPortrait.width());
        assertEquals(1080, loadedPortrait.screenWidth());
        assertEquals(900, loadedLandscape.width());
        assertEquals(2400, loadedLandscape.screenWidth());

        // Per-prefix oriented storage works too, and falls back to legacy before first oriented write.
        FloatingKeyboardSettings.store(prefs, FloatingKeyboardSettings.HANJA_PREFIX, portrait);
        assertEquals(1000, FloatingKeyboardSettings.load(
            prefs, FloatingKeyboardSettings.HANJA_PREFIX, ScreenOrientation.LANDSCAPE).width());
        FloatingKeyboardSettings.store(
            prefs, FloatingKeyboardSettings.HANJA_PREFIX, ScreenOrientation.LANDSCAPE, landscape);
        assertEquals(900, FloatingKeyboardSettings.load(
            prefs, FloatingKeyboardSettings.HANJA_PREFIX, ScreenOrientation.LANDSCAPE).width());
        assertEquals(1000, FloatingKeyboardSettings.load(
            prefs, FloatingKeyboardSettings.HANJA_PREFIX, ScreenOrientation.PORTRAIT).width());
    }
}
