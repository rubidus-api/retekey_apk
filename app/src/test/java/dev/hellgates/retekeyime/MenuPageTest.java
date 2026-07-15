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
    public void hasFourRowsOfTenColumns() {
        assertEquals(10, menu.columns());
        assertEquals(4, menu.rows().size());
        // Each of the top three rows holds five two-column tiles.
        for (int row = 0; row < 3; row++) {
            int span = 0;
            for (SoftwareKeySpec key : menu.rows().get(row)) {
                span += key.columnSpan();
            }
            assertEquals("row " + row + " fills ten columns", 10, span);
            assertEquals("row " + row + " has five tiles", 5, menu.rows().get(row).size());
        }
    }

    @Test
    public void theWorkingTilesAreWiredToTheirControls() {
        assertEquals(ControlKey.OPEN_SETTINGS, menu.findById("touch.menu.settings").control());
        assertEquals(ControlKey.INSERT_DATE, menu.findById("touch.menu.date").control());
        assertEquals(ControlKey.UNDO, menu.findById("touch.menu.undo").control());
        assertEquals(ControlKey.COPY, menu.findById("touch.menu.copy").control());
        assertEquals(ControlKey.PASTE, menu.findById("touch.menu.paste").control());
        assertEquals(ControlKey.HEIGHT_DOWN, menu.findById("touch.menu.height.down").control());
        assertEquals(ControlKey.HEIGHT_UP, menu.findById("touch.menu.height.up").control());
    }

    @Test
    public void thePlaceholderTilesStayDisabled() {
        for (String id : Arrays.asList("touch.menu.emoji", "touch.menu.clipboard",
            "touch.menu.voice", "touch.menu.custom1", "touch.menu.custom2",
            "touch.menu.onehand.left", "touch.menu.onehand.right", "touch.menu.onehand.full")) {
            SoftwareKeySpec key = menu.findById(id);
            assertNotNull(id, key);
            assertFalse(id + " stays disabled", key.enabled());
            assertFalse(id + " is not a control yet", key.isControl());
        }
    }

    @Test
    public void tilesAreTwoColumnsWide() {
        assertEquals(2, menu.findById("touch.menu.settings").columnSpan());
        assertEquals(2, menu.findById("touch.menu.emoji").columnSpan());
    }

    @Test
    public void theBottomRowReturnsToLetters() {
        List<SoftwareKeySpec> bottom = menu.rows().get(3);
        SoftwareKeySpec returnKey = bottom.get(5);
        assertEquals("가", returnKey.label());
        assertEquals(ControlKey.PREVIOUS_LAYER, returnKey.control());
        // The ☰ key is still present and re-opens the menu page.
        assertEquals(ControlKey.MENU_LAYER, menu.findById("touch.menu").control());
    }
}
