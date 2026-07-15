package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** Page 4: the special-keys page (keypad plus special/function keys), with Num and Fn variants. */
public final class SpecialKeysPageTest {
    private static final KeyboardLayout NUMBERS = KeyboardLayouts.specialKeys(NumpadMode.NUMBERS);
    private static final KeyboardLayout ARROWS = KeyboardLayouts.specialKeys(NumpadMode.ARROWS);
    private static final KeyboardLayout FUNCTIONS = KeyboardLayouts.specialKeys(NumpadMode.FUNCTIONS);

    @Test
    public void theBaseKeypadLayoutMatchesTheSpec() {
        assertEquals(KeyboardLayoutId.SPECIAL_KEYS, NUMBERS.id());
        assertEquals(
            Arrays.asList("Esc", "PrtSc", "ScrLk", "Pause", "한자", "Num", "7", "8", "9", "0"),
            labels(NUMBERS, 0)
        );
        assertEquals(
            Arrays.asList("RAlt", "RCtrl", "RShft", "Menu", "Lang", "Fn", "4", "5", "6", "⏎"),
            labels(NUMBERS, 1)
        );
        assertEquals(
            Arrays.asList("⇧", "가", "+", "-", "=", ".", "1", "2", "3", "⌫"),
            labels(NUMBERS, 2)
        );
    }

    @Test
    public void theRawSpecialKeysSendKeyEvents() {
        assertEquals(RawKey.ESCAPE, raw(NUMBERS, "touch.key.esc"));
        assertEquals(RawKey.PRINT_SCREEN, raw(NUMBERS, "touch.key.prtsc"));
        assertEquals(RawKey.SCROLL_LOCK, raw(NUMBERS, "touch.key.scrlk"));
        assertEquals(RawKey.BREAK, raw(NUMBERS, "touch.key.pause"));
        assertEquals(RawKey.MENU, raw(NUMBERS, "touch.key.menu"));
    }

    @Test
    public void digitsAndArithmeticCommitText() {
        for (String label : Arrays.asList("7", "8", "9", "0", "4", "5", "6", "1", "2", "3",
            "+", "-", "=", ".")) {
            SoftwareKeySpec key = findByLabel(NUMBERS, label);
            assertNotNull(label, key);
            assertTrue(label + " commits text", key.enabled());
            assertEquals(SemanticInput.Kind.TEXT, key.semanticInput().kind());
        }
    }

    @Test
    public void koreanAndMediaSpecialKeysStayDisabled() {
        for (String id : Arrays.asList("touch.key.hanja", "touch.key.lang",
            "touch.key.ralt", "touch.key.rctrl", "touch.key.rshift")) {
            SoftwareKeySpec key = NUMBERS.findById(id);
            assertNotNull(id, key);
            assertFalse(id + " stays disabled", key.enabled());
            assertFalse(key.isControl());
        }
    }

    @Test
    public void numTurnsTheKeypadIntoArrows() {
        assertEquals("Home", ARROWS.rows().get(0).get(6).label());
        assertEquals(RawKey.HOME, ARROWS.rows().get(0).get(6).semanticInput().rawKey());
        assertEquals(RawKey.LEFT, findByLabel(ARROWS, "←").semanticInput().rawKey());
        assertEquals(RawKey.PAGE_DOWN, findByLabel(ARROWS, "PgDn").semanticInput().rawKey());
        // 0 / Enter / Backspace strip and the special keys stay put.
        assertEquals("0", ARROWS.rows().get(0).get(9).label());
        assertEquals("Esc", ARROWS.rows().get(0).get(0).label());
    }

    @Test
    public void fnSwapsTheWholePageToFunctionAndMediaKeys() {
        assertEquals(
            Arrays.asList("F11", "F12", "F13", "F14", "F15", "Num", "F7", "F8", "F9", "F10"),
            labels(FUNCTIONS, 0)
        );
        assertEquals(
            Arrays.asList("☀+", "☀-", "Vol+", "Vol-", "Mute", "Fn", "F4", "F5", "F6", "⏎"),
            labels(FUNCTIONS, 1)
        );
        assertEquals(
            Arrays.asList("⇧", "⏮", "⏯", "⏭", "검색", "뒤로", "F1", "F2", "F3", "⌫"),
            labels(FUNCTIONS, 2)
        );
        // F1-F12 send key events; F13-F15 have no Android keycode, so they stay disabled.
        assertEquals(RawKey.F1, raw(FUNCTIONS, "touch.key.fn.6"));
        assertEquals(RawKey.F11, raw(FUNCTIONS, "touch.key.f11"));
        assertEquals(RawKey.SEARCH, raw(FUNCTIONS, "touch.key.search"));
        assertFalse(FUNCTIONS.findById("touch.key.f13").enabled());
        assertFalse(FUNCTIONS.findById("touch.key.mute").enabled());
        assertFalse(FUNCTIONS.findById("touch.key.back").enabled());
    }

    @Test
    public void theReturnKeyGoesBackToLetters() {
        assertEquals(
            ControlKey.PREVIOUS_LAYER,
            NUMBERS.findById("touch.layer.letters").control()
        );
        assertEquals(ControlKey.NUMLOCK, NUMBERS.findById("touch.numpad.numlock").control());
        assertEquals(ControlKey.FUNCTION_LOCK, NUMBERS.findById("touch.numpad.fnlock").control());
    }

    private static RawKey raw(KeyboardLayout layout, String id) {
        return layout.findById(id).semanticInput().rawKey();
    }

    private static SoftwareKeySpec findByLabel(KeyboardLayout layout, String label) {
        for (List<SoftwareKeySpec> row : layout.rows()) {
            for (SoftwareKeySpec key : row) {
                if (label.equals(key.label())) {
                    return key;
                }
            }
        }
        return null;
    }

    private static List<String> labels(KeyboardLayout layout, int rowIndex) {
        List<String> labels = new ArrayList<>();
        for (SoftwareKeySpec key : layout.rows().get(rowIndex)) {
            labels.add(key.label());
        }
        return labels;
    }
}
