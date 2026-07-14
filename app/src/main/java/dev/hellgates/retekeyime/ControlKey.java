package dev.hellgates.retekeyime;

/**
 * View-local key commands. They change which layer, layout, or numpad mode the keyboard shows and
 * never reach the input dispatcher or the editor.
 */
public enum ControlKey {
    SHIFT,
    LAYOUT_TOGGLE,
    /** Enter the number/symbol/function layer. */
    SYMBOL_LAYER,
    /** Leave the symbol layer and return to letters. */
    LETTER_LAYER,
    /** Toggle the right-hand 3x3 block between digits and the arrow/navigation cluster. */
    NUMLOCK,
    /** Toggle the right-hand 3x3 block between digits and the function keys. */
    FUNCTION_LOCK
}
