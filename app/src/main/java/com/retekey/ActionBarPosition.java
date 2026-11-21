package com.retekey;

/**
 * Where the action bar sits relative to the keys: above them or below them.
 *
 * <p>Stored per screen orientation as a word so missing or unrecognised values fall back to
 * {@link #ABOVE} — what every version before this setting did. Android-free.
 */
enum ActionBarPosition {
    ABOVE("above"),
    BELOW("below");

    static final String PREF_KEY = "action_bar_position";

    private final String stored;

    ActionBarPosition(String stored) {
        this.stored = stored;
    }

    /** The word written to preferences. */
    String stored() {
        return stored;
    }

    /** The position for a stored word; {@link #ABOVE} for anything else, including missing. */
    static ActionBarPosition parse(String value) {
        if (value != null) {
            for (ActionBarPosition position : values()) {
                if (position.stored.equals(value)) {
                    return position;
                }
            }
        }
        return ABOVE;
    }

    /** Whether a changed preference key is this setting, either un-oriented or per orientation. */
    static boolean isKey(String changed) {
        return PREF_KEY.equals(changed)
            || (changed != null && changed.startsWith(PREF_KEY + "."));
    }
}
