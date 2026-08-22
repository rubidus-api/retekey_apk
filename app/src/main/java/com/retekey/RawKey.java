package com.retekey;

/**
 * A platform-neutral name for a hardware key. The Android editor bridge maps each to a concrete
 * {@code KeyEvent.KEYCODE_*}; nothing in the input core depends on Android. ENTER and BACKSPACE are
 * included so the existing raw Enter and delete fallbacks share this path.
 */
public enum RawKey {
    ENTER,
    BACKSPACE,
    ESCAPE,
    TAB,
    FORWARD_DELETE,
    INSERT,
    LEFT,
    RIGHT,
    UP,
    DOWN,
    HOME,
    END,
    PAGE_UP,
    PAGE_DOWN,
    PRINT_SCREEN,
    SCROLL_LOCK,
    CAPS_LOCK,
    BREAK,
    MENU,
    SEARCH,
    F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12,
    // Letter keys, for modifier chords such as Ctrl+B in a terminal. They must stay contiguous
    // and in A..Z order: the bridge maps them to KEYCODE_A..KEYCODE_Z by offset.
    A, B, C, D, E, F, G, H, I, J, K, L, M,
    N, O, P, Q, R, S, T, U, V, W, X, Y, Z,
    // Digits and Space, so a user-assembled chord can be Ctrl+1 or Alt+Space. They must stay
    // contiguous and in 0..9 order: the bridge maps them to KEYCODE_0..KEYCODE_9 by offset.
    DIGIT_0, DIGIT_1, DIGIT_2, DIGIT_3, DIGIT_4,
    DIGIT_5, DIGIT_6, DIGIT_7, DIGIT_8, DIGIT_9,
    SPACE
}
