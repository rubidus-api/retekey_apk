package dev.hellgates.retekeyime;

/**
 * Held-key auto-repeat configuration: whether repeat is on, the delay before the first repeat, and
 * the interval between repeats. Values are plain milliseconds so this stays Android-free and
 * unit-testable; the view and settings screen do the {@code SharedPreferences} I/O.
 */
public final class KeyRepeatSettings {
    static final String KEY_ENABLED = "repeat_enabled";
    static final String KEY_DELAY_MS = "repeat_delay_ms";
    static final String KEY_INTERVAL_MS = "repeat_interval_ms";

    public static final boolean DEFAULT_ENABLED = true;
    public static final int DEFAULT_DELAY_MS = 400;
    public static final int DEFAULT_INTERVAL_MS = 60;

    public static final int MIN_DELAY_MS = 100;
    public static final int MAX_DELAY_MS = 1200;
    public static final int MIN_INTERVAL_MS = 20;
    public static final int MAX_INTERVAL_MS = 500;

    private KeyRepeatSettings() {
    }

    public static int clampDelay(int ms) {
        return Math.max(MIN_DELAY_MS, Math.min(MAX_DELAY_MS, ms));
    }

    public static int clampInterval(int ms) {
        return Math.max(MIN_INTERVAL_MS, Math.min(MAX_INTERVAL_MS, ms));
    }
}
