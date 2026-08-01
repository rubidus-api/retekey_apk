package dev.hellgates.retekeyime;

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
    public void theDefaultIsTranslucentButReadable() {
        int alpha = FloatingKeyboardSettings.alphaOf(FloatingKeyboardSettings.DEFAULT_OPACITY_PERCENT);
        assertTrue("lets some of the app through", alpha < 255);
        assertTrue("still readable", alpha > 200);
    }
}
