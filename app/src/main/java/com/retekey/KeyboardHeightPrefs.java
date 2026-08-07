package com.retekey;

import android.content.SharedPreferences;
import android.util.DisplayMetrics;

/**
 * Reads and writes the keyboard height, in percent of the screen, per orientation — and carries a
 * keyboard set under the old unit across to the new one.
 *
 * <p>The height used to be stored as a multiple of a nominal row height (`height_scale`). That
 * number means a different share of the screen on every device, so it is not the same setting
 * under a new name: the first time each orientation is read, an old value is converted into the
 * percentage it was actually occupying and written under the new key. The keyboard therefore keeps
 * exactly the size it had, and the old key is never read again.
 */
final class KeyboardHeightPrefs {
    static final String KEY_PERCENT = "height_percent";
    /** The pre-percentage key: a scale over a nominal row height. Read once, to convert it. */
    private static final String LEGACY_KEY_SCALE = "height_scale";

    private KeyboardHeightPrefs() {
    }

    /**
     * The height for this orientation: the stored percentage, an old setting converted, or the
     * screen-derived default.
     *
     * @param screenHeightPx the height of the screen in {@code orientation}
     * @param longestScreenPx the display's long edge, which the default is a quarter of
     */
    static int percent(
        SharedPreferences prefs,
        ScreenOrientation orientation,
        int screenHeightPx,
        int longestScreenPx,
        float density
    ) {
        int fallback = KeyboardHeightPercent.defaultPercent(screenHeightPx, longestScreenPx);
        if (prefs == null) {
            return fallback;
        }
        int stored = OrientedPrefs.getInt(prefs, KEY_PERCENT, orientation, 0);
        if (stored > 0) {
            return KeyboardHeightPercent.clamp(stored);
        }
        float legacy = OrientedPrefs.getFloat(prefs, LEGACY_KEY_SCALE, orientation, Float.NaN);
        if (!Float.isNaN(legacy)) {
            int converted = KeyboardHeightPercent.fromLegacyScale(legacy, density, screenHeightPx);
            setPercent(prefs, orientation, converted);
            return converted;
        }
        return fallback;
    }

    /** Stores the height for one orientation. */
    static void setPercent(SharedPreferences prefs, ScreenOrientation orientation, int percent) {
        if (prefs == null) {
            return;
        }
        OrientedPrefs.putInt(prefs, KEY_PERCENT, orientation, KeyboardHeightPercent.clamp(percent));
    }

    /** The height of the screen in one orientation, whichever way the device is held right now. */
    static int screenHeightPx(ScreenOrientation orientation, DisplayMetrics metrics) {
        int longEdge = Math.max(metrics.widthPixels, metrics.heightPixels);
        int shortEdge = Math.min(metrics.widthPixels, metrics.heightPixels);
        return orientation == ScreenOrientation.LANDSCAPE ? shortEdge : longEdge;
    }
}
