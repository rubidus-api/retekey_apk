package com.retekey;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Issue #2: the settings screens' first rows sat under the title bar on One UI 8.5 and could not be
 * scrolled to. The numbers are read off the reporter's 1080×2340 screenshot — status bar and title
 * bar together reach about 280px down, the content began at 0 — and off the project's emulator and
 * the reporter's Xiaomi, where the framework already placed the content below the bar.
 */
public final class ScreenFitTest {
    @Test
    public void contentLaidOutUnderTheTitleBarIsPaddedDownPastIt() {
        // One UI 8.5: the frame begins at the top of the window, the bar ends 280px down.
        assertEquals(280, ScreenFit.topOverlap(280, 0));
    }

    @Test
    public void contentAlreadyPlacedBelowTheBarIsLeftAlone() {
        // Xiaomi on PixelOS, and the emulator: the frame begins exactly where the bar ends.
        assertEquals(0, ScreenFit.topOverlap(280, 280));
        assertEquals(0, ScreenFit.topOverlap(84, 84));
        assertEquals("lower still is still nothing", 0, ScreenFit.topOverlap(84, 120));
    }

    @Test
    public void aFrameThatOverlapsTheBarPartlyIsPaddedByTheOverlapOnly() {
        assertEquals(40, ScreenFit.topOverlap(280, 240));
    }

    @Test
    public void theBottomIsTheKeyboardsLiftRule() {
        // Gesture bar 135px tall; the frame runs to the very bottom of a 2340px screen.
        assertEquals(135, ScreenFit.bottomOverlap(135, 2340, 2340));
        // The framework already stopped the frame above the bar: nothing to add.
        assertEquals(0, ScreenFit.bottomOverlap(135, 2340, 2205));
        // Stopped above a keyboard that is taller than the bar: the keyboard is the furniture.
        assertEquals(0, ScreenFit.bottomOverlap(900, 2340, 1440));
        assertEquals(60, ScreenFit.bottomOverlap(900, 2340, 1500));
    }

    @Test
    public void theSidesFollowTheSameRuleInLandscape() {
        // Xiaomi landscape: navigation furniture 130px on the right, the frame runs to the edge.
        assertEquals(130, ScreenFit.rightOverlap(130, 2340, 2340));
        assertEquals(0, ScreenFit.rightOverlap(130, 2340, 2210));
        assertEquals(100, ScreenFit.leftOverlap(100, 0));
        assertEquals(0, ScreenFit.leftOverlap(100, 100));
    }

    @Test
    public void nothingIsEverNegative() {
        assertEquals(0, ScreenFit.topOverlap(-5, 0));
        assertEquals(0, ScreenFit.bottomOverlap(-5, 2340, 2340));
        assertEquals(0, ScreenFit.leftOverlap(-5, 0));
        assertEquals(0, ScreenFit.rightOverlap(-5, 2340, 2340));
    }
}
