package com.retekey;

/**
 * How a physical keyboard types into an editor with no composing region — a terminal, or a
 * remote-desktop window — kept apart from the service so it can be tested (issue #7).
 *
 * <p>A terminal used to get every physical key passed straight through, so that Ctrl-C and the
 * arrows reach the program. That also meant Korean did nothing from a hardware keyboard there:
 * the letters never reached the composer. Now that a terminal can be written into by commits —
 * each syllable taken back and re-sent as it grows — a key the current layout maps (a Korean
 * letter, a Colemak letter) goes through the composer like anywhere else, and every key it does
 * not map is still passed through untouched. With nothing mapped at all — English on a QWERTY
 * keyboard — the terminal keeps the plain passthrough it always had.
 */
final class TerminalHardwareKeys {
    private TerminalHardwareKeys() {
    }

    /**
     * Whether a physical key goes to the dispatcher rather than straight to the editor.
     *
     * @param rawKeyEditor the editor is a terminal (the raw-key deletion mode)
     * @param hasMapper    the current layout maps physical keys to something of its own
     */
    static boolean throughComposer(boolean rawKeyEditor, boolean hasMapper) {
        return !rawKeyEditor || hasMapper;
    }

    /**
     * Whether the syllable being built must be ended before this key goes to the editor itself.
     *
     * <p>Where composition is materialised, the syllable is already on the far side as committed
     * text; what has to end is this keyboard's claim on it. Otherwise the next jamo takes the
     * syllable back with backspaces — and after a Space, an arrow or Enter those backspaces land
     * on what that key typed instead. A modifier pressed on its own types nothing, so it ends
     * nothing: Shift held for the next letter must not split the syllable.
     *
     * @param materialises the editor takes composition as commits (deletes by key events)
     * @param composing    a syllable or word is being built
     * @param firstDown    the key's first down, not a release or a repeat
     * @param modifierKey  the key is Shift, Ctrl, Alt, Meta or the like on its own
     */
    static boolean endSyllableFirst(
        boolean materialises, boolean composing, boolean firstDown, boolean modifierKey) {
        return materialises && composing && firstDown && !modifierKey;
    }
}
