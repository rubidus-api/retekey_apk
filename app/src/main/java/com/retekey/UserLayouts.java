package com.retekey;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * The one layout the user installed, and where it is kept.
 *
 * <p>One, not many: a list of user layouts would need its own screen, its own ordering, its own
 * names in every list the keyboard shows. One slot answers the request in issue #11 — a keyboard
 * you can add yourself — while leaving the rest of the app as it was. Installing another replaces
 * it, which is said plainly when it happens.
 *
 * <p>The parsed layout is held in a static so that {@link KeyboardLayouts}, which knows nothing of
 * Android, can draw it. It is set when the service starts and whenever one is installed or removed,
 * which are the only moments it changes.
 */
final class UserLayouts {
    private static final String KEY_TEXT = "user_layout";

    private static volatile UserLayout current;
    private static volatile int revision;

    private UserLayouts() {
    }

    /** Monotonic revision bumped whenever the active user layout changes or reloads. */
    static int revision() {
        return revision;
    }

    /** The installed layout, or null when there is none. */
    static UserLayout current() {
        return current;
    }

    static boolean installed() {
        return current != null;
    }

    /** Reads what is stored and makes it the current one. Called when the keyboard starts. */
    static UserLayout load(Context context) {
        if (context == null) {
            return current;
        }
        String text = AppPrefs.layout(context).getString(KEY_TEXT, "");
        current = text.isEmpty() ? null : UserLayout.parse(text);
        revision++;
        return current;
    }

    /** Stores a layout and makes it the current one; returns it, or null when it does not parse. */
    static UserLayout install(Context context, String text) {
        UserLayout parsed = UserLayout.parse(text);
        if (parsed == null || context == null) {
            return null;
        }
        AppPrefs.layout(context).edit().putString(KEY_TEXT, text).apply();
        current = parsed;
        revision++;
        return parsed;
    }

    static void remove(Context context) {
        if (context != null) {
            AppPrefs.layout(context).edit().remove(KEY_TEXT).apply();
        }
        current = null;
        revision++;
    }
}
