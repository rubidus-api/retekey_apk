package com.retekey;

/**
 * Which way the screen is held, and the preference key a setting takes in that position.
 *
 * <p>A keyboard held upright and one held sideways are barely the same keyboard: the height that
 * leaves room to read is different, a floating panel makes sense on a wide screen and rarely on a
 * tall one, and the layouts worth carrying may differ too. So the settings that depend on the shape
 * of the screen are stored twice, under a key with the orientation appended.
 *
 * <p>Settings written before this split have no suffix. They are read as the value for
 * <em>either</em> orientation until that orientation is set on its own, so an upgrade keeps what
 * the user had rather than resetting them to a default they never chose.
 *
 * <p>Android-free: the naming rule is arithmetic on strings, and is unit-tested as such.
 */
public enum ScreenOrientation {
    PORTRAIT("portrait"),
    LANDSCAPE("landscape");

    private final String suffix;

    ScreenOrientation(String suffix) {
        this.suffix = suffix;
    }

    /** The key this setting takes in this orientation. */
    public String key(String base) {
        if (base == null || base.isEmpty()) {
            throw new IllegalArgumentException("a preference key must not be empty");
        }
        return base + "." + suffix;
    }

    /** The key a setting had before the split, still read when the oriented one is unset. */
    public static String legacyKey(String base) {
        if (base == null || base.isEmpty()) {
            throw new IllegalArgumentException("a preference key must not be empty");
        }
        return base;
    }

    /** Which orientation a screen of these dimensions is being held in. */
    public static ScreenOrientation of(int width, int height) {
        return width > height ? LANDSCAPE : PORTRAIT;
    }

    public ScreenOrientation other() {
        return this == PORTRAIT ? LANDSCAPE : PORTRAIT;
    }
}
