package com.retekey;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * The bottom-band setting, and the last thing the keyboard's window was told about the insets.
 *
 * <p>The measurement is kept because the answer is different on every ROM and the only place it can
 * be taken is inside the IME window — not in the settings screen, which is an ordinary activity
 * with insets of its own. Writing it down when the keyboard sees it lets the settings screen show
 * what this phone actually reports, which turns "why is there a gap under my keyboard" into a
 * number the owner can read.
 */
final class SystemBandSettings {
    private static final String PREFS = "retekey_view";
    private static final String KEY_TAPPABLE = "system_band_seen_tappable";
    private static final String KEY_NAVIGATION = "system_band_seen_navigation";
    private static final String KEY_NAV_VISIBLE = "system_band_seen_nav_visible";
    private static final String KEY_RESERVED = "system_band_seen_reserved";

    private SystemBandSettings() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static SystemBarInsets.Mode mode(Context context) {
        try {
            return SystemBarInsets.Mode.parse(
                prefs(context).getString(SystemBarInsets.Mode.PREF_KEY, null));
        } catch (RuntimeException noPreferences) {
            return SystemBarInsets.Mode.AUTOMATIC;
        }
    }

    static void setMode(Context context, SystemBarInsets.Mode mode) {
        prefs(context).edit()
            .putString(SystemBarInsets.Mode.PREF_KEY, mode.stored())
            .apply();
    }

    /** Records what the window was told, for the settings screen to show. */
    static void remember(Context context, int tappable, int navigation, boolean navigationVisible,
            int reserved) {
        try {
            prefs(context).edit()
                .putInt(KEY_TAPPABLE, tappable)
                .putInt(KEY_NAVIGATION, navigation)
                .putBoolean(KEY_NAV_VISIBLE, navigationVisible)
                .putInt(KEY_RESERVED, reserved)
                .apply();
        } catch (RuntimeException ignored) {
            // A measurement that cannot be written down is not worth failing a keystroke for.
        }
    }

    /** A line for the settings screen: what the keyboard last saw, in pixels. */
    static String lastSeen(Context context) {
        SharedPreferences prefs = prefs(context);
        if (!prefs.contains(KEY_RESERVED)) {
            return "Not measured yet — open the keyboard once and come back.";
        }
        return "This phone reports: tappable " + prefs.getInt(KEY_TAPPABLE, 0)
            + "px, navigation bar " + prefs.getInt(KEY_NAVIGATION, 0)
            + "px" + (prefs.getBoolean(KEY_NAV_VISIBLE, true) ? "" : " (hidden)")
            + " → reserved " + prefs.getInt(KEY_RESERVED, 0) + "px.";
    }
}
