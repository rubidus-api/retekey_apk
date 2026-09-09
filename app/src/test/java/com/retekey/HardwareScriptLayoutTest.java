package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Locale;
import java.util.TreeSet;
import org.junit.Test;

/**
 * The physical tables for the layouts that also have a soft page of their own — Turkish F and
 * Bulgarian BDS — checked against that page.
 *
 * <p>A hardware table and a soft page are two transcriptions of one keyboard, made from the same
 * source, and a transcription is the kind of thing that is wrong in one cell and looks right. The
 * two cannot prove the source was read correctly, but they can prove they still agree: if one is
 * edited and the other is not, this fails. The letters are compared as a set, because the phone's
 * page reorders them into rows of ten and adds a fourth row for the overflow.
 */
public final class HardwareScriptLayoutTest {

    @Test
    public void turkishFTypesTheSameLettersOnGlassAndOnKeys() {
        assertEquals(softLetters(KeyboardLayoutId.TR_F, new Locale("tr")),
            tableLetters(KeyboardLayoutId.TR_F));
    }

    @Test
    public void bulgarianBdsTypesTheSameLettersOnGlassAndOnKeys() {
        assertEquals(softLetters(KeyboardLayoutId.BG_BDS, new Locale("bg")),
            tableLetters(KeyboardLayoutId.BG_BDS));
    }

    @Test
    public void theKeysTheSourceNamesAreWhereTheSourcePutsThem() {
        // Spot checks quoted from Microsoft's own tables (KBDTUF, KBDBU): the ones a wrong row
        // offset would move. Turkish F's home row starts at u and its j is on the US j.
        HardwareSemanticMapper f = HardwareLayoutTables.of(KeyboardLayoutId.TR_F);
        assertNotNull(f);
        assertEquals("u", f.map("hardware.key.a", false).text());
        assertEquals("k", f.map("hardware.key.j", false).text());
        assertEquals("ç", f.map("hardware.key.b", false).text());
        // Turkish casing, which is the trap in this language: i → İ and ı → I.
        assertEquals("İ", f.map("hardware.key.s", true).text());
        assertEquals("I", f.map("hardware.key.r", true).text());

        // BDS begins its home row with the soft sign and its top row with a comma.
        HardwareSemanticMapper bds = HardwareLayoutTables.of(KeyboardLayoutId.BG_BDS);
        assertNotNull(bds);
        assertEquals("ь", bds.map("hardware.key.a", false).text());
        assertEquals(",", bds.map("hardware.key.q", false).text());
        assertEquals("ю", bds.map("hardware.key.z", false).text());
    }

    /** Every letter the soft page shows, as a sorted set. */
    private static TreeSet<String> softLetters(KeyboardLayoutId id, Locale locale) {
        TreeSet<String> letters = new TreeSet<>();
        KeyboardLayout page = KeyboardLayouts.of(id, false);
        for (java.util.List<SoftwareKeySpec> row : page.rows()) {
            for (SoftwareKeySpec key : row) {
                if (key.isControl() || key.semanticInput() == null
                        || key.semanticInput().kind() != SemanticInput.Kind.TEXT) {
                    continue;
                }
                String text = key.semanticInput().text();
                if (isLetter(text, locale)) {
                    letters.add(text);
                }
            }
        }
        return letters;
    }

    /** Every letter the physical table types unshifted, as a sorted set. */
    private static TreeSet<String> tableLetters(KeyboardLayoutId id) {
        HardwareSemanticMapper mapper = HardwareLayoutTables.of(id);
        assertNotNull("no table for " + id, mapper);
        TreeSet<String> letters = new TreeSet<>();
        for (String key : allKeyIds()) {
            SemanticInput input = mapper.map(key, false);
            if (input != null && isLetter(input.text(), Locale.ROOT)) {
                letters.add(input.text());
            }
        }
        return letters;
    }

    private static boolean isLetter(String text, Locale locale) {
        return text != null && text.length() == 1 && Character.isLetter(text.charAt(0));
    }

    private static java.util.List<String> allKeyIds() {
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (char letter = 'a'; letter <= 'z'; letter++) {
            ids.add("hardware.key." + letter);
        }
        // The punctuation positions a script layout may take over.
        for (int code : new int[] {55, 56, 68, 69, 70, 71, 72, 73, 74, 75, 76}) {
            ids.add("hardware.keycode." + code);
        }
        return ids;
    }
}
