package com.retekey;

/**
 * What a slot on the action bar can do.
 *
 * <p>Stage 1 of the bar carries only actions that need no new state: the editor's own selection and
 * clipboard commands, plus cursor movement. The clipboard list, the symbol panel, chords and macros
 * are later stages and will join this enum then.
 *
 * <p>Stored as a word, like {@link ThemeMode} and the layout order, so an action added in the middle
 * later cannot silently rename someone's saved bar.
 */
enum BarAction {
    /** Selects the word around the cursor — the run between whitespace or punctuation. */
    SELECT_WORD("word", "Word"),
    SELECT_ALL("all", "All"),
    CUT("cut", "Cut"),
    COPY("copy", "Copy"),
    PASTE("paste", "Paste"),
    LEFT("left", "←"),
    RIGHT("right", "→"),
    UP("up", "↑"),
    DOWN("down", "↓"),
    HOME("home", "Home"),
    END("end", "End"),
    PAGE_UP("pgup", "PgUp"),
    PAGE_DOWN("pgdn", "PgDn"),
    /** The clipboard list the keyboard keeps of what was cut and copied through it. */
    CLIPBOARD("clip", "Clip"),
    /** The symbols page, from wherever you are. */
    SYMBOLS("symbols", "Sym"),
    /** Raises the notepad. */
    NOTEPAD("memo", "Memo");

    private final String stored;
    private final String label;

    BarAction(String stored, String label) {
        this.stored = stored;
        this.label = label;
    }

    /** The word written to preferences. */
    String stored() {
        return stored;
    }

    /** What the slot says on the bar. */
    String label() {
        return label;
    }

    /** The raw key an action sends, or null where it is not a key at all. */
    RawKey rawKey() {
        switch (this) {
            case LEFT:
                return RawKey.LEFT;
            case RIGHT:
                return RawKey.RIGHT;
            case UP:
                return RawKey.UP;
            case DOWN:
                return RawKey.DOWN;
            case HOME:
                return RawKey.HOME;
            case END:
                return RawKey.END;
            case PAGE_UP:
                return RawKey.PAGE_UP;
            case PAGE_DOWN:
                return RawKey.PAGE_DOWN;
            default:
                return null;
        }
    }

    /** The action for a stored word, or null if it names nothing this build knows. */
    static BarAction parse(String value) {
        if (value != null) {
            for (BarAction action : values()) {
                if (action.stored.equals(value)) {
                    return action;
                }
            }
        }
        return null;
    }
}
