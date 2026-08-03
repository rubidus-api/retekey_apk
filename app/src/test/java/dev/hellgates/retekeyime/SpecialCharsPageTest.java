package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
            Arrays.asList("⇧", ";", ":", "`", "'", "\"", "?", "~", "_", "⏎"),
            labels(PAGE, 2)
        );
        assertEquals(
            Arrays.asList("Ctrl", "Meta", "Alt", "Tab", "space", " ", "!#", "\uD83C\uDF10"),
            labels(PAGE, 3)
        );
    }

    @Test
    public void minusIsTheUnderscoresHold() {
        // The pair a physical keyboard puts on one key stays on one key here too.
        SoftwareKeySpec underscore = PAGE.findById("touch.sym.underscore");
        assertNotNull(underscore);
        assertEquals(Arrays.asList("-"), underscore.longPressTexts());
        assertTrue("backtick no longer carries minus",
            PAGE.findById("touch.sym.backtick").longPressTexts().isEmpty());
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
    public void theUnderscorePairSitsBesideEnter() {
        List<SoftwareKeySpec> row = PAGE.rows().get(2);
        SoftwareKeySpec underscore = row.get(row.size() - 2);
        assertEquals("_", underscore.label());
        assertEquals("-", underscore.longPressTexts().get(0));
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
}
