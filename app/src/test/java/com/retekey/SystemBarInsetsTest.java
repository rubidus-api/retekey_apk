package com.retekey;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Issue #1: the system's bottom buttons must not sit on top of the bottom key row. */
public final class SystemBarInsetsTest {
    @Test
    public void threeButtonNavigationReservesTheBarsHeight() {
        // API 33, three-button navigation: the tappable inset is the bar, and that is the case that
        // was broken — the report's `!#` and layout keys were underneath it.
        assertEquals(126, SystemBarInsets.bandPx(33, 126, 126));
    }

    @Test
    public void gestureNavigationReservesNothing() {
        // The navigation bar still reports a height for its handle, but nothing there takes a tap,
        // so the keyboard keeps every pixel.
        assertEquals(0, SystemBarInsets.bandPx(33, 0, 48));
    }

    @Test
    public void beforeTappableInsetsTheSystemWindowInsetIsAllThereIs() {
        assertEquals(96, SystemBarInsets.bandPx(28, 0, 96));
        assertEquals(96, SystemBarInsets.bandPx(20, 0, 96));
    }

    @Test
    public void beforeAnyInsetsThereIsNoBand() {
        assertEquals(0, SystemBarInsets.bandPx(19, 126, 126));
        assertEquals(0, SystemBarInsets.bandPx(14, 126, 126));
    }

    @Test
    public void negativeInsetsAreNotABand() {
        assertEquals(0, SystemBarInsets.bandPx(33, -5, -5));
    }

    @Test
    public void theBandNeverSwallowsTheKeyboard() {
        // A full-screen inset arriving mid-resize would otherwise leave no keys at all.
        assertEquals(150, SystemBarInsets.clampToHeight(2000, 600));
        assertEquals(126, SystemBarInsets.clampToHeight(126, 600));
    }

    @Test
    public void anUnmeasuredViewTakesTheBandAsGiven() {
        assertEquals(126, SystemBarInsets.clampToHeight(126, 0));
    }
}
