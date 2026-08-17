package com.retekey;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Issue #1: the system's bottom buttons must not sit on top of the bottom key row. */
public final class SystemBarInsetsTest {
    private static int auto(int sdk, int tappable, int navigation, boolean navVisible,
            int systemWindow) {
        return SystemBarInsets.bandPx(sdk, tappable, navigation, navVisible, systemWindow,
            SystemBarInsets.Mode.AUTOMATIC);
    }

    @Test
    public void threeButtonNavigationReservesTheBarsHeight() {
        // API 33, three-button navigation: both insets agree, and that is the case that was broken
        // — the report's `!#` and layout keys were underneath the system's own buttons.
        assertEquals(126, auto(33, 126, 126, true, 126));
    }

    @Test
    public void gestureNavigationReservesNothing() {
        // The navigation bar still reports a height for its handle, but nothing there takes a tap.
        assertEquals(0, auto(33, 0, 48, true, 48));
    }

    @Test
    public void aPhoneThatDrawsNothingDownThereKeepsItsHeight() {
        // Several Samsung ROMs let the keyboard buttons be turned off; the navigation bar is then
        // not showing over the IME, and reserving a band for it would be giving up a row of keys
        // for furniture that is not there.
        assertEquals(0, auto(33, 126, 126, false, 126));
    }

    @Test
    public void theBandIsNeverMoreThanTheFurnitureItIsFor() {
        // A ROM that reports a large tappable inset and a small bar — or the reverse — gets the
        // smaller of the two rather than the more alarming one.
        assertEquals(48, auto(33, 126, 48, true, 126));
        assertEquals(48, auto(33, 48, 126, true, 126));
    }

    @Test
    public void theUserCanOverrideBothWays() {
        assertEquals("always keeps a band even where nothing is showing",
            126, SystemBarInsets.bandPx(33, 126, 126, false, 126, SystemBarInsets.Mode.ALWAYS));
        assertEquals("never keeps one even in three-button navigation",
            0, SystemBarInsets.bandPx(33, 126, 126, true, 126, SystemBarInsets.Mode.NEVER));
    }

    @Test
    public void beforeTappableInsetsTheSystemWindowInsetIsAllThereIs() {
        assertEquals(96, auto(28, 0, 0, true, 96));
        assertEquals(96, auto(20, 0, 0, true, 96));
    }

    @Test
    public void beforeAnyInsetsThereIsNoBand() {
        assertEquals(0, auto(19, 126, 126, true, 126));
        assertEquals(0, auto(14, 126, 126, true, 126));
    }

    @Test
    public void negativeInsetsAreNotABand() {
        assertEquals(0, auto(33, -5, -5, true, -5));
    }

    @Test
    public void aStoredModeRoundTripsAndAnythingElseIsAutomatic() {
        for (SystemBarInsets.Mode mode : SystemBarInsets.Mode.values()) {
            assertEquals(mode, SystemBarInsets.Mode.parse(mode.stored()));
        }
        assertEquals(SystemBarInsets.Mode.AUTOMATIC, SystemBarInsets.Mode.parse(null));
        assertEquals(SystemBarInsets.Mode.AUTOMATIC, SystemBarInsets.Mode.parse("ALWAYS"));
        assertEquals(SystemBarInsets.Mode.AUTOMATIC, SystemBarInsets.Mode.parse("1"));
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
