package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public final class KeyboardLayoutTest {
    private static final int WIDTH = 1080;
    private static final int HEIGHT = 720;

    private static final List<KeyboardLayout> ALL = Arrays.asList(
        KeyboardLayouts.of(KeyboardLayoutId.EN_QWERTY, false),
        KeyboardLayouts.of(KeyboardLayoutId.EN_QWERTY, true),
        KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false),
        KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, true),
        KeyboardLayouts.specialChars(),
        KeyboardLayouts.specialKeys(NumpadMode.NUMBERS),
        KeyboardLayouts.specialKeys(NumpadMode.ARROWS),
        KeyboardLayouts.specialKeys(NumpadMode.FUNCTIONS)
    );

    @Test
    public void everyRowSpansTheSameColumnCount() {
        for (KeyboardLayout layout : ALL) {
            assertEquals(KeyboardLayouts.COLUMNS, layout.columns());
            for (List<SoftwareKeySpec> row : layout.rows()) {
                int spanned = 0;
                for (SoftwareKeySpec key : row) {
                    spanned += key.columnSpan();
                }
                assertEquals(layout.id() + " row width", KeyboardLayouts.COLUMNS, spanned);
            }
        }
    }

    @Test
    public void onlySpaceIsWiderThanOneColumn() {
        for (KeyboardLayout layout : ALL) {
            for (List<SoftwareKeySpec> row : layout.rows()) {
                for (SoftwareKeySpec key : row) {
                    if ("touch.text.space".equals(key.stableKeyId())) {
                        assertEquals(KeyboardLayouts.SPACE_COLUMN_SPAN, key.columnSpan());
                    } else {
                        assertEquals(
                            key.stableKeyId() + " must be one column",
                            1,
                            key.columnSpan()
                        );
                    }
                }
            }
        }
    }

    @Test
    public void keyIdsAreUniqueWithinALayout() {
        for (KeyboardLayout layout : ALL) {
            Set<String> ids = new HashSet<>();
            for (List<SoftwareKeySpec> row : layout.rows()) {
                for (SoftwareKeySpec key : row) {
                    assertTrue(
                        "duplicate key id " + key.stableKeyId(),
                        ids.add(key.stableKeyId())
                    );
                }
            }
        }
    }

    @Test
    public void koreanKeepsQwertyGeometryAndCellIdentity() {
        KeyboardLayout english = KeyboardLayouts.of(KeyboardLayoutId.EN_QWERTY, false);
        KeyboardLayout korean = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);
        assertEquals(english.rows().size(), korean.rows().size());
        for (int rowIndex = 0; rowIndex < english.rows().size(); rowIndex++) {
            List<SoftwareKeySpec> englishRow = english.rows().get(rowIndex);
            List<SoftwareKeySpec> koreanRow = korean.rows().get(rowIndex);
            assertEquals("row " + rowIndex + " key count", englishRow.size(), koreanRow.size());
            for (int keyIndex = 0; keyIndex < englishRow.size(); keyIndex++) {
                assertEquals(
                    "row " + rowIndex + " key " + keyIndex + " span",
                    englishRow.get(keyIndex).columnSpan(),
                    koreanRow.get(keyIndex).columnSpan()
                );
                assertEquals(
                    "row " + rowIndex + " key " + keyIndex + " start column",
                    english.startColumn(rowIndex, keyIndex),
                    korean.startColumn(rowIndex, keyIndex)
                );
            }
        }
    }

    @Test
    public void dubeolsikRowsCarryTheStandardJamo() {
        KeyboardLayout korean = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);
        assertEquals(
            Arrays.asList("ㅂ", "ㅈ", "ㄷ", "ㄱ", "ㅅ", "ㅛ", "ㅕ", "ㅑ", "ㅐ", "ㅔ"),
            labels(korean, 0)
        );
        assertEquals(
            Arrays.asList("ㅁ", "ㄴ", "ㅇ", "ㄹ", "ㅎ", "ㅗ", "ㅓ", "ㅏ", "ㅣ", "⏎"),
            labels(korean, 1)
        );
        assertEquals(
            Arrays.asList("⇧", "ㅋ", "ㅌ", "ㅊ", "ㅍ", "ㅠ", "ㅜ", "ㅡ", ".", "⌫"),
            labels(korean, 2)
        );
        assertEquals(
            Arrays.asList("Ctrl", "Meta", "Alt", "space", "한/영", "pad", "Tab", "☰"),
            labels(korean, 3)
        );
    }

    @Test
    public void qwertyRowsCarryTheEnglishLetterBlock() {
        KeyboardLayout english = KeyboardLayouts.of(KeyboardLayoutId.EN_QWERTY, false);
        assertEquals(
            Arrays.asList("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
            labels(english, 0)
        );
        assertEquals(
            Arrays.asList("a", "s", "d", "f", "g", "h", "j", "k", "l", "⏎"),
            labels(english, 1)
        );
        assertEquals(
            Arrays.asList("⇧", "z", "x", "c", "v", "b", "n", "m", ".", "⌫"),
            labels(english, 2)
        );
        assertEquals(
            "the bottom row is identical in every layout",
            labels(KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false), 3),
            labels(english, 3)
        );
    }

    @Test
    public void shiftLayerProducesTenseConsonantsAndTheWideVowels() {
        KeyboardLayout shifted = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, true);
        assertEquals(
            Arrays.asList("ㅃ", "ㅉ", "ㄸ", "ㄲ", "ㅆ", "ㅛ", "ㅕ", "ㅑ", "ㅒ", "ㅖ"),
            labels(shifted, 0)
        );
        assertEquals(
            SemanticJamo.contextualConsonant(1),
            shifted.findById("touch.ko2.giyeok").semanticInput().jamo()
        );
        assertEquals(
            SemanticJamo.vowel(3),
            shifted.findById("touch.ko2.ae").semanticInput().jamo()
        );
        assertEquals(
            "unshifted home row stays identical",
            Arrays.asList("ㅁ", "ㄴ", "ㅇ", "ㄹ", "ㅎ", "ㅗ", "ㅓ", "ㅏ", "ㅣ", "⏎"),
            labels(shifted, 1)
        );
    }

    @Test
    public void englishShiftLayerUppercasesTheLetters() {
        KeyboardLayout shifted = KeyboardLayouts.of(KeyboardLayoutId.EN_QWERTY, true);
        assertEquals(
            Arrays.asList("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
            labels(shifted, 0)
        );
        assertEquals(
            SemanticInput.text("Q"),
            shifted.findById("touch.en.q").semanticInput()
        );
    }

    @Test
    public void functionKeysCarryTheExpectedSemantics() {
        KeyboardLayout korean = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);
        assertEquals(
            SemanticInput.Kind.PRIMARY_ACTION,
            korean.findById("touch.edit.enter").semanticInput().kind()
        );
        assertEquals(
            SemanticInput.Kind.DELETE_BACKWARD,
            korean.findById("touch.edit.backspace").semanticInput().kind()
        );
        assertEquals(
            SemanticInput.text(" "),
            korean.findById("touch.text.space").semanticInput()
        );
        assertEquals(
            ControlKey.SHIFT,
            korean.findById("touch.modifier.shift").control()
        );
        assertEquals(
            ControlKey.LAYOUT_TOGGLE,
            korean.findById("touch.layout.toggle").control()
        );
    }

    @Test
    public void controlKeysNeverReachTheDispatcher() {
        KeyboardLayout korean = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);
        SoftwareKeySpec shift = korean.findById("touch.modifier.shift");
        assertTrue(shift.isControl());
        assertFalse(shift.enabled());
        assertNull(shift.semanticInput());
    }

    @Test
    public void theMenuKeyOpensSettings() {
        KeyboardLayout korean = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);
        SoftwareKeySpec menu = korean.findById("touch.menu");
        assertNotNull(menu);
        assertTrue("the ☰ menu key is a control that opens settings", menu.isControl());
        assertEquals(ControlKey.OPEN_SETTINGS, menu.control());
    }

    @Test
    public void theBottomRowModifiersAreLatchingControls() {
        KeyboardLayout korean = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);
        assertEquals(ControlKey.CTRL, korean.findById("touch.modifier.ctrl").control());
        assertEquals(ControlKey.META, korean.findById("touch.modifier.meta").control());
        assertEquals(ControlKey.ALT, korean.findById("touch.modifier.alt").control());
        assertEquals(ControlKey.TAB, korean.findById("touch.edit.tab").control());
    }

    @Test
    public void thePadKeyEntersTheSpecialKeysPage() {
        SoftwareKeySpec padKey = KeyboardLayouts
            .of(KeyboardLayoutId.KO_DUBEOLSIK, false)
            .findById("touch.layer.pad");
        assertNotNull(padKey);
        assertTrue(padKey.isControl());
        assertEquals("pad", padKey.label());
        assertEquals(ControlKey.SPECIAL_KEYS_LAYER, padKey.control());
    }

    @Test
    public void holdingThePeriodSwitchesToTheSpecialCharsPage() {
        SoftwareKeySpec period = KeyboardLayouts
            .of(KeyboardLayoutId.KO_DUBEOLSIK, false)
            .findById("touch.text.period");
        assertNotNull(period);
        assertEquals(SemanticInput.text("."), period.semanticInput());
        assertTrue(period.hasLongPressControl());
        assertEquals(ControlKey.SPECIAL_CHARS_LAYER, period.longPressControl());
    }

    @Test
    public void hitTestingFollowsTheOrthogonalGrid() {
        KeyboardLayout korean = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);
        int rowHeight = HEIGHT / 4;
        int columnWidth = WIDTH / KeyboardLayouts.COLUMNS;

        for (int rowIndex = 0; rowIndex < korean.rows().size(); rowIndex++) {
            List<SoftwareKeySpec> row = korean.rows().get(rowIndex);
            float y = rowIndex * rowHeight + rowHeight * 0.5f;
            for (int keyIndex = 0; keyIndex < row.size(); keyIndex++) {
                SoftwareKeySpec expected = row.get(keyIndex);
                int startColumn = korean.startColumn(rowIndex, keyIndex);
                for (int span = 0; span < expected.columnSpan(); span++) {
                    float x = (startColumn + span) * columnWidth + columnWidth * 0.5f;
                    assertSame(
                        "row " + rowIndex + " column " + (startColumn + span),
                        expected,
                        korean.keyAt(x, y, WIDTH, HEIGHT)
                    );
                }
            }
        }
    }

    @Test
    public void spaceOwnsExactlyThreeColumnsOfTheBottomRow() {
        KeyboardLayout korean = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);
        int bottom = korean.rows().size() - 1;
        float y = HEIGHT - 1;
        int hits = 0;
        for (int column = 0; column < KeyboardLayouts.COLUMNS; column++) {
            float x = korean.columnEdge(column, WIDTH) + 1.0f;
            SoftwareKeySpec key = korean.keyAt(x, y, WIDTH, HEIGHT);
            if ("touch.text.space".equals(key.stableKeyId())) {
                hits++;
            }
        }
        assertEquals(KeyboardLayouts.SPACE_COLUMN_SPAN, hits);
        assertEquals(bottom, korean.rows().size() - 1);
    }

    @Test
    public void touchesOutsideTheViewHitNoKey() {
        KeyboardLayout korean = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);
        assertNull(korean.keyAt(-1.0f, 10.0f, WIDTH, HEIGHT));
        assertNull(korean.keyAt(10.0f, -1.0f, WIDTH, HEIGHT));
        assertNull(korean.keyAt(WIDTH, 10.0f, WIDTH, HEIGHT));
        assertNull(korean.keyAt(10.0f, HEIGHT, WIDTH, HEIGHT));
        assertNull(korean.keyAt(10.0f, 10.0f, 0, HEIGHT));
    }

    @Test
    public void layoutToggleSwapsTheTwoBaseLayouts() {
        assertEquals(
            KeyboardLayoutId.KO_DUBEOLSIK,
            KeyboardLayouts.otherLetters(KeyboardLayoutId.EN_QWERTY)
        );
        assertEquals(
            KeyboardLayoutId.EN_QWERTY,
            KeyboardLayouts.otherLetters(KeyboardLayoutId.KO_DUBEOLSIK)
        );
    }

    private static List<String> labels(KeyboardLayout layout, int rowIndex) {
        List<String> labels = new ArrayList<>();
        for (SoftwareKeySpec key : layout.rows().get(rowIndex)) {
            labels.add(key.label());
        }
        return labels;
    }
}
