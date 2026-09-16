package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/**
 * The IPA page (issue #11): the symbols sit at the letters they sound like, each key holds its
 * family, and Shift is the plain letters rather than a second set of symbols — a transcription is
 * full of ordinary p, t, k, s, and a page that could not type them would not be usable.
 */
public final class IpaLayoutTest {
    private static final KeyboardLayout SYMBOLS = KeyboardLayouts.of(KeyboardLayoutId.ETC_IPA, false);
    private static final KeyboardLayout LETTERS = KeyboardLayouts.of(KeyboardLayoutId.ETC_IPA, true);

    @Test
    public void itIsOfferedInSettingsButNotOnByDefault() {
        assertTrue(LetterLayouts.ALL.contains(KeyboardLayoutId.ETC_IPA));
        assertFalse(LetterLayouts.DEFAULT.contains(KeyboardLayoutId.ETC_IPA));
        assertEquals("ipa", LetterLayouts.keyCapName(KeyboardLayoutId.ETC_IPA));
        assertEquals("etc", LetterLayouts.languageTag(KeyboardLayoutId.ETC_IPA));
    }

    @Test
    public void theSymbolsSitAtTheLettersTheySoundLike() {
        // The X-SAMPA reading of a US keyboard, which is what a phonetician already types in ASCII;
        // e is the schwa and a is æ by the reporter's own request.
        assertEquals("ə", typedAt(SYMBOLS, 0, 2));
        assertEquals("ɪ", typedAt(SYMBOLS, 0, 7));
        assertEquals("ʊ", typedAt(SYMBOLS, 0, 6));
        assertEquals("ɔ", typedAt(SYMBOLS, 0, 8));
        assertEquals("θ", typedAt(SYMBOLS, 0, 4));
        assertEquals("ɹ", typedAt(SYMBOLS, 0, 3));
        assertEquals("æ", typedAt(SYMBOLS, 1, 0));
        assertEquals("ʃ", typedAt(SYMBOLS, 1, 1));
        assertEquals("ð", typedAt(SYMBOLS, 1, 2));
        assertEquals("ʤ", typedAt(SYMBOLS, 1, 6));
        assertEquals("ʒ", typedAt(SYMBOLS, 2, 0));
        assertEquals("ʌ", typedAt(SYMBOLS, 2, 3));
        assertEquals("ŋ", typedAt(SYMBOLS, 2, 5));
        assertEquals("ˈ", typedAt(SYMBOLS, 2, 7));
    }

    @Test
    public void shiftIsThePlainLettersWhereTheyAreOnQwerty() {
        assertEquals(Arrays.asList("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
            typedRow(LETTERS, 0));
        assertEquals(Arrays.asList("a", "s", "d", "f", "g", "h", "j", "k", "l"),
            typedRow(LETTERS, 1));
        assertEquals(Arrays.asList("z", "x", "c", "v", "b", "n", "m", "/"),
            typedRow(LETTERS, 2));
        // The brackets a transcription is written between are held on that last key.
        assertTrue(symbolKeys(LETTERS, 2).get(7).longPressTexts().containsAll(
            Arrays.asList("[", "]")));
    }

    @Test
    public void everySymbolKeyHoldsItsOwnFamily() {
        for (KeyboardLayout page : new KeyboardLayout[] {SYMBOLS, LETTERS}) {
            for (SoftwareKeySpec key : symbolKeys(page)) {
                assertEquals(key.stableKeyId(), key.label(), key.semanticInput().text());
                for (String held : key.longPressTexts()) {
                    assertFalse(key.stableKeyId(), held.equals(key.label()));
                }
            }
        }
        for (SoftwareKeySpec key : symbolKeys(SYMBOLS)) {
            assertFalse("every symbol key holds something: " + key.stableKeyId(),
                key.longPressTexts().isEmpty());
        }
    }

    @Test
    public void noSymbolIsTypedFromTwoDifferentKeys() {
        Set<String> seen = new HashSet<>();
        for (SoftwareKeySpec key : symbolKeys(SYMBOLS)) {
            assertTrue("two keys type " + key.label(), seen.add(key.label()));
        }
    }

    @Test
    public void thePhysicalKeyboardTypesTheSamePagesInTheSamePlaces() {
        // The table is derived from the same rows the keys are drawn from, so the check is that
        // the derivation reached the right key positions: unshifted symbol, shifted letter.
        HardwareSemanticMapper mapper = HardwareLayoutTables.of(KeyboardLayoutId.ETC_IPA);
        assertNotNull(mapper);
        for (int row = 0; row < IpaKeys.POSITIONS.length; row++) {
            String[] positions = IpaKeys.POSITIONS[row];
            List<SoftwareKeySpec> symbols = symbolKeys(SYMBOLS, row);
            List<SoftwareKeySpec> letters = symbolKeys(LETTERS, row);
            assertEquals("row " + row, positions.length, symbols.size());
            for (int column = 0; column < positions.length; column++) {
                String id = positions[column].startsWith("keycode.")
                    ? "hardware." + positions[column] : "hardware.key." + positions[column];
                assertEquals(id, symbols.get(column).label(), mapper.map(id, false).text());
                assertEquals(id, letters.get(column).label(), mapper.map(id, true).text());
            }
        }
    }

    private static String typedAt(KeyboardLayout page, int row, int column) {
        return symbolKeys(page, row).get(column).label();
    }

    private static List<String> typedRow(KeyboardLayout page, int row) {
        List<String> labels = new ArrayList<>();
        for (SoftwareKeySpec key : symbolKeys(page, row)) {
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
