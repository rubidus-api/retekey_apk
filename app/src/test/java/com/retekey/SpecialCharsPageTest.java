package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** Page 3: the special-characters page, reached by holding the period. Every key commits text. */
public final class SpecialCharsPageTest {
    private static final KeyboardLayout PAGE = KeyboardLayouts.specialChars();

    @Test
    public void theLayoutMatchesTheSpec() {
        assertEquals(KeyboardLayoutId.SPECIAL_CHARS, PAGE.id());
        assertEquals(
            Arrays.asList("!", "@", "#", "$", "%", "^", "&", "*", "(", ")"),
            labels(PAGE, 0)
        );
        assertEquals(
            Arrays.asList("\\", "|", "/", "[", "]", "{", "}", "<", ">", "⌫"),
            labels(PAGE, 1)
        );
        assertEquals(
            Arrays.asList("⇧", ";", ":", "`", "'", "\"", "?", "-", "_", "⏎"),
            labels(PAGE, 2)
        );
        assertEquals(
            Arrays.asList("Ctrl", "Meta", "Alt", "Tab", "space", "Esc", "!#", "\uD83C\uDF10"),
            labels(PAGE, 3)
        );
    }

    @Test
    public void minusHasItsOwnKeyAndTheUnderscoreHoldsNothing() {
        // Minus took the cell the tilde had; the tilde moved onto the backtick, which is the
        // key a physical keyboard shifts it from. The underscore is left with nothing on its hold.
        SoftwareKeySpec minus = PAGE.findById("touch.sym.minus");
        assertNotNull(minus);
        assertEquals("-", minus.label());
        assertEquals("-", minus.semanticInput().text());

        assertEquals(Arrays.asList("~"), PAGE.findById("touch.sym.backtick").longPressTexts());

        SoftwareKeySpec underscore = PAGE.findById("touch.sym.underscore");
        assertNotNull(underscore);
        assertTrue("the underscore's hold is empty", underscore.longPressTexts().isEmpty());
        assertTrue("the underscore has no hold at all", !underscore.hasLongPress());
    }

    @Test
    public void theTildeKeyIsGone() {
        assertNull(PAGE.findById("touch.sym.tilde"));
    }

    @Test
    public void everyHoldOffersExactlyOneAlternate() {
        // Holding types the alternate straight away, so a second entry would be unreachable.
        for (List<SoftwareKeySpec> row : PAGE.rows()) {
            for (SoftwareKeySpec key : row) {
                if (key.hasLongPress()) {
                    assertEquals(
                        key.label() + " offers one alternate", 1, key.longPressTexts().size());
                }
            }
        }
    }

    @Test
    public void everySymbolCommitsItsOwnCharacter() {
        for (int rowIndex = 0; rowIndex < 3; rowIndex++) {
            for (SoftwareKeySpec key : PAGE.rows().get(rowIndex)) {
                if (key.isControl() || !key.enabled()) {
                    continue;
                }
                if (key.semanticInput().kind() != SemanticInput.Kind.TEXT) {
                    // enter/backspace are the only non-text enabled keys here
                    assertTrue(
                        key.label(),
                        "⏎".equals(key.label()) || "⌫".equals(key.label())
                    );
                    continue;
                }
                assertEquals(key.label(), key.semanticInput().text());
            }
        }
    }

    @Test
    public void holdingTheSymbolsKeyLeadsToTheSpecialKeysPage() {
        SoftwareKeySpec chars = PAGE.findById("touch.layer.chars");
        assertNotNull(chars);
        assertEquals(ControlKey.SPECIAL_KEYS_LAYER, chars.longPressControl());
        assertEquals("p", chars.longPressHint());
    }

    @Test
    public void theMinusUnderscorePairSitsBesideEnter() {
        List<SoftwareKeySpec> row = PAGE.rows().get(2);
        SoftwareKeySpec underscore = row.get(row.size() - 2);
        assertEquals("_", underscore.label());
        assertEquals("the pair a keyboard puts on one key stays side by side",
            "-", row.get(row.size() - 3).label());
        assertEquals("⏎", row.get(row.size() - 1).label());
        assertEquals("the row still starts with shift", "⇧", row.get(0).label());
    }

    private static List<String> labels(KeyboardLayout layout, int rowIndex) {
        List<String> labels = new ArrayList<>();
        for (SoftwareKeySpec key : layout.rows().get(rowIndex)) {
            labels.add(key.label());
        }
        return labels;
    }

    @Test
    public void theNumberRowHoldsTheNumbers() {
        // Each shifted symbol holds the digit it shares a key with on a physical keyboard.
        String[] digits = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
        for (int column = 0; column < digits.length; column++) {
            SoftwareKeySpec key = PAGE.rows().get(0).get(column);
            assertEquals(key.label(), Arrays.asList(digits[column]), key.longPressTexts());
        }
    }

    @Test
    public void theClauseMarksHoldTheSentenceMarks() {
        assertEquals(Arrays.asList(","), PAGE.findById("touch.sym.semicolon").longPressTexts());
        assertEquals(Arrays.asList("."), PAGE.findById("touch.sym.colon").longPressTexts());
    }

    @Test
    public void escapeSitsBesideSpace() {
        assertEquals(RawKey.ESCAPE,
            PAGE.rows().get(3).get(5).semanticInput().rawKey());
    }
}
