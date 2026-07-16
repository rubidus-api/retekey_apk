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
            Arrays.asList("⇧", "_", ";", ":", "`", "'", "\"", "?", "~", "⏎"),
            labels(PAGE, 2)
        );
        assertEquals(
            Arrays.asList("Ctrl", "Meta", "Alt", "space", "한/영", "pad", "Tab", "☰"),
            labels(PAGE, 3)
        );
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
    public void thePadKeyLeadsToTheSpecialKeysPage() {
        SoftwareKeySpec pad = PAGE.findById("touch.layer.pad");
        assertNotNull(pad);
        assertEquals(ControlKey.SPECIAL_KEYS_LAYER, pad.control());
    }

    private static List<String> labels(KeyboardLayout layout, int rowIndex) {
        List<String> labels = new ArrayList<>();
        for (SoftwareKeySpec key : layout.rows().get(rowIndex)) {
            labels.add(key.label());
        }
        return labels;
    }
}
