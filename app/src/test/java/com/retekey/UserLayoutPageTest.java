package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/**
 * The page a layout file becomes. Everything asserted here is something {@code docs/user-layouts.md}
 * tells somebody else to rely on, so the guide is only as true as this file.
 */
public final class UserLayoutPageTest {
    private static final String WIDE = String.join("\n",
        "retekey-layout 1",
        "name: Wide",
        "cap: wid",
        // Twelve keys, which is two more than the grid is wide.
        "row: a b c d e f g h i j k l",
        "row: m n o p q r s t u v",
        "row: w x y z 1 2 3 4 5");

    @Test
    public void theRowsAreAsWideAsTheKeyboardLeavesThem() {
        // The guide's table: ten on the first row, nine on the second because backspace takes a
        // column, eight on the third because Shift and Enter take one each.
        assertEquals(10, typed(page(WIDE, false), 0).size());
        assertEquals(9, typed(page(WIDE, false), 1).size());
        assertEquals(8, typed(page(WIDE, false), 2).size());
        // And what fell off the end is the end, not the beginning.
        assertEquals("a", typed(page(WIDE, false), 0).get(0).label());
        assertEquals("j", typed(page(WIDE, false), 0).get(9).label());
    }

    @Test
    public void aShortRowKeepsThePagesShape() {
        KeyboardLayout page = page(String.join("\n",
            "retekey-layout 1", "name: Short", "row: a b", "row: c d", "row: e f"), false);
        // The three letter rows only: the bottom row is the keyboard's own and counts its wide
        // space bar as one cell.
        for (int row = 0; row < UserLayout.ROWS; row++) {
            assertEquals("row " + row + " is the grid's width",
                10, page.rows().get(row).size());
        }
        assertEquals(2, typed(page, 0).size());
    }

    @Test
    public void shiftTypesTheCapitalTheFileImplies() {
        KeyboardLayout shifted = page(String.join("\n",
            "retekey-layout 1", "name: Greek", "row: α β γ", "row: δ ε ζ", "row: η θ ι"), true);
        assertEquals("Α", typed(shifted, 0).get(0).label());
        assertEquals("Α", typed(shifted, 0).get(0).semanticInput().text());
    }

    @Test
    public void aKeyMayTypeMoreThanOneCharacterAndTwoSuchKeysStayApart() {
        // The guide offers digraph keys; two of them can start with the same letter, and a page
        // whose keys shared a name would be a page with one of them missing.
        KeyboardLayout page = page(String.join("\n",
            "retekey-layout 1", "name: Digraphs", "row: c ch cs", "row: a e i", "row: s š ž"), false);
        List<SoftwareKeySpec> row = typed(page, 0);
        assertEquals("ch", row.get(1).label());
        assertEquals("ch", row.get(1).semanticInput().text());
        Set<String> ids = new HashSet<>();
        for (SoftwareKeySpec key : typed(page, 0)) {
            assertTrue("two keys share the name " + key.stableKeyId(), ids.add(key.stableKeyId()));
        }
    }

    @Test
    public void whatAKeyHoldsIsOnTheKey() {
        KeyboardLayout page = page(String.join("\n",
            "retekey-layout 1", "name: Holds", "row: e|é|è a", "row: b c", "row: d f"), false);
        assertEquals(java.util.Arrays.asList("é", "è"), typed(page, 0).get(0).longPressTexts());
        assertTrue(typed(page, 0).get(1).longPressTexts().isEmpty());
    }

    @Test
    public void thePageKeepsTheKeyboardsOwnKeys() {
        KeyboardLayout page = page(WIDE, false);
        assertNotNull("backspace ends the second row",
            page.rows().get(1).get(9).semanticInput());
        assertEquals(SemanticInput.Kind.DELETE_BACKWARD,
            page.rows().get(1).get(9).semanticInput().kind());
        assertEquals(SemanticInput.Kind.PRIMARY_ACTION,
            page.rows().get(2).get(9).semanticInput().kind());
        assertTrue("Shift starts the third row",
            page.rows().get(2).get(0).isControl());
    }

    private static KeyboardLayout page(String file, boolean shifted) {
        UserLayout layout = UserLayout.parse(file);
        assertNotNull(layout);
        return KeyboardLayouts.user(layout, shifted);
    }

    /** The keys the file put on a row, without the keyboard's own around them. */
    private static List<SoftwareKeySpec> typed(KeyboardLayout page, int row) {
        List<SoftwareKeySpec> keys = new ArrayList<>();
        for (SoftwareKeySpec key : page.rows().get(row)) {
            if (key.stableKeyId().startsWith("touch.user.") && !key.isControl()
                    && key.semanticInput() != null && key.semanticInput().text() != null) {
                keys.add(key);
            }
        }
        return keys;
    }
}
