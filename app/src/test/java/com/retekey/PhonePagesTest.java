package com.retekey;

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
            assertEquals(Arrays.asList("Tab", "Alt", "Meta", "Ctrl"), column(layout, 0));
            for (List<SoftwareKeySpec> row : layout.rows()) {
                assertEquals(1, row.get(0).columnSpan());
            }
        }
    }

    @Test
    public void theSecondColumnCarriesTheOverlayToggles() {
        // The once-empty cells now hold the overlay toggles: 123 shows the keypad digits on the
        // pad's own keys, Move the cursor cluster. 천지인 keeps Next in the Alt row, and both
        // pages keep 漢 in the Tab row.
        for (KeyboardLayout layout : Arrays.asList(CHEONJIIN, NARATGEUL)) {
            assertEquals("row 0", "123", layout.rows().get(0).get(1).label());
            assertTrue(layout.rows().get(0).get(1).isControl());
            assertEquals("row 1", "Move", layout.rows().get(1).get(1).label());
            assertTrue(layout.rows().get(1).get(1).isControl());
            assertEquals("漢 has the Tab-row cell", "漢",
                layout.rows().get(3).get(1).label());
        }
        assertFalse("나랏글 keeps its Alt-row cell empty", NARATGEUL.rows().get(2).get(1).enabled());
        assertTrue("천지인 puts Next there", CHEONJIIN.rows().get(2).get(1).enabled());
    }

    @Test
    public void hanjaSitsBesideCtrlWhileHangulIsBeingTyped() {
        // 2026-08-30: the modifier column flipped to Tab/Alt/Meta/Ctrl top-to-bottom, so the
        // bottom row's own cell is Ctrl now.
        for (KeyboardLayout layout : Arrays.asList(CHEONJIIN, NARATGEUL)) {
            List<SoftwareKeySpec> bottom = layout.rows().get(3);
            assertEquals("Ctrl", bottom.get(0).label());
            SoftwareKeySpec hanja = bottom.get(1);
            assertEquals("漢", hanja.label());
            assertEquals(ControlKey.HANJA, hanja.control());
            assertEquals("a tap runs it; there is no hold", 1, hanja.columnSpan());
            assertFalse(hanja.hasLongPressControl());
        }
    }

    @Test
    public void hanjaIsNotOfferedWhileAnOverlayCoversThePad() {
        // Under the keypad or the cursor cluster there is no reading in front of the cursor to
        // convert, so the cell is blank rather than a key that cannot work.
        for (KeyboardLayoutId id
                : Arrays.asList(KeyboardLayoutId.KO_CHEONJIIN, KeyboardLayoutId.KO_NARATGEUL)) {
            for (PhoneOverlay overlay : Arrays.asList(PhoneOverlay.DIGITS, PhoneOverlay.NAV)) {
                SoftwareKeySpec cell =
                    KeyboardLayouts.phone(id, overlay).rows().get(3).get(1);
                assertFalse(id + " " + overlay, cell.enabled());
                assertFalse(id + " " + overlay, cell.isControl());
            }
        }
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
    public void cheonjiinPutsIeungUnderSiotBetweenItsPunctuation() {
        List<SoftwareKeySpec> bottom = CHEONJIIN.rows().get(3);
        List<SoftwareKeySpec> above = CHEONJIIN.rows().get(2);

        // Same cell index, so ㅇㅁ lands directly under ㅅㅎ.
        assertEquals("ㅅㅎ", above.get(3).label());
        assertEquals("ㅇㅁ", bottom.get(3).label());

        SoftwareKeySpec period = bottom.get(2);
        SoftwareKeySpec exclaim = bottom.get(4);
        assertEquals(".,", period.label());
        assertEquals("!?", exclaim.label());
        assertEquals("as wide as the Hangul keys they sit between", 2, period.columnSpan());
        assertEquals(2, exclaim.columnSpan());
    }

    @Test
    public void theBottomRowPunctuationCyclesRatherThanHolding() {
        List<SoftwareKeySpec> bottom = CHEONJIIN.rows().get(3);
        SoftwareKeySpec period = bottom.get(2);
        SoftwareKeySpec exclaim = bottom.get(4);

        assertEquals(".,", period.label());
        assertEquals("!?", exclaim.label());
        for (SoftwareKeySpec key : Arrays.asList(period, exclaim)) {
            assertTrue("both characters are on the face, not under a hold",
                key.longPressTexts().isEmpty());
            assertFalse(key.hasLongPressControl());
            assertEquals(2, key.columnSpan());
            assertEquals("the tap types the first of them",
                key.label().substring(0, 1), key.semanticInput().text());
        }
    }

    @Test
    public void theCommitKeySitsBesideMeta() {
        // 2026-08-30: the modifier column flipped, so row 2's own modifier is Meta now.
        SoftwareKeySpec commit = CHEONJIIN.rows().get(2).get(1);
        assertEquals("Next", commit.label());
        assertEquals(SemanticInput.Kind.FLUSH, commit.semanticInput().kind());
        assertEquals("the cell beside Meta is one column", 1, commit.columnSpan());
        assertEquals("Meta", CHEONJIIN.rows().get(2).get(0).label());
    }

    @Test
    public void naratgeulHoldsAPhoneKeypad() {
        // The twelve keys sit where a phone keypad's do, so they hold what a phone keypad holds.
        // The calculator set lives on the Keypad layout, which is a pad rather than a way of
        // writing Hangul.
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
