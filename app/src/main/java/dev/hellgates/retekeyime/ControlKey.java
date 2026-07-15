package dev.hellgates.retekeyime;

/**
 * View-local key commands. Most change which page, layout, or keypad mode the keyboard shows, or
 * arm a modifier, without reaching the editor themselves. A few invoke a host-provided handler
 * (settings, edit commands, date insertion) or adjust view state (keyboard height); those act
 * through the service, never by touching the input dispatcher directly.
 */
public enum ControlKey {
    SHIFT,
    LAYOUT_TOGGLE,
    /** Enter the special-characters page. */
    SPECIAL_CHARS_LAYER,
    /** Enter the special-keys page (keypad plus special/function keys). */
    SPECIAL_KEYS_LAYER,
    /** Enter the menu-and-functions page (settings, edit commands, height, and placeholders). */
    MENU_LAYER,
    /** Leave a special page and return to the letter layout it was reached from. */
    PREVIOUS_LAYER,
    /** Toggle the keypad between digits and the arrow/navigation cluster. */
    NUMLOCK,
    /** Toggle the special-keys page to its function/media variant. */
    FUNCTION_LOCK,
    /** Open ReteKey's settings screen. Handled by the host service, not the input pipeline. */
    OPEN_SETTINGS,
    /** Grow the keyboard height by one step (view-local, persisted). */
    HEIGHT_UP,
    /** Shrink the keyboard height by one step (view-local, persisted). */
    HEIGHT_DOWN,
    /** Copy the selection via the host's editor context-menu action. */
    COPY,
    /** Paste the clipboard via the host's editor context-menu action. */
    PASTE,
    /** Undo the last edit via the host's editor context-menu action. */
    UNDO,
    /** Insert the current date and time as text through the host. */
    INSERT_DATE,
    /**
     * Latching modifiers. Their armed state is view-local; it feeds the raw-key action so a
     * subsequent raw key becomes a chord.
     */
    CTRL,
    META,
    ALT,
    TAB
}
