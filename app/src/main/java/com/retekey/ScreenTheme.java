package com.retekey;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Applies the chosen {@link ThemeMode} to the app's own screens.
 *
 * <p>The keyboard paints itself and only needs the palette, but the launcher and the settings
 * screen are made of stock views and take their colours from the activity's theme. Rather than
 * overriding the configuration — {@code createConfigurationContext} is API 17 and this app still
 * runs on API 14 — the choice picks the theme resource: the day/night one for {@link
 * ThemeMode#SYSTEM}, or the fixed light or dark parent otherwise. That works on every version the
 * app supports, and it is the same set of DeviceDefault parents the system would have chosen.
 */
final class ScreenTheme {
    private static final String PREFS = "retekey_view";

    private ScreenTheme() {
    }

    /** The mode stored in preferences, or {@link ThemeMode#SYSTEM} if none is readable. */
    static ThemeMode mode(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            return ThemeMode.parse(prefs.getString(ThemeMode.PREF_KEY, null));
        } catch (RuntimeException noPreferences) {
            // Direct-boot or a context without storage: the device's own setting is the safe answer.
            return ThemeMode.SYSTEM;
        }
    }

    /** Whether the keyboard is drawn in paper and ink only (issue #15). Off when unreadable. */
    static boolean monochrome(Context context) {
        return flag(context, PlainDisplay.KEY_MONOCHROME);
    }

    /**
     * Whether keys stay still when pressed: nothing flashed, shaded or echoed (issue #15). The
     * user's own switch, or Android's "remove animations", which asks the same of every app.
     */
    static boolean still(Context context) {
        float scale = 1.0f;
        try {
            // Global settings are API 17; below that the legacy build keeps only its own switch.
            if (android.os.Build.VERSION.SDK_INT >= 17) {
                scale = android.provider.Settings.Global.getFloat(context.getContentResolver(),
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f);
            }
        } catch (RuntimeException unreadable) {
            // Keep the default: animations as usual unless the user switched them off here.
        }
        return PlainDisplay.still(flag(context, PlainDisplay.KEY_STILL), scale);
    }

    static void setFlag(Context context, String key, boolean value) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(key, value).apply();
    }

    private static boolean flag(Context context, String key) {
        try {
            return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(key, false);
        } catch (RuntimeException noPreferences) {
            return false;
        }
    }

    /** Stores the mode. The keyboard picks it up through its preference-change listener. */
    static void setMode(Context context, ThemeMode mode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(ThemeMode.PREF_KEY, mode.stored())
            .apply();
    }

    /**
     * Sets the activity's theme from the stored mode. Call before {@code setContentView}, since a
     * theme applies to views as they are created.
     */
    static void apply(Activity activity) {
        switch (mode(activity)) {
            case LIGHT:
                activity.setTheme(R.style.ReteScreenThemeLight);
                break;
            case DARK:
                activity.setTheme(R.style.ReteScreenThemeDark);
                break;
            default:
                // ReteScreenTheme is already on the activity from the manifest, and it is the one
                // that follows the system through the -night resource qualifier.
                break;
        }
    }
}
