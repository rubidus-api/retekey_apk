package dev.hellgates.retekeyime;

/**
 * View-local key commands. They change which layer, layout, or keypad mode the keyboard shows, or
 * arm a modifier, and never reach the input dispatcher or the editor by themselves.
 */
public enum ControlKey {
    SHIFT,
    LAYOUT_TOGGLE,
    /** Enter the number/symbol/special-key layer. */
    SYMBOL_LAYER,
    /** Leave the symbol layer and return to the letter layout it was reached from. */
    PREVIOUS_LAYER,
    /** Toggle the right-hand 3x3 block between digits and the arrow/navigation cluster. */
    NUMLOCK,
    /** Toggle the right-hand 3x3 block between digits and the function keys. */
    FUNCTION_LOCK,
    /**
     * Latching modifiers. Their armed state is view-local today; it feeds the raw-key action once
     * that lands, at which point a subsequent key becomes a chord.
     */
    CTRL,
    META,
    ALT,
    TAB
}
