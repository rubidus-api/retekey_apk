package dev.hellgates.retekeyime;

import java.util.ArrayList;
import java.util.List;

/**
 * The base touch layouts.
 *
 * <p>Every layout shares one orthogonal ten-column grid. The letter layouts (English QWERTY and
 * Korean 2-beolsik) share their cells so a key keeps its position across a language switch. The
 * symbol layer replaces the letters with numbers, punctuation, arithmetic, and a right-hand 3x3
 * keypad whose meaning follows {@link NumpadMode}. Every key occupies exactly one column except the
 * space bar, which spans three. The bottom row is identical everywhere but for its layer key, which
 * reads {@code !#1} on letters and {@code ABC} on the symbol layer.
 */
public final class KeyboardLayouts {
    public static final int COLUMNS = 10;
    public static final int SPACE_COLUMN_SPAN = 3;

    private static final String[] NUMBER_CELLS = {"7", "8", "9", "4", "5", "6", "1", "2", "3"};
    private static final String[] ARROW_CELLS = {
        "Home", "↑", "PgUp", "←", "Ins", "→", "End", "↓", "PgDn"
    };
    private static final int[] FUNCTION_CELLS = {7, 8, 9, 4, 5, 6, 1, 2, 3};

    private static final KeyboardLayout EN_BASE = english(false);
    private static final KeyboardLayout EN_SHIFTED = english(true);
    private static final KeyboardLayout KO_BASE = korean(false);
    private static final KeyboardLayout KO_SHIFTED = korean(true);
    private static final KeyboardLayout SYM_NUMBERS = buildSymbol(NumpadMode.NUMBERS);
    private static final KeyboardLayout SYM_ARROWS = buildSymbol(NumpadMode.ARROWS);
    private static final KeyboardLayout SYM_FUNCTIONS = buildSymbol(NumpadMode.FUNCTIONS);

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
            default:
                return SYM_NUMBERS;
        }
    }

    /** The symbol layer in a specific keypad mode. */
    public static KeyboardLayout symbol(NumpadMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("numpad mode must not be null");
        }
        switch (mode) {
            case NUMBERS:
                return SYM_NUMBERS;
            case ARROWS:
                return SYM_ARROWS;
            default:
                return SYM_FUNCTIONS;
        }
    }

    /** The letter layout to switch to when the language toggle is pressed. */
    public static KeyboardLayoutId otherLetters(KeyboardLayoutId id) {
        return id == KeyboardLayoutId.EN_QWERTY
            ? KeyboardLayoutId.KO_DUBEOLSIK
            : KeyboardLayoutId.EN_QWERTY;
    }

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
        rows.add(bottomRow(false));
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
        rows.add(bottomRow(false));
        return KeyboardLayout.of(KeyboardLayoutId.KO_DUBEOLSIK, shifted, COLUMNS, rows);
    }

    private static KeyboardLayout buildSymbol(NumpadMode mode) {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(4);
        rows.add(KeyboardLayout.row(
            symDigit("0", "0"),
            symText("bang", "!", "¡"),
            symText("question", "?", "¿"),
            symText("hash", "#", "@", "$"),
            symText("star", "*", "%", "^"),
            symText("amp", "&", "|", "§"),
            SoftwareKeySpec.control("touch.numpad.numlock", "Num", ControlKey.NUMLOCK),
            numpadKey(mode, 0), numpadKey(mode, 1), numpadKey(mode, 2)
        ));
        rows.add(KeyboardLayout.row(
            enterKey(),
            symText("comma", ",", ""),
            symText("colon", ":", ""),
            symText("semicolon", ";", ""),
            symText("apostrophe", "'", "\"", "`"),
            symText("hyphen", "-", "–", "—"),
            SoftwareKeySpec.control("touch.numpad.fnlock", "Fn", ControlKey.FUNCTION_LOCK),
            numpadKey(mode, 3), numpadKey(mode, 4), numpadKey(mode, 5)
        ));
        rows.add(KeyboardLayout.row(
            symText("period", ".", ""),
            symText("underscore", "_", "~"),
            symText("plus", "+", "±"),
            symText("times", "×", "*"),
            symText("divide", "÷", "/", "\\"),
            symText("equals", "=", "(", ")", "[", "]", "{", "}", "<", ">"),
            backspaceKey(),
            numpadKey(mode, 6), numpadKey(mode, 7), numpadKey(mode, 8)
        ));
        rows.add(bottomRow(true));
        return KeyboardLayout.of(KeyboardLayoutId.SYMBOL, false, COLUMNS, rows);
    }

    /** The bottom row, shared by every layout except for its layer key. */
    private static List<SoftwareKeySpec> bottomRow(boolean symbolLayer) {
        SoftwareKeySpec layerKey = symbolLayer
            ? SoftwareKeySpec.control("touch.layer.letters", "ABC", ControlKey.LETTER_LAYER)
            : SoftwareKeySpec.control("touch.layer.symbols", "!#1", ControlKey.SYMBOL_LAYER);
        return KeyboardLayout.row(
            SoftwareKeySpec.disabled("touch.modifier.ctrl", "Ctrl"),
            SoftwareKeySpec.disabled("touch.modifier.meta", "Meta"),
            SoftwareKeySpec.disabled("touch.modifier.alt", "Alt"),
            SoftwareKeySpec
                .enabled("touch.text.space", "space", SemanticInput.text(" "))
                .withColumnSpan(SPACE_COLUMN_SPAN),
            SoftwareKeySpec.control("touch.layout.toggle", "한/영", ControlKey.LAYOUT_TOGGLE),
            layerKey,
            SoftwareKeySpec.disabled("touch.edit.tab", "Tab"),
            SoftwareKeySpec.disabled("touch.menu", "☰")
        );
    }

    /**
     * One 3x3 cell. Digits commit text today. The arrow/navigation and function keys are drawn but
     * disabled until the raw-key action lands, so the toggles reveal the design without any key
     * silently doing nothing.
     */
    private static SoftwareKeySpec numpadKey(NumpadMode mode, int cell) {
        switch (mode) {
            case NUMBERS: {
                String digit = NUMBER_CELLS[cell];
                return symDigit("num." + digit, digit);
            }
            case ARROWS:
                return SoftwareKeySpec.disabled("touch.numpad.arrow." + cell, ARROW_CELLS[cell]);
            default:
                return SoftwareKeySpec.disabled(
                    "touch.numpad.fn." + cell,
                    "F" + FUNCTION_CELLS[cell]
                );
        }
    }

    private static SoftwareKeySpec shiftKey(boolean shifted) {
        return SoftwareKeySpec.control(
            "touch.modifier.shift",
            shifted ? "⇧•" : "⇧",
            ControlKey.SHIFT
        );
    }

    private static SoftwareKeySpec enterKey() {
        return SoftwareKeySpec.enabled(
            "touch.edit.enter",
            "⏎",
            SemanticInput.primaryAction()
        );
    }

    private static SoftwareKeySpec backspaceKey() {
        return SoftwareKeySpec.enabled(
            "touch.edit.backspace",
            "⌫",
            SemanticInput.deleteBackward()
        );
    }

    /**
     * The letter layers' period. A tap commits a period; holding it switches to the symbol layer,
     * which is where the rest of the punctuation lives.
     */
    private static SoftwareKeySpec letterPeriodKey() {
        return SoftwareKeySpec
            .enabled("touch.text.period", ".", SemanticInput.text("."))
            .withLongPressControl(ControlKey.SYMBOL_LAYER);
    }

    private static SoftwareKeySpec symDigit(String idSuffix, String digit) {
        return SoftwareKeySpec.enabled("touch.sym." + idSuffix, digit, SemanticInput.text(digit));
    }

    private static SoftwareKeySpec symText(String id, String label, String... longPress) {
        SoftwareKeySpec key = SoftwareKeySpec.enabled(
            "touch.sym." + id,
            label,
            SemanticInput.text(label)
        );
        if (longPress.length == 1 && longPress[0].isEmpty()) {
            return key;
        }
        return longPress.length == 0 ? key : key.withLongPress(longPress);
    }

    private static SoftwareKeySpec letter(String lowercase, boolean shifted) {
        String label = shifted ? lowercase.toUpperCase(java.util.Locale.ROOT) : lowercase;
        return SoftwareKeySpec.enabled(
            "touch.en." + lowercase,
            label,
            SemanticInput.text(label)
        );
    }

    private static SoftwareKeySpec consonant(String id, String label, int index) {
        return SoftwareKeySpec.enabled(
            "touch.ko2." + id,
            label,
            SemanticInput.jamo(SemanticJamo.contextualConsonant(index))
        );
    }

    private static SoftwareKeySpec vowel(String id, String label, int index) {
        return SoftwareKeySpec.enabled(
            "touch.ko2." + id,
            label,
            SemanticInput.jamo(SemanticJamo.vowel(index))
        );
    }
}
