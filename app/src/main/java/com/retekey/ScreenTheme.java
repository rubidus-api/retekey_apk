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
