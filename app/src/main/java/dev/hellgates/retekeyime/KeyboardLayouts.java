package dev.hellgates.retekeyime;

import java.util.ArrayList;
import java.util.Arrays;
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
    /**
     * Space is three columns wide. It took two until the menu key left the bottom row; rather
     * than leave a hole beside the biggest key on the keyboard, space took the cell.
     */
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

    /**
     * The hold alternates for the letter pages, in three groups: the digits, the shifted number
     * row, and the punctuation. Their sizes are 10/9/7, which is exactly QWERTY's row shape — and
     * exactly Dvorak's 7/10/9 read in a different order, so the same three groups serve both
     * without any of them being split or padded.
     */
    private static final String HOLD_DIGITS = "1234567890";
    private static final String HOLD_SYMBOLS = "!@#$%^&*;";
    private static final String HOLD_MARKS = "_-:='\"?";

    /** Ten letters, then nine, then seven: 2-beolsik and QWERTY. */
    private static final String[] HOLDS_10_9_7 = {HOLD_DIGITS, HOLD_SYMBOLS, HOLD_MARKS};
    /** Seven, ten, nine: Dvorak, carrying the same three groups rotated to fit its rows. */
    private static final String[] HOLDS_7_10_9 = {HOLD_MARKS, HOLD_DIGITS, HOLD_SYMBOLS};

    private static final KeyboardLayout EN_BASE = english(false);
    private static final KeyboardLayout EN_SHIFTED = english(true);
    private static final KeyboardLayout KO_BASE = korean(false);
    private static final KeyboardLayout KO_SHIFTED = korean(true);
    private static final KeyboardLayout DVORAK_BASE = dvorak(false);
    private static final KeyboardLayout DVORAK_SHIFTED = dvorak(true);
    private static final KeyboardLayout CHEONJIIN = cheonjiin();
    private static final KeyboardLayout NARATGEUL = naratgeul();
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
            case EN_DVORAK:
                return shifted ? DVORAK_SHIFTED : DVORAK_BASE;
            case KO_DUBEOLSIK:
                return shifted ? KO_SHIFTED : KO_BASE;
            case KO_CHEONJIIN:
                return CHEONJIIN;
            case KO_NARATGEUL:
                return NARATGEUL;
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

    // ---- Phone letter pages: the 12-key modes ----

    private static SoftwareKeySpec phoneKey(String id, String label, int span) {
        return SoftwareKeySpec.enabled(id, label, SemanticInput.text(label)).withColumnSpan(span);
    }

    private static SoftwareKeySpec cheonjiinKey(CheonjiinInterpreter.Key key, String label) {
        return phoneKey(
            "touch.cheonjiin." + key.name().toLowerCase(java.util.Locale.ROOT), label, 2);
    }

    /**
     * A 천지인 key with the keypad digit it holds. The ten Hangul keys sit where a phone keypad's
     * do — ㅣ ㆍ ㅡ across the top, then the consonant groups, then ㅇㅁ at the bottom — so they
     * carry 1 to 9 and 0, the same numbers those keys have always carried.
     */
    private static SoftwareKeySpec cheonjiinKey(
            CheonjiinInterpreter.Key key, String label, String hold) {
        return cheonjiinKey(key, label).withLongPress(hold);
    }

    private static SoftwareKeySpec naratgeulKey(NaratgeulInterpreter.Key key, String label,
            int span) {
        return phoneKey(
            "touch.naratgeul." + key.name().toLowerCase(java.util.Locale.ROOT), label, span);
    }

    /**
     * A 나랏글 key with the keypad character it holds. The page's twelve keys sit exactly where a
     * phone keypad's do, so they carry exactly what a phone keypad carries: 1-9 across the jamo,
     * 0 under ㅡ in the middle of the bottom row, and * and # either side of it.
     */
    private static SoftwareKeySpec naratgeulKey(NaratgeulInterpreter.Key key, String label,
            int span, String hold) {
        return naratgeulKey(key, label, span).withLongPress(hold);
    }

    /** The modifier that owns this row's leftmost cell. */
    private static SoftwareKeySpec phoneModifier(int row) {
        switch (row) {
            case 0:
                return SoftwareKeySpec.control("touch.modifier.ctrl", "Ctrl", ControlKey.CTRL);
            case 1:
                return SoftwareKeySpec.control("touch.modifier.meta", "Meta", ControlKey.META);
            case 2:
                return SoftwareKeySpec.control("touch.modifier.alt", "Alt", ControlKey.ALT);
            default:
                return tabKey();
        }
    }

    /**
     * An empty cell in a 12-key bottom row that still answers a hold: 한자 conversion, marked 漢
     * in the corner. A tap does nothing, which is what an empty cell should do — the conversion is
     * deliberate enough to be worth a hold, and a stray thumb cannot trigger it.
     */
    private static SoftwareKeySpec hanjaHoldGap(String id, int span) {
        return SoftwareKeySpec
            .disabled("touch.phone.gap." + id, " ")
            .withColumnSpan(span)
            .withLongPressControl(ControlKey.HANJA)
            .withLongPressHint("漢");
    }

    /**
     * Tab. A tap types one, chording with whatever modifier is armed; a hold latches it down and
     * leaves it there until the next hold lets it up, which is the only way to hold a key on a
     * keyboard with no key to hold.
     */
    private static SoftwareKeySpec tabKey() {
        return SoftwareKeySpec
            .enabled("touch.edit.tab", "Tab", SemanticInput.rawKey(RawKey.TAB))
            .withLongPressControl(ControlKey.TAB_HOLD);
    }

    /** An empty cell: the column a 12-key page does not need, left blank rather than padded out. */
    private static SoftwareKeySpec phoneGap(String id, int span) {
        return SoftwareKeySpec.disabled("touch.phone.gap." + id, " ").withColumnSpan(span);
    }

    /**
     * The globe: a tap moves to the next layout, a hold opens the menu page. The menu had a key
     * of its own and no longer needs one — it is opened rarely and from anywhere, which is what a
     * hold is for. The corner "m" says where the hold goes, since a control has no character of
     * its own to show there. Holding it used to reach 한자; that key still sits on the pad page.
     */
    private static SoftwareKeySpec layoutToggleKey() {
        return SoftwareKeySpec
            .control("touch.layout.toggle", "\uD83C\uDF10", ControlKey.LAYOUT_TOGGLE)
            .withLongPressControl(ControlKey.MENU_LAYER)
            .withLongPressHint("m");
    }

    /** !#: a tap opens the symbols page, a hold opens the pad — marked "p" in the corner. */
    private static SoftwareKeySpec specialCharsKey() {
        return SoftwareKeySpec
            .control("touch.layer.chars", "!#", ControlKey.SPECIAL_CHARS_LAYER)
            .withLongPressControl(ControlKey.SPECIAL_KEYS_LAYER)
            .withLongPressHint("p");
    }

    /**
     * Converts the reading before the cursor to Hanja. Labelled 漢 — the character for itself,
     * which is the shortest way to say what the key does and needs no translating.
     */
    private static SoftwareKeySpec hanjaKey() {
        return SoftwareKeySpec.control("touch.key.hanja.letters", "漢", ControlKey.HANJA);
    }

    /** A cell left empty because the key that was in it moved onto another key's hold. */
    private static SoftwareKeySpec vacatedCell(String id) {
        return SoftwareKeySpec.disabled("touch.gap." + id, " ");
    }

    /** The two page keys that close a 12-key page's bottom row, one column each. */
    private static List<SoftwareKeySpec> phoneBottomPageKeys() {
        return Arrays.asList(
            specialCharsKey(),
            layoutToggleKey()
        );
    }

    /** Ends the syllable being composed and starts the next one, without typing anything. */
    private static SoftwareKeySpec commitKey() {
        return SoftwareKeySpec
            .enabled("touch.phone.commit", "다음", SemanticInput.flush())
            .withColumnSpan(2);
    }

    private static SoftwareKeySpec phoneSpaceKey() {
        return SoftwareKeySpec
            .enabled("touch.text.space", "space", SemanticInput.text(" "))
            .withColumnSpan(2);
    }

    /** Puts one 12-key row together: the modifier, the row's own cells, and nothing else. */
    private static List<SoftwareKeySpec> phoneRow(int row, SoftwareKeySpec... cells) {
        List<SoftwareKeySpec> keys = new ArrayList<>();
        keys.add(phoneModifier(row));
        keys.addAll(Arrays.asList(cells));
        return keys;
    }

    /**
     * 천지인: the vowel elements across the top, the grouped consonants below, and ㅇㅁ on the
     * bottom letter row. Ten Hangul keys leave the second column empty throughout.
     */
    private static KeyboardLayout cheonjiin() {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(4);
        rows.add(phoneRow(0, phoneGap("menu", 1),
            cheonjiinKey(CheonjiinInterpreter.Key.I, "ㅣ", "1"),
            cheonjiinKey(CheonjiinInterpreter.Key.DOT, "ㆍ", "2"),
            cheonjiinKey(CheonjiinInterpreter.Key.EU, "ㅡ", "3"),
            backspaceKey().withColumnSpan(2)));
        rows.add(phoneRow(1, phoneGap("pad", 1),
            cheonjiinKey(CheonjiinInterpreter.Key.GIYEOK, "ㄱㅋ", "4"),
            cheonjiinKey(CheonjiinInterpreter.Key.NIEUN, "ㄴㄹ", "5"),
            cheonjiinKey(CheonjiinInterpreter.Key.DIGEUT, "ㄷㅌ", "6"),
            phoneSpaceKey()));
        rows.add(phoneRow(2, phoneGap("r2", 1),
            cheonjiinKey(CheonjiinInterpreter.Key.BIEUP, "ㅂㅍ", "7"),
            cheonjiinKey(CheonjiinInterpreter.Key.SIOT, "ㅅㅎ", "8"),
            cheonjiinKey(CheonjiinInterpreter.Key.JIEUT, "ㅈㅊ", "9"),
            letterPeriodKey(),
            enterKey()));
        // ㅇㅁ sits under ㅅㅎ; to its right the key that ends the syllable being composed and
        // moves on, so two same-key letters can follow each other without a space between them.
        List<SoftwareKeySpec> bottom = phoneRow(3, phoneGap("r3", 1),
            hanjaHoldGap("r3b", 2),
            cheonjiinKey(CheonjiinInterpreter.Key.IEUNG, "ㅇㅁ", "0"),
            commitKey());
        bottom.addAll(phoneBottomPageKeys());
        rows.add(bottom);
        return KeyboardLayout.of(KeyboardLayoutId.KO_CHEONJIIN, false, COLUMNS, rows);
    }

    /**
     * 나랏글: the consonant block and the vowels beside it, with ㅡ and the two transformation keys
     * on the bottom letter row. Twelve Hangul keys need the second column, so ㅡ takes it there.
     */
    private static KeyboardLayout naratgeul() {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(4);
        rows.add(phoneRow(0, phoneGap("menu", 1),
            naratgeulKey(NaratgeulInterpreter.Key.GIYEOK, "ㄱ", 2, "1"),
            naratgeulKey(NaratgeulInterpreter.Key.NIEUN, "ㄴ", 2, "2"),
            naratgeulKey(NaratgeulInterpreter.Key.A, "ㅏ", 2, "3"),
            backspaceKey().withColumnSpan(2)));
        rows.add(phoneRow(1, phoneGap("pad", 1),
            naratgeulKey(NaratgeulInterpreter.Key.RIEUL, "ㄹ", 2, "4"),
            naratgeulKey(NaratgeulInterpreter.Key.MIEUM, "ㅁ", 2, "5"),
            naratgeulKey(NaratgeulInterpreter.Key.O, "ㅗ", 2, "6"),
            phoneSpaceKey()));
        rows.add(phoneRow(2, phoneGap("r2", 1),
            naratgeulKey(NaratgeulInterpreter.Key.SIOT, "ㅅ", 2, "7"),
            naratgeulKey(NaratgeulInterpreter.Key.IEUNG, "ㅇ", 2, "8"),
            naratgeulKey(NaratgeulInterpreter.Key.I, "ㅣ", 2, "9"),
            letterPeriodKey(),
            enterKey()));
        List<SoftwareKeySpec> bottom = phoneRow(3, hanjaHoldGap("r3", 1),
            naratgeulKey(NaratgeulInterpreter.Key.STROKE, "획", 2, "*"),
            naratgeulKey(NaratgeulInterpreter.Key.EU, "ㅡ", 2, "0"),
            naratgeulKey(NaratgeulInterpreter.Key.TWIN, "쌍", 2, "#"));
        bottom.addAll(phoneBottomPageKeys());
        rows.add(bottom);
        return KeyboardLayout.of(KeyboardLayoutId.KO_NARATGEUL, false, COLUMNS, rows);
    }

    // ---- Letter pages ----

    private static KeyboardLayout english(boolean shifted) {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(3);
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
            backspaceKey()
        ));
        rows.add(KeyboardLayout.row(
            shiftKey(shifted),
            letter("z", shifted), letter("x", shifted), letter("c", shifted),
            letter("v", shifted), letter("b", shifted), letter("n", shifted),
            letter("m", shifted),
            letterPeriodKey(), enterKey()
        ));
        return letterPage(KeyboardLayoutId.EN_QWERTY, shifted, rows, HOLDS_10_9_7);
    }

    /**
     * Dvorak in its own 7/10/9 shape. The three cells the top row does not need for letters carry
     * Enter, backspace and the period, on the left where they are out of the way of the letters.
     */
    private static KeyboardLayout dvorak(boolean shifted) {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(3);
        rows.add(KeyboardLayout.row(
            enterKey(), backspaceKey(), letterPeriodKey(),
            letter("p", shifted), letter("y", shifted), letter("f", shifted),
            letter("g", shifted), letter("c", shifted), letter("r", shifted),
            letter("l", shifted)
        ));
        rows.add(KeyboardLayout.row(
            letter("a", shifted), letter("o", shifted), letter("e", shifted),
            letter("u", shifted), letter("i", shifted), letter("d", shifted),
            letter("h", shifted), letter("t", shifted), letter("n", shifted),
            letter("s", shifted)
        ));
        rows.add(KeyboardLayout.row(
            shiftKey(shifted),
            letter("q", shifted), letter("j", shifted), letter("k", shifted),
            letter("x", shifted), letter("b", shifted), letter("m", shifted),
            letter("w", shifted), letter("v", shifted), letter("z", shifted)
        ));
        return letterPage(KeyboardLayoutId.EN_DVORAK, shifted, rows, HOLDS_7_10_9);
    }

    /** Adds the hold groups and the fixed bottom row to a page's three letter rows. */
    private static KeyboardLayout letterPage(
        KeyboardLayoutId id,
        boolean shifted,
        List<List<SoftwareKeySpec>> letterRows,
        String[] groups
    ) {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(withHolds(letterRows, groups));
        // 2-beolsik is the layout Hanja is for, and the cell the pad key left is right beside the
        // symbols key. The other letter pages leave it empty.
        rows.add(bottomRow(id == KeyboardLayoutId.KO_DUBEOLSIK
            ? hanjaKey()
            : vacatedCell("layer")));
        return KeyboardLayout.of(id, shifted, COLUMNS, rows);
    }

    private static KeyboardLayout korean(boolean shifted) {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(3);
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
            backspaceKey()
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
            letterPeriodKey(), enterKey()
        ));
        return letterPage(KeyboardLayoutId.KO_DUBEOLSIK, shifted, rows, HOLDS_10_9_7);
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
            backspaceKey()
        ));
        rows.add(KeyboardLayout.row(
            shiftKey(false),
            text("semicolon", ";"), text("colon", ":"),
            text("backtick", "`"),
            text("apostrophe", "'").withLongPress("="),
            text("quote", "\"").withLongPress("÷"),
            text("question", "?").withLongPress("×"),
            text("tilde", "~").withLongPress("+"),
            // Minus lives on the underscore's hold, where a physical keyboard puts it too, and
            // the pair sits beside Enter rather than at the far end of the row.
            text("underscore", "_").withLongPress("-"),
            enterKey()
        ));
        rows.add(bottomRow(vacatedCell("layer")));
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
                disabled("brightup", "Bright+"), disabled("brightdown", "Bright−"),
                disabled("volup", "Vol+"), disabled("voldown", "Vol-"),
                disabled("mute", "Mute"),
                fnKey(), fnRawKey(3), fnRawKey(4), fnRawKey(5), backspaceKey()
            ));
            rows.add(KeyboardLayout.row(
                shiftKey(false),
                disabled("prevtrack", "Prev"), disabled("playpause", "Play"),
                disabled("nexttrack", "Next"),
                rawKey("search", "Search", RawKey.SEARCH), disabled("back", "Back"),
                fnRawKey(6), fnRawKey(7), fnRawKey(8), enterKey()
            ));
            rows.add(bottomRow(returnToLettersKey()));
            return KeyboardLayout.of(KeyboardLayoutId.SPECIAL_KEYS, false, COLUMNS, rows);
        }

        rows.add(KeyboardLayout.row(
            rawKey("esc", "Esc", RawKey.ESCAPE), rawKey("prtsc", "PrtSc", RawKey.PRINT_SCREEN),
            rawKey("scrlk", "ScrLk", RawKey.SCROLL_LOCK), rawKey("pause", "Pause", RawKey.BREAK),
            SoftwareKeySpec.control("touch.key.hanja", "Hanja", ControlKey.HANJA), numKey(),
            padCell(mode, 0), padCell(mode, 1), padCell(mode, 2),
            // Number mode needs a 0 to type; the arrow/navigation mode keeps forward-delete here.
            mode == NumpadMode.NUMBERS
                ? digit("num.0", "0")
                : rawKey("del", "Del", RawKey.FORWARD_DELETE)
        ));
        rows.add(KeyboardLayout.row(
            disabled("ralt", "RAlt"), disabled("rctrl", "RCtrl"), disabled("rshift", "RShft"),
            rawKey("menu", "Menu", RawKey.MENU), disabled("lang", "Lang"),
            fnKey(), padCell(mode, 3), padCell(mode, 4), padCell(mode, 5), backspaceKey()
        ));
        rows.add(KeyboardLayout.row(
            shiftKey(false), text("e", "e"),
            text("plus", "+"), text("minus", "-"), text("equals", "="), text("period", "."),
            padCell(mode, 6), padCell(mode, 7), padCell(mode, 8), enterKey()
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
        // The right half is the editing hand: the arrows in a cross with select-all at their
        // centre, copy/cut/paste down the near edge, the jump keys around them, and settings in
        // the corner where a thumb reaches without looking.
        rows.add(KeyboardLayout.row(
            menuControl("undo", "↶", ControlKey.UNDO),
            menuControl("redo", "↷", ControlKey.REDO),
            menuControl("date", "📅", ControlKey.INSERT_DATE),
            menuDisabled("emoji", "☺"),
            menuDisabled("clipboard", "🗒"),
            menuControl("copy", "⧉", ControlKey.COPY),
            menuRaw("cursor.home", "Home", RawKey.HOME),
            menuRaw("cursor.up", "↑", RawKey.UP),
            menuRaw("cursor.pageup", "PgUp", RawKey.PAGE_UP),
            menuRaw("cursor.insert", "Ins", RawKey.INSERT)
        ));
        rows.add(KeyboardLayout.row(
            menuControl("height.down", "size−", ControlKey.HEIGHT_DOWN),
            menuControl("height.up", "size+", ControlKey.HEIGHT_UP),
            menuControl("switchime", "⌨↔", ControlKey.SWITCH_IME),
            menuControl("manageime", "⌨⚙", ControlKey.MANAGE_IME),
            menuControl("floating", "❐", ControlKey.FLOATING_TOGGLE),
            menuControl("cut", "✂", ControlKey.CUT),
            menuRaw("cursor.left", "←", RawKey.LEFT),
            menuControl("selectall", "⬚A", ControlKey.SELECT_ALL),
            menuRaw("cursor.right", "→", RawKey.RIGHT),
            menuRaw("cursor.delete", "Del", RawKey.FORWARD_DELETE)
        ));
        rows.add(KeyboardLayout.row(
            menuDisabled("onehand.left", "◀|"),
            menuDisabled("onehand.full", "|↔|"),
            menuDisabled("theme", "◐"),
            menuDisabled("custom1", "★1"),
            menuDisabled("custom2", "★2"),
            menuControl("paste", "📋", ControlKey.PASTE),
            menuRaw("cursor.end", "End", RawKey.END),
            menuRaw("cursor.down", "↓", RawKey.DOWN),
            menuRaw("cursor.pagedown", "PgDn", RawKey.PAGE_DOWN),
            menuControl("settings", "Set", ControlKey.OPEN_SETTINGS)
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
            tabKey(),
            SoftwareKeySpec
                .enabled("touch.text.space", "space", SemanticInput.text(" "))
                .withColumnSpan(SPACE_COLUMN_SPAN),
            layerKey,
            specialCharsKey(),
            layoutToggleKey()
        );
    }

    private static SoftwareKeySpec returnToLettersKey() {
        return SoftwareKeySpec.control("touch.layer.letters", "ABC", ControlKey.PREVIOUS_LAYER);
    }

    /** The in-page return key ("영문자" position); distinct id from the shared bottom-row one. */

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

    /** Gives each row its own group of alternates, laid over that row's letter keys in order. */
    private static List<List<SoftwareKeySpec>> withHolds(
        List<List<SoftwareKeySpec>> rows,
        String[] groups
    ) {
        List<List<SoftwareKeySpec>> held = new ArrayList<>(rows.size());
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<SoftwareKeySpec> updated = new ArrayList<>(rows.get(rowIndex));
            String group = rowIndex < groups.length ? groups[rowIndex] : "";
            int next = 0;
            for (int i = 0; i < updated.size(); i++) {
                SoftwareKeySpec key = updated.get(i);
                if (!takesHold(key) || next >= group.length()) {
                    continue;
                }
                updated.set(i, key.withLongPress(String.valueOf(group.charAt(next++))));
            }
            held.add(updated);
        }
        return held;
    }

    /** A key takes an alternate when it types a letter and does not already carry one. */
    private static boolean takesHold(SoftwareKeySpec key) {
        if (key.isControl() || !key.enabled() || key.hasLongPress() || key.hasLongPressControl()) {
            return false;
        }
        SemanticInput input = key.semanticInput();
        if (input == null) {
            return false;
        }
        return input.kind() == SemanticInput.Kind.TEXT || input.kind() == SemanticInput.Kind.JAMO;
    }

    private static SoftwareKeySpec letterPeriodKey() {
        return SoftwareKeySpec
            .enabled("touch.text.period", ".", SemanticInput.text("."))
            .withLongPress(",");
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
