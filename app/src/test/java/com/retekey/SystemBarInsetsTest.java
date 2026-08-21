package com.retekey;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Issue #1: the system's bottom buttons must not sit on top of the bottom key row — and the
 * keyboard must not give up height to furniture that is not over it.
 *
 * <p>The numbers here are real. The reporter read them off the settings screen of a Galaxy A56 (One
 * UI 8.5) and a Xiaomi on PixelOS, in three-button and gesture navigation, with the gesture bar on
 * and off; the owner's phone supplied the "already lifted" case. The rule is his: the furniture is
 * {@code max(tappable, navigation)}, the lift is how far the framework already put the window above
 * the physical bottom, and the band is the difference.
 */
public final class SystemBarInsetsTest {
    private static final SystemBarInsets.Mode AUTO = SystemBarInsets.Mode.AUTOMATIC;

    private static int auto(int sdk, int tappable, int navigation, boolean navVisible,
            int systemWindow, int lift) {
        return SystemBarInsets.bandPx(sdk, tappable, navigation, navVisible, systemWindow, lift, AUTO);
    }

    // ---- the reporter's phones: the window sits at the physical bottom (lift 0) ----

    @Test
    public void galaxyA56GestureNavigationWithKeyboardButtons() {
        // tappable 42 (the gesture bar's box), navigation 135 (the button zone). The keyboard must
        // clear the buttons, so 135 — the same as Always, which is what worked for him.
        assertEquals(135, auto(35, 42, 135, true, 135, 0));
    }

    @Test
    public void galaxyA56ThreeButtonNavigation() {
        // Both insets are the zone; nothing to choose.
        assertEquals(135, auto(35, 135, 135, true, 135, 0));
    }

    @Test
    public void xiaomiGestureNavigationWithGestureBar() {
        // Keyboard buttons not configurable on that ROM; the gesture bar is shown. The taller of the
        // two insets is what the keys have to clear, whichever of them it is.
        assertEquals(130, auto(35, 130, 42, true, 130, 0));
        assertEquals(130, auto(35, 42, 130, true, 130, 0));
    }

    @Test
    public void xiaomiGestureNavigationWithGestureBarHidden() {
        // The bar is hidden but the buttons are still drawn and still take taps.
        assertEquals(120, auto(35, 120, 0, false, 120, 0));
    }

    // ---- the owner's phone: the framework has already lifted the window ----

    @Test
    public void aWindowAlreadyLiftedAboveTheFurnitureNeedsNoBand() {
        // Keyboard buttons off, gesture navigation: the framework places the IME window above the
        // bar, so the lift equals the furniture and there is nothing left to add. This is the case
        // that made Always leave a gap under the owner's keyboard.
        assertEquals(0, auto(35, 42, 42, true, 42, 42));
        assertEquals("lifted further than the furniture reaches is still nothing",
            0, auto(35, 42, 42, true, 42, 60));
    }

    @Test
    public void aPartlyLiftedWindowReservesOnlyTheRemainder() {
        // The reporter's guess at the owner's phone with buttons turned on: the framework lifts the
        // window above the slim bar (70) but the button zone reaches 140. Only the difference is ours.
        assertEquals(70, auto(35, 140, 70, true, 140, 70));
    }

    // ---- edges ----

    @Test
    public void nothingShowingAndNothingTappableReservesNothing() {
        assertEquals(0, auto(35, 0, 0, false, 0, 0));
    }

    @Test
    public void beforeTheWindowHasASizeTheWholeFurnitureIsReserved() {
        // The first answer is taken with no geometry; the frame re-asks once laid out. Until then the
        // safe answer is the one Always gives, not zero — a covered row is worse than a brief gap.
        assertEquals(135, auto(35, 42, 135, true, 135, SystemBarInsets.LIFT_UNKNOWN));
    }

    @Test
    public void theUserCanOverrideBothWays() {
        assertEquals("Always reserves the furniture whatever the lift",
            135, SystemBarInsets.bandPx(35, 42, 135, true, 135, 135, SystemBarInsets.Mode.ALWAYS));
        assertEquals("Never reserves nothing whatever the furniture",
            0, SystemBarInsets.bandPx(35, 42, 135, true, 135, 0, SystemBarInsets.Mode.NEVER));
    }

    @Test
    public void beforeTappableInsetsTheSystemWindowInsetIsAllThereIs() {
        assertEquals(96, auto(28, 0, 0, true, 96, 0));
        assertEquals(96, auto(20, 0, 0, true, 96, 0));
    }

    @Test
    public void beforeAnyInsetsThereIsNoBand() {
        assertEquals(0, auto(19, 126, 126, true, 126, 0));
        assertEquals(0, auto(14, 126, 126, true, 126, 0));
    }

    @Test
    public void negativeInsetsAreNotABand() {
        assertEquals(0, auto(35, -5, -5, true, -5, 0));
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
