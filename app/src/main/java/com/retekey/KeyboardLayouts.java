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
    /** Nine, ten, seven: Colemak — the symbols up top, the digits on the home row, the marks below. */
    private static final String[] HOLDS_9_10_7 = {HOLD_SYMBOLS, HOLD_DIGITS, HOLD_MARKS};
    /**
     * Ten, ten, six: AZERTY. The digits up top as ever; the home row has ten letters and the symbol
     * group nine, so m takes the apostrophe from the marks; the bottom row has six letters and
     * takes the first six marks.
     */
    private static final String[] HOLDS_10_10_6 = {HOLD_DIGITS, HOLD_SYMBOLS + "'", HOLD_MARKS};

    private static final KeyboardLayout EN_BASE = english(false);
    private static final KeyboardLayout EN_SHIFTED = english(true);
    private static final KeyboardLayout KO_BASE = korean(false);
    private static final KeyboardLayout KO_SHIFTED = korean(true);
    private static final KeyboardLayout DVORAK_BASE = dvorak(false);
    private static final KeyboardLayout DVORAK_SHIFTED = dvorak(true);
    private static final KeyboardLayout COLEMAK_BASE = colemak(false);
    private static final KeyboardLayout COLEMAK_SHIFTED = colemak(true);
    private static final KeyboardLayout ES_BASE = spanish(false);
    private static final KeyboardLayout ES_SHIFTED = spanish(true);
    private static final KeyboardLayout PT_BASE = qwertyWithAccents(KeyboardLayoutId.PT_QWERTY, false, LatinAccents.PORTUGUESE);
    private static final KeyboardLayout PT_SHIFTED = qwertyWithAccents(KeyboardLayoutId.PT_QWERTY, true, LatinAccents.PORTUGUESE);
    private static final KeyboardLayout IT_BASE = qwertyWithAccents(KeyboardLayoutId.IT_QWERTY, false, LatinAccents.ITALIAN);
    private static final KeyboardLayout IT_SHIFTED = qwertyWithAccents(KeyboardLayoutId.IT_QWERTY, true, LatinAccents.ITALIAN);
    private static final KeyboardLayout PL_BASE = qwertyWithAccents(KeyboardLayoutId.PL_QWERTY, false, LatinAccents.POLISH);
    private static final KeyboardLayout PL_SHIFTED = qwertyWithAccents(KeyboardLayoutId.PL_QWERTY, true, LatinAccents.POLISH);
    private static final KeyboardLayout VI_BASE = qwertyWithAccents(KeyboardLayoutId.VI_TELEX, false, LatinAccents.VIETNAMESE);
    private static final KeyboardLayout VI_SHIFTED = qwertyWithAccents(KeyboardLayoutId.VI_TELEX, true, LatinAccents.VIETNAMESE);
    private static final KeyboardLayout DE_BASE = german(false);
    private static final KeyboardLayout DE_SHIFTED = german(true);
    private static final KeyboardLayout TR_BASE = turkish(false);
    private static final KeyboardLayout TR_SHIFTED = turkish(true);
    private static final KeyboardLayout FR_BASE = french(false);
    private static final KeyboardLayout FR_SHIFTED = french(true);
    private static final KeyboardLayout EL_BASE = greek(false);
    private static final KeyboardLayout EL_SHIFTED = greek(true);
    private static final KeyboardLayout HEBREW = hebrew();
    private static final KeyboardLayout PERSIAN = persian();
    private static final KeyboardLayout JA_BASE = qwertyWithAccents(KeyboardLayoutId.JA_ROMAJI, false, java.util.Collections.emptyMap());
    private static final KeyboardLayout JA_SHIFTED = qwertyWithAccents(KeyboardLayoutId.JA_ROMAJI, true, java.util.Collections.emptyMap());
    private static final KeyboardLayout JA_FLICK_LAYOUT = kanaFlick(PhoneOverlay.NONE);
    private static final KeyboardLayout JA_FLICK_DIGITS = kanaFlick(PhoneOverlay.DIGITS);
    private static final KeyboardLayout JA_FLICK_NAV = kanaFlick(PhoneOverlay.NAV);
    private static final KeyboardLayout CHEONJIIN = cheonjiin();
    private static final KeyboardLayout NARATGEUL = naratgeul();
    private static final KeyboardLayout PAD_ARROWS_LAYOUT =
        padLayout(KeyboardLayoutId.PAD_ARROWS, false);
    private static final KeyboardLayout PAD_KEYPAD_LAYOUT =
        padLayout(KeyboardLayoutId.PAD_KEYPAD, true);
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
            case EN_COLEMAK:
                return shifted ? COLEMAK_SHIFTED : COLEMAK_BASE;
            case ES_QWERTY:
                return shifted ? ES_SHIFTED : ES_BASE;
            case PT_QWERTY:
                return shifted ? PT_SHIFTED : PT_BASE;
            case IT_QWERTY:
                return shifted ? IT_SHIFTED : IT_BASE;
            case PL_QWERTY:
                return shifted ? PL_SHIFTED : PL_BASE;
            case VI_TELEX:
                return shifted ? VI_SHIFTED : VI_BASE;
            case DE_QWERTZ:
                return shifted ? DE_SHIFTED : DE_BASE;
            case TR_QWERTY:
                return shifted ? TR_SHIFTED : TR_BASE;
            case FR_AZERTY:
                return shifted ? FR_SHIFTED : FR_BASE;
            case EL_QWERTY:
                return shifted ? EL_SHIFTED : EL_BASE;
            case HE_STANDARD:
                // Hebrew has no capitals: one page, whatever Shift says.
                return HEBREW;
            case FA_ISIRI:
                // Neither has Persian: one page.
                return PERSIAN;
            case JA_ROMAJI:
                return shifted ? JA_SHIFTED : JA_BASE;
            case JA_FLICK:
                return JA_FLICK_LAYOUT;
            case KO_DUBEOLSIK:
                return shifted ? KO_SHIFTED : KO_BASE;
            case KO_CHEONJIIN:
                return CHEONJIIN;
            case KO_NARATGEUL:
                return NARATGEUL;
            case PAD_ARROWS:
                return PAD_ARROWS_LAYOUT;
            case PAD_KEYPAD:
                return PAD_KEYPAD_LAYOUT;
            case SPECIAL_CHARS:
                return CHARS;
            default:
                return KEYS_NUMBERS;
        }
    }

    /**
     * The keypad and the cursor cluster as layouts in their own right, on 나랏글's frame.
     *
     * <p>They exist as overlays already — a toggle in a Hangul pad's left column swaps the twelve
     * cells and swaps back — which is right for a glance at the arrows mid-word and wrong for
     * sitting in a number pad for a while: an overlay is reachable only from a Hangul pad, and it
     * is a mode on top of a layout rather than a layout. These are stops on the globe key, off
     * unless the user turns them on.
     */
    private static KeyboardLayout padLayout(KeyboardLayoutId id, boolean digits) {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(4);
        rows.add(phoneRow(0, phoneGap("pad0", 1),
            padLayoutCell(digits, 0), padLayoutCell(digits, 1), padLayoutCell(digits, 2),
            backspaceKey().withColumnSpan(2)));
        rows.add(phoneRow(1, phoneGap("pad1", 1),
            padLayoutCell(digits, 3), padLayoutCell(digits, 4), padLayoutCell(digits, 5),
            phoneSpaceKey()));
        rows.add(phoneRow(2, phoneGap("pad2", 1),
            padLayoutCell(digits, 6), padLayoutCell(digits, 7), padLayoutCell(digits, 8),
            letterPeriodKey(),
            enterKey()));
        List<SoftwareKeySpec> bottom = phoneRow(3, phoneGap("pad3", 1),
            padLayoutCell(digits, 9), padLayoutCell(digits, 10), padLayoutCell(digits, 11));
        bottom.addAll(phoneBottomPageKeys());
        rows.add(bottom);
        return KeyboardLayout.of(id, false, COLUMNS, rows);
    }

    /**
     * One cell of those two layouts. The keypad's digits type with a tap and hold the same
     * calculator character the Hangul pads hold in that place, so a hold means the same thing
     * wherever the user is; the cursor cluster is the one the overlay already draws, so the two
     * can never disagree about where ← is.
     */
    private static SoftwareKeySpec padLayoutCell(boolean digits, int cell) {
        return digits ? keypadCell(cell) : overlayPadCell(PhoneOverlay.NAV, cell);
    }

    /**
     * One keypad cell: the digit on a tap and the calculator character on a hold.
     *
     * <p>There is one of these rather than two. The 123 overlay and the Keypad layout are the same
     * pad reached two ways, and a cell that held nothing under the overlay while its twin held `+`
     * was a difference with no reason behind it.
     */
    private static SoftwareKeySpec keypadCell(int cell) {
        return digit("pad.num." + cell, PadHolds.digit(cell))
            .withColumnSpan(2)
            .withLongPress(PadHolds.symbol(cell));
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
        if (id == KeyboardLayoutId.JA_FLICK) {
            switch (overlay) {
                case DIGITS: return JA_FLICK_DIGITS;
                case NAV: return JA_FLICK_NAV;
                default: return JA_FLICK_LAYOUT;
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

    /**
     * 漢 on the 12-key pads, in the cell beside Tab. It is one of only three places Hanja
     * conversion can be reached from, so it belongs where Hangul is being typed — and nowhere else:
     * while an overlay has turned the pad into a keypad or a cursor cluster there is no reading in
     * front of the cursor to convert, so the cell is blank there.
     */
    private static SoftwareKeySpec phoneHanjaKey(PhoneOverlay overlay) {
        if (overlay != PhoneOverlay.NONE) {
            return phoneGap("hanja", 1);
        }
        return SoftwareKeySpec.control("touch.phone.hanja", "漢", ControlKey.HANJA);
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

    /** Move: shows the cursor cluster on the pad's own keys; pressed again, the Hangul. */
    private static SoftwareKeySpec phoneNavToggleKey() {
        return SoftwareKeySpec.control(
            "touch.phone.overlay.nav", "Move", ControlKey.PHONE_NAV);
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
            return keypadCell(cell);
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
            .enabled("touch.phone.commit", "Next", SemanticInput.flush());
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
            padCell(overlay, 0, cheonjiinKey(CheonjiinInterpreter.Key.I, "ㅣ", PadHolds.digit(0))),
            padCell(overlay, 1, cheonjiinKey(CheonjiinInterpreter.Key.DOT, "ㆍ", PadHolds.digit(1))),
            padCell(overlay, 2, cheonjiinKey(CheonjiinInterpreter.Key.EU, "ㅡ", PadHolds.digit(2))),
            backspaceKey().withColumnSpan(2)));
        rows.add(phoneRow(1, phoneNavToggleKey(),
            padCell(overlay, 3, cheonjiinKey(CheonjiinInterpreter.Key.GIYEOK, "ㄱㅋ", PadHolds.digit(3))),
            padCell(overlay, 4, cheonjiinKey(CheonjiinInterpreter.Key.NIEUN, "ㄴㄹ", PadHolds.digit(4))),
            padCell(overlay, 5, cheonjiinKey(CheonjiinInterpreter.Key.DIGEUT, "ㄷㅌ", PadHolds.digit(5))),
            phoneSpaceKey()));
        rows.add(phoneRow(2, commitKey(),
            padCell(overlay, 6, cheonjiinKey(CheonjiinInterpreter.Key.BIEUP, "ㅂㅍ", PadHolds.digit(6))),
            padCell(overlay, 7, cheonjiinKey(CheonjiinInterpreter.Key.SIOT, "ㅅㅎ", PadHolds.digit(7))),
            padCell(overlay, 8, cheonjiinKey(CheonjiinInterpreter.Key.JIEUT, "ㅈㅊ", PadHolds.digit(8))),
            letterPeriodKey(),
            enterKey()));
        // ㅇㅁ sits under ㅅㅎ, with punctuation either side of it and 漢 beside Tab.
        List<SoftwareKeySpec> bottom = phoneRow(3, phoneHanjaKey(overlay),
            padCell(overlay, 9, phoneCycleKey("period", ".,")),
            padCell(overlay, 10, cheonjiinKey(CheonjiinInterpreter.Key.IEUNG, "ㅇㅁ", PadHolds.digit(10))),
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
            padCell(overlay, 0, naratgeulKey(NaratgeulInterpreter.Key.GIYEOK, "ㄱ", 2, PadHolds.digit(0))),
            padCell(overlay, 1, naratgeulKey(NaratgeulInterpreter.Key.NIEUN, "ㄴ", 2, PadHolds.digit(1))),
            padCell(overlay, 2, naratgeulKey(NaratgeulInterpreter.Key.A, "ㅏ", 2, PadHolds.digit(2))),
            backspaceKey().withColumnSpan(2)));
        rows.add(phoneRow(1, phoneNavToggleKey(),
            padCell(overlay, 3, naratgeulKey(NaratgeulInterpreter.Key.RIEUL, "ㄹ", 2, PadHolds.digit(3))),
            padCell(overlay, 4, naratgeulKey(NaratgeulInterpreter.Key.MIEUM, "ㅁ", 2, PadHolds.digit(4))),
            padCell(overlay, 5, naratgeulKey(NaratgeulInterpreter.Key.O, "ㅗ", 2, PadHolds.digit(5))),
            phoneSpaceKey()));
        rows.add(phoneRow(2, phoneGap("r2", 1),
            padCell(overlay, 6, naratgeulKey(NaratgeulInterpreter.Key.SIOT, "ㅅ", 2, PadHolds.digit(6))),
            padCell(overlay, 7, naratgeulKey(NaratgeulInterpreter.Key.IEUNG, "ㅇ", 2, PadHolds.digit(7))),
            padCell(overlay, 8, naratgeulKey(NaratgeulInterpreter.Key.I, "ㅣ", 2, PadHolds.digit(8))),
            letterPeriodKey(),
            enterKey()));
        List<SoftwareKeySpec> bottom = phoneRow(3, phoneHanjaKey(overlay),
            padCell(overlay, 9, naratgeulKey(NaratgeulInterpreter.Key.STROKE, "획", 2, PadHolds.digit(9))),
            padCell(overlay, 10, naratgeulKey(NaratgeulInterpreter.Key.EU, "ㅡ", 2, PadHolds.digit(10))),
            padCell(overlay, 11, naratgeulKey(NaratgeulInterpreter.Key.TWIN, "쌍", 2, PadHolds.digit(11))));
        bottom.addAll(phoneBottomPageKeys());
        rows.add(bottom);
        return KeyboardLayout.of(KeyboardLayoutId.KO_NARATGEUL, false, COLUMNS, rows);
    }

    /**
     * The Japanese 12-key flick pad, in the same frame as 천지인: the ten kana keys on the phone
     * cells, the ゛゜小 modifier and the cycling 、。？！ key on the bottom pad row beside わ, and
     * the same surround — toggles, space, Next, ⌫ ⏎ and the page keys. Flick-only, so a tap is
     * always the あ-column kana and every press is one character; the digits ride the holds the
     * way they do on every 12-key page.
     */
    private static KeyboardLayout kanaFlick(PhoneOverlay overlay) {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(4);
        rows.add(phoneRow(0, phoneDigitsToggleKey(),
            padCell(overlay, 0, kanaKey(KanaFlick.Key.A, PadHolds.digit(0))),
            padCell(overlay, 1, kanaKey(KanaFlick.Key.KA, PadHolds.digit(1))),
            padCell(overlay, 2, kanaKey(KanaFlick.Key.SA, PadHolds.digit(2))),
            backspaceKey().withColumnSpan(2)));
        rows.add(phoneRow(1, phoneNavToggleKey(),
            padCell(overlay, 3, kanaKey(KanaFlick.Key.TA, PadHolds.digit(3))),
            padCell(overlay, 4, kanaKey(KanaFlick.Key.NA, PadHolds.digit(4))),
            padCell(overlay, 5, kanaKey(KanaFlick.Key.HA, PadHolds.digit(5))),
            phoneSpaceKey()));
        rows.add(phoneRow(2, commitKey(),
            padCell(overlay, 6, kanaKey(KanaFlick.Key.MA, PadHolds.digit(6))),
            padCell(overlay, 7, kanaKey(KanaFlick.Key.YA, PadHolds.digit(7))),
            padCell(overlay, 8, kanaKey(KanaFlick.Key.RA, PadHolds.digit(8))),
            letterPeriodKey(),
            enterKey()));
        List<SoftwareKeySpec> bottom = phoneRow(3, vacatedCell("kana"),
            padCell(overlay, 9, SoftwareKeySpec
                .control("touch.kana.modifier", "゛゜小", ControlKey.KANA_MODIFIER)
                .withColumnSpan(2)),
            padCell(overlay, 10, kanaKey(KanaFlick.Key.WA, PadHolds.digit(10))),
            padCell(overlay, 11, phoneCycleKey("kana-punct", "、。？！")));
        bottom.addAll(phoneBottomPageKeys());
        rows.add(bottom);
        return KeyboardLayout.of(KeyboardLayoutId.JA_FLICK, false, COLUMNS, rows);
    }

    /** One kana key: its tap character as label and text, the pad digit held under it. */
    private static SoftwareKeySpec kanaKey(KanaFlick.Key key, String digit) {
        return SoftwareKeySpec
            .enabled("touch.kana." + key.name().toLowerCase(java.util.Locale.ROOT),
                KanaFlick.tap(key), SemanticInput.text(KanaFlick.tap(key)))
            .withColumnSpan(2)
            .withLongPress(digit);
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

    /**
     * Colemak, on QWERTY's grid with every letter in Colemak's own place: nine across the top, ten
     * on the home row (the {@code o} takes the cell QWERTY gives to the semicolon), seven below.
     * That leaves one cell at the top right — where Colemak's semicolon sits — and backspace goes
     * there, since the home row is full. The hold groups follow the rows: symbols up top, digits
     * on the home row, marks below, none of them split or padded.
     */
    private static KeyboardLayout colemak(boolean shifted) {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(3);
        rows.add(KeyboardLayout.row(
            letter("q", shifted), letter("w", shifted), letter("f", shifted),
            letter("p", shifted), letter("g", shifted), letter("j", shifted),
            letter("l", shifted), letter("u", shifted), letter("y", shifted),
            backspaceKey()
        ));
        rows.add(KeyboardLayout.row(
            letter("a", shifted), letter("r", shifted), letter("s", shifted),
            letter("t", shifted), letter("d", shifted), letter("h", shifted),
            letter("n", shifted), letter("e", shifted), letter("i", shifted),
            letter("o", shifted)
        ));
        rows.add(KeyboardLayout.row(
            shiftKey(shifted),
            letter("z", shifted), letter("x", shifted), letter("c", shifted),
            letter("v", shifted), letter("b", shifted), letter("k", shifted),
            letter("m", shifted),
            letterPeriodKey(), enterKey()
        ));
        return letterPage(KeyboardLayoutId.EN_COLEMAK, shifted, rows, HOLDS_9_10_7);
    }

    // ---- Latin pages beyond English (RFC-0011 §2.14) ----

    /**
     * QWERTY's own shape with a language's accented letters held under their base letters. The
     * group hold — the digit, symbol or mark the key has always carried — stays the first candidate,
     * so holding without moving types what it always did; the accents follow, reached by sliding
     * along the strip the hold raises.
     */
    private static KeyboardLayout qwertyWithAccents(
            KeyboardLayoutId id, boolean shifted, java.util.Map<String, String[]> accents) {
        return latinQwertyShape(id, shifted, "qwertyuiop", "asdfghjkl", "zxcvbnm", accents,
            java.util.Locale.ROOT);
    }

    /**
     * QWERTY's 10/9/7 shape with the letters given row by row — QWERTZ hands in its own top and
     * bottom rows — the accents held, and capitals made in {@code locale} (Turkish: i → İ, ı → I).
     */
    private static KeyboardLayout latinQwertyShape(
            KeyboardLayoutId id, boolean shifted, String top, String home, String bottom,
            java.util.Map<String, String[]> accents, java.util.Locale locale) {
        if (top.length() != 10 || home.length() != 9 || bottom.length() != 7) {
            throw new IllegalArgumentException("QWERTY shape is 10/9/7 letters");
        }
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(3);
        List<SoftwareKeySpec> first = new ArrayList<>(10);
        for (char c : top.toCharArray()) {
            first.add(letter(String.valueOf(c), shifted, locale));
        }
        rows.add(KeyboardLayout.row(first.toArray(new SoftwareKeySpec[0])));
        List<SoftwareKeySpec> second = new ArrayList<>(10);
        for (char c : home.toCharArray()) {
            second.add(letter(String.valueOf(c), shifted, locale));
        }
        second.add(backspaceKey());
        rows.add(KeyboardLayout.row(second.toArray(new SoftwareKeySpec[0])));
        List<SoftwareKeySpec> third = new ArrayList<>(10);
        third.add(shiftKey(shifted));
        for (char c : bottom.toCharArray()) {
            third.add(letter(String.valueOf(c), shifted, locale));
        }
        third.add(letterPeriodKey());
        third.add(enterKey());
        rows.add(KeyboardLayout.row(third.toArray(new SoftwareKeySpec[0])));
        return letterPage(id, shifted,
            withAccents(withHolds(rows, HOLDS_10_9_7), accents, shifted, locale), null);
    }

    /**
     * Spanish, for Spain and Latin America alike: QWERTY with ñ as the tenth key of the home row —
     * the cell QWERTY gives to backspace — so backspace drops to the bottom letter row and the
     * period, which that row can no longer hold, goes to the language cell beside space with
     * {@code , ¿ ¡} under it. The owner's decision (RFC-0011 §7): Gboard's arrangement.
     */
    private static KeyboardLayout spanish(boolean shifted) {
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
            letter("ñ", shifted)
        ));
        rows.add(KeyboardLayout.row(
            shiftKey(shifted),
            letter("z", shifted), letter("x", shifted), letter("c", shifted),
            letter("v", shifted), letter("b", shifted), letter("n", shifted),
            letter("m", shifted),
            backspaceKey(), enterKey()
        ));
        return letterPage(KeyboardLayoutId.ES_QWERTY, shifted,
            withAccents(withHolds(rows, HOLDS_10_9_7), LatinAccents.SPANISH, shifted,
                java.util.Locale.ROOT), null);
    }

    /** German QWERTZ in ten columns: y and z swapped, ü ö ä under u o a, ß under s. */
    private static KeyboardLayout german(boolean shifted) {
        return latinQwertyShape(KeyboardLayoutId.DE_QWERTZ, shifted,
            "qwertzuiop", "asdfghjkl", "yxcvbnm", LatinAccents.GERMAN, java.util.Locale.GERMAN);
    }

    /**
     * Turkish Q in ten columns. Turkish has both i and ı; the key is i (the commoner, and the one
     * every other layout has there) and ı is held under it — the owner's decision (RFC-0011 §7).
     * Capitals follow Turkish: i → İ and ı → I.
     */
    private static KeyboardLayout turkish(boolean shifted) {
        return latinQwertyShape(KeyboardLayoutId.TR_QWERTY, shifted,
            "qwertyuiop", "asdfghjkl", "zxcvbnm", LatinAccents.TURKISH, new java.util.Locale("tr"));
    }

    /**
     * French AZERTY as phones draw it: 10/10/6. The home row is ten letters (m where QWERTY has the
     * semicolon), so backspace drops to the bottom letter row, which has only six letters and so
     * holds {@code ⇧ w x c v b n ⌫ . ⏎} exactly (RFC-0011 §2.12). The fifteen accented letters are
     * held under their vowels and c — the hold strip is what makes that workable.
     */
    private static KeyboardLayout french(boolean shifted) {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(3);
        rows.add(KeyboardLayout.row(
            letter("a", shifted), letter("z", shifted), letter("e", shifted),
            letter("r", shifted), letter("t", shifted), letter("y", shifted),
            letter("u", shifted), letter("i", shifted), letter("o", shifted),
            letter("p", shifted)
        ));
        rows.add(KeyboardLayout.row(
            letter("q", shifted), letter("s", shifted), letter("d", shifted),
            letter("f", shifted), letter("g", shifted), letter("h", shifted),
            letter("j", shifted), letter("k", shifted), letter("l", shifted),
            letter("m", shifted)
        ));
        rows.add(KeyboardLayout.row(
            shiftKey(shifted),
            letter("w", shifted), letter("x", shifted), letter("c", shifted),
            letter("v", shifted), letter("b", shifted), letter("n", shifted),
            backspaceKey(), frenchPeriodKey(), enterKey()
        ));
        return letterPage(KeyboardLayoutId.FR_AZERTY, shifted,
            withAccents(withHolds(rows, HOLDS_10_10_6), LatinAccents.FRENCH, shifted,
                java.util.Locale.FRENCH), null);
    }

    /**
     * Greek, each letter on the key its PC layout uses: the top row is {@code ; ς ε ρ τ υ θ ι ο π}
     * — the {@code ;} is the Greek question mark, on the very key that carries the tonos dead key
     * on a PC, and here it holds the ano teleia {@code ·}, Greek's semicolon. The tone vowels are
     * held under their plain vowels, the way the Latin pages hold their accents; capitals keep the
     * tonos (Ά), which is how dictionaries write them.
     */
    private static KeyboardLayout greek(boolean shifted) {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(3);
        List<SoftwareKeySpec> first = new ArrayList<>(10);
        // The question-mark key keeps its column's digit as its first hold, like every other key
        // in the row, with the ano teleia after it; the letters then take 2 through 0.
        first.add(SoftwareKeySpec
            .enabled("touch.text.erotimatiko", ";", SemanticInput.text(";"))
            .withLongPress("1")
            .withFlicks(null, "·", null, null));
        for (char c : "ςερτυθιοπ".toCharArray()) {
            first.add(letter(String.valueOf(c), shifted));
        }
        rows.add(KeyboardLayout.row(first.toArray(new SoftwareKeySpec[0])));
        List<SoftwareKeySpec> second = new ArrayList<>(10);
        for (char c : "ασδφγηξκλ".toCharArray()) {
            second.add(letter(String.valueOf(c), shifted));
        }
        second.add(backspaceKey());
        rows.add(KeyboardLayout.row(second.toArray(new SoftwareKeySpec[0])));
        List<SoftwareKeySpec> third = new ArrayList<>(10);
        third.add(shiftKey(shifted));
        for (char c : "ζχψωβνμ".toCharArray()) {
            third.add(letter(String.valueOf(c), shifted));
        }
        third.add(letterPeriodKey());
        third.add(enterKey());
        rows.add(KeyboardLayout.row(third.toArray(new SoftwareKeySpec[0])));
        return letterPage(KeyboardLayoutId.EL_QWERTY, shifted,
            withAccents(withHolds(rows, new String[] {"234567890", HOLD_SYMBOLS, HOLD_MARKS}),
                LatinAccents.GREEK, shifted, java.util.Locale.ROOT), null);
    }

    /**
     * Hebrew, each letter where the standard layout puts it, written here in visual left-to-right
     * order (the top row reads ק ר א ט ו ן ם פ from the right, which is how Hebrew reads it).
     * Hebrew has no capitals, so there is no Shift and no shifted page; the cell Shift would take
     * goes back to the letters, and the freed top-row corner widens backspace to two columns. The
     * period sits in the language cell with the comma, geresh, gershayim and maqaf under it.
     */
    private static KeyboardLayout hebrew() {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(3);
        List<SoftwareKeySpec> first = new ArrayList<>(9);
        for (char c : "פםןוטארק".toCharArray()) {
            first.add(letter(String.valueOf(c), false));
        }
        first.add(backspaceKey().withColumnSpan(2));
        rows.add(KeyboardLayout.row(first.toArray(new SoftwareKeySpec[0])));
        List<SoftwareKeySpec> second = new ArrayList<>(10);
        for (char c : "ףךלחיעכגדש".toCharArray()) {
            second.add(letter(String.valueOf(c), false));
        }
        rows.add(KeyboardLayout.row(second.toArray(new SoftwareKeySpec[0])));
        List<SoftwareKeySpec> third = new ArrayList<>(10);
        for (char c : "ץתצמנהבסז".toCharArray()) {
            third.add(letter(String.valueOf(c), false));
        }
        third.add(enterKey());
        rows.add(KeyboardLayout.row(third.toArray(new SoftwareKeySpec[0])));
        return letterPage(KeyboardLayoutId.HE_STANDARD, false,
            withHolds(rows, HOLDS_9_10_7), null);
    }

    /**
     * Persian, on ISIRI 9147's positions squeezed into ten columns (RFC-0011 §2.6.1, the owner's
     * 10-column decision): the four dotted twins ث ذ ظ ژ ride upward flicks on ت د ط ز — the dots
     * sit on top, so the mark's-own-way rule already says up — which leaves 28 base letters. The
     * rows keep ISIRI's order, written here in visual left-to-right; چ and گ, the last letters of
     * ISIRI's crowded first and second rows, close the third. No capitals, so no Shift and one
     * page, like Hebrew. The hamza family rides flicks on ا و ی ه; the digits held under the keys
     * are Persian's own ۰–۹, which is what ISIRI puts on its primary layer; and the space bar
     * flicks up to the ZWNJ — Persian's half-space, ISIRI's Shift+Space — which everyday spelling
     * cannot do without.
     */
    private static KeyboardLayout persian() {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(3);
        List<SoftwareKeySpec> first = new ArrayList<>(10);
        for (char c : "جحخهعغفقصض".toCharArray()) {
            first.add(letter(String.valueOf(c), false));
        }
        rows.add(KeyboardLayout.row(first.toArray(new SoftwareKeySpec[0])));
        List<SoftwareKeySpec> second = new ArrayList<>(10);
        for (char c : "کمنتالبیسش".toCharArray()) {
            second.add(letter(String.valueOf(c), false));
        }
        rows.add(KeyboardLayout.row(second.toArray(new SoftwareKeySpec[0])));
        List<SoftwareKeySpec> third = new ArrayList<>(10);
        for (char c : "گچوپدرزط".toCharArray()) {
            third.add(letter(String.valueOf(c), false));
        }
        third.add(backspaceKey());
        third.add(enterKey());
        rows.add(KeyboardLayout.row(third.toArray(new SoftwareKeySpec[0])));
        List<List<SoftwareKeySpec>> held = withAccents(
            withHolds(rows, new String[] {"۱۲۳۴۵۶۷۸۹۰", "۱۲۳۴۵۶۷۸۹۰", "۱۲۳۴۵۶۷۸"}),
            LatinAccents.PERSIAN, false, java.util.Locale.ROOT);
        List<SoftwareKeySpec> bottom =
            new ArrayList<>(bottomRow(bottomRowCellFor(KeyboardLayoutId.FA_ISIRI)));
        for (int i = 0; i < bottom.size(); i++) {
            if ("touch.text.space".equals(bottom.get(i).stableKeyId())) {
                bottom.set(i, bottom.get(i).withFlicks(null, "\u200c", null, null));
            }
        }
        List<List<SoftwareKeySpec>> all = new ArrayList<>(held);
        all.add(bottom);
        return KeyboardLayout.of(KeyboardLayoutId.FA_ISIRI, false, COLUMNS, all);
    }

    /** The Persian period: the Persian comma held, «» ؟ ؛ on the flicks. */
    private static SoftwareKeySpec persianPeriodKey() {
        return SoftwareKeySpec
            .enabled("touch.text.period.letters", ".", SemanticInput.text("."))
            .withLongPress("،")
            .withFlicks("«", "؟", "»", "؛");
    }

    /** The Hebrew period: the comma held, and geresh, gershayim and maqaf on the flicks. */
    private static SoftwareKeySpec hebrewPeriodKey() {
        return SoftwareKeySpec
            .enabled("touch.text.period.letters", ".", SemanticInput.text("."))
            .withLongPress(",")
            .withFlicks("׳", "״", "־", "₪");
    }

    /** The French period: the comma below the finger's ways, the guillemets left and right. */
    private static SoftwareKeySpec frenchPeriodKey() {
        return SoftwareKeySpec
            .enabled("touch.text.period", ".", SemanticInput.text("."))
            .withLongPress(",")
            .withFlicks("«", ",", "»", "€");
    }

    /** The period the Spanish page keeps beside space, with the comma and the inverted marks under it. */
    private static SoftwareKeySpec spanishPeriodKey() {
        return SoftwareKeySpec
            .enabled("touch.text.period.letters", ".", SemanticInput.text("."))
            .withLongPress(",")
            .withFlicks(",", "¿", "¡", "€");
    }

    /**
     * Puts a language's accented letters on their base keys as flicks — one direction each, the
     * mark's own direction — so the digit and symbol holds never crowd the letters the language
     * cannot do without (the owner's rule, 2026-08-24). The shifted page flicks their capitals.
     */
    private static List<List<SoftwareKeySpec>> withAccents(
            List<List<SoftwareKeySpec>> rows, java.util.Map<String, String[]> accents, boolean shifted,
            java.util.Locale locale) {
        List<List<SoftwareKeySpec>> out = new ArrayList<>(rows.size());
        for (List<SoftwareKeySpec> row : rows) {
            List<SoftwareKeySpec> updated = new ArrayList<>(row);
            for (int i = 0; i < updated.size(); i++) {
                SoftwareKeySpec key = updated.get(i);
                if (key.isControl() || !key.enabled() || key.semanticInput() == null
                        || key.semanticInput().kind() != SemanticInput.Kind.TEXT) {
                    continue;
                }
                // The base letter comes from the key's id, not its label: the shifted label is a
                // capital, and Turkish's İ does not lower-case back to i in the root locale. The
                // period key answers to "." for a layout's own punctuation and currency.
                String base;
                if (key.stableKeyId().startsWith("touch.en.")) {
                    base = key.stableKeyId().substring("touch.en.".length());
                } else if ("touch.text.period".equals(key.stableKeyId())) {
                    base = ".";
                } else {
                    continue;
                }
                String[] ways = accents.get(base);
                if (ways == null) {
                    continue;
                }
                if (ways.length > 4 && !ways[4].isEmpty()) {
                    // A fifth character replaces the group hold: the digit is the expendable one,
                    // never what the language cannot do without (Portuguese ª º).
                    key = key.withLongPress(flickWay(ways[4], shifted, locale));
                }
                updated.set(i, key.withFlicks(
                    flickWay(ways[0], shifted, locale),
                    flickWay(ways[1], shifted, locale),
                    flickWay(ways[2], shifted, locale),
                    flickWay(ways[3], shifted, locale)));
            }
            out.add(updated);
        }
        return out;
    }

    private static String flickWay(String accent, boolean shifted, java.util.Locale locale) {
        if (accent == null || accent.isEmpty()) {
            return null;
        }
        return shifted ? capital(accent, locale) : accent;
    }

    /** Adds the hold groups and the fixed bottom row to a page's three letter rows. */
    private static KeyboardLayout letterPage(
        KeyboardLayoutId id,
        boolean shifted,
        List<List<SoftwareKeySpec>> letterRows,
        String[] groups
    ) {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>(
            groups == null ? letterRows : withHolds(letterRows, groups));
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
            case EN_COLEMAK:
            case PT_QWERTY:
            case IT_QWERTY:
            case PL_QWERTY:
            case VI_TELEX:
            case DE_QWERTZ:
            case TR_QWERTY:
            case FR_AZERTY:
            case EL_QWERTY:
            case JA_ROMAJI:
                return rawKey("escape.letters", "Esc", RawKey.ESCAPE);
            case ES_QWERTY:
                // ñ took the home row's last cell and backspace took the period's: the period
                // lives here, with what Spanish needs under it. Esc is on the special-keys page.
                return spanishPeriodKey();
            case HE_STANDARD:
                // The letter rows are all letters: the period lives beside space.
                return hebrewPeriodKey();
            case FA_ISIRI:
                return persianPeriodKey();
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
            // Caps Lock where the dead Lang cell was, with Menu moved one to the right; the face
            // inverts while the lock is on, so the page says what state the editor is in.
            SoftwareKeySpec.control("touch.key.capslock", "Caps", ControlKey.CAPS_LOCK),
            rawKey("menu", "Menu", RawKey.MENU),
            fnKey(), padCell(mode, 3), padCell(mode, 4), padCell(mode, 5), backspaceKey()
        ));
        rows.add(KeyboardLayout.row(
            shiftKey(false), text("e", "e").withLongPress("_"),
            // The other half of each pair, where a keypad would have had room for both.
            text("plus", "+").withLongPress("*"), text("minus", "-").withLongPress("/"),
            // "=" was the only key in this row holding nothing; a colon is what shares a key with
            // it on a full keyboard, and it is what a time or a ratio needs.
            text("equals", "=").withLongPress(":"), text("period", ".").withLongPress(","),
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
            menuControl("theme", "Theme", ControlKey.THEME_CYCLE),
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
        return letter(lowercase, shifted, java.util.Locale.ROOT);
    }

    /** A letter key whose capital is made in {@code locale} — Turkish turns i into İ. */
    private static SoftwareKeySpec letter(String lowercase, boolean shifted, java.util.Locale locale) {
        String label = shifted ? capital(lowercase, locale) : lowercase;
        return SoftwareKeySpec.enabled("touch.en." + lowercase, label, SemanticInput.text(label));
    }

    /**
     * The one-character capital of a letter. Java's upper-casing of ß is "SS"; the keyboard wants
     * the capital letter ẞ, which German orthography has had since 2017.
     */
    private static String capital(String lowercase, java.util.Locale locale) {
        if ("ß".equals(lowercase)) {
            return "ẞ";
        }
        return lowercase.toUpperCase(locale);
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
