package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * The 12-key overlays: 123 turns the pad's own cells into the phone keypad's digits, Move into
 * the cursor cluster. Only the twelve pad cells change; the frame — modifiers, backspace, space,
 * enter, 한자, the page keys, and the two toggles themselves — stays exactly where it was.
 */
public final class PhoneOverlayLayoutTest {
    private static final List<KeyboardLayoutId> PHONE_IDS = Arrays.asList(
        KeyboardLayoutId.KO_CHEONJIIN, KeyboardLayoutId.KO_NARATGEUL);

    @Test
    public void digitsOverlayShowsThePhoneKeypad() {
        for (KeyboardLayoutId id : PHONE_IDS) {
            KeyboardLayout layout = KeyboardLayouts.phone(id, PhoneOverlay.DIGITS);
            assertEquals(Arrays.asList("1", "2", "3"), padLabels(layout, 0));
            assertEquals(Arrays.asList("4", "5", "6"), padLabels(layout, 1));
            assertEquals(Arrays.asList("7", "8", "9"), padLabels(layout, 2));
            assertEquals(Arrays.asList("*", "0", "#"), padLabels(layout, 3));
        }
    }

    @Test
    public void navOverlayShowsTheCursorCluster() {
        for (KeyboardLayoutId id : PHONE_IDS) {
            KeyboardLayout layout = KeyboardLayouts.phone(id, PhoneOverlay.NAV);
            assertEquals(Arrays.asList("Home", "↑", "PgUp"), padLabels(layout, 0));
            assertEquals(Arrays.asList("←", "Ins", "→"), padLabels(layout, 1));
            assertEquals(Arrays.asList("End", "↓", "PgDn"), padLabels(layout, 2));
            assertEquals(Arrays.asList("Esc", "Del", " "), padLabels(layout, 3));
        }
    }

    @Test
    public void theFrameSurvivesEveryOverlay() {
        for (KeyboardLayoutId id : PHONE_IDS) {
            for (PhoneOverlay overlay : PhoneOverlay.values()) {
                KeyboardLayout layout = KeyboardLayouts.phone(id, overlay);
                assertEquals("123", layout.rows().get(0).get(1).label());
                assertEquals("Move", layout.rows().get(1).get(1).label());
                List<SoftwareKeySpec> top = layout.rows().get(0);
                assertEquals("⌫", top.get(top.size() - 1).label());
                List<SoftwareKeySpec> second = layout.rows().get(1);
                assertEquals("space", second.get(second.size() - 1).label());
                assertFalse("the cell 漢 left stays empty",
                    layout.rows().get(3).get(1).enabled());
            }
        }
    }

    @Test
    public void noOverlayIsTheBasePage() {
        for (KeyboardLayoutId id : PHONE_IDS) {
            assertSame(KeyboardLayouts.of(id, false),
                KeyboardLayouts.phone(id, PhoneOverlay.NONE));
        }
    }

    /** The three pad cells of a row — the 2-column keys between the frame's edges. */
    private static List<String> padLabels(KeyboardLayout layout, int row) {
        List<String> labels = new ArrayList<>(3);
        for (SoftwareKeySpec key : layout.rows().get(row)) {
            if (key.columnSpan() == 2
                && !key.stableKeyId().startsWith("touch.edit.backspace")
                && !key.stableKeyId().startsWith("touch.text.space")) {
                labels.add(key.label());
            }
        }
        return labels;
    }
}
