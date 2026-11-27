package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Central preferences helpers: orientation reset & live-rebuild key classification. */
public final class AppPrefsTest {
    @Test
    public void resetOrientedClearsLegacyAndPreservesOtherOrientation() {
        LockedPreferences prefs = new LockedPreferences();
        String primary = KeyGapSettings.KEY_CELL_PERCENT;
        String legacy = KeyGapSettings.LEGACY_KEY_PERCENT;
        // Pre-split user had legacy keys set for both portrait and landscape via fallback.
        prefs.edit()
            .putInt(primary, 12)
            .putInt(legacy, 8)
            .apply();

        AppPrefs.resetOriented(prefs, ScreenOrientation.PORTRAIT, primary, legacy);

        // Portrait + legacy keys are cleared so portrait returns to default calculation.
        assertFalse(prefs.contains(ScreenOrientation.PORTRAIT.key(primary)));
        assertFalse(prefs.contains(ScreenOrientation.PORTRAIT.key(legacy)));
        assertFalse(prefs.contains(primary));
        assertFalse(prefs.contains(legacy));
        // Landscape preserves the pre-split value it had inherited.
        assertEquals(12, prefs.getInt(ScreenOrientation.LANDSCAPE.key(primary), -1));
        assertEquals(8, prefs.getInt(ScreenOrientation.LANDSCAPE.key(legacy), -1));
    }

    @Test
    public void liveUpdateClassifiersMatchSettingsKeys() {
        assertTrue(AppPrefs.requiresInputViewRebuild(ActionBarSlots.KEY_ENABLED));
        assertTrue(AppPrefs.requiresInputViewRebuild(
            ScreenOrientation.PORTRAIT.key(FloatingKeyboardSettings.KEY_ENABLED)));
        assertTrue(AppPrefs.requiresInputViewRebuild(ThemeMode.PREF_KEY));
        assertTrue(AppPrefs.requiresInputViewRebuild(PlainDisplay.KEY_MONOCHROME));
        assertTrue(AppPrefs.requiresInputViewRebuild(PlainDisplay.KEY_STILL));
        assertFalse(AppPrefs.requiresInputViewRebuild("unrelated_key"));

        assertTrue(AppPrefs.requiresOpacityRefresh(
            ScreenOrientation.LANDSCAPE.key(FloatingKeyboardSettings.KEY_OPACITY)));
        assertFalse(AppPrefs.requiresOpacityRefresh(FloatingKeyboardSettings.KEY_ENABLED));

        assertTrue(AppPrefs.requiresBandRefresh(SystemBarInsets.Mode.PREF_KEY));
        assertFalse(AppPrefs.requiresBandRefresh(ThemeMode.PREF_KEY));
    }
}
