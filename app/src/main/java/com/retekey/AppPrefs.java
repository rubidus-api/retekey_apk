package com.retekey;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Map;

/**
 * Central accessors and orientation-reset helpers for all preference files used by the IME and
 * its settings activities. Keeping preference names and live-rebuild rules in one place ensures
 * UI edits and background service updates stay aligned.
 */
final class AppPrefs {
    static final String VIEW = "retekey_view";
    static final String CLIPS = "retekey_clips";
    static final String NOTES = "retekey_notes";
    static final String STASH = "retekey_stash";
    static final String LAYOUT = "retekey_layout";

    private AppPrefs() {
    }

    static SharedPreferences view(Context context) {
        return context.getSharedPreferences(VIEW, Context.MODE_PRIVATE);
    }

    static SharedPreferences clips(Context context) {
        return context.getSharedPreferences(CLIPS, Context.MODE_PRIVATE);
    }

    static SharedPreferences notes(Context context) {
        return context.getSharedPreferences(NOTES, Context.MODE_PRIVATE);
    }

    static SharedPreferences stash(Context context) {
        return context.getSharedPreferences(STASH, Context.MODE_PRIVATE);
    }

    static SharedPreferences layout(Context context) {
        return context.getSharedPreferences(LAYOUT, Context.MODE_PRIVATE);
    }

    /**
     * Clears the oriented and un-suffixed legacy keys for {@code orientation} while preserving any
     * un-suffixed legacy value on the other orientation if it has not yet written its own.
     */
    static void resetOriented(
            SharedPreferences prefs, ScreenOrientation orientation, String... baseKeys) {
        if (prefs == null || orientation == null || baseKeys == null) {
            return;
        }
        ScreenOrientation other = orientation.other();
        Map<String, ?> all = prefs.getAll();
        SharedPreferences.Editor editor = prefs.edit();
        for (String base : baseKeys) {
            if (base == null || base.isEmpty()) {
                continue;
            }
            String legacy = ScreenOrientation.legacyKey(base);
            String otherKey = other.key(base);
            if (prefs.contains(legacy) && !prefs.contains(otherKey)) {
                copyEntry(editor, otherKey, all.get(legacy));
            }
            editor.remove(orientation.key(base));
            editor.remove(legacy);
        }
        editor.apply();
    }

    private static void copyEntry(SharedPreferences.Editor editor, String targetKey, Object value) {
        if (value instanceof Integer) {
            editor.putInt(targetKey, (Integer) value);
        } else if (value instanceof Float) {
            editor.putFloat(targetKey, (Float) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(targetKey, (Boolean) value);
        } else if (value instanceof Long) {
            editor.putLong(targetKey, (Long) value);
        } else if (value instanceof String) {
            editor.putString(targetKey, (String) value);
        }
    }

    static boolean isOrientedKey(String key, String base) {
        if (key == null || base == null || base.isEmpty()) {
            return false;
        }
        return base.equals(key)
            || ScreenOrientation.PORTRAIT.key(base).equals(key)
            || ScreenOrientation.LANDSCAPE.key(base).equals(key);
    }

    /** Whether a change to {@code key} requires rebuilding the IME input view hierarchy. */
    static boolean requiresInputViewRebuild(String key) {
        if (key == null) {
            return false;
        }
        return ActionBarSlots.KEY_ENABLED.equals(key)
            || ActionBarSlots.KEY_SLOTS.equals(key)
            || ActionBarHeightSettings.isKey(key)
            || ActionBarPosition.isKey(key)
            || isOrientedKey(key, FloatingKeyboardSettings.KEY_ENABLED)
            || isOrientedKey(key, FloatingKeyboardSettings.KEY_PANELS_FOLLOW)
            || ThemeMode.PREF_KEY.equals(key)
            || PlainDisplay.KEY_MONOCHROME.equals(key)
            || PlainDisplay.KEY_STILL.equals(key);
    }

    /** Whether a change to {@code key} updates the floating frame opacity live. */
    static boolean requiresOpacityRefresh(String key) {
        return isOrientedKey(key, FloatingKeyboardSettings.KEY_OPACITY);
    }

    /** Whether a change to {@code key} triggers a system-band inset re-measure. */
    static boolean requiresBandRefresh(String key) {
        return SystemBarInsets.Mode.PREF_KEY.equals(key);
    }
}
