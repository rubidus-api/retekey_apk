package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public final class SymbolLayerTest {
    private static final KeyboardLayout BASE = KeyboardLayouts.symbol(NumpadMode.NUMBERS, false);
    private static final KeyboardLayout SPECIAL = KeyboardLayouts.symbol(NumpadMode.NUMBERS, true);

    @Test
    public void baseNumberPageLayout() {
        assertEquals(KeyboardLayoutId.SYMBOL, BASE.id());
        assertEquals(
            Arrays.asList("!", "?", "#", "*", "&", "Num", "7", "8", "9", "0"),
            labels(BASE, 0)
        );
        assertEquals(
            Arrays.asList(",", ":", ";", "'", "_", "Fn", "4", "5", "6", "⏎"),
            labels(BASE, 1)
        );
        assertEquals(
            Arrays.asList("⇧", "+", "-", "×", "÷", ".", "1", "2", "3", "⌫"),
            labels(BASE, 2)
        );
    }

    @Test
    public void theRightColumnIsZeroEnterBackspaceTopToBottom() {
        assertEquals(SemanticInput.text("0"), BASE.rows().get(0).get(9).semanticInput());
        assertEquals(
            SemanticInput.Kind.PRIMARY_ACTION,
            BASE.rows().get(1).get(9).semanticInput().kind()
        );
        assertEquals(
            SemanticInput.Kind.DELETE_BACKWARD,
            BASE.rows().get(2).get(9).semanticInput().kind()
        );
    }

    @Test
    public void theFourArithmeticOperatorsAreGroupedAndEqualsBecameAPeriod() {
        assertEquals(
            Arrays.asList("+", "-", "×", "÷"),
            Arrays.asList(
                BASE.rows().get(2).get(1).label(),
                BASE.rows().get(2).get(2).label(),
                BASE.rows().get(2).get(3).label(),
                BASE.rows().get(2).get(4).label()
            )
        );
        SoftwareKeySpec period = BASE.findById("touch.sym.period");
        assertEquals(".", period.label());
        assertEquals(SemanticInput.text("."), period.semanticInput());
        // Equals is no longer its own key; it hangs off the period's long press.
        assertTrue(period.longPressTexts().contains("="));
    }

    @Test
    public void shiftAtColumnZeroRowTwoLikeTheLetterLayouts() {
        SoftwareKeySpec shift = BASE.rows().get(2).get(0);
        assertTrue(shift.isControl());
        assertEquals(ControlKey.SHIFT, shift.control());
    }

    @Test
    public void theShiftPageShowsTheSpecialKeys() {
        assertEquals(
            Arrays.asList("Esc", "PrtSc", "ScrLk", "Pause", "한자", "Num", "7", "8", "9", "0"),
            labels(SPECIAL, 0)
        );
        assertEquals(
            Arrays.asList("RCtrl", "RAlt", "RShft", "Menu", "Lang", "Fn", "4", "5", "6", "⏎"),
            labels(SPECIAL, 1)
        );
        // Play / Mute / Vol+ / Vol- are the media row; Lang sits above Vol-, as requested.
        assertEquals(
            Arrays.asList("⇧•", "Play", "Mute", "Vol+", "Vol-", ".", "1", "2", "3", "⌫"),
            labels(SPECIAL, 2)
        );
    }

    @Test
    public void mediaKeysDoNotAppearOnTheBaseNumberPage() {
        for (String label : Arrays.asList("Play", "Mute", "Vol+", "Vol-", "Esc", "한자")) {
            for (List<SoftwareKeySpec> row : BASE.rows()) {
                for (SoftwareKeySpec key : row) {
                    assertFalse(label + " must be Shift-only", label.equals(key.label()));
                }
            }
        }
    }

    @Test
    public void theRawSpecialKeysSendKeyEvents() {
        for (String id : Arrays.asList(
            "touch.special.esc", "touch.special.prtsc", "touch.special.scrlk",
            "touch.special.pause", "touch.special.menu"
        )) {
            SoftwareKeySpec key = SPECIAL.findById(id);
            assertNotNull(id, key);
            assertTrue(id + " sends a raw key", key.enabled());
            assertEquals(SemanticInput.Kind.RAW_KEY, key.semanticInput().kind());
        }
        assertEquals(RawKey.ESCAPE, SPECIAL.findById("touch.special.esc").semanticInput().rawKey());
        assertEquals(RawKey.MENU, SPECIAL.findById("touch.special.menu").semanticInput().rawKey());
    }

    @Test
    public void mediaAndKoreanSpecialKeysStayDisabledPendingTheirSystems() {
        for (String id : Arrays.asList(
            "touch.special.play", "touch.special.mute", "touch.special.volup",
            "touch.special.voldn", "touch.special.hanja", "touch.special.lang",
            "touch.special.rctrl", "touch.special.ralt", "touch.special.rshift"
        )) {
            SoftwareKeySpec key = SPECIAL.findById(id);
            assertNotNull(id, key);
            assertFalse(id + " stays disabled for now", key.enabled());
            assertFalse(key.isControl());
        }
    }

    @Test
    public void theBottomRowUsesThePreviousLayerKeyAndLatchingModifiers() {
        int bottom = BASE.rows().size() - 1;
        assertEquals(
            Arrays.asList("Ctrl", "Meta", "Alt", "space", "한/영", "이전", "Tab", "☰"),
            labels(BASE, bottom)
        );
        assertEquals(ControlKey.PREVIOUS_LAYER, BASE.findById("touch.layer.previous").control());
        assertEquals(ControlKey.CTRL, BASE.findById("touch.modifier.ctrl").control());
        assertEquals(ControlKey.META, BASE.findById("touch.modifier.meta").control());
        assertEquals(ControlKey.ALT, BASE.findById("touch.modifier.alt").control());
        assertEquals(ControlKey.TAB, BASE.findById("touch.edit.tab").control());
    }

    @Test
    public void theNineDigitsAreOnTheRightPadAndCommitText() {
        List<String> pad = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < 3; rowIndex++) {
            for (int keyIndex = 6; keyIndex <= 8; keyIndex++) {
                SoftwareKeySpec key = BASE.rows().get(rowIndex).get(keyIndex);
                pad.add(key.label());
                assertEquals(SemanticInput.Kind.TEXT, key.semanticInput().kind());
            }
        }
        assertEquals(Arrays.asList("7", "8", "9", "4", "5", "6", "1", "2", "3"), pad);
    }

    @Test
    public void numlockTurnsThePadIntoArrowsAndFnIntoFunctionKeys() {
        KeyboardLayout arrows = KeyboardLayouts.symbol(NumpadMode.ARROWS, false);
        assertEquals("Home", arrows.rows().get(0).get(6).label());
        assertEquals("↑", arrows.rows().get(0).get(7).label());
        assertEquals("↓", arrows.rows().get(2).get(7).label());

        KeyboardLayout functions = KeyboardLayouts.symbol(NumpadMode.FUNCTIONS, false);
        assertEquals("F7", functions.rows().get(0).get(6).label());
        assertEquals("F1", functions.rows().get(2).get(6).label());
        assertEquals("F3", functions.rows().get(2).get(8).label());
    }

    @Test
    public void theArrowAndFunctionPadsSendRawKeys() {
        KeyboardLayout arrows = KeyboardLayouts.symbol(NumpadMode.ARROWS, false);
        SoftwareKeySpec left = arrows.rows().get(1).get(6);
        assertEquals("←", left.label());
        assertTrue(left.enabled());
        assertEquals(RawKey.LEFT, left.semanticInput().rawKey());
        assertEquals(RawKey.HOME, arrows.rows().get(0).get(6).semanticInput().rawKey());
        assertEquals(RawKey.PAGE_DOWN, arrows.rows().get(2).get(8).semanticInput().rawKey());

        KeyboardLayout functions = KeyboardLayouts.symbol(NumpadMode.FUNCTIONS, false);
        assertEquals(RawKey.F7, functions.rows().get(0).get(6).semanticInput().rawKey());
        assertEquals(RawKey.F1, functions.rows().get(2).get(6).semanticInput().rawKey());
    }

    @Test
    public void theToggleKeysAreViewLocalControls() {
        assertEquals(ControlKey.NUMLOCK, BASE.findById("touch.numpad.numlock").control());
        assertEquals(ControlKey.FUNCTION_LOCK, BASE.findById("touch.numpad.fnlock").control());
    }

    private static List<String> labels(KeyboardLayout layout, int rowIndex) {
        List<String> labels = new ArrayList<>();
        for (SoftwareKeySpec key : layout.rows().get(rowIndex)) {
            labels.add(key.label());
        }
        return labels;
    }
}
