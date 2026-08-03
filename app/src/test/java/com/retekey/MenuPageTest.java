package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public final class MenuPageTest {
    private static final KeyboardLayout menu = KeyboardLayouts.menu();

    @Test
    public void theEditingKeysAreAllOnTheRight() {
        // Columns 5-9 are the editing hand: copy/cut/paste, the arrows, the jump keys, settings.
        for (int row = 0; row < 3; row++) {
            for (int column = 5; column < 10; column++) {
                SoftwareKeySpec key = menu.rows().get(row).get(column);
                assertTrue(key.label() + " is usable", key.enabled() || key.isControl());
            }
        }
        assertEquals(ControlKey.COPY, menu.rows().get(0).get(5).control());
        assertEquals(ControlKey.CUT, menu.rows().get(1).get(5).control());
        assertEquals(ControlKey.PASTE, menu.rows().get(2).get(5).control());
    }

    @Test
    public void theArrowsFormACrossAroundSelectAll() {
        assertEquals(RawKey.UP, menu.rows().get(0).get(7).semanticInput().rawKey());
        assertEquals(RawKey.LEFT, menu.rows().get(1).get(6).semanticInput().rawKey());
        assertEquals(ControlKey.SELECT_ALL, menu.rows().get(1).get(7).control());
        assertEquals(RawKey.RIGHT, menu.rows().get(1).get(8).semanticInput().rawKey());
        assertEquals(RawKey.DOWN, menu.rows().get(2).get(7).semanticInput().rawKey());
    }

    @Test
    public void theJumpKeysSitBesideTheArrows() {
        assertEquals(RawKey.HOME, menu.rows().get(0).get(6).semanticInput().rawKey());
        assertEquals(RawKey.PAGE_UP, menu.rows().get(0).get(8).semanticInput().rawKey());
        assertEquals(RawKey.INSERT, menu.rows().get(0).get(9).semanticInput().rawKey());
        assertEquals(RawKey.FORWARD_DELETE, menu.rows().get(1).get(9).semanticInput().rawKey());
        assertEquals(RawKey.END, menu.rows().get(2).get(6).semanticInput().rawKey());
        assertEquals(RawKey.PAGE_DOWN, menu.rows().get(2).get(8).semanticInput().rawKey());
    }

    @Test
    public void settingsIsInTheBottomRightCorner() {
        List<SoftwareKeySpec> lastLetterRow = menu.rows().get(2);
        SoftwareKeySpec corner = lastLetterRow.get(lastLetterRow.size() - 1);

        assertEquals(ControlKey.OPEN_SETTINGS, corner.control());
    }

    @Test
    public void theLeftHalfKeepsTheKeyboardAndHistoryCommands() {
        assertEquals(ControlKey.UNDO, menu.rows().get(0).get(0).control());
        assertEquals(ControlKey.REDO, menu.rows().get(0).get(1).control());
        assertEquals(ControlKey.INSERT_DATE, menu.rows().get(0).get(2).control());
        assertEquals(ControlKey.HEIGHT_DOWN, menu.rows().get(1).get(0).control());
        assertEquals(ControlKey.HEIGHT_UP, menu.rows().get(1).get(1).control());
        assertEquals(ControlKey.SWITCH_IME, menu.rows().get(1).get(2).control());
        assertEquals(ControlKey.MANAGE_IME, menu.rows().get(1).get(3).control());
        assertEquals(ControlKey.FLOATING_TOGGLE, menu.rows().get(1).get(4).control());
    }

    @Test
    public void thePlaceholderTilesStayDisabled() {
        for (String id : Arrays.asList("touch.menu.emoji", "touch.menu.clipboard",
            "touch.menu.custom1", "touch.menu.custom2", "touch.menu.theme",
            "touch.menu.onehand.left", "touch.menu.onehand.full")) {
            SoftwareKeySpec key = menu.findById(id);
            assertNotNull(id, key);
            assertFalse(id + " stays disabled", key.enabled());
            assertFalse(id + " is not a control yet", key.isControl());
        }
    }

    @Test
    public void everyCellIsFilled() {
        for (List<SoftwareKeySpec> row : menu.rows()) {
            int span = 0;
            for (SoftwareKeySpec key : row) {
                span += key.columnSpan();
            }
            assertEquals(KeyboardLayouts.COLUMNS, span);
        }
    }

    @Test
    public void theBottomRowReturnsToLetters() {
        SoftwareKeySpec returnKey = menu.rows().get(3).get(5);
        assertEquals("ABC", returnKey.label());
        assertEquals(ControlKey.PREVIOUS_LAYER, returnKey.control());
        assertEquals(ControlKey.MENU_LAYER,
            menu.findById("touch.layout.toggle").longPressControl());
    }
}
