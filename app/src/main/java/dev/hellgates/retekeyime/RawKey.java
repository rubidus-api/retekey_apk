package dev.hellgates.retekeyime;

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
    BREAK,
    MENU,
    F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12
}
