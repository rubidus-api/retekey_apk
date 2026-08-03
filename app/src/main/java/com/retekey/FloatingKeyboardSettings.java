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

    public static void store(SharedPreferences prefs, FloatingKeyboardBounds bounds) {
        if (prefs == null || bounds == null) {
            return;
        }
        prefs.edit()
            .putBoolean(KEY_SIDE_LEFT, bounds.isLeft())
            .putInt(KEY_LEFT, bounds.left())
            .putInt(KEY_TOP, bounds.top())
            .putInt(KEY_WIDTH, bounds.width())
            .putInt(KEY_HEIGHT, bounds.height())
            .putInt(KEY_SCREEN_WIDTH, bounds.screenWidth())
            .putInt(KEY_SCREEN_HEIGHT, bounds.screenHeight())
            .apply();
    }

    /** The stored panel, or {@code null} when the user has never placed one. */
    public static FloatingKeyboardBounds load(SharedPreferences prefs) {
        if (prefs == null) {
            return null;
        }
        int screenWidth = prefs.getInt(KEY_SCREEN_WIDTH, 0);
        int screenHeight = prefs.getInt(KEY_SCREEN_HEIGHT, 0);
        int width = prefs.getInt(KEY_WIDTH, 0);
        int height = prefs.getInt(KEY_HEIGHT, 0);
        if (screenWidth <= 0 || screenHeight <= 0 || width <= 0 || height <= 0) {
            return null;
        }
        return FloatingKeyboardBounds.of(
            screenWidth,
            screenHeight,
            prefs.getBoolean(KEY_SIDE_LEFT, false)
                ? FloatingKeyboardBounds.Side.LEFT
                : FloatingKeyboardBounds.Side.RIGHT,
            prefs.getInt(KEY_LEFT, 0),
            prefs.getInt(KEY_TOP, 0),
            width,
            height
        );
    }
}
