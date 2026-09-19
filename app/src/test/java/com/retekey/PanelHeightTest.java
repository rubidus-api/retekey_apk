package com.retekey;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Issue #8: the notepad and the clipboard are measured against the room an input method actually
 * has, not against the whole screen — a window as tall as the screen reached up behind the status
 * bar, and asking for more than the system means to give is also how an app can drop the keyboard
 * the moment the panel opens.
 */
public class PanelHeightTest {

    /** Four fifths of 1920, which is the cap every case below runs into. */
    private static final int CAP = 1920 * 80 / 100;

    @Test
    public void theAppKeepsAStripOfTheScreen() {
        // Without a cap the window took the whole screen, and an app squeezed to nothing could
        // answer by putting the keyboard down the moment the panel opened.
        assertEquals(CAP, PanelHeight.forPanel(1920, 0, 0));
        assertEquals(CAP, PanelHeight.forPanel(1920, -1, 0));
    }

    @Test
    public void theTopBandAndTheBottomReserveComeOffBeforeTheCapIsReached() {
        // A tall band and a tall reserve leave less than the cap; then they decide the height.
        assertEquals(1920 - 500 - 200, PanelHeight.forPanel(1920, 500, 200));
    }

    @Test
    public void nonsenseNeverGrowsOrGoesNegative() {
        // A band taller than the screen is not believed; a reserve taller than the screen leaves
        // nothing rather than a negative height the measure spec would refuse.
        assertEquals(CAP, PanelHeight.forPanel(1920, 4000, 0));
        assertEquals(0, PanelHeight.forPanel(1920, 72, 5000));
    }
}
