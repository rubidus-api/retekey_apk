package com.retekey;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** RFC-0014 F11: a key the service writes itself is not taken for a change from settings. */
public class OwnPrefWritesTest {
    @Test
    public void aKeyIsOwnOnlyWhileTheServiceIsWritingIt() {
        OwnPrefWrites own = new OwnPrefWrites();
        boolean[] seen = new boolean[2];
        own.write(ThemeMode.PREF_KEY, () -> {
            seen[0] = own.isOwn(ThemeMode.PREF_KEY);
            seen[1] = own.isOwn(ActionBarSlots.KEY_SLOTS);
        });
        assertTrue("the listener runs inside apply() and sees its own write", seen[0]);
        assertFalse("another key is still a change from settings", seen[1]);
        assertFalse("after the write the key is anyone's again", own.isOwn(ThemeMode.PREF_KEY));
    }

    @Test
    public void anOrientedKeyMatchesItsBase() {
        OwnPrefWrites own = new OwnPrefWrites();
        boolean[] seen = new boolean[3];
        own.write(FloatingKeyboardSettings.KEY_ENABLED, () -> {
            seen[0] = own.isOwn(ScreenOrientation.PORTRAIT.key(FloatingKeyboardSettings.KEY_ENABLED));
            seen[1] = own.isOwn(ScreenOrientation.LANDSCAPE.key(FloatingKeyboardSettings.KEY_ENABLED));
            seen[2] = own.isOwn(null);
        });
        assertTrue(seen[0]);
        assertTrue(seen[1]);
        assertFalse(seen[2]);
    }
}
