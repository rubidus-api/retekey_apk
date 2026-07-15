package dev.hellgates.retekeyime;

/**
 * View-local key commands. They change which page, layout, or keypad mode the keyboard shows, or
 * arm a modifier, and never reach the input dispatcher or the editor by themselves.
 */
public enum ControlKey {
    SHIFT,
    LAYOUT_TOGGLE,
    /** Enter the special-characters page. */
    SPECIAL_CHARS_LAYER,
    /** Enter the special-keys page (keypad plus special/function keys). */
    SPECIAL_KEYS_LAYER,
    /** Leave a special page and return to the letter layout it was reached from. */
    PREVIOUS_LAYER,
    /** Toggle the keypad between digits and the arrow/navigation cluster. */
    NUMLOCK,
    /** Toggle the special-keys page to its function/media variant. */
    FUNCTION_LOCK,
    /**
     * Latching modifiers. Their armed state is view-local; it feeds the raw-key action so a
     * subsequent raw key becomes a chord.
     */
    CTRL,
    META,
    ALT,
    TAB
}
