package com.retekey;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Issue #1: the system's bottom buttons must not sit on top of the bottom key row. */
public final class SystemBarInsetsTest {
    /** Automatic, with the window measured as reaching right into the bar. */
    private static int auto(int sdk, int tappable, int navigation, boolean navVisible,
            int systemWindow) {
        return SystemBarInsets.bandPx(sdk, tappable, navigation, navVisible, systemWindow,
            navigation, SystemBarInsets.Mode.AUTOMATIC);
    }

    /** Automatic, with the overlap given explicitly. */
    private static int auto(int sdk, int tappable, int navigation, boolean navVisible,
            int systemWindow, int overlap) {
        return SystemBarInsets.bandPx(sdk, tappable, navigation, navVisible, systemWindow,
            overlap, SystemBarInsets.Mode.AUTOMATIC);
    }

    @Test
    public void threeButtonNavigationReservesTheBarsHeight() {
        // API 33, three-button navigation: both insets agree, and that is the case that was broken
        // — the report's `!#` and layout keys were underneath the system's own buttons.
        assertEquals(126, auto(33, 126, 126, true, 126));
    }

    @Test
    public void gestureNavigationWithKeyboardButtonsReservesTheWholeZone() {
        // Measured on a Galaxy A56, One UI 8.5, gesture navigation (issue #1): the tappable inset is
        // the gesture bar's bounding box, 42px, and the navigation-bar inset is the whole zone the
        // hide-keyboard and switch-keyboard buttons live in, 135px. Reserving the smaller lifted the
        // keyboard clear of the gesture bar and left it under the buttons.
        assertEquals(135, auto(33, 42, 135, true, 135));
    }

    @Test
    public void threeButtonNavigationIsTheSameNumberTwice() {
        // Same phone in three-button navigation: both insets are the zone, so nothing to choose.
        assertEquals(135, auto(33, 135, 135, true, 135));
    }

    @Test
    public void aPhoneThatDrawsNothingDownThereKeepsItsHeight() {
        // Several Samsung ROMs let the keyboard buttons be turned off; the navigation bar is then
        // not showing over the IME, and reserving a band for it would be giving up a row of keys
        // for furniture that is not there.
        assertEquals(0, auto(33, 126, 126, false, 126));
    }

    @Test
    public void theTappableInsetHasNoSayInIt() {
        // Whatever it reports, the furniture is the navigation bar's height — capped by how much of
        // it is over this window, which is the term that can still answer zero.
        assertEquals(135, auto(33, 0, 135, true, 135));
        assertEquals(135, auto(33, 999, 135, true, 135));
        assertEquals(0, auto(33, 42, 135, true, 135, 0));
    }

    @Test
    public void aWindowTheFrameworkAlreadyLiftedNeedsNoBand() {
        // The Note 20 case: the bar is there, it takes taps, and none of it is over the keyboard's
        // own window — the framework placed the window above it. Reserving here is giving up a
        // strip of keyboard for furniture that is not on top of it.
        assertEquals(0, auto(33, 126, 126, true, 126, 0));
    }

    @Test
    public void aWindowDrawnIntoTheBarReservesWhatIsActuallyOverIt() {
        // Issue #1: the window reaches the physical bottom, so the whole bar is over the keys.
        assertEquals(126, auto(33, 126, 126, true, 126, 126));
        // Half in, half out: only the part over the window is ours to give up.
        assertEquals(60, auto(33, 126, 126, true, 126, 60));
    }

    @Test
    public void beforeTheWindowHasASizeTheInsetsAreAllThereIs() {
        assertEquals(126, auto(33, 126, 126, true, 126, SystemBarInsets.OVERLAP_UNKNOWN));
    }

    @Test
    public void theUserCanOverrideBothWays() {
        assertEquals("always keeps a band even where nothing overlaps",
            126, SystemBarInsets.bandPx(33, 126, 126, false, 126, 0,
                SystemBarInsets.Mode.ALWAYS));
        assertEquals("never keeps one even where the whole bar is over the keys",
            0, SystemBarInsets.bandPx(33, 126, 126, true, 126, 126,
                SystemBarInsets.Mode.NEVER));
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
