package com.retekey;

import android.content.SharedPreferences;

/**
 * Persistence for the floating keyboard: whether it is on, and where the user left the panel.
 *
 * <p>The geometry is stored as raw pixels together with the screen it was measured against, so a
 * rotation or a different display can rescale it through
 * {@link FloatingKeyboardBounds#onScreen(int, int)} instead of dropping the panel somewhere
 * unreachable.
 */
public final class FloatingKeyboardSettings {
    static final String KEY_ENABLED = "floating_enabled";
    static final String KEY_OPACITY = "floating_opacity";

    /**
     * How solid the panel is, as a percentage: 1 is a ghost, 100 is a solid keyboard. The default
     * is the same in both orientations — a floating panel sits over what you are reading, and
     * starting it mostly see-through says that better than starting it nearly solid.
     */
    public static final int MIN_OPACITY_PERCENT = 1;
    public static final int MAX_OPACITY_PERCENT = 100;
    public static final int DEFAULT_OPACITY_PERCENT = 40;
    static final String KEY_SIDE_LEFT = "floating_side_left";
    static final String KEY_LEFT = "floating_left";
    static final String KEY_TOP = "floating_top";
    static final String KEY_WIDTH = "floating_width";
    static final String KEY_HEIGHT = "floating_height";
    static final String KEY_SCREEN_WIDTH = "floating_screen_width";
    static final String KEY_SCREEN_HEIGHT = "floating_screen_height";

    private FloatingKeyboardSettings() {
    }

    /**
     * Whether the panel is on in this orientation. A floating keyboard earns its place on a wide
     * screen and is often in the way on a tall one, so the two are remembered separately.
     */
    public static boolean isEnabled(SharedPreferences prefs, ScreenOrientation orientation) {
        return OrientedPrefs.getBoolean(prefs, KEY_ENABLED, orientation, false);
    }

    public static void setEnabled(
            SharedPreferences prefs, ScreenOrientation orientation, boolean enabled) {
        OrientedPrefs.putBoolean(prefs, KEY_ENABLED, orientation, enabled);
    }

    /** Whether the Hanja list and the code-point pad follow the keyboard here (issue #14). */
    static final String KEY_PANELS_FOLLOW = "floating_panels_follow";

    public static boolean panelsFollow(SharedPreferences prefs, ScreenOrientation orientation) {
        return OrientedPrefs.getBoolean(prefs, KEY_PANELS_FOLLOW, orientation, false);
    }

    public static void setPanelsFollow(
            SharedPreferences prefs, ScreenOrientation orientation, boolean follow) {
        OrientedPrefs.putBoolean(prefs, KEY_PANELS_FOLLOW, orientation, follow);
    }

    public static int clampOpacity(int percent) {
        return Math.max(MIN_OPACITY_PERCENT, Math.min(MAX_OPACITY_PERCENT, percent));
    }

    /** The user's chosen opacity, as a 0-255 alpha the panel can paint with. */
    public static int alphaOf(int percent) {
        return Math.round(clampOpacity(percent) * 255.0f / 100.0f);
    }

    public static int opacityPercent(SharedPreferences prefs, ScreenOrientation orientation) {
        return clampOpacity(OrientedPrefs.getInt(
            prefs, KEY_OPACITY, orientation, DEFAULT_OPACITY_PERCENT));
    }

    public static void setOpacityPercent(
            SharedPreferences prefs, ScreenOrientation orientation, int percent) {
        OrientedPrefs.putInt(prefs, KEY_OPACITY, orientation, clampOpacity(percent));
    }

    /**
     * The prefix for a panel that floats on its own terms. The Unicode pad is a floating panel too,
     * and it is not the keyboard: it is a different size and belongs somewhere else on screen, so
     * it keeps its own geometry under this prefix. Opacity is deliberately not prefixed — how
     * see-through a floating panel is, is one preference about floating panels.
     */
    public static final String UNICODE_PREFIX = "unicode_";
    /** The Hanja candidate list's own geometry: a wider, shorter panel than either keyboard. */
    public static final String HANJA_PREFIX = "hanja_";

    public static void store(SharedPreferences prefs, FloatingKeyboardBounds bounds) {
        store(prefs, "", bounds);
    }

    public static void store(
            SharedPreferences prefs, ScreenOrientation orientation, FloatingKeyboardBounds bounds) {
        store(prefs, "", orientation, bounds);
    }

    public static void store(SharedPreferences prefs, String prefix, FloatingKeyboardBounds bounds) {
        if (prefs == null || bounds == null) {
            return;
        }
        String p = prefix == null ? "" : prefix;
        prefs.edit()
            .putBoolean(p + KEY_SIDE_LEFT, bounds.isLeft())
            .putInt(p + KEY_LEFT, bounds.left())
            .putInt(p + KEY_TOP, bounds.top())
            .putInt(p + KEY_WIDTH, bounds.width())
            .putInt(p + KEY_HEIGHT, bounds.height())
            .putInt(p + KEY_SCREEN_WIDTH, bounds.screenWidth())
            .putInt(p + KEY_SCREEN_HEIGHT, bounds.screenHeight())
            .apply();
    }

    public static void store(
            SharedPreferences prefs, String prefix, ScreenOrientation orientation,
            FloatingKeyboardBounds bounds) {
        if (prefs == null || bounds == null) {
            return;
        }
        if (orientation == null) {
            store(prefs, prefix, bounds);
            return;
        }
        String p = prefix == null ? "" : prefix;
        prefs.edit()
            .putBoolean(orientation.key(p + KEY_SIDE_LEFT), bounds.isLeft())
            .putInt(orientation.key(p + KEY_LEFT), bounds.left())
            .putInt(orientation.key(p + KEY_TOP), bounds.top())
            .putInt(orientation.key(p + KEY_WIDTH), bounds.width())
            .putInt(orientation.key(p + KEY_HEIGHT), bounds.height())
            .putInt(orientation.key(p + KEY_SCREEN_WIDTH), bounds.screenWidth())
            .putInt(orientation.key(p + KEY_SCREEN_HEIGHT), bounds.screenHeight())
            .apply();
    }

    /** The stored panel, or {@code null} when the user has never placed one. */
    public static FloatingKeyboardBounds load(SharedPreferences prefs) {
        return load(prefs, "");
    }

    public static FloatingKeyboardBounds load(
            SharedPreferences prefs, ScreenOrientation orientation) {
        return load(prefs, "", orientation);
    }

    public static FloatingKeyboardBounds load(SharedPreferences prefs, String prefix) {
        if (prefs == null) {
            return null;
        }
        String p = prefix == null ? "" : prefix;
        return readBounds(prefs, p, null);
    }

    public static FloatingKeyboardBounds load(
            SharedPreferences prefs, String prefix, ScreenOrientation orientation) {
        if (prefs == null) {
            return null;
        }
        if (orientation == null) {
            return load(prefs, prefix);
        }
        String p = prefix == null ? "" : prefix;
        if (!prefs.contains(orientation.key(p + KEY_SCREEN_WIDTH))) {
            return load(prefs, prefix);
        }
        return readBounds(prefs, p, orientation);
    }

    private static FloatingKeyboardBounds readBounds(
            SharedPreferences prefs, String prefix, ScreenOrientation orientation) {
        int screenWidth = prefs.getInt(boundKey(prefix, orientation, KEY_SCREEN_WIDTH), 0);
        int screenHeight = prefs.getInt(boundKey(prefix, orientation, KEY_SCREEN_HEIGHT), 0);
        int width = prefs.getInt(boundKey(prefix, orientation, KEY_WIDTH), 0);
        int height = prefs.getInt(boundKey(prefix, orientation, KEY_HEIGHT), 0);
        if (screenWidth <= 0 || screenHeight <= 0 || width <= 0 || height <= 0) {
            return null;
        }
        return FloatingKeyboardBounds.of(
            screenWidth,
            screenHeight,
            prefs.getBoolean(boundKey(prefix, orientation, KEY_SIDE_LEFT), false)
                ? FloatingKeyboardBounds.Side.LEFT
                : FloatingKeyboardBounds.Side.RIGHT,
            prefs.getInt(boundKey(prefix, orientation, KEY_LEFT), 0),
            prefs.getInt(boundKey(prefix, orientation, KEY_TOP), 0),
            width,
            height
        );
    }

    private static String boundKey(String prefix, ScreenOrientation orientation, String baseKey) {
        String key = prefix + baseKey;
        return orientation == null ? key : orientation.key(key);
    }
}
