package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Whether the action bar sits above or below the keys. */
public final class ActionBarPositionTest {
    @Test
    public void defaultIsAboveWhenUnsetOrUnreadable() {
        assertEquals(ActionBarPosition.ABOVE, ActionBarPosition.parse(null));
        assertEquals(ActionBarPosition.ABOVE, ActionBarPosition.parse(""));
        assertEquals(ActionBarPosition.ABOVE, ActionBarPosition.parse("bottom"));
        assertEquals(ActionBarPosition.ABOVE, ActionBarPosition.parse("BELOW"));
    }

    @Test
    public void roundTripsEveryPosition() {
        for (ActionBarPosition position : ActionBarPosition.values()) {
            assertEquals(position, ActionBarPosition.parse(position.stored()));
        }
        assertEquals(ActionBarPosition.ABOVE, ActionBarPosition.parse("above"));
        assertEquals(ActionBarPosition.BELOW, ActionBarPosition.parse("below"));
    }

    @Test
    public void recognisesItsPreferenceKeys() {
        assertTrue(ActionBarPosition.isKey(ActionBarPosition.PREF_KEY));
        assertTrue(ActionBarPosition.isKey(
            ScreenOrientation.PORTRAIT.key(ActionBarPosition.PREF_KEY)));
        assertTrue(ActionBarPosition.isKey(
            ScreenOrientation.LANDSCAPE.key(ActionBarPosition.PREF_KEY)));
        assertFalse(ActionBarPosition.isKey(null));
        assertFalse(ActionBarPosition.isKey(ActionBarSlots.KEY_ENABLED));
        assertFalse(ActionBarPosition.isKey(
            ScreenOrientation.PORTRAIT.key(ActionBarHeightSettings.KEY_SCREEN_PERCENT)));
    }
}
