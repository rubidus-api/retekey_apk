package dev.hellgates.retekeyime;

import java.util.ArrayList;
import java.util.List;

/**
 * The base touch layouts.
 *
 * <p>Both layouts share one orthogonal ten-column grid derived from the English QWERTY letter
 * block: row one is the {@code q}-row, row two is the {@code a}-row plus the primary editor
 * action, and row three is Shift plus the {@code z}-row. Every key occupies exactly one column
 * except the space bar, which spans three. Korean 2-beolsik reuses the same cells, so a key keeps
 * its position across layouts and only its label and semantic input change.
 */
public final class KeyboardLayouts {
    public static final int COLUMNS = 10;
    public static final int SPACE_COLUMN_SPAN = 3;

    private static final KeyboardLayout EN_BASE = english(false);
    private static final KeyboardLayout EN_SHIFTED = english(true);
    private static final KeyboardLayout KO_BASE = korean(false);
    private static final KeyboardLayout KO_SHIFTED = korean(true);

    private KeyboardLayouts() {
    }

    public static KeyboardLayout of(KeyboardLayoutId id, boolean shifted) {
        if (id == null) {
            throw new IllegalArgumentException("layout id must not be null");
        }
        if (id == KeyboardLayoutId.EN_QWERTY) {
            return shifted ? EN_SHIFTED : EN_BASE;
        }
        return shifted ? KO_SHIFTED : KO_BASE;
    }

    public static KeyboardLayoutId other(KeyboardLayoutId id) {
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
            periodKey(), backspaceKey()
        ));
        rows.add(bottomRow());
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
            periodKey(), backspaceKey()
        ));
        rows.add(bottomRow());
        return KeyboardLayout.of(KeyboardLayoutId.KO_DUBEOLSIK, shifted, COLUMNS, rows);
    }

    /**
     * The bottom row is identical in every layout, as RFC-0002's wide-profile candidate specified:
     * Ctrl, Meta, Alt, a three-column space bar, the layout toggle, the symbol layer (which also
     * owns navigation and the other special keys), Tab, and the menu key that opens settings and
     * the keyboard's own functions.
     *
     * <p>Only space and the layout toggle act today. The modifiers, the symbol/navigation layer,
     * Tab, and the menu need a raw-key action, a second layer, and a settings surface that do not
     * exist yet, so they are drawn disabled rather than silently doing nothing. `Meta` is never
     * labelled `Win`.
     */
    private static List<SoftwareKeySpec> bottomRow() {
        return KeyboardLayout.row(
            SoftwareKeySpec.disabled("touch.modifier.ctrl", "Ctrl"),
            SoftwareKeySpec.disabled("touch.modifier.meta", "Meta"),
            SoftwareKeySpec.disabled("touch.modifier.alt", "Alt"),
            SoftwareKeySpec
                .enabled("touch.text.space", "space", SemanticInput.text(" "))
                .withColumnSpan(SPACE_COLUMN_SPAN),
            SoftwareKeySpec.control("touch.layout.toggle", "한/영", ControlKey.LAYOUT_TOGGLE),
            SoftwareKeySpec.disabled("touch.layer.symbols", "!#1"),
            SoftwareKeySpec.disabled("touch.edit.tab", "Tab"),
            SoftwareKeySpec.disabled("touch.menu", "☰")
        );
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
     * The one punctuation key. Holding it reveals the rest of the everyday punctuation instead of
     * spending a second cell on a comma.
     */
    private static SoftwareKeySpec periodKey() {
        return SoftwareKeySpec
            .enabled("touch.text.period", ".", SemanticInput.text("."))
            .withLongPress(",", "?", "!", ":", ";", "'", "\"", "-", "_", "#", "*", "&");
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
