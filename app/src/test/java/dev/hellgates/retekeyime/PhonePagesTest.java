package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * The frame the two 12-key pages share: the modifiers own the leftmost column, the menu and pad
 * keys ride the second one, every Hangul key is two columns, and the right-hand column carries
 * backspace, space, then the period and Enter.
 */
public final class PhonePagesTest {
    private static final KeyboardLayout CHEONJIIN =
        KeyboardLayouts.of(KeyboardLayoutId.KO_CHEONJIIN, false);
    private static final KeyboardLayout NARATGEUL =
        KeyboardLayouts.of(KeyboardLayoutId.KO_NARATGEUL, false);

    @Test
    public void everyRowFillsTheGrid() {
        for (KeyboardLayout layout : Arrays.asList(CHEONJIIN, NARATGEUL)) {
            for (List<SoftwareKeySpec> row : layout.rows()) {
                int span = 0;
                for (SoftwareKeySpec key : row) {
                    span += key.columnSpan();
                }
                assertEquals("row fills " + layout.id(), KeyboardLayouts.COLUMNS, span);
            }
        }
    }

    @Test
    public void theModifiersOwnTheLeftmostColumn() {
        for (KeyboardLayout layout : Arrays.asList(CHEONJIIN, NARATGEUL)) {
            assertEquals(Arrays.asList("Ctrl", "Meta", "Alt", "Tab"), column(layout, 0));
            for (List<SoftwareKeySpec> row : layout.rows()) {
                assertEquals(1, row.get(0).columnSpan());
            }
        }
    }

    @Test
    public void theSecondColumnIsEmptyNowThatBothPagesAreHolds() {
        // The menu and the pad used to sit here. They are reached by holding the globe and the
        // symbols key, and the cells they left are empty rather than filled with something else.
        for (KeyboardLayout layout : Arrays.asList(CHEONJIIN, NARATGEUL)) {
            for (int row = 0; row < 4; row++) {
                assertFalse("row " + row, layout.rows().get(row).get(1).enabled());
            }
        }
    }

    @Test
    public void theEmptyCellInTheBottomRowConvertsToHanjaOnAHold() {
        // 천지인 puts it under ㅂㅍ, two columns wide; 나랏글's bottom row has only the one empty
        // cell, beside 획. Both are inert to a tap: the conversion is worth a hold.
        SoftwareKeySpec cheonjiin = CHEONJIIN.findById("touch.phone.gap.r3b");
        SoftwareKeySpec naratgeul = NARATGEUL.findById("touch.phone.gap.r3");
        for (SoftwareKeySpec cell : Arrays.asList(cheonjiin, naratgeul)) {
            assertNotNull(cell);
            assertFalse("a tap on it must do nothing", cell.enabled());
            assertEquals(ControlKey.HANJA, cell.longPressControl());
            assertEquals("漢", cell.longPressHint());
        }
        assertEquals(2, cheonjiin.columnSpan());
        assertEquals("the span must survive the hold being added", 1, naratgeul.columnSpan());
    }

    @Test
    public void theRightHandColumnIsTheSameOnBothPages() {
        for (KeyboardLayout layout : Arrays.asList(CHEONJIIN, NARATGEUL)) {
            List<SoftwareKeySpec> first = layout.rows().get(0);
            List<SoftwareKeySpec> second = layout.rows().get(1);
            List<SoftwareKeySpec> third = layout.rows().get(2);
            assertEquals("⌫", first.get(first.size() - 1).label());
            assertEquals(2, first.get(first.size() - 1).columnSpan());
            assertEquals("space", second.get(second.size() - 1).label());
            assertEquals(2, second.get(second.size() - 1).columnSpan());
            assertEquals(".", third.get(third.size() - 2).label());
            assertEquals("⏎", third.get(third.size() - 1).label());
        }
    }

    @Test
    public void theBottomRowEndsWithTheSymbolAndLayoutKeys() {
        for (KeyboardLayout layout : Arrays.asList(CHEONJIIN, NARATGEUL)) {
            List<SoftwareKeySpec> bottom = layout.rows().get(3);
            SoftwareKeySpec chars = bottom.get(bottom.size() - 2);
            SoftwareKeySpec globe = bottom.get(bottom.size() - 1);
            assertEquals(ControlKey.SPECIAL_CHARS_LAYER, chars.control());
            assertEquals(1, chars.columnSpan());
            assertEquals(ControlKey.LAYOUT_TOGGLE, globe.control());
            assertEquals(1, globe.columnSpan());
            assertEquals(ControlKey.MENU_LAYER, globe.longPressControl());
            assertEquals("m", globe.longPressHint());
            assertEquals(ControlKey.SPECIAL_KEYS_LAYER, chars.longPressControl());
            assertEquals("p", chars.longPressHint());
        }
    }

    @Test
    public void cheonjiinPutsIeungUnderSiotWithTheCommitKeyBesideIt() {
        List<SoftwareKeySpec> bottom = CHEONJIIN.rows().get(3);
        List<SoftwareKeySpec> above = CHEONJIIN.rows().get(2);

        // Same cell index, so ㅇㅁ lands directly under ㅅㅎ.
        assertEquals("ㅅㅎ", above.get(3).label());
        assertEquals("ㅇㅁ", bottom.get(3).label());
        assertFalse("the cell to its left is empty", bottom.get(2).enabled());
        SoftwareKeySpec commit = bottom.get(4);
        assertEquals(SemanticInput.Kind.FLUSH, commit.semanticInput().kind());
        assertEquals(2, commit.columnSpan());
    }

    @Test
    public void naratgeulHoldsAPhoneKeypad() {
        // The twelve keys sit where a phone keypad's do, so they hold what a phone keypad holds.
        assertEquals(Arrays.asList("1", "2", "3"), holds(NARATGEUL, 0));
        assertEquals(Arrays.asList("4", "5", "6"), holds(NARATGEUL, 1));
        assertEquals(Arrays.asList("7", "8", "9"), holds(NARATGEUL, 2));
        assertEquals(Arrays.asList("*", "0", "#"), holds(NARATGEUL, 3));
    }

    /** The alternates of a row's Hangul and transform keys, in order. */
    private static List<String> holds(KeyboardLayout layout, int row) {
        List<String> found = new ArrayList<>();
        for (SoftwareKeySpec key : layout.rows().get(row)) {
            if (key.stableKeyId().startsWith("touch.naratgeul.")) {
                found.add(key.hasLongPress() ? key.longPressTexts().get(0) : null);
            }
        }
        return found;
    }

    @Test
    public void naratgeulPutsTheStrokeKeysEitherSideOfTheEarthVowel() {
        assertEquals(Arrays.asList("획", "ㅡ", "쌍"), hangulLabels(NARATGEUL.rows().get(3)));
    }

    @Test
    public void everyHangulKeyIsTwoColumnsWide() {
        for (KeyboardLayout layout : Arrays.asList(CHEONJIIN, NARATGEUL)) {
            for (List<SoftwareKeySpec> row : layout.rows()) {
                for (SoftwareKeySpec key : row) {
                    if (key.stableKeyId().startsWith("touch.cheonjiin.")
                        || key.stableKeyId().startsWith("touch.naratgeul.")) {
                        assertEquals(key.label(), 2, key.columnSpan());
                    }
                }
            }
        }
    }

    @Test
    public void bothPagesCarryEveryKeyTheirInterpreterKnows() {
        assertEquals(CheonjiinInterpreter.Key.values().length, phoneKeyCount(CHEONJIIN));
        assertEquals(NaratgeulInterpreter.Key.values().length, phoneKeyCount(NARATGEUL));
    }

    private static int phoneKeyCount(KeyboardLayout layout) {
        int found = 0;
        for (List<SoftwareKeySpec> row : layout.rows()) {
            for (SoftwareKeySpec key : row) {
                if (key.stableKeyId().startsWith("touch.cheonjiin.")
                    || key.stableKeyId().startsWith("touch.naratgeul.")) {
                    found++;
                }
            }
        }
        return found;
    }

    private static List<String> hangulLabels(List<SoftwareKeySpec> row) {
        List<String> found = new ArrayList<>();
        for (SoftwareKeySpec key : row) {
            if (key.stableKeyId().startsWith("touch.naratgeul.")) {
                found.add(key.label());
            }
        }
        return found;
    }

    private static List<String> column(KeyboardLayout layout, int index) {
        List<String> found = new ArrayList<>();
        for (List<SoftwareKeySpec> row : layout.rows()) {
            found.add(row.get(index).label());
        }
        return found;
    }

    @Test
    public void theEmptyCellsTakeNoInput() {
        for (KeyboardLayout layout : Arrays.asList(CHEONJIIN, NARATGEUL)) {
            for (List<SoftwareKeySpec> row : layout.rows()) {
                for (SoftwareKeySpec key : row) {
                    if (key.stableKeyId().startsWith("touch.phone.gap.")) {
                        assertFalse(key.enabled());
                        assertTrue(key.label().trim().isEmpty());
                    }
                }
            }
        }
    }
}
