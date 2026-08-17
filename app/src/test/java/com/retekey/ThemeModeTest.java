package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ThemeModeTest {
    @Test
    public void systemFollowsTheDevice() {
        assertTrue(ThemeMode.SYSTEM.night(true));
        assertFalse(ThemeMode.SYSTEM.night(false));
    }

    @Test
    public void fixedModesIgnoreTheDevice() {
        assertFalse(ThemeMode.LIGHT.night(true));
        assertFalse(ThemeMode.LIGHT.night(false));
        assertTrue(ThemeMode.DARK.night(true));
        assertTrue(ThemeMode.DARK.night(false));
    }

    @Test
    public void storedWordsRoundTrip() {
        for (ThemeMode mode : ThemeMode.values()) {
            assertEquals(mode, ThemeMode.parse(mode.stored()));
        }
    }

    @Test
    public void anythingUnreadableIsTheDeviceSetting() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parse(null));
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parse(""));
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parse("SYSTEM"));
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parse("nacht"));
        // The ordinal is not the stored form; a stale numeric value must not select a mode.
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parse("2"));
    }
}
