package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * Arrows and Keypad: the cursor cluster and the digits as layouts of their own, on 나랏글's frame.
 * They must agree with the overlays that already draw the same cells, or the same key would move
 * depending on how it was reached.
 */
public final class PadLayoutsTest {
    private static final KeyboardLayout ARROWS =
        KeyboardLayouts.of(KeyboardLayoutId.PAD_ARROWS, false);
    private static final KeyboardLayout KEYPAD =
        KeyboardLayouts.of(KeyboardLayoutId.PAD_KEYPAD, true);

    @Test
    public void bothWearTheNaratgeulFrame() {
        KeyboardLayout naratgeul = KeyboardLayouts.of(KeyboardLayoutId.KO_NARATGEUL, false);
        for (KeyboardLayout layout : Arrays.asList(ARROWS, KEYPAD)) {
            assertEquals(naratgeul.rows().size(), layout.rows().size());
            assertEquals(Arrays.asList("Ctrl", "Meta", "Alt", "Tab"), column(layout));
            assertEquals("⌫", last(layout.rows().get(0)).label());
            assertEquals("space", last(layout.rows().get(1)).label());
            assertEquals("⏎", last(layout.rows().get(2)).label());
        }
    }

    @Test
    public void theArrowsAreTheOnesTheOverlayDraws() {
        KeyboardLayout overlaid =
            KeyboardLayouts.phone(KeyboardLayoutId.KO_NARATGEUL, PhoneOverlay.NAV);
        for (int row = 0; row < 4; row++) {
            assertEquals("row " + row, padLabels(overlaid, row), padLabels(ARROWS, row));
        }
    }

    @Test
    public void theKeypadTypesDigitsAndHoldsTheCalculatorSet() {
        assertEquals(Arrays.asList("1", "2", "3"), padLabels(KEYPAD, 0));
        assertEquals(Arrays.asList("4", "5", "6"), padLabels(KEYPAD, 1));
        assertEquals(Arrays.asList("7", "8", "9"), padLabels(KEYPAD, 2));
        assertEquals(Arrays.asList("*", "0", "#"), padLabels(KEYPAD, 3));

        SoftwareKeySpec one = KEYPAD.findById("touch.sym.pad.num.0");
        assertEquals("1", one.semanticInput().text());
        assertEquals(Arrays.asList("+"), one.longPressTexts());
        assertEquals(Arrays.asList("e"), KEYPAD.findById("touch.sym.pad.num.7").longPressTexts());
        assertEquals(Arrays.asList("@"), KEYPAD.findById("touch.sym.pad.num.11").longPressTexts());
    }

    @Test
    public void neitherIsOnByDefaultAndBothAreOfferedInSettings() {
        assertTrue(LetterLayouts.ALL.contains(KeyboardLayoutId.PAD_ARROWS));
        assertTrue(LetterLayouts.ALL.contains(KeyboardLayoutId.PAD_KEYPAD));
        assertFalse(LetterLayouts.DEFAULT.contains(KeyboardLayoutId.PAD_ARROWS));
        assertFalse(LetterLayouts.DEFAULT.contains(KeyboardLayoutId.PAD_KEYPAD));
    }

    @Test
    public void theTwoPadsAreNamedInEnglishAlone() {
        // Arrows and Keypad are not Korean layouts with Korean names; there is nothing for a
        // Hangul gloss to add to either.
        assertEquals("Arrows", LetterLayouts.displayName(KeyboardLayoutId.PAD_ARROWS));
        assertEquals("Keypad", LetterLayouts.displayName(KeyboardLayoutId.PAD_KEYPAD));
        assertEquals("arw", LetterLayouts.keyCapName(KeyboardLayoutId.PAD_ARROWS));
        assertEquals("num", LetterLayouts.keyCapName(KeyboardLayoutId.PAD_KEYPAD));
    }

    /** The three pad cells of a row: the two-column keys between the frame's edges. */
    private static List<String> padLabels(KeyboardLayout layout, int row) {
        List<String> labels = new ArrayList<>(3);
        for (SoftwareKeySpec key : layout.rows().get(row)) {
            if (key.columnSpan() == 2 && labels.size() < 3) {
                labels.add(key.label());
            }
        }
        return labels;
    }

    private static List<String> column(KeyboardLayout layout) {
        List<String> labels = new ArrayList<>();
        for (List<SoftwareKeySpec> row : layout.rows()) {
            labels.add(row.get(0).label());
        }
        return labels;
    }

    private static SoftwareKeySpec last(List<SoftwareKeySpec> row) {
        return row.get(row.size() - 1);
    }
}
