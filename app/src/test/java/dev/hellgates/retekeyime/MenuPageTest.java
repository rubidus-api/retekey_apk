package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public final class MenuPageTest {
    private final KeyboardLayout menu = KeyboardLayouts.menu();

    @Test
    public void hasFourRowsOfTenEqualColumns() {
        assertEquals(10, menu.columns());
        assertEquals(4, menu.rows().size());
        // Every menu key is one column wide, the same size as other pages' keys.
        for (int row = 0; row < 3; row++) {
            List<SoftwareKeySpec> keys = menu.rows().get(row);
            assertEquals("row " + row + " has ten keys", 10, keys.size());
            for (SoftwareKeySpec key : keys) {
                assertEquals("menu keys are one column wide", 1, key.columnSpan());
            }
        }
    }

    @Test
    public void theEditingTilesAreWiredToTheirControls() {
        assertEquals(ControlKey.OPEN_SETTINGS, menu.findById("touch.menu.settings").control());
        assertEquals(ControlKey.COPY, menu.findById("touch.menu.copy").control());
        assertEquals(ControlKey.CUT, menu.findById("touch.menu.cut").control());
        assertEquals(ControlKey.PASTE, menu.findById("touch.menu.paste").control());
        assertEquals(ControlKey.SELECT_ALL, menu.findById("touch.menu.selectall").control());
        assertEquals(ControlKey.UNDO, menu.findById("touch.menu.undo").control());
        assertEquals(ControlKey.REDO, menu.findById("touch.menu.redo").control());
        assertEquals(ControlKey.INSERT_DATE, menu.findById("touch.menu.date").control());
        assertEquals(ControlKey.HEIGHT_DOWN, menu.findById("touch.menu.height.down").control());
        assertEquals(ControlKey.HEIGHT_UP, menu.findById("touch.menu.height.up").control());
        assertEquals(ControlKey.SWITCH_IME, menu.findById("touch.menu.switchime").control());
        assertEquals(ControlKey.MANAGE_IME, menu.findById("touch.menu.manageime").control());
    }

    @Test
    public void theCursorTilesSendRawKeys() {
        SoftwareKeySpec left = menu.findById("touch.menu.cursor.left");
        assertTrue("cursor keys are enabled", left.enabled());
        assertEquals(SemanticInput.Kind.RAW_KEY, left.semanticInput().kind());
        assertEquals(RawKey.LEFT, left.semanticInput().rawKey());
        assertEquals(RawKey.END, menu.findById("touch.menu.cursor.end").semanticInput().rawKey());
        assertEquals(RawKey.PAGE_UP,
            menu.findById("touch.menu.cursor.pageup").semanticInput().rawKey());
    }

    @Test
    public void thePlaceholderTilesStayDisabled() {
        for (String id : Arrays.asList("touch.menu.emoji", "touch.menu.clipboard",
            "touch.menu.custom1", "touch.menu.custom2", "touch.menu.theme",
            "touch.menu.onehand.left", "touch.menu.onehand.right", "touch.menu.onehand.full")) {
            SoftwareKeySpec key = menu.findById(id);
            assertNotNull(id, key);
            assertFalse(id + " stays disabled", key.enabled());
            assertFalse(id + " is not a control yet", key.isControl());
        }
    }

    @Test
    public void theBottomRowReturnsToLetters() {
        SoftwareKeySpec returnKey = menu.rows().get(3).get(5);
        assertEquals("가", returnKey.label());
        assertEquals(ControlKey.PREVIOUS_LAYER, returnKey.control());
        assertEquals(ControlKey.MENU_LAYER, menu.findById("touch.menu").control());
    }
}
