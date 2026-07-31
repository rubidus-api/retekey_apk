package dev.hellgates.retekeyime;

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
    public void theHoldRunFlowsAcrossWhateverShapeTheRowsTake() {
        KeyboardLayout dvorak = KeyboardLayouts.of(KeyboardLayoutId.EN_DVORAK, false);

        // Seven letters on the top row take 1-7, so the home row picks up at 8.
        assertEquals(Arrays.asList("1", "2", "3", "4", "5", "6", "7"), holds(dvorak, 0, 3, 7));
        assertEquals(Arrays.asList("8", "9", "0", "!", "@", "#", "$", "%", "^", "&"),
            holds(dvorak, 1, 0, 10));
        assertEquals(Arrays.asList("*", ";", "_", "-", ":", "=", "'", "\"", "?"),
            holds(dvorak, 2, 1, 9));
        // The period keeps its comma rather than being swept into the run.
        assertEquals(Arrays.asList(","), dvorak.rows().get(0).get(2).longPressTexts());
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
