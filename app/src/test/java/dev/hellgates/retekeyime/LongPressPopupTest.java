package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public final class LongPressPopupTest {
    private static final int WIDTH = 1080;
    private static final int HEIGHT = 720;
    private static final KeyboardLayout SYMBOL = KeyboardLayouts.symbol(NumpadMode.NUMBERS);

    @Test
    public void aSymbolKeyHidesItsVariantsBehindAHold() {
        SoftwareKeySpec divide = SYMBOL.findById("touch.sym.divide");
        assertNotNull(divide);
        assertTrue(divide.hasLongPress());
        assertEquals(Arrays.asList("/", "\\"), divide.longPressTexts());
        assertEquals(SemanticInput.text("÷"), divide.semanticInput());
    }

    @Test
    public void plainSymbolKeysHaveNoPopup() {
        for (String id : Arrays.asList("touch.sym.comma", "touch.sym.colon", "touch.sym.period")) {
            SoftwareKeySpec key = SYMBOL.findById(id);
            assertNotNull(id, key);
            assertFalse(id + " has no long press", key.hasLongPress());
            assertFalse(id + " has no long-press control", key.hasLongPressControl());
        }
    }

    @Test
    public void everyVariantCommitsItsOwnCharacter() {
        SoftwareKeySpec equals = SYMBOL.findById("touch.sym.equals");
        List<String> variants = Arrays.asList("(", ")", "[", "]", "{", "}", "<", ">");
        assertEquals(variants, equals.longPressTexts());
        for (int index = 0; index < variants.size(); index++) {
            ProjectKeyEvent event = equals.longPressEvent(index);
            assertEquals(SemanticInput.text(variants.get(index)), event.semanticInput());
            assertEquals(InputSource.SOFTWARE, event.source());
        }
    }

    @Test
    public void keysWithoutVariantsOpenNoPopup() {
        int keyIndex = indexOf(SYMBOL, 0, "touch.sym.0");
        assertTrue(keyIndex >= 0);
        assertNull(LongPressPopup.open(SYMBOL, 0, keyIndex, WIDTH, HEIGHT));
    }

    @Test
    public void aPopupWithNoViewOpensNothing() {
        int row = 2;
        int keyIndex = indexOf(SYMBOL, row, "touch.sym.divide");
        assertNull(LongPressPopup.open(SYMBOL, row, keyIndex, 0, HEIGHT));
        assertNull(LongPressPopup.open(SYMBOL, row, keyIndex, WIDTH, 0));
    }

    @Test
    public void thePopupStaysInsideTheKeyboardAndShowsEveryVariant() {
        LongPressPopup popup = equalsPopup();
        assertNotNull(popup);
        assertEquals(8, popup.candidateCount());
        assertTrue("popup starts inside the view", popup.left() >= 0);
        assertTrue("popup ends inside the view", popup.right() <= WIDTH);
        assertTrue("popup sits inside the view", popup.top() >= 0);
    }

    @Test
    public void everyCandidateCellSelectsItsOwnCandidate() {
        LongPressPopup popup = equalsPopup();
        float y = popup.top() + 1.0f;
        for (int index = 0; index < popup.candidateCount(); index++) {
            float x = popup.cellLeft(index) + popup.cellWidth() * 0.5f;
            assertEquals(index, popup.indexAt(x, y));
        }
    }

    @Test
    public void slidingPastThePopupEdgesSelectsNothing() {
        LongPressPopup popup = equalsPopup();
        float y = popup.top() + 1.0f;
        assertEquals(-1, popup.indexAt(popup.left() - 1.0f, y));
        assertEquals(-1, popup.indexAt(popup.right(), y));
        assertEquals(-1, popup.indexAt(popup.left() + 1.0f, popup.bottom() + 1.0f));
    }

    private static LongPressPopup equalsPopup() {
        int keyIndex = indexOf(SYMBOL, 2, "touch.sym.equals");
        assertTrue("the equals key sits in the third row", keyIndex >= 0);
        return LongPressPopup.open(SYMBOL, 2, keyIndex, WIDTH, HEIGHT);
    }

    private static int indexOf(KeyboardLayout layout, int rowIndex, String id) {
        List<SoftwareKeySpec> row = layout.rows().get(rowIndex);
        for (int index = 0; index < row.size(); index++) {
            if (id.equals(row.get(index).stableKeyId())) {
                return index;
            }
        }
        return -1;
    }
}
