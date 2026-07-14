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
    @Test
    public void numberModeShowsTheLeftStripDigitsToggleColumnAndPad() {
        KeyboardLayout symbol = KeyboardLayouts.symbol(NumpadMode.NUMBERS);
        assertEquals(KeyboardLayoutId.SYMBOL, symbol.id());
        assertEquals(
            Arrays.asList("0", "!", "?", "#", "*", "&", "Num", "7", "8", "9"),
            labels(symbol, 0)
        );
        assertEquals(
            Arrays.asList("⏎", ",", ":", ";", "'", "-", "Fn", "4", "5", "6"),
            labels(symbol, 1)
        );
        assertEquals(
            Arrays.asList(".", "_", "+", "×", "÷", "=", "⌫", "1", "2", "3"),
            labels(symbol, 2)
        );
    }

    @Test
    public void theLeftStripKeepsZeroEnterAndPeriodForCalculatorCompatibility() {
        KeyboardLayout symbol = KeyboardLayouts.symbol(NumpadMode.NUMBERS);
        assertEquals(SemanticInput.text("0"), symbol.rows().get(0).get(0).semanticInput());
        assertEquals(
            SemanticInput.Kind.PRIMARY_ACTION,
            symbol.rows().get(1).get(0).semanticInput().kind()
        );
        assertEquals(SemanticInput.text("."), symbol.rows().get(2).get(0).semanticInput());
    }

    @Test
    public void theBottomRowIsSharedExceptForTheLayerKey() {
        KeyboardLayout letters = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);
        KeyboardLayout symbol = KeyboardLayouts.symbol(NumpadMode.NUMBERS);
        int bottom = symbol.rows().size() - 1;
        assertEquals(
            Arrays.asList("Ctrl", "Meta", "Alt", "space", "한/영", "ABC", "Tab", "☰"),
            labels(symbol, bottom)
        );
        assertEquals(
            ControlKey.LETTER_LAYER,
            symbol.findById("touch.layer.letters").control()
        );
        // The layer key is the only difference from the letter layouts' bottom row.
        assertEquals("!#1", letters.rows().get(bottom).get(5).label());
        assertEquals("ABC", symbol.rows().get(bottom).get(5).label());
    }

    @Test
    public void theArithmeticAndPunctuationCommitAsText() {
        KeyboardLayout symbol = KeyboardLayouts.symbol(NumpadMode.NUMBERS);
        for (String id : Arrays.asList(
            "touch.sym.plus", "touch.sym.times", "touch.sym.divide", "touch.sym.equals",
            "touch.sym.comma", "touch.sym.bang", "touch.sym.question"
        )) {
            SoftwareKeySpec key = symbol.findById(id);
            assertNotNull(id, key);
            assertTrue(id + " commits text", key.enabled());
            assertEquals(SemanticInput.Kind.TEXT, key.semanticInput().kind());
        }
        assertEquals(SemanticInput.text("×"), symbol.findById("touch.sym.times").semanticInput());
        assertEquals(SemanticInput.text("÷"), symbol.findById("touch.sym.divide").semanticInput());
    }

    @Test
    public void theNineDigitsAreOnTheRightPad() {
        KeyboardLayout symbol = KeyboardLayouts.symbol(NumpadMode.NUMBERS);
        List<String> pad = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < 3; rowIndex++) {
            for (int keyIndex = 7; keyIndex <= 9; keyIndex++) {
                pad.add(symbol.rows().get(rowIndex).get(keyIndex).label());
            }
        }
        assertEquals(Arrays.asList("7", "8", "9", "4", "5", "6", "1", "2", "3"), pad);
        for (int rowIndex = 0; rowIndex < 3; rowIndex++) {
            for (int keyIndex = 7; keyIndex <= 9; keyIndex++) {
                SoftwareKeySpec key = symbol.rows().get(rowIndex).get(keyIndex);
                assertEquals(SemanticInput.Kind.TEXT, key.semanticInput().kind());
            }
        }
    }

    @Test
    public void numlockTurnsThePadIntoArrowsAndFnIntoFunctionKeys() {
        KeyboardLayout arrows = KeyboardLayouts.symbol(NumpadMode.ARROWS);
        assertEquals("Home", arrows.rows().get(0).get(7).label());
        assertEquals("↑", arrows.rows().get(0).get(8).label());
        assertEquals("→", arrows.rows().get(1).get(9).label());
        assertEquals("↓", arrows.rows().get(2).get(8).label());

        KeyboardLayout functions = KeyboardLayouts.symbol(NumpadMode.FUNCTIONS);
        assertEquals("F7", functions.rows().get(0).get(7).label());
        assertEquals("F1", functions.rows().get(2).get(7).label());
        assertEquals("F3", functions.rows().get(2).get(9).label());
    }

    @Test
    public void arrowAndFunctionKeysStayDisabledUntilRawKeyLands() {
        for (NumpadMode mode : Arrays.asList(NumpadMode.ARROWS, NumpadMode.FUNCTIONS)) {
            KeyboardLayout symbol = KeyboardLayouts.symbol(mode);
            for (int rowIndex = 0; rowIndex < 3; rowIndex++) {
                for (int keyIndex = 7; keyIndex <= 9; keyIndex++) {
                    SoftwareKeySpec key = symbol.rows().get(rowIndex).get(keyIndex);
                    assertFalse(
                        mode + " pad key must stay disabled: " + key.label(),
                        key.enabled()
                    );
                    assertFalse(key.isControl());
                }
            }
        }
    }

    @Test
    public void theToggleKeysAreViewLocalControls() {
        KeyboardLayout symbol = KeyboardLayouts.symbol(NumpadMode.NUMBERS);
        assertEquals(ControlKey.NUMLOCK, symbol.findById("touch.numpad.numlock").control());
        assertEquals(ControlKey.FUNCTION_LOCK, symbol.findById("touch.numpad.fnlock").control());
    }

    private static List<String> labels(KeyboardLayout layout, int rowIndex) {
        List<String> labels = new ArrayList<>();
        for (SoftwareKeySpec key : layout.rows().get(rowIndex)) {
            labels.add(key.label());
        }
        return labels;
    }
}
