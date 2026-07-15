package dev.hellgates.retekeyime;

import java.util.ArrayList;
import java.util.List;

/**
 * The four touch pages on one orthogonal ten-column grid:
 *
 * <ol>
 *   <li>English QWERTY;</li>
 *   <li>Korean 2-beolsik;</li>
 *   <li>special characters (reached by holding the period);</li>
 *   <li>special keys — the keypad plus special/function keys (reached by the {@code pad} key),
 *       with Num and Fn variants.</li>
 * </ol>
 *
 * The two letter pages share their cells so a key keeps its position across a language switch. Every
 * key occupies one column except the space bar, which spans three. The bottom row is shared; only
 * its layer key differs (`pad` on the letter/char pages, a return key on the special-keys page).
 */
public final class KeyboardLayouts {
    public static final int COLUMNS = 10;
    public static final int SPACE_COLUMN_SPAN = 3;

    // The keypad occupies columns 6-8; column 9 is the 0 / Enter / Backspace strip.
    private static final String[] DIGIT_CELLS = {"7", "8", "9", "4", "5", "6", "1", "2", "3"};
    private static final String[] ARROW_CELLS = {
        "Home", "↑", "PgUp", "←", "Ins", "→", "End", "↓", "PgDn"
    };
    private static final RawKey[] ARROW_KEYS = {
        RawKey.HOME, RawKey.UP, RawKey.PAGE_UP,
        RawKey.LEFT, RawKey.INSERT, RawKey.RIGHT,
        RawKey.END, RawKey.DOWN, RawKey.PAGE_DOWN
    };
    private static final String[] FUNCTION_CELLS = {
        "F7", "F8", "F9", "F4", "F5", "F6", "F1", "F2", "F3"
    };
    private static final RawKey[] FUNCTION_KEYS = {
        RawKey.F7, RawKey.F8, RawKey.F9,
        RawKey.F4, RawKey.F5, RawKey.F6,
        RawKey.F1, RawKey.F2, RawKey.F3
    };

    private static final KeyboardLayout EN_BASE = english(false);
    private static final KeyboardLayout EN_SHIFTED = english(true);
    private static final KeyboardLayout KO_BASE = korean(false);
    private static final KeyboardLayout KO_SHIFTED = korean(true);
    private static final KeyboardLayout CHARS = buildSpecialChars();
    private static final KeyboardLayout KEYS_NUMBERS = buildSpecialKeys(NumpadMode.NUMBERS);
    private static final KeyboardLayout KEYS_ARROWS = buildSpecialKeys(NumpadMode.ARROWS);
    private static final KeyboardLayout KEYS_FUNCTIONS = buildSpecialKeys(NumpadMode.FUNCTIONS);
    private static final KeyboardLayout MENU = buildMenu();

    private KeyboardLayouts() {
    }

    public static KeyboardLayout of(KeyboardLayoutId id, boolean shifted) {
        if (id == null) {
            throw new IllegalArgumentException("layout id must not be null");
        }
        switch (id) {
            case EN_QWERTY:
                return shifted ? EN_SHIFTED : EN_BASE;
            case KO_DUBEOLSIK:
                return shifted ? KO_SHIFTED : KO_BASE;
            case SPECIAL_CHARS:
                return CHARS;
            default:
                return KEYS_NUMBERS;
        }
    }

    public static KeyboardLayout specialChars() {
        return CHARS;
    }

    /** The menu-and-functions page (reached by the ☰ menu key). */
    public static KeyboardLayout menu() {
        return MENU;
    }

    /** The special-keys page in a specific keypad/function mode. */
    public static KeyboardLayout specialKeys(NumpadMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("numpad mode must not be null");
        }
        switch (mode) {
            case ARROWS:
                return KEYS_ARROWS;
            case FUNCTIONS:
                return KEYS_FUNCTIONS;
            default:
                return KEYS_NUMBERS;
        }
    }

    public static KeyboardLayoutId otherLetters(KeyboardLayoutId id) {
        return id == KeyboardLayoutId.EN_QWERTY
            ? KeyboardLayoutId.KO_DUBEOLSIK
            : KeyboardLayoutId.EN_QWERTY;
    }

    // ---- Letter pages ----

    private static KeyboardLayout english(boolean shifted) {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(4);
        rows.add(KeyboardLayout.row(
            letter("q", shifted), letter("w", shifted), letter("e", shifted),
            letter("r", shifted), letter("t", shifted), letter("y", shifted),
            letter("u", shifted), letter("i", shifted), letter("o", shifted),
            letter("p", shifted)
        ));
        rows.add(KeyboardLayout.row(
            letter("a", shifted), letter("s", shifted), letter("d", shifted),
            letter("f", shifted), letter("g", shifted), letter("h", shifted),
            letter("j", shifted), letter("k", shifted), letter("l", shifted),
            enterKey()
        ));
        rows.add(KeyboardLayout.row(
            shiftKey(shifted),
            letter("z", shifted), letter("x", shifted), letter("c", shifted),
            letter("v", shifted), letter("b", shifted), letter("n", shifted),
            letter("m", shifted),
            letterPeriodKey(), backspaceKey()
        ));
        rows.add(bottomRow(padKey()));
        return KeyboardLayout.of(KeyboardLayoutId.EN_QWERTY, shifted, COLUMNS, rows);
    }

    private static KeyboardLayout korean(boolean shifted) {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(4);
        rows.add(KeyboardLayout.row(
            consonant("bieup", shifted ? "ㅃ" : "ㅂ", shifted ? 8 : 7),
            consonant("jieut", shifted ? "ㅉ" : "ㅈ", shifted ? 13 : 12),
            consonant("digeut", shifted ? "ㄸ" : "ㄷ", shifted ? 4 : 3),
            consonant("giyeok", shifted ? "ㄲ" : "ㄱ", shifted ? 1 : 0),
            consonant("siot", shifted ? "ㅆ" : "ㅅ", shifted ? 10 : 9),
            vowel("yo", "ㅛ", 12),
            vowel("yeo", "ㅕ", 6),
            vowel("ya", "ㅑ", 2),
            vowel("ae", shifted ? "ㅒ" : "ㅐ", shifted ? 3 : 1),
            vowel("e", shifted ? "ㅖ" : "ㅔ", shifted ? 7 : 5)
        ));
        rows.add(KeyboardLayout.row(
            consonant("mieum", "ㅁ", 6),
            consonant("nieun", "ㄴ", 2),
            consonant("ieung", "ㅇ", 11),
            consonant("rieul", "ㄹ", 5),
            consonant("hieuh", "ㅎ", 18),
            vowel("o", "ㅗ", 8),
            vowel("eo", "ㅓ", 4),
            vowel("a", "ㅏ", 0),
            vowel("i", "ㅣ", 20),
            enterKey()
        ));
        rows.add(KeyboardLayout.row(
            shiftKey(shifted),
            consonant("kieuk", "ㅋ", 15),
            consonant("tieut", "ㅌ", 16),
            consonant("chieut", "ㅊ", 14),
            consonant("pieup", "ㅍ", 17),
            vowel("yu", "ㅠ", 17),
            vowel("u", "ㅜ", 13),
            vowel("eu", "ㅡ", 18),
            letterPeriodKey(), backspaceKey()
        ));
        rows.add(bottomRow(padKey()));
        return KeyboardLayout.of(KeyboardLayoutId.KO_DUBEOLSIK, shifted, COLUMNS, rows);
    }

    // ---- Page 3: special characters ----

    private static KeyboardLayout buildSpecialChars() {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(4);
        rows.add(KeyboardLayout.row(
            text("bang", "!"), text("at", "@"), text("hash", "#"), text("dollar", "$"),
            text("percent", "%"), text("caret", "^"), text("amp", "&"), text("star", "*"),
            text("lparen", "("), text("rparen", ")")
        ));
        rows.add(KeyboardLayout.row(
            text("backslash", "\\"), text("pipe", "|"), text("slash", "/"),
            text("lbracket", "["), text("rbracket", "]"), text("lbrace", "{"),
            text("rbrace", "}"), text("lt", "<"), text("gt", ">"),
            enterKey()
        ));
        rows.add(KeyboardLayout.row(
            shiftKey(false),
            text("underscore", "_"), text("semicolon", ";"), text("colon", ":"),
            text("backtick", "`").withLongPress("-"),
            text("apostrophe", "'").withLongPress("="),
            text("quote", "\"").withLongPress("÷"),
            text("question", "?").withLongPress("×"),
            text("tilde", "~").withLongPress("+"),
            backspaceKey()
        ));
        rows.add(bottomRow(padKey()));
        return KeyboardLayout.of(KeyboardLayoutId.SPECIAL_CHARS, false, COLUMNS, rows);
    }

    // ---- Page 4: special keys (keypad + special/function keys) ----

    private static KeyboardLayout buildSpecialKeys(NumpadMode mode) {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(4);
        if (mode == NumpadMode.FUNCTIONS) {
            rows.add(KeyboardLayout.row(
                rawKey("f11", "F11", RawKey.F11), rawKey("f12", "F12", RawKey.F12),
                disabled("f13", "F13"), disabled("f14", "F14"), disabled("f15", "F15"),
                numKey(), fnRawKey(0), fnRawKey(1), fnRawKey(2), rawKey("f10", "F10", RawKey.F10)
            ));
            rows.add(KeyboardLayout.row(
                disabled("brightup", "밝게"), disabled("brightdown", "어둡게"),
                disabled("volup", "Vol+"), disabled("voldown", "Vol-"),
                disabled("mute", "Mute"),
                fnKey(), fnRawKey(3), fnRawKey(4), fnRawKey(5), enterKey()
            ));
            rows.add(KeyboardLayout.row(
                shiftKey(false),
                disabled("prevtrack", "이전곡"), disabled("playpause", "재생"),
                disabled("nexttrack", "다음곡"),
                rawKey("search", "검색", RawKey.SEARCH), disabled("back", "뒤로"),
                fnRawKey(6), fnRawKey(7), fnRawKey(8), backspaceKey()
            ));
            rows.add(bottomRow(returnToLettersKey()));
            return KeyboardLayout.of(KeyboardLayoutId.SPECIAL_KEYS, false, COLUMNS, rows);
        }

        rows.add(KeyboardLayout.row(
            rawKey("esc", "Esc", RawKey.ESCAPE), rawKey("prtsc", "PrtSc", RawKey.PRINT_SCREEN),
            rawKey("scrlk", "ScrLk", RawKey.SCROLL_LOCK), rawKey("pause", "Pause", RawKey.BREAK),
            disabled("hanja", "한자"), numKey(),
            padCell(mode, 0), padCell(mode, 1), padCell(mode, 2),
            rawKey("del", "Del", RawKey.FORWARD_DELETE)
        ));
        rows.add(KeyboardLayout.row(
            disabled("ralt", "RAlt"), disabled("rctrl", "RCtrl"), disabled("rshift", "RShft"),
            rawKey("menu", "Menu", RawKey.MENU), disabled("lang", "Lang"),
            fnKey(), padCell(mode, 3), padCell(mode, 4), padCell(mode, 5), enterKey()
        ));
        rows.add(KeyboardLayout.row(
            shiftKey(false), inPageReturnKey(),
            text("plus", "+"), text("minus", "-"), text("equals", "="), text("period", "."),
            padCell(mode, 6), padCell(mode, 7), padCell(mode, 8), backspaceKey()
        ));
        rows.add(bottomRow(returnToLettersKey()));
        return KeyboardLayout.of(KeyboardLayoutId.SPECIAL_KEYS, false, COLUMNS, rows);
    }

    /** One keypad cell: a digit, or an arrow when Num is on. */
    private static SoftwareKeySpec padCell(NumpadMode mode, int cell) {
        if (mode == NumpadMode.ARROWS) {
            return rawKey("arrow." + cell, ARROW_CELLS[cell], ARROW_KEYS[cell]);
        }
        return digit("num." + DIGIT_CELLS[cell], DIGIT_CELLS[cell]);
    }

    private static SoftwareKeySpec fnRawKey(int cell) {
        return rawKey("fn." + cell, FUNCTION_CELLS[cell], FUNCTION_KEYS[cell]);
    }

    // ---- Shared keys ----

    // ---- Menu page: settings, edit commands, height, and function placeholders ----

    private static KeyboardLayout buildMenu() {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(4);
        // Ten one-column keys per row, the same size as every other page's keys; long labels
        // auto-fit to the key width. Row 1: editing and clipboard. Row 2: cursor navigation.
        // Row 3: keyboard adjustment and function placeholders.
        rows.add(KeyboardLayout.row(
            menuControl("settings", "설정", ControlKey.OPEN_SETTINGS),
            menuControl("copy", "복사", ControlKey.COPY),
            menuControl("cut", "잘라내기", ControlKey.CUT),
            menuControl("paste", "붙여넣기", ControlKey.PASTE),
            menuControl("selectall", "전체선택", ControlKey.SELECT_ALL),
            menuControl("undo", "실행취소", ControlKey.UNDO),
            menuControl("redo", "다시실행", ControlKey.REDO),
            menuDisabled("emoji", "이모지"),
            menuDisabled("clipboard", "클립보드"),
            menuControl("date", "날짜", ControlKey.INSERT_DATE)
        ));
        rows.add(KeyboardLayout.row(
            menuRaw("cursor.left", "←", RawKey.LEFT),
            menuRaw("cursor.right", "→", RawKey.RIGHT),
            menuRaw("cursor.up", "↑", RawKey.UP),
            menuRaw("cursor.down", "↓", RawKey.DOWN),
            menuRaw("cursor.home", "처음", RawKey.HOME),
            menuRaw("cursor.end", "끝", RawKey.END),
            menuRaw("cursor.pageup", "PgUp", RawKey.PAGE_UP),
            menuRaw("cursor.pagedown", "PgDn", RawKey.PAGE_DOWN),
            menuRaw("cursor.delete", "Del", RawKey.FORWARD_DELETE),
            menuRaw("cursor.insert", "Ins", RawKey.INSERT)
        ));
        rows.add(KeyboardLayout.row(
            menuControl("height.down", "높이 −", ControlKey.HEIGHT_DOWN),
            menuControl("height.up", "높이 ＋", ControlKey.HEIGHT_UP),
            menuControl("switchime", "키보드전환", ControlKey.SWITCH_IME),
            menuControl("manageime", "키보드관리", ControlKey.MANAGE_IME),
            menuDisabled("onehand.left", "한손 ◀"),
            menuDisabled("onehand.right", "한손 ▶"),
            menuDisabled("onehand.full", "전체폭"),
            menuDisabled("theme", "테마"),
            menuDisabled("custom1", "커스텀1"),
            menuDisabled("custom2", "커스텀2")
        ));
        rows.add(bottomRow(returnToLettersKey()));
        return KeyboardLayout.of(KeyboardLayoutId.MENU, false, COLUMNS, rows);
    }

    private static SoftwareKeySpec menuControl(String id, String label, ControlKey control) {
        return SoftwareKeySpec.control("touch.menu." + id, label, control);
    }

    private static SoftwareKeySpec menuDisabled(String id, String label) {
        return SoftwareKeySpec.disabled("touch.menu." + id, label);
    }

    private static SoftwareKeySpec menuRaw(String id, String label, RawKey rawKey) {
        return SoftwareKeySpec.enabled("touch.menu." + id, label, SemanticInput.rawKey(rawKey));
    }

    private static List<SoftwareKeySpec> bottomRow(SoftwareKeySpec layerKey) {
        return KeyboardLayout.row(
            SoftwareKeySpec.control("touch.modifier.ctrl", "Ctrl", ControlKey.CTRL),
            SoftwareKeySpec.control("touch.modifier.meta", "Meta", ControlKey.META),
            SoftwareKeySpec.control("touch.modifier.alt", "Alt", ControlKey.ALT),
            SoftwareKeySpec
                .enabled("touch.text.space", "space", SemanticInput.text(" "))
                .withColumnSpan(SPACE_COLUMN_SPAN),
            SoftwareKeySpec.control("touch.layout.toggle", "한/영", ControlKey.LAYOUT_TOGGLE),
            layerKey,
            SoftwareKeySpec.control("touch.edit.tab", "Tab", ControlKey.TAB),
            SoftwareKeySpec.control("touch.menu", "☰", ControlKey.MENU_LAYER)
        );
    }

    private static SoftwareKeySpec padKey() {
        return SoftwareKeySpec.control("touch.layer.pad", "pad", ControlKey.SPECIAL_KEYS_LAYER);
    }

    private static SoftwareKeySpec returnToLettersKey() {
        return SoftwareKeySpec.control("touch.layer.letters", "가", ControlKey.PREVIOUS_LAYER);
    }

    /** The in-page return key ("영문자" position); distinct id from the shared bottom-row one. */
    private static SoftwareKeySpec inPageReturnKey() {
        return SoftwareKeySpec.control("touch.layer.letters.inpage", "가", ControlKey.PREVIOUS_LAYER);
    }

    private static SoftwareKeySpec numKey() {
        return SoftwareKeySpec.control("touch.numpad.numlock", "Num", ControlKey.NUMLOCK);
    }

    private static SoftwareKeySpec fnKey() {
        return SoftwareKeySpec.control("touch.numpad.fnlock", "Fn", ControlKey.FUNCTION_LOCK);
    }

    private static SoftwareKeySpec shiftKey(boolean active) {
        return SoftwareKeySpec.control(
            "touch.modifier.shift", active ? "⇧•" : "⇧", ControlKey.SHIFT);
    }

    private static SoftwareKeySpec enterKey() {
        return SoftwareKeySpec.enabled("touch.edit.enter", "⏎", SemanticInput.primaryAction());
    }

    private static SoftwareKeySpec backspaceKey() {
        return SoftwareKeySpec.enabled("touch.edit.backspace", "⌫", SemanticInput.deleteBackward());
    }

    private static SoftwareKeySpec letterPeriodKey() {
        return SoftwareKeySpec
            .enabled("touch.text.period", ".", SemanticInput.text("."))
            .withLongPressControl(ControlKey.SPECIAL_CHARS_LAYER);
    }

    private static SoftwareKeySpec text(String id, String label) {
        return SoftwareKeySpec.enabled("touch.sym." + id, label, SemanticInput.text(label));
    }

    private static SoftwareKeySpec digit(String id, String value) {
        return SoftwareKeySpec.enabled("touch.sym." + id, value, SemanticInput.text(value));
    }

    private static SoftwareKeySpec rawKey(String id, String label, RawKey key) {
        return SoftwareKeySpec.enabled("touch.key." + id, label, SemanticInput.rawKey(key));
    }

    private static SoftwareKeySpec disabled(String id, String label) {
        return SoftwareKeySpec.disabled("touch.key." + id, label);
    }

    private static SoftwareKeySpec letter(String lowercase, boolean shifted) {
        String label = shifted ? lowercase.toUpperCase(java.util.Locale.ROOT) : lowercase;
        return SoftwareKeySpec.enabled("touch.en." + lowercase, label, SemanticInput.text(label));
    }

    private static SoftwareKeySpec consonant(String id, String label, int index) {
        return SoftwareKeySpec.enabled(
            "touch.ko2." + id, label, SemanticInput.jamo(SemanticJamo.contextualConsonant(index)));
    }

    private static SoftwareKeySpec vowel(String id, String label, int index) {
        return SoftwareKeySpec.enabled(
            "touch.ko2." + id, label, SemanticInput.jamo(SemanticJamo.vowel(index)));
    }
}
