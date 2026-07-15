package dev.hellgates.retekeyime;

/**
 * Decides which hardware key events the IME must hand back to the focused app instead of consuming.
 *
 * <p>Modifier keys and any chord that holds Ctrl, Alt, or Meta are application shortcuts
 * (Ctrl+A select-all, Ctrl+C copy, Alt+…, Meta+…). A soft keyboard must not swallow them, or the
 * app never sees the shortcut. Shift is not listed because Shift+letter is ordinary text the IME
 * still composes; the framework reports the shift meta state on the letter event regardless.
 */
final class ModifierChordPolicy {
    private ModifierChordPolicy() {
    }

    /**
     * @param isModifierKey whether the key itself is a modifier (Ctrl/Alt/Meta/Shift/Sym/Fn/Num)
     * @param ctrl          whether Ctrl is held
     * @param alt           whether Alt is held
     * @param meta          whether Meta is held
     * @return true when the event should be delegated to the app rather than handled by the IME
     */
    static boolean passThroughToApp(boolean isModifierKey, boolean ctrl, boolean alt, boolean meta) {
        return isModifierKey || ctrl || alt || meta;
    }
}
