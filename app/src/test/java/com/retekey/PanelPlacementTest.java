package com.retekey;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Issue #14: where the Hanja candidates and the code-point pad appear. */
public final class PanelPlacementTest {
    @Test
    public void byDefaultTheyFloatOnTheirOwn() {
        assertEquals(PanelPlacement.OWN_FLOATING, PanelPlacement.of(false, false));
        assertEquals(PanelPlacement.OWN_FLOATING, PanelPlacement.of(false, true));
    }

    @Test
    public void followingADockedKeyboardTheyDockToo() {
        assertEquals(PanelPlacement.DOCKED, PanelPlacement.of(true, false));
    }

    @Test
    public void followingAFloatingKeyboardTheyTakeItsPlace() {
        assertEquals(PanelPlacement.KEYBOARD_FLOATING, PanelPlacement.of(true, true));
    }
}
