package com.retekey.core;

/**
 * A platform-neutral name for a hardware key. The Android editor bridge maps each to a concrete
 * {@code KeyEvent.KEYCODE_*}; nothing in the input core depends on Android. ENTER and BACKSPACE are
 * included so the existing raw Enter and delete fallbacks share this path.
 */
public enum RawKey {
    ENTER(28),
    BACKSPACE(14),
    ESCAPE(1),
    TAB(15),
    FORWARD_DELETE(111),
    INSERT(110),
    LEFT(105),
    RIGHT(106),
    UP(103),
    DOWN(108),
    HOME(102),
    END(107),
    PAGE_UP(104),
    PAGE_DOWN(109),
    PRINT_SCREEN(99),
    SCROLL_LOCK(70),
    CAPS_LOCK(58),
    BREAK(119),
    MENU(127),
    SEARCH(217),
    F1(59), F2(60), F3(61), F4(62), F5(63), F6(64),
    F7(65), F8(66), F9(67), F10(68), F11(87), F12(88),
    // Letter keys, for modifier chords such as Ctrl+B in a terminal. They must stay contiguous
    // and in A..Z order: the bridge maps them to KEYCODE_A..KEYCODE_Z by offset.
    A(30), B(48), C(46), D(32), E(18), F(33), G(34), H(35), I(23), J(36), K(37), L(38), M(50),
    N(49), O(24), P(25), Q(16), R(19), S(31), T(20), U(22), V(47), W(17), X(45), Y(21), Z(44),
    // Digits and Space, so a user-assembled chord can be Ctrl+1 or Alt+Space. They must stay
    // contiguous and in 0..9 order: the bridge maps them to KEYCODE_0..KEYCODE_9 by offset.
    DIGIT_0(11), DIGIT_1(2), DIGIT_2(3), DIGIT_3(4), DIGIT_4(5),
    DIGIT_5(6), DIGIT_6(7), DIGIT_7(8), DIGIT_8(9), DIGIT_9(10),
    SPACE(57),
    // ★ 수식키 자신. 로컬 편집기에는 필요 없다 — 거기서는 metaState 하나로 충분하고, 그래서
    //   여기 없었다. 그러나 **원격데스크톱 릴레이는 metaState 를 안 본다**: 저쪽 OS 의 수식
    //   상태를 세우는 것은 진짜 Ctrl 키의 down/up 이다. 그것이 없으면 Ctrl+C 가 저쪽에 그냥
    //   `c` 로 도착한다(사용자 보고 2026-08-29 — 액션바 잘라내기·복사와 Ctrl+X/C/V/A 가
    //   원격에서만 죽었다). 왼쪽 것만 둔다: 한 벌이면 충분하고, 오른쪽은 뜻이 같다.
    CTRL_LEFT(29), SHIFT_LEFT(42), ALT_LEFT(56), META_LEFT(125),
    // The period key, for the remote-desktop page: the far side's IME reads it by position — it
    // is ю on a Russian layout — so it has to arrive as the key, not as a "." this side typed.
    PERIOD(52),
    // The right-hand Ctrl and Alt, as keys of their own. A Korean IME reads them as 한자 and
    // 한/영 (owner, 2026-11-18), which is what the remote-desktop page sends them for.
    CTRL_RIGHT(97), ALT_RIGHT(100),
    // The rest of a US keyboard's punctuation keys. In remote-desktop mode every printable ASCII
    // character goes out as the US key that types it (RemoteDesktopKeys), so these have to exist.
    GRAVE(41), MINUS(12), EQUALS(13), LEFT_BRACKET(26), RIGHT_BRACKET(27), BACKSLASH(43),
    SEMICOLON(39), APOSTROPHE(40), COMMA(51), SLASH(53);

    private final int scanCode;

    RawKey(int scanCode) {
        this.scanCode = scanCode;
    }

    /** The Linux evdev scan code a physical keyboard reports for this key. */
    public int scanCode() {
        return scanCode;
    }
}
