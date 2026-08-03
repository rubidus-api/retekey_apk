package com.retekey;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

/**
 * Reads and writes the settings that are kept per screen orientation.
 *
 * <p>Every read falls back to the un-suffixed key the setting had before the split, so a keyboard
 * that has been in use keeps its height, its layouts and its floating panel when this arrives, in
 * both orientations, until each is set on its own. Every write goes to the oriented key only: the
 * moment the user changes something for one orientation, that orientation stops following the old
 * shared value.
 *
 * <p>The naming rule lives in {@link ScreenOrientation}, which is Android-free and tested; this is
 * the thin part that talks to {@link SharedPreferences}.
 */
public final class OrientedPrefs {
    private OrientedPrefs() {
    }

    /** Which way the device is being held right now. */
    public static ScreenOrientation current(Context context) {
        if (context == null) {
            return ScreenOrientation.PORTRAIT;
        }
        Configuration configuration = context.getResources().getConfiguration();
        return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            ? ScreenOrientation.LANDSCAPE
            : ScreenOrientation.PORTRAIT;
    }

    public static float getFloat(
            SharedPreferences prefs, String base, ScreenOrientation orientation, float fallback) {
        if (prefs == null) {
            return fallback;
        }
        String key = orientation.key(base);
        return prefs.contains(key)
            ? prefs.getFloat(key, fallback)
            : prefs.getFloat(ScreenOrientation.legacyKey(base), fallback);
    }

    public static int getInt(
            SharedPreferences prefs, String base, ScreenOrientation orientation, int fallback) {
        if (prefs == null) {
            return fallback;
        }
        String key = orientation.key(base);
        return prefs.contains(key)
            ? prefs.getInt(key, fallback)
            : prefs.getInt(ScreenOrientation.legacyKey(base), fallback);
    }

    public static boolean getBoolean(
            SharedPreferences prefs, String base, ScreenOrientation orientation, boolean fallback) {
        if (prefs == null) {
            return fallback;
        }
        String key = orientation.key(base);
        return prefs.contains(key)
            ? prefs.getBoolean(key, fallback)
            : prefs.getBoolean(ScreenOrientation.legacyKey(base), fallback);
    }

    public static String getString(
            SharedPreferences prefs, String base, ScreenOrientation orientation, String fallback) {
        if (prefs == null) {
            return fallback;
        }
        String key = orientation.key(base);
        return prefs.contains(key)
            ? prefs.getString(key, fallback)
            : prefs.getString(ScreenOrientation.legacyKey(base), fallback);
    }

    public static void putFloat(
            SharedPreferences prefs, String base, ScreenOrientation orientation, float value) {
        if (prefs != null) {
            prefs.edit().putFloat(orientation.key(base), value).apply();
        }
    }

    public static void putInt(
            SharedPreferences prefs, String base, ScreenOrientation orientation, int value) {
        if (prefs != null) {
            prefs.edit().putInt(orientation.key(base), value).apply();
        }
    }

    public static void putBoolean(
            SharedPreferences prefs, String base, ScreenOrientation orientation, boolean value) {
        if (prefs != null) {
            prefs.edit().putBoolean(orientation.key(base), value).apply();
        }
    }

    public static void putString(
            SharedPreferences prefs, String base, ScreenOrientation orientation, String value) {
        if (prefs != null) {
            prefs.edit().putString(orientation.key(base), value).apply();
        }
    }
}
