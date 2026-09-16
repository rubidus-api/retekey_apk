package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * The IPA page: a phonetic alphabet typed the way the letters are, two pages deep, with the rarer
 * symbol on each key's hold. It sits with the pads under "etc" because it writes no language of
 * its own — a transcription is written beside prose, not instead of it (issue #11).
 */
public final class IpaLayoutTest {
    private static final KeyboardLayout FIRST = KeyboardLayouts.of(KeyboardLayoutId.ETC_IPA, false);
    private static final KeyboardLayout SECOND = KeyboardLayouts.of(KeyboardLayoutId.ETC_IPA, true);

    @Test
    public void itIsOfferedInSettingsButNotOnByDefault() {
        assertTrue(LetterLayouts.ALL.contains(KeyboardLayoutId.ETC_IPA));
        assertFalse(LetterLayouts.DEFAULT.contains(KeyboardLayoutId.ETC_IPA));
        assertEquals("ipa", LetterLayouts.keyCapName(KeyboardLayoutId.ETC_IPA));
        assertEquals("etc", LetterLayouts.languageTag(KeyboardLayoutId.ETC_IPA));
    }

    @Test
    public void theTwoPagesAreDifferentSymbolsInTheSamePlaces() {
        assertEquals(symbols(FIRST).size(), symbols(SECOND).size());
        assertFalse(symbols(FIRST).equals(symbols(SECOND)));
        // Every symbol key types exactly what its cap says — a transcription is read off the keys.
        for (KeyboardLayout page : new KeyboardLayout[] {FIRST, SECOND}) {
            for (SoftwareKeySpec key : symbolKeys(page)) {
                assertEquals(key.label(), key.semanticInput().text());
            }
        }
    }

    @Test
    public void everySymbolKeyHoldsARarerSymbol() {
        for (KeyboardLayout page : new KeyboardLayout[] {FIRST, SECOND}) {
            for (SoftwareKeySpec key : symbolKeys(page)) {
                assertEquals(key.stableKeyId(), 1, key.longPressTexts().size());
                assertFalse(key.stableKeyId(), key.longPressTexts().get(0).equals(key.label()));
            }
        }
    }

    @Test
    public void thePhysicalKeyboardTypesTheSameSymbolsInTheSamePlaces() {
        // A US keyboard carries none of these, so the table remaps the letter positions outright:
        // unshifted is the first page and Shift is the second, read across the glass rows.
        HardwareSemanticMapper mapper = HardwareLayoutTables.of(KeyboardLayoutId.ETC_IPA);
        assertTrue(mapper != null);
        String[][] positions = {
            {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
            {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
            {"z", "x", "c", "v", "b", "n", "m", null},
        };
        for (int row = 0; row < positions.length; row++) {
            List<SoftwareKeySpec> glassFirst = symbolKeys(FIRST, row);
            List<SoftwareKeySpec> glassSecond = symbolKeys(SECOND, row);
            assertEquals("row " + row, positions[row].length, glassFirst.size());
            for (int column = 0; column < positions[row].length; column++) {
                String position = positions[row][column] == null
                    ? "hardware.keycode.55" : "hardware.key." + positions[row][column];
                assertEquals(position,
                    glassFirst.get(column).label(), mapper.map(position, false).text());
                assertEquals(position,
                    glassSecond.get(column).label(), mapper.map(position, true).text());
            }
        }
    }

    private static List<String> symbols(KeyboardLayout page) {
        List<String> labels = new ArrayList<>();
        for (SoftwareKeySpec key : symbolKeys(page)) {
            labels.add(key.label());
        }
        return labels;
    }

    private static List<SoftwareKeySpec> symbolKeys(KeyboardLayout page) {
        List<SoftwareKeySpec> keys = new ArrayList<>();
        for (int row = 0; row < page.rows().size(); row++) {
            keys.addAll(symbolKeys(page, row));
        }
        return keys;
    }

    private static List<SoftwareKeySpec> symbolKeys(KeyboardLayout page, int row) {
        List<SoftwareKeySpec> keys = new ArrayList<>();
        for (SoftwareKeySpec key : page.rows().get(row)) {
            if (key.stableKeyId().startsWith("touch.ipa.")) {
                keys.add(key);
            }
        }
        return keys;
    }
}
