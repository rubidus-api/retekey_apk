package com.retekey;

/**
 * Which colour scheme the keyboard and the app's screens are painted in: the one the device is set
 * to, or light or dark whatever the device says.
 *
 * <p>The setting is stored as a word rather than an ordinal so a future mode can be added anywhere
 * in the list without silently renaming everyone's stored choice, and an unreadable value falls
 * back to {@link #SYSTEM} — the behaviour every version before this setting existed had.
 *
 * <p>This class knows nothing about Android: it is the rule, and {@link KeyboardPalette} and
 * {@link ScreenTheme} are the two places that apply it.
 */
enum ThemeMode {
    /** Follow the device's own light/dark setting. */
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    /** The preference key, in the same {@code retekey_view} preferences everything else uses. */
    static final String PREF_KEY = "theme_mode";

    private final String stored;

    ThemeMode(String stored) {
        this.stored = stored;
    }

    /** The word written to preferences. */
    String stored() {
        return stored;
    }

    /** The mode for a stored word; {@link #SYSTEM} for anything else, including a missing value. */
    static ThemeMode parse(String value) {
        if (value != null) {
            for (ThemeMode mode : values()) {
                if (mode.stored.equals(value)) {
                    return mode;
                }
            }
        }
        return SYSTEM;
    }

    /**
     * Whether to paint dark, given what the device itself is set to. Only {@link #SYSTEM} consults
     * {@code systemNight}; the other two are the answer.
     */
    boolean night(boolean systemNight) {
        switch (this) {
            case LIGHT:
                return false;
            case DARK:
                return true;
            default:
                return systemNight;
        }
    }
}
