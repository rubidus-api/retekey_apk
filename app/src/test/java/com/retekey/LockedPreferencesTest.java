package com.retekey;

import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * What the keyboard reads and writes its settings through before the user first unlocks the
 * device (review finding R04). Credential-encrypted storage cannot be opened then, and personal
 * data must not be moved to device-protected storage to get round that, so these hold nothing
 * on disk: every read is the default until a write in the same session, and writes last only
 * until the real settings are opened after unlock.
 */
public final class LockedPreferencesTest {
    @Test
    public void everyReadIsTheDefaultToBeginWith() {
        SharedPreferences prefs = new LockedPreferences();
        Assert.assertEquals("d", prefs.getString("k", "d"));
        Assert.assertEquals(7, prefs.getInt("k", 7));
        Assert.assertEquals(8L, prefs.getLong("k", 8L));
        Assert.assertEquals(0.5f, prefs.getFloat("k", 0.5f), 0f);
        Assert.assertTrue(prefs.getBoolean("k", true));
        Assert.assertNull(prefs.getStringSet("k", null));
        Assert.assertFalse(prefs.contains("k"));
        Assert.assertTrue(prefs.getAll().isEmpty());
    }

    @Test
    public void aWriteIsReadBackInTheSameSession() {
        SharedPreferences prefs = new LockedPreferences();
        Assert.assertTrue(prefs.edit().putString("s", "v").putInt("i", 3).putBoolean("b", false)
            .putStringSet("set", Collections.singleton("x")).commit());
        Assert.assertEquals("v", prefs.getString("s", null));
        Assert.assertEquals(3, prefs.getInt("i", 0));
        Assert.assertFalse(prefs.getBoolean("b", true));
        Assert.assertEquals(Collections.singleton("x"), prefs.getStringSet("set", null));
        prefs.edit().remove("s").apply();
        Assert.assertFalse(prefs.contains("s"));
        prefs.edit().clear().apply();
        Assert.assertTrue(prefs.getAll().isEmpty());
    }

    @Test
    public void listenersHearWhatChanged() {
        SharedPreferences prefs = new LockedPreferences();
        List<String> heard = new ArrayList<>();
        SharedPreferences.OnSharedPreferenceChangeListener listener = (p, key) -> heard.add(key);
        prefs.registerOnSharedPreferenceChangeListener(listener);
        prefs.edit().putInt("a", 1).apply();
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
        prefs.edit().putInt("b", 1).apply();
        Assert.assertEquals(Collections.singletonList("a"), heard);
    }

    /** A value of another type reads as the default, as the platform's would throw instead. */
    @Test
    public void aValueOfTheWrongTypeReadsAsTheDefault() {
        SharedPreferences prefs = new LockedPreferences();
        prefs.edit().putString("k", "text").apply();
        Assert.assertEquals(4, prefs.getInt("k", 4));
    }
}
