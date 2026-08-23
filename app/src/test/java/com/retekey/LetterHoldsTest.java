package com.retekey;

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
        assertEquals(Arrays.asList(",", "¿", "¡"), period.longPressTexts());
        assertEquals("no Esc beside space on Spanish", null, es.findById("touch.key.escape.letters"));
        assertEquals("Ñ", labels(KeyboardLayouts.of(KeyboardLayoutId.ES_QWERTY, true), 1).get(9));
    }

    @Test
    public void accentsFollowTheGroupHoldSoHoldingStillTypesTheDigit() {
        KeyboardLayout es = KeyboardLayouts.of(KeyboardLayoutId.ES_QWERTY, false);
        assertEquals(Arrays.asList("3", "é"), es.rows().get(0).get(2).longPressTexts());
        assertEquals(Arrays.asList("7", "ú", "ü"), es.rows().get(0).get(6).longPressTexts());
        assertEquals(Arrays.asList("!", "á"), es.rows().get(1).get(0).longPressTexts());
        // ñ is the tenth home-row key and the symbol group has nine: it holds nothing.
        assertEquals(false, es.rows().get(1).get(9).hasLongPress());
        // Shifted, the accents are capitals.
        KeyboardLayout shifted = KeyboardLayouts.of(KeyboardLayoutId.ES_QWERTY, true);
        assertEquals(Arrays.asList("3", "É"), shifted.rows().get(0).get(2).longPressTexts());
    }

    @Test
    public void portugueseItalianAndPolishKeepQwertysShapeAndHoldTheirLetters() {
        KeyboardLayout pt = KeyboardLayouts.of(KeyboardLayoutId.PT_QWERTY, false);
        assertEquals(labels(EN, 2), labels(pt, 2));
        assertEquals(Arrays.asList(":", "ç"), pt.rows().get(2).get(3).longPressTexts());
        assertEquals(Arrays.asList("!", "á", "â", "ã", "à", "ª"), pt.rows().get(1).get(0).longPressTexts());
        assertEquals(Arrays.asList("9", "ó", "ô", "õ", "º"), pt.rows().get(0).get(8).longPressTexts());

        KeyboardLayout it = KeyboardLayouts.of(KeyboardLayoutId.IT_QWERTY, false);
        assertEquals(Arrays.asList("3", "è", "é"), it.rows().get(0).get(2).longPressTexts());

        KeyboardLayout pl = KeyboardLayouts.of(KeyboardLayoutId.PL_QWERTY, false);
        assertEquals(Arrays.asList("_", "ż", "ź"), pl.rows().get(2).get(1).longPressTexts());
        assertEquals(Arrays.asList(";", "ł"), pl.rows().get(1).get(8).longPressTexts());
        // Letters the table does not name hold only their group character.
        assertEquals(Arrays.asList("1"), pl.rows().get(0).get(0).longPressTexts());
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
