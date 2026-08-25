package com.retekey;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * The hold alternates on the letter pages: digits on the top row, the shifted number-row symbols on
 * the middle one, and punctuation on the bottom one. Both languages share them, because a key keeps
 * its position — and so its alternate — across the language switch.
 */
public final class LetterHoldsTest {
    private static final KeyboardLayout EN =
        KeyboardLayouts.of(KeyboardLayoutId.EN_QWERTY, false);
    private static final KeyboardLayout KO =
        KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);

    @Test
    public void theTopRowHoldsTheDigits() {
        List<String> digits = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "0");
        assertEquals(digits, holds(EN, 0, 0, 10));
        assertEquals(digits, holds(KO, 0, 0, 10));
    }

    @Test
    public void theMiddleRowHoldsTheShiftedNumberRow() {
        // Nine letters, nine alternates: the key before backspace carries the semicolon.
        List<String> symbols = Arrays.asList("!", "@", "#", "$", "%", "^", "&", "*", ";");
        assertEquals(symbols, holds(EN, 1, 0, 9));
        assertEquals(symbols, holds(KO, 1, 0, 9));
    }

    @Test
    public void theBottomRowHoldsPunctuation() {
        List<String> marks = Arrays.asList("_", "-", ":", "=", "'", "\"", "?");
        // Column 0 is Shift, so the run starts at column 1.
        assertEquals(marks, holds(EN, 2, 1, 7));
        assertEquals(marks, holds(KO, 2, 1, 7));
    }

    @Test
    public void theKeysWithNoAlternateAreTheOnesWithTheirOwn() {
        // Backspace and Enter keep repeating and acting; the period keeps its comma.
        assertTrue(EN.rows().get(1).get(9).longPressTexts().isEmpty());
        assertTrue(EN.rows().get(2).get(9).longPressTexts().isEmpty());
        assertEquals(Arrays.asList(","), EN.rows().get(2).get(8).longPressTexts());
    }

    @Test
    public void dvorakKeepsItsOwnSevenTenNineShape() {
        KeyboardLayout dvorak = KeyboardLayouts.of(KeyboardLayoutId.EN_DVORAK, false);

        // The three cells the top row does not need for letters carry Enter, backspace and the
        // period, on the left, so Dvorak's own rows survive intact behind them.
        assertEquals(Arrays.asList("⏎", "⌫", ".", "p", "y", "f", "g", "c", "r", "l"),
            labels(dvorak, 0));
        assertEquals(Arrays.asList("a", "o", "e", "u", "i", "d", "h", "t", "n", "s"),
            labels(dvorak, 1));
        assertEquals(Arrays.asList("⇧", "q", "j", "k", "x", "b", "m", "w", "v", "z"),
            labels(dvorak, 2));
    }

    @Test
    public void dvorakCarriesTheSameThreeGroupsRotatedToItsRows() {
        KeyboardLayout dvorak = KeyboardLayouts.of(KeyboardLayoutId.EN_DVORAK, false);

        // QWERTY's groups are 10/9/7 and Dvorak's rows are 7/10/9, so rows 1-2-3 become 3-1-2 and
        // no group is split or padded.
        assertEquals(Arrays.asList("_", "-", ":", "=", "'", "\"", "?"), holds(dvorak, 0, 3, 7));
        assertEquals(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
            holds(dvorak, 1, 0, 10));
        assertEquals(Arrays.asList("!", "@", "#", "$", "%", "^", "&", "*", ";"),
            holds(dvorak, 2, 1, 9));
        // The period keeps its comma rather than being swept into a group.
        assertEquals(Arrays.asList(","), dvorak.rows().get(0).get(2).longPressTexts());
    }

    @Test
    public void colemakKeepsEveryLetterInItsOwnPlaceOnQwertysGrid() {
        KeyboardLayout colemak = KeyboardLayouts.of(KeyboardLayoutId.EN_COLEMAK, false);

        // Nine, ten, seven. The home row is full — o takes the semicolon's cell — so backspace
        // goes to the top right, the one cell Colemak's letters do not need.
        assertEquals(Arrays.asList("q", "w", "f", "p", "g", "j", "l", "u", "y", "⌫"),
            labels(colemak, 0));
        assertEquals(Arrays.asList("a", "r", "s", "t", "d", "h", "n", "e", "i", "o"),
            labels(colemak, 1));
        assertEquals(Arrays.asList("⇧", "z", "x", "c", "v", "b", "k", "m", ".", "⏎"),
            labels(colemak, 2));
    }

    @Test
    public void colemakCarriesTheSameThreeGroupsOnItsNineTenSevenRows() {
        KeyboardLayout colemak = KeyboardLayouts.of(KeyboardLayoutId.EN_COLEMAK, false);

        assertEquals(Arrays.asList("!", "@", "#", "$", "%", "^", "&", "*", ";"),
            holds(colemak, 0, 0, 9));
        assertEquals(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
            holds(colemak, 1, 0, 10));
        assertEquals(Arrays.asList("_", "-", ":", "=", "'", "\"", "?"), holds(colemak, 2, 1, 7));
        assertEquals(Arrays.asList(","), colemak.rows().get(2).get(8).longPressTexts());
    }

    // ---- the Latin pages beyond English (RFC-0011 §2.14) ----

    @Test
    public void spanishPutsEnyeOnTheHomeRowAndThePeriodBesideSpace() {
        KeyboardLayout es = KeyboardLayouts.of(KeyboardLayoutId.ES_QWERTY, false);
        assertEquals(Arrays.asList("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"), labels(es, 0));
        assertEquals(Arrays.asList("a", "s", "d", "f", "g", "h", "j", "k", "l", "ñ"), labels(es, 1));
        assertEquals(Arrays.asList("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫", "⏎"), labels(es, 2));
        // The language cell beside space is the period, with the comma and the inverted marks.
        List<SoftwareKeySpec> bottom = es.rows().get(3);
        SoftwareKeySpec period = es.findById("touch.text.period.letters");
        assertEquals("space", bottom.get(bottom.indexOf(period) - 1).label());
        assertEquals(".", period.label());
        assertEquals(Arrays.asList(","), period.longPressTexts());
        assertEquals("no Esc beside space on Spanish", null, es.findById("touch.key.escape.letters"));
        assertEquals("Ñ", labels(KeyboardLayouts.of(KeyboardLayoutId.ES_QWERTY, true), 1).get(9));
    }

    @Test
    public void accentsRideTheFlicksAndTheDigitKeepsTheStillHold() {
        // The owner's rule: the digit and symbol holds must never crowd a language's own letters,
        // so the accents live on directional flicks and a still hold stays the group character.
        KeyboardLayout es = KeyboardLayouts.of(KeyboardLayoutId.ES_QWERTY, false);
        assertEquals(Arrays.asList("3"), es.rows().get(0).get(2).longPressTexts());
        assertEquals("é", es.rows().get(0).get(2).flickText(CheonjiinInterpreter.Flick.UP));
        assertEquals("ú", es.rows().get(0).get(6).flickText(CheonjiinInterpreter.Flick.UP));
        assertEquals("ü", es.rows().get(0).get(6).flickText(CheonjiinInterpreter.Flick.DOWN));
        assertEquals("á", es.rows().get(1).get(0).flickText(CheonjiinInterpreter.Flick.UP));
        // ñ is the tenth home-row key and the symbol group has nine: it holds nothing.
        assertEquals(false, es.rows().get(1).get(9).hasLongPress());
        // Shifted, the flicks are capitals.
        KeyboardLayout shifted = KeyboardLayouts.of(KeyboardLayoutId.ES_QWERTY, true);
        assertEquals("É", shifted.rows().get(0).get(2).flickText(CheonjiinInterpreter.Flick.UP));
        // And the period beside space carries the inverted marks on its flicks.
        SoftwareKeySpec period = es.findById("touch.text.period.letters");
        assertEquals(Arrays.asList(","), period.longPressTexts());
        assertEquals("¿", period.flickText(CheonjiinInterpreter.Flick.UP));
        assertEquals("¡", period.flickText(CheonjiinInterpreter.Flick.RIGHT));
        assertEquals("€", period.flickText(CheonjiinInterpreter.Flick.DOWN));
    }

    @Test
    public void portugueseItalianAndPolishFlickByTheMarksOwnDirection() {
        KeyboardLayout pt = KeyboardLayouts.of(KeyboardLayoutId.PT_QWERTY, false);
        assertEquals(labels(EN, 2), labels(pt, 2));
        assertEquals("ç", pt.rows().get(2).get(3).flickText(CheonjiinInterpreter.Flick.DOWN));
        assertEquals("à", pt.rows().get(1).get(0).flickText(CheonjiinInterpreter.Flick.LEFT));
        assertEquals("â", pt.rows().get(1).get(0).flickText(CheonjiinInterpreter.Flick.UP));
        assertEquals("á", pt.rows().get(1).get(0).flickText(CheonjiinInterpreter.Flick.RIGHT));
        assertEquals("ã", pt.rows().get(1).get(0).flickText(CheonjiinInterpreter.Flick.DOWN));
        assertEquals("õ", pt.rows().get(0).get(8).flickText(CheonjiinInterpreter.Flick.DOWN));
        // The ordinal signs replace those keys' group holds, and the period carries the
        // guillemets and the euro: the digits and symbols are the expendable ones.
        assertEquals(Arrays.asList("ª"), pt.rows().get(1).get(0).longPressTexts());
        assertEquals(Arrays.asList("º"), pt.rows().get(0).get(8).longPressTexts());
        SoftwareKeySpec ptPeriod = pt.rows().get(2).get(8);
        assertEquals("«", ptPeriod.flickText(CheonjiinInterpreter.Flick.LEFT));
        assertEquals("»", ptPeriod.flickText(CheonjiinInterpreter.Flick.RIGHT));
        assertEquals("€", ptPeriod.flickText(CheonjiinInterpreter.Flick.DOWN));

        KeyboardLayout it = KeyboardLayouts.of(KeyboardLayoutId.IT_QWERTY, false);
        assertEquals("è", it.rows().get(0).get(2).flickText(CheonjiinInterpreter.Flick.LEFT));
        assertEquals("é", it.rows().get(0).get(2).flickText(CheonjiinInterpreter.Flick.RIGHT));

        KeyboardLayout pl = KeyboardLayouts.of(KeyboardLayoutId.PL_QWERTY, false);
        assertEquals("ż", pl.rows().get(2).get(1).flickText(CheonjiinInterpreter.Flick.UP));
        assertEquals("ź", pl.rows().get(2).get(1).flickText(CheonjiinInterpreter.Flick.RIGHT));
        assertEquals("ą", pl.rows().get(1).get(0).flickText(CheonjiinInterpreter.Flick.DOWN));
        // The still hold stays the group character, and unnamed letters have no flicks.
        assertEquals(Arrays.asList("_"), pl.rows().get(2).get(1).longPressTexts());
        assertEquals(false, pl.rows().get(0).get(0).hasFlicks());
        assertEquals(Arrays.asList("1"), pl.rows().get(0).get(0).longPressTexts());
    }

    @Test
    public void vietnameseIsQwertyWithTheMarkedLettersAsFallbackFlicks() {
        KeyboardLayout vi = KeyboardLayouts.of(KeyboardLayoutId.VI_TELEX, false);
        assertEquals(labels(EN, 0), labels(vi, 0));
        assertEquals(labels(EN, 2), labels(vi, 2));
        assertEquals("â", vi.rows().get(1).get(0).flickText(CheonjiinInterpreter.Flick.UP));
        assertEquals("ă", vi.rows().get(1).get(0).flickText(CheonjiinInterpreter.Flick.DOWN));
        assertEquals("đ", vi.rows().get(1).get(2).flickText(CheonjiinInterpreter.Flick.DOWN));
        assertEquals("ư", vi.rows().get(0).get(6).flickText(CheonjiinInterpreter.Flick.RIGHT));
        assertEquals(Arrays.asList("!"), vi.rows().get(1).get(0).longPressTexts());
    }

    @Test
    public void germanSwapsYAndZAndHoldsTheUmlautsAndSharpS() {
        KeyboardLayout de = KeyboardLayouts.of(KeyboardLayoutId.DE_QWERTZ, false);
        assertEquals(Arrays.asList("q", "w", "e", "r", "t", "z", "u", "i", "o", "p"), labels(de, 0));
        assertEquals(Arrays.asList("⇧", "y", "x", "c", "v", "b", "n", "m", ".", "⏎"), labels(de, 2));
        assertEquals("ü", de.rows().get(0).get(6).flickText(CheonjiinInterpreter.Flick.UP));
        assertEquals("ß", de.rows().get(1).get(1).flickText(CheonjiinInterpreter.Flick.DOWN));
        assertEquals(Arrays.asList("7"), de.rows().get(0).get(6).longPressTexts());
        KeyboardLayout shifted = KeyboardLayouts.of(KeyboardLayoutId.DE_QWERTZ, true);
        assertEquals("capital sharp s, not SS", "ẞ",
            shifted.rows().get(1).get(1).flickText(CheonjiinInterpreter.Flick.DOWN));
        assertEquals("Z", labels(shifted, 0).get(5));
    }

    @Test
    public void turkishKeepsTheDottedIOnTheKeyAndCasesTheTurkishWay() {
        KeyboardLayout tr = KeyboardLayouts.of(KeyboardLayoutId.TR_QWERTY, false);
        assertEquals("i", labels(tr, 0).get(7));
        assertEquals("ı", tr.rows().get(0).get(7).flickText(CheonjiinInterpreter.Flick.DOWN));
        assertEquals("₺", tr.rows().get(2).get(8).flickText(CheonjiinInterpreter.Flick.DOWN));
        assertEquals("ğ", tr.rows().get(1).get(4).flickText(CheonjiinInterpreter.Flick.UP));
        assertEquals(Arrays.asList("8"), tr.rows().get(0).get(7).longPressTexts());
        KeyboardLayout shifted = KeyboardLayouts.of(KeyboardLayoutId.TR_QWERTY, true);
        assertEquals("İ", labels(shifted, 0).get(7));
        assertEquals("I", shifted.rows().get(0).get(7).flickText(CheonjiinInterpreter.Flick.DOWN));
        assertEquals("Ş", shifted.rows().get(1).get(1).flickText(CheonjiinInterpreter.Flick.DOWN));
    }

    @Test
    public void frenchIsAzertyTenTenSixWithTheAccentsHeld() {
        KeyboardLayout fr = KeyboardLayouts.of(KeyboardLayoutId.FR_AZERTY, false);
        assertEquals(Arrays.asList("a", "z", "e", "r", "t", "y", "u", "i", "o", "p"), labels(fr, 0));
        assertEquals(Arrays.asList("q", "s", "d", "f", "g", "h", "j", "k", "l", "m"), labels(fr, 1));
        assertEquals(Arrays.asList("⇧", "w", "x", "c", "v", "b", "n", "⌫", ".", "⏎"), labels(fr, 2));
        // Holds stay the group characters alone; the fifteen accents ride the flicks, the mark's
        // own way round: grave left, circumflex up, acute right, the diaeresis and cedilla down.
        assertEquals(Arrays.asList("3"), fr.rows().get(0).get(2).longPressTexts());
        SoftwareKeySpec e = fr.rows().get(0).get(2);
        assertEquals("è", e.flickText(CheonjiinInterpreter.Flick.LEFT));
        assertEquals("ê", e.flickText(CheonjiinInterpreter.Flick.UP));
        assertEquals("é", e.flickText(CheonjiinInterpreter.Flick.RIGHT));
        assertEquals("ë", e.flickText(CheonjiinInterpreter.Flick.DOWN));
        assertEquals("æ", fr.rows().get(0).get(0).flickText(CheonjiinInterpreter.Flick.RIGHT));
        assertEquals("ç", fr.rows().get(2).get(3).flickText(CheonjiinInterpreter.Flick.DOWN));
        assertEquals(Arrays.asList("'"), fr.rows().get(1).get(9).longPressTexts());
        SoftwareKeySpec period = fr.rows().get(2).get(8);
        assertEquals(Arrays.asList(","), period.longPressTexts());
        assertEquals("«", period.flickText(CheonjiinInterpreter.Flick.LEFT));
        assertEquals("»", period.flickText(CheonjiinInterpreter.Flick.RIGHT));
        assertEquals("€", period.flickText(CheonjiinInterpreter.Flick.DOWN));
        KeyboardLayout shifted = KeyboardLayouts.of(KeyboardLayoutId.FR_AZERTY, true);
        assertEquals("É", shifted.rows().get(0).get(2).flickText(CheonjiinInterpreter.Flick.RIGHT));
        assertEquals("Œ", shifted.rows().get(0).get(8).flickText(CheonjiinInterpreter.Flick.RIGHT));
    }

    @Test
    public void greekSitsOnThePcPositionsWithTheToneVowelsHeld() {
        KeyboardLayout el = KeyboardLayouts.of(KeyboardLayoutId.EL_QWERTY, false);
        assertEquals(Arrays.asList(";", "ς", "ε", "ρ", "τ", "υ", "θ", "ι", "ο", "π"), labels(el, 0));
        assertEquals(Arrays.asList("α", "σ", "δ", "φ", "γ", "η", "ξ", "κ", "λ", "⌫"), labels(el, 1));
        assertEquals(Arrays.asList("⇧", "ζ", "χ", "ψ", "ω", "β", "ν", "μ", ".", "⏎"), labels(el, 2));
        // The Greek question mark flicks up to the ano teleia; the tonos leans right, the
        // diaeresis goes down, and both together go up. Holds stay the digits alone.
        assertEquals(Arrays.asList("1"), el.rows().get(0).get(0).longPressTexts());
        assertEquals("·", el.rows().get(0).get(0).flickText(CheonjiinInterpreter.Flick.UP));
        assertEquals("έ", el.rows().get(0).get(2).flickText(CheonjiinInterpreter.Flick.RIGHT));
        assertEquals("ί", el.rows().get(0).get(7).flickText(CheonjiinInterpreter.Flick.RIGHT));
        assertEquals("ϊ", el.rows().get(0).get(7).flickText(CheonjiinInterpreter.Flick.DOWN));
        assertEquals("ΐ", el.rows().get(0).get(7).flickText(CheonjiinInterpreter.Flick.UP));
        assertEquals("ά", el.rows().get(1).get(0).flickText(CheonjiinInterpreter.Flick.RIGHT));
        KeyboardLayout shifted = KeyboardLayouts.of(KeyboardLayoutId.EL_QWERTY, true);
        assertEquals("Σ", labels(shifted, 0).get(1));
        assertEquals("Έ", shifted.rows().get(0).get(2).flickText(CheonjiinInterpreter.Flick.RIGHT));
    }

    @Test
    public void hebrewHasNoShiftAndOnePageAndThePeriodBesideSpace() {
        KeyboardLayout he = KeyboardLayouts.of(KeyboardLayoutId.HE_STANDARD, false);
        assertEquals(Arrays.asList("פ", "ם", "ן", "ו", "ט", "א", "ר", "ק", "⌫"), labels(he, 0));
        assertEquals(2, he.rows().get(0).get(8).columnSpan());
        assertEquals(Arrays.asList("ף", "ך", "ל", "ח", "י", "ע", "כ", "ג", "ד", "ש"), labels(he, 1));
        assertEquals(Arrays.asList("ץ", "ת", "צ", "מ", "נ", "ה", "ב", "ס", "ז", "⏎"), labels(he, 2));
        // One page whatever Shift says, and no Shift key anywhere on it.
        assertEquals(he, KeyboardLayouts.of(KeyboardLayoutId.HE_STANDARD, true));
        assertNull(he.findById("touch.modifier.shift"));
        // Digits ride the full home row; the period beside space carries Hebrew's own marks.
        assertEquals(Arrays.asList("1"), he.rows().get(1).get(0).longPressTexts());
        assertEquals(Arrays.asList("0"), he.rows().get(1).get(9).longPressTexts());
        SoftwareKeySpec period = he.findById("touch.text.period.letters");
        assertEquals(Arrays.asList(","), period.longPressTexts());
        assertEquals("׳", period.flickText(CheonjiinInterpreter.Flick.LEFT));
        assertEquals("״", period.flickText(CheonjiinInterpreter.Flick.UP));
        assertEquals("־", period.flickText(CheonjiinInterpreter.Flick.RIGHT));
        assertNull(he.findById("touch.key.escape.letters"));
    }

    private static List<String> labels(KeyboardLayout layout, int row) {
        List<String> found = new ArrayList<>();
        for (SoftwareKeySpec key : layout.rows().get(row)) {
            found.add(key.label());
        }
        return found;
    }

    @Test
    public void shiftedLayoutsCarryTheSameAlternates() {
        KeyboardLayout shiftedEn = KeyboardLayouts.of(KeyboardLayoutId.EN_QWERTY, true);
        KeyboardLayout shiftedKo = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, true);

        assertEquals(holds(EN, 0, 0, 10), holds(shiftedEn, 0, 0, 10));
        assertEquals(holds(KO, 0, 0, 10), holds(shiftedKo, 0, 0, 10));
    }

    @Test
    public void everyHoldOffersExactlyOneAlternate() {
        // Holding types the alternate at once, so a second entry would be unreachable.
        for (KeyboardLayout layout : Arrays.asList(EN, KO)) {
            for (List<SoftwareKeySpec> row : layout.rows()) {
                for (SoftwareKeySpec key : row) {
                    if (key.hasLongPress()) {
                        assertEquals(
                            key.label() + " offers one alternate",
                            1,
                            key.longPressTexts().size());
                    }
                }
            }
        }
    }

    private static List<String> holds(KeyboardLayout layout, int row, int from, int count) {
        List<String> found = new ArrayList<>();
        for (int i = from; i < from + count; i++) {
            List<String> texts = layout.rows().get(row).get(i).longPressTexts();
            found.add(texts.isEmpty() ? null : texts.get(0));
        }
        return found;
    }
}
