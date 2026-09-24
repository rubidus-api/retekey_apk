package com.retekey;

/**
 * Where the Hanja candidates and the code-point pad appear (issue #14).
 *
 * <p>By default each floats as a panel of its own, with a place and a size of its own: the pad is
 * small and the list is for reading, and neither is a keyboard. That moves them away from where
 * the hand is, and for someone who keeps the keyboard docked it turns a docked keyboard into a
 * floating one for the length of one conversion. So there is a second way, per orientation:
 * <em>follow the keyboard</em> — docked above a docked keyboard, and in the floating keyboard's
 * own place, size and opacity when it floats.
 *
 * <p>Android-free: the service applies it.
 */
enum PanelPlacement {
    /** A floating panel with its own remembered place — the behaviour since 0.1.89. */
    OWN_FLOATING,
    /** Docked with the keyboard: the list above it, the pad in place of its keys. */
    DOCKED,
    /** Floating where the floating keyboard is, in its place and at its size. */
    KEYBOARD_FLOATING;

    static PanelPlacement of(boolean followKeyboard, boolean keyboardFloating) {
        if (!followKeyboard) {
            return OWN_FLOATING;
        }
        return keyboardFloating ? KEYBOARD_FLOATING : DOCKED;
    }
}
