package com.retekey;

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
    private static final KeyboardLayout UNICODE_ENTRY = buildUnicodeEntry();
    private static final KeyboardLayout CHEONJIIN_DIGITS = cheonjiin(PhoneOverlay.DIGITS);
    private static final KeyboardLayout CHEONJIIN_NAV = cheonjiin(PhoneOverlay.NAV);
    private static final KeyboardLayout NARATGEUL_DIGITS = naratgeul(PhoneOverlay.DIGITS);
    private static final KeyboardLayout NARATGEUL_NAV = naratgeul(PhoneOverlay.NAV);

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

    /** A 12-key page under an overlay: its cells as digits or the cursor cluster. */
    public static KeyboardLayout phone(KeyboardLayoutId id, PhoneOverlay overlay) {
        if (overlay == null) {
            throw new IllegalArgumentException("overlay must not be null");
        }
        if (id == KeyboardLayoutId.KO_CHEONJIIN) {
            switch (overlay) {
                case DIGITS: return CHEONJIIN_DIGITS;
                case NAV: return CHEONJIIN_NAV;
                default: return CHEONJIIN;
            }
        }
        if (id == KeyboardLayoutId.KO_NARATGEUL) {
            switch (overlay) {
                case DIGITS: return NARATGEUL_DIGITS;
                case NAV: return NARATGEUL_NAV;
                default: return NARATGEUL;
            }
        }
        throw new IllegalArgumentException("not a 12-key layout: " + id);
    }

    /**
     * The pad the keyboard shows while a code point is being typed: the sixteen hex digits, a
     * backspace, an enter that commits, and an Esc that leaves. Without it the U+ key only worked
     * with a hardware keyboard attached — the letters page has no A-F to press as digits, and the
     * keypad page has no letters at all.
     */
    public static KeyboardLayout unicodeEntry() {
        return UNICODE_ENTRY;
    }

    /** The id of the strip that shows the code being typed; the view fills in its text. */
    public static final String UNICODE_DISPLAY_ID = "touch.uni.display";

    private static KeyboardLayout buildUnicodeEntry() {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(4);
        // The code and the character it names sit on the keyboard itself. A separate window for
        // four characters of feedback put the answer somewhere else than the question.
        rows.add(Arrays.asList(
            SoftwareKeySpec.disabled(UNICODE_DISPLAY_ID, "U+").withColumnSpan(10)));
        rows.add(Arrays.asList(
            digit("uni.1", "1"), digit("uni.2", "2"), digit("uni.3", "3"), digit("uni.4", "4"),
            digit("uni.5", "5"), digit("uni.6", "6"), digit("uni.7", "7"), digit("uni.8", "8"),
            digit("uni.9", "9"), digit("uni.0", "0")));
        // Everything else on one row: the six letters a hex code can also use, and the three ways
        // the entry ends. Two rows of keys is the whole pad — it was three, half of them the empty
        // cells left over from spacing a keyboard's worth of grid around sixteen digits.
        rows.add(Arrays.asList(
            digit("uni.a", "A"), digit("uni.b", "B"), digit("uni.c", "C"), digit("uni.d", "D"),
            digit("uni.e", "E"), digit("uni.f", "F"),
            // Named rather than drawn here: this pad says OK and Cancel in words, and a lone glyph
            // between them reads as a different kind of thing than it is.
            SoftwareKeySpec.enabled("touch.edit.backspace", "Bksp", SemanticInput.deleteBackward()),
            // Cancel leaves without typing anything and gives the previous keyboard back; OK is
            // the only thing that puts a character in the document.
            rawKey("uni.cancel", "Cancel", RawKey.ESCAPE),
            // The one key that puts a character in the document gets the width to say so.
            SoftwareKeySpec.enabled("touch.uni.ok", "OK", SemanticInput.primaryAction())
                .withColumnSpan(2)));
        return KeyboardLayout.of(KeyboardLayoutId.SPECIAL_CHARS, false, COLUMNS, rows);
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

    /** 한자 conversion, beside Tab on both 12-key pages. A tap runs it; there is no hold. */
    private static SoftwareKeySpec phoneHanjaKey() {
        return SoftwareKeySpec.control("touch.phone.hanja", "漢", ControlKey.HANJA);
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

    /** 123: shows the phone keypad's digits on the pad's own keys; pressed again, the Hangul. */
    private static SoftwareKeySpec phoneDigitsToggleKey() {
        return SoftwareKeySpec.control(
            "touch.phone.overlay.digits", "123", ControlKey.PHONE_DIGITS);
    }

    /** 이동: shows the cursor cluster on the pad's own keys; pressed again, the Hangul. */
    private static SoftwareKeySpec phoneNavToggleKey() {
        return SoftwareKeySpec.control(
            "touch.phone.overlay.nav", "이동", ControlKey.PHONE_NAV);
    }

    /**
     * The twelve overlay cells, in pad order (three per row, top to bottom). DIGITS is exactly
     * the phone keypad the long-presses already carry — 1..9, then * 0 # — so the overlay types
     * with a tap what a hold types today. NAV arranges the cursor cluster the way the pad page's
     * arrow mode does (Home ↑ PgUp / ← Ins → / End ↓ PgDn), so the two never disagree, and puts
     * Esc and Del on the bottom row where * and # would be.
     */
    private static SoftwareKeySpec overlayPadCell(PhoneOverlay overlay, int cell) {
        if (overlay == PhoneOverlay.DIGITS) {
            String[] digits = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};
            return digit("phone." + digits[cell], digits[cell]).withColumnSpan(2);
        }
        String[] labels = {
            "Home", "↑", "PgUp", "←", "Ins", "→", "End", "↓", "PgDn", "Esc", "Del", ""
        };
        RawKey[] keys = {
            RawKey.HOME, RawKey.UP, RawKey.PAGE_UP,
            RawKey.LEFT, RawKey.INSERT, RawKey.RIGHT,
            RawKey.END, RawKey.DOWN, RawKey.PAGE_DOWN,
            RawKey.ESCAPE, RawKey.FORWARD_DELETE, null
        };
        if (keys[cell] == null) {
            return phoneGap("nav", 2);
        }
        return rawKey("phone.nav." + cell, labels[cell], keys[cell]).withColumnSpan(2);
    }

    /** A pad cell: the Hangul key, unless an overlay has taken the pad over. */
    private static SoftwareKeySpec padCell(
            PhoneOverlay overlay, int cell, SoftwareKeySpec letters) {
        return overlay == PhoneOverlay.NONE ? letters : overlayPadCell(overlay, cell);
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

    /**
     * Ends the syllable being composed and starts the next one, without typing anything. It sits
     * beside Alt, in the one-column cell the pad key left, so the bottom row can carry the
     * punctuation that flanks ㅇㅁ.
     */
    private static SoftwareKeySpec commitKey() {
        return SoftwareKeySpec
            .enabled("touch.phone.commit", "다음", SemanticInput.flush());
    }

    /**
     * The punctuation either side of ㅇㅁ on 천지인's bottom row, two columns each like the Hangul
     * keys around them, and behaving like them: the label holds both characters, tapping cycles
     * between them, and dragging left or right picks the one written on that side.
     */
    private static SoftwareKeySpec phoneCycleKey(String id, String characters) {
        return SoftwareKeySpec
            .enabled("touch.phone.cycle." + id, characters,
                SemanticInput.text(characters.substring(0, 1)))
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
        return cheonjiin(PhoneOverlay.NONE);
    }

    private static KeyboardLayout cheonjiin(PhoneOverlay overlay) {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(4);
        rows.add(phoneRow(0, phoneDigitsToggleKey(),
            padCell(overlay, 0, cheonjiinKey(CheonjiinInterpreter.Key.I, "ㅣ", "1")),
            padCell(overlay, 1, cheonjiinKey(CheonjiinInterpreter.Key.DOT, "ㆍ", "2")),
            padCell(overlay, 2, cheonjiinKey(CheonjiinInterpreter.Key.EU, "ㅡ", "3")),
            backspaceKey().withColumnSpan(2)));
        rows.add(phoneRow(1, phoneNavToggleKey(),
            padCell(overlay, 3, cheonjiinKey(CheonjiinInterpreter.Key.GIYEOK, "ㄱㅋ", "4")),
            padCell(overlay, 4, cheonjiinKey(CheonjiinInterpreter.Key.NIEUN, "ㄴㄹ", "5")),
            padCell(overlay, 5, cheonjiinKey(CheonjiinInterpreter.Key.DIGEUT, "ㄷㅌ", "6")),
            phoneSpaceKey()));
        rows.add(phoneRow(2, commitKey(),
            padCell(overlay, 6, cheonjiinKey(CheonjiinInterpreter.Key.BIEUP, "ㅂㅍ", "7")),
            padCell(overlay, 7, cheonjiinKey(CheonjiinInterpreter.Key.SIOT, "ㅅㅎ", "8")),
            padCell(overlay, 8, cheonjiinKey(CheonjiinInterpreter.Key.JIEUT, "ㅈㅊ", "9")),
            letterPeriodKey(),
            enterKey()));
        // ㅇㅁ sits under ㅅㅎ, with punctuation either side of it and 한자 beside Tab.
        List<SoftwareKeySpec> bottom = phoneRow(3, phoneHanjaKey(),
            padCell(overlay, 9, phoneCycleKey("period", ".,")),
            padCell(overlay, 10, cheonjiinKey(CheonjiinInterpreter.Key.IEUNG, "ㅇㅁ", "0")),
            padCell(overlay, 11, phoneCycleKey("exclaim", "!?")));
        bottom.addAll(phoneBottomPageKeys());
        rows.add(bottom);
        return KeyboardLayout.of(KeyboardLayoutId.KO_CHEONJIIN, false, COLUMNS, rows);
    }

    /**
     * 나랏글: the consonant block and the vowels beside it, with ㅡ and the two transformation keys
     * on the bottom letter row. Twelve Hangul keys need the second column, so ㅡ takes it there.
     */
    private static KeyboardLayout naratgeul() {
        return naratgeul(PhoneOverlay.NONE);
    }

    private static KeyboardLayout naratgeul(PhoneOverlay overlay) {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(4);
        rows.add(phoneRow(0, phoneDigitsToggleKey(),
            padCell(overlay, 0, naratgeulKey(NaratgeulInterpreter.Key.GIYEOK, "ㄱ", 2, "1")),
            padCell(overlay, 1, naratgeulKey(NaratgeulInterpreter.Key.NIEUN, "ㄴ", 2, "2")),
            padCell(overlay, 2, naratgeulKey(NaratgeulInterpreter.Key.A, "ㅏ", 2, "3")),
            backspaceKey().withColumnSpan(2)));
        rows.add(phoneRow(1, phoneNavToggleKey(),
            padCell(overlay, 3, naratgeulKey(NaratgeulInterpreter.Key.RIEUL, "ㄹ", 2, "4")),
            padCell(overlay, 4, naratgeulKey(NaratgeulInterpreter.Key.MIEUM, "ㅁ", 2, "5")),
            padCell(overlay, 5, naratgeulKey(NaratgeulInterpreter.Key.O, "ㅗ", 2, "6")),
            phoneSpaceKey()));
        rows.add(phoneRow(2, phoneGap("r2", 1),
            padCell(overlay, 6, naratgeulKey(NaratgeulInterpreter.Key.SIOT, "ㅅ", 2, "7")),
            padCell(overlay, 7, naratgeulKey(NaratgeulInterpreter.Key.IEUNG, "ㅇ", 2, "8")),
            padCell(overlay, 8, naratgeulKey(NaratgeulInterpreter.Key.I, "ㅣ", 2, "9")),
            letterPeriodKey(),
            enterKey()));
        List<SoftwareKeySpec> bottom = phoneRow(3, phoneHanjaKey(),
            padCell(overlay, 9, naratgeulKey(NaratgeulInterpreter.Key.STROKE, "획", 2, "*")),
            padCell(overlay, 10, naratgeulKey(NaratgeulInterpreter.Key.EU, "ㅡ", 2, "0")),
            padCell(overlay, 11, naratgeulKey(NaratgeulInterpreter.Key.TWIN, "쌍", 2, "#")));
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
        rows.add(bottomRow(bottomRowCellFor(id)));
        return KeyboardLayout.of(id, shifted, COLUMNS, rows);
    }

    /**
     * What a letter page puts in the cell beside space — the one the pad key left when the keypad
     * moved onto a hold. Each layout gets the key its own users reach for: 한자 on 2-beolsik, and
     * Esc on the Latin layouts, where it saves a trip to the keypad page every time vi is opened
     * over ssh. A layout with no obvious answer leaves it empty rather than inventing one.
     */
    private static SoftwareKeySpec bottomRowCellFor(KeyboardLayoutId id) {
        switch (id) {
            case KO_DUBEOLSIK:
                return hanjaKey();
            case EN_QWERTY:
            case EN_DVORAK:
                return rawKey("escape.letters", "Esc", RawKey.ESCAPE);
            default:
                return vacatedCell("layer");
        }
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
        // Each symbol holds the digit it shares a key with on a physical keyboard, so the row
        // reads as the number row it is and a digit is a long press away without changing page.
        rows.add(KeyboardLayout.row(
            text("bang", "!").withLongPress("1"), text("at", "@").withLongPress("2"),
            text("hash", "#").withLongPress("3"), text("dollar", "$").withLongPress("4"),
            text("percent", "%").withLongPress("5"), text("caret", "^").withLongPress("6"),
            text("amp", "&").withLongPress("7"), text("star", "*").withLongPress("8"),
            text("lparen", "(").withLongPress("9"), text("rparen", ")").withLongPress("0")
        ));
        rows.add(KeyboardLayout.row(
            text("backslash", "\\"), text("pipe", "|"), text("slash", "/"),
            text("lbracket", "["), text("rbracket", "]"), text("lbrace", "{"),
            text("rbrace", "}"), text("lt", "<"), text("gt", ">"),
            backspaceKey()
        ));
        rows.add(KeyboardLayout.row(
            shiftKey(false),
            // The two marks that punctuate a clause hold the two that end one.
            text("semicolon", ";").withLongPress(","), text("colon", ":").withLongPress("."),
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
        // The cell beside space was empty; Escape is the key this page had nowhere else to put.
        rows.add(bottomRow(rawKey("chars.esc", "Esc", RawKey.ESCAPE)));
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
                disabled("brightup", "Br+"), disabled("brightdown", "Br−"),
                disabled("volup", "Vol+"), disabled("voldown", "Vol-"),
                disabled("mute", "Mute"),
                fnKey(), fnRawKey(3), fnRawKey(4), fnRawKey(5), backspaceKey()
            ));
            rows.add(KeyboardLayout.row(
                shiftKey(false),
                disabled("prevtrack", "Prev"), disabled("playpause", "Play"),
                disabled("nexttrack", "Next"),
                rawKey("search", "Fnd", RawKey.SEARCH), disabled("back", "Back"),
                fnRawKey(6), fnRawKey(7), fnRawKey(8), enterKey()
            ));
            rows.add(bottomRow(padLayerCell(mode)));
            return KeyboardLayout.of(KeyboardLayoutId.SPECIAL_KEYS, false, COLUMNS, rows);
        }

        rows.add(KeyboardLayout.row(
            rawKey("esc", "Esc", RawKey.ESCAPE), rawKey("prtsc", "Prt", RawKey.PRINT_SCREEN),
            rawKey("scrlk", "Scr", RawKey.SCROLL_LOCK), rawKey("pause", "Brk", RawKey.BREAK),
            SoftwareKeySpec.control("touch.key.hanja", "漢", ControlKey.HANJA), numKey(),
            padCell(mode, 0), padCell(mode, 1), padCell(mode, 2),
            // Number mode needs a 0 to type; the arrow/navigation mode keeps forward-delete here.
            mode == NumpadMode.NUMBERS
                ? digit("num.0", "0")
                : rawKey("del", "Del", RawKey.FORWARD_DELETE)
        ));
        rows.add(KeyboardLayout.row(
            disabled("ralt", "RAlt"), disabled("rctrl", "RCt"), disabled("rshift", "RSh"),
            rawKey("menu", "Menu", RawKey.MENU), disabled("lang", "Lang"),
            fnKey(), padCell(mode, 3), padCell(mode, 4), padCell(mode, 5), backspaceKey()
        ));
        rows.add(KeyboardLayout.row(
            shiftKey(false), text("e", "e").withLongPress("_"),
            // The other half of each pair, where a keypad would have had room for both.
            text("plus", "+").withLongPress("*"), text("minus", "-").withLongPress("/"),
            text("equals", "="), text("period", ".").withLongPress(","),
            padCell(mode, 6), padCell(mode, 7), padCell(mode, 8), enterKey()
        ));
        rows.add(bottomRow(padLayerCell(mode)));
        return KeyboardLayout.of(KeyboardLayoutId.SPECIAL_KEYS, false, COLUMNS, rows);
    }

    /**
     * The cell beside space on the pad page. It used to be a way back to the letters, which the
     * layout key beside it already does; it now finishes each mode's own keypad — the zero the
     * digits are missing, the tenth function key, the forward delete the arrows want.
     */
    private static SoftwareKeySpec padLayerCell(NumpadMode mode) {
        switch (mode) {
            case FUNCTIONS:
                return rawKey("pad.f10", "F10", RawKey.F10);
            case ARROWS:
                return rawKey("pad.del", "Del", RawKey.FORWARD_DELETE);
            default:
                return digit("pad.0", "0");
        }
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
            menuControl("undo", "Undo", ControlKey.UNDO),
            menuControl("redo", "Redo", ControlKey.REDO),
            menuControl("date", "Date", ControlKey.INSERT_DATE),
            menuDisabled("emoji", "Emoji"),
            menuDisabled("clipboard", "Clip"),
            menuControl("copy", "Copy", ControlKey.COPY),
            menuRaw("cursor.home", "Home", RawKey.HOME),
            menuRaw("cursor.up", "↑", RawKey.UP),
            menuRaw("cursor.pageup", "PgUp", RawKey.PAGE_UP),
            menuRaw("cursor.insert", "Ins", RawKey.INSERT)
        ));
        rows.add(KeyboardLayout.row(
            menuControl("height.down", "Size−", ControlKey.HEIGHT_DOWN),
            menuControl("height.up", "Size+", ControlKey.HEIGHT_UP),
            menuControl("switchime", "Switch", ControlKey.SWITCH_IME),
            menuControl("manageime", "Manage", ControlKey.MANAGE_IME),
            menuControl("floating", "Flt", ControlKey.FLOATING_TOGGLE),
            menuControl("cut", "Cut", ControlKey.CUT),
            menuRaw("cursor.left", "←", RawKey.LEFT),
            menuControl("selectall", "SelA", ControlKey.SELECT_ALL),
            menuRaw("cursor.right", "→", RawKey.RIGHT),
            menuRaw("cursor.delete", "Del", RawKey.FORWARD_DELETE)
        ));
        rows.add(KeyboardLayout.row(
            menuDisabled("onehand.left", "1Hand"),
            menuDisabled("onehand.full", "Full"),
            menuDisabled("theme", "Theme"),
            menuControl("unicode", "Uni", ControlKey.UNICODE_INPUT),
            menuControl("notepad", "Memo", ControlKey.NOTEPAD),
            menuControl("paste", "Paste", ControlKey.PASTE),
            menuRaw("cursor.end", "End", RawKey.END),
            menuRaw("cursor.down", "↓", RawKey.DOWN),
            menuRaw("cursor.pagedown", "PgDn", RawKey.PAGE_DOWN),
            menuControl("settings", "Set", ControlKey.OPEN_SETTINGS)
        ));
        // The menu page reaches the letters with the layout key beside it, so this cell says
        // what the menu alone can start: the code-point entry.
        rows.add(bottomRow(menuControl("unicode.pad", "Uni", ControlKey.UNICODE_INPUT)));
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
