package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

/**
 * The long-press popup geometry and hit testing. No shipping layout uses character variants right
 * now (the period's hold switches pages instead), so this exercises the mechanism with a synthetic
 * key placed on a real grid.
 */
public final class LongPressPopupTest {
    private static final int WIDTH = 1080;
    private static final int HEIGHT = 720;
    private static final KeyboardLayout GRID = variantGrid();

    /** A one-row grid whose middle key carries character variants, for testing the popup. */
    private static KeyboardLayout variantGrid() {
        SoftwareKeySpec variantKey = SoftwareKeySpec
            .enabled("test.variant", "(", SemanticInput.text("("))
            .withLongPress("[", "{", "<");
        return KeyboardLayout.of(
            KeyboardLayoutId.SPECIAL_CHARS,
            false,
            10,
            Collections.singletonList(KeyboardLayout.row(
                SoftwareKeySpec.enabled("a", "a", SemanticInput.text("a")),
                SoftwareKeySpec.enabled("b", "b", SemanticInput.text("b")),
                SoftwareKeySpec.enabled("c", "c", SemanticInput.text("c")),
                SoftwareKeySpec.enabled("d", "d", SemanticInput.text("d")),
                variantKey,
                SoftwareKeySpec.enabled("f", "f", SemanticInput.text("f")),
                SoftwareKeySpec.enabled("g", "g", SemanticInput.text("g")),
                SoftwareKeySpec.enabled("h", "h", SemanticInput.text("h")),
                SoftwareKeySpec.enabled("i", "i", SemanticInput.text("i")),
                SoftwareKeySpec.enabled("j", "j", SemanticInput.text("j"))
            ))
        );
    }

    @Test
    public void aKeyWithVariantsExposesThemInOrder() {
        SoftwareKeySpec key = GRID.findById("test.variant");
        assertTrue(key.hasLongPress());
        assertEquals(Arrays.asList("[", "{", "<"), key.longPressTexts());
        assertEquals(SemanticInput.text("{"), key.longPressEvent(1).semanticInput());
        assertEquals(InputSource.SOFTWARE, key.longPressEvent(0).source());
    }

    @Test
    public void plainKeysOpenNoPopup() {
        assertNull(LongPressPopup.open(GRID, 0, 0, WIDTH, HEIGHT));
    }

    @Test
    public void aPopupWithNoViewOpensNothing() {
        assertNull(LongPressPopup.open(GRID, 0, 4, 0, HEIGHT));
        assertNull(LongPressPopup.open(GRID, 0, 4, WIDTH, 0));
    }

    @Test
    public void thePopupStaysInsideTheKeyboardAndShowsEveryVariant() {
        LongPressPopup popup = popup();
        assertNotNull(popup);
        assertEquals(3, popup.candidateCount());
        assertTrue(popup.left() >= 0);
        assertTrue(popup.right() <= WIDTH);
        assertTrue(popup.top() >= 0);
    }

    @Test
    public void everyCandidateCellSelectsItsOwnCandidate() {
        LongPressPopup popup = popup();
        float y = popup.top() + 1.0f;
        for (int index = 0; index < popup.candidateCount(); index++) {
            float x = popup.cellLeft(index) + popup.cellWidth() * 0.5f;
            assertEquals(index, popup.indexAt(x, y));
        }
    }

    @Test
    public void slidingPastThePopupEdgesSelectsNothing() {
        LongPressPopup popup = popup();
        float y = popup.top() + 1.0f;
        assertEquals(-1, popup.indexAt(popup.left() - 1.0f, y));
        assertEquals(-1, popup.indexAt(popup.right(), y));
        assertEquals(-1, popup.indexAt(popup.left() + 1.0f, popup.bottom() + 1.0f));
    }

    private static LongPressPopup popup() {
        return LongPressPopup.open(GRID, 0, 4, WIDTH, HEIGHT);
    }
}
