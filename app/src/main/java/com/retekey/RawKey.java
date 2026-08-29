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
    SPACE,
    // ★ 수식키 자신. 로컬 편집기에는 필요 없다 — 거기서는 metaState 하나로 충분하고, 그래서
    //   여기 없었다. 그러나 **원격데스크톱 릴레이는 metaState 를 안 본다**: 저쪽 OS 의 수식
    //   상태를 세우는 것은 진짜 Ctrl 키의 down/up 이다. 그것이 없으면 Ctrl+C 가 저쪽에 그냥
    //   `c` 로 도착한다(사용자 보고 2026-08-29 — 액션바 잘라내기·복사와 Ctrl+X/C/V/A 가
    //   원격에서만 죽었다). 왼쪽 것만 둔다: 한 벌이면 충분하고, 오른쪽은 뜻이 같다.
    CTRL_LEFT, SHIFT_LEFT, ALT_LEFT, META_LEFT
}
