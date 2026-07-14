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
    private static final KeyboardLayout KOREAN =
        KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);

    private static final List<String> EXPECTED = Arrays.asList(
        ",", "?", "!", ":", ";", "'", "\"", "-", "_", "#", "*", "&"
    );

    @Test
    public void thePeriodKeyHidesTheEverydayPunctuation() {
        SoftwareKeySpec period = KOREAN.findById("touch.text.period");
        assertTrue(period.hasLongPress());
        assertEquals(EXPECTED, period.longPressTexts());
        assertEquals(SemanticInput.text("."), period.semanticInput());
    }

    @Test
    public void noCommaKeyRemains() {
        for (KeyboardLayoutId id : KeyboardLayoutId.values()) {
            for (boolean shifted : Arrays.asList(false, true)) {
                KeyboardLayout layout = KeyboardLayouts.of(id, shifted);
                assertNull(layout.findById("touch.text.comma"));
                for (List<SoftwareKeySpec> row : layout.rows()) {
                    for (SoftwareKeySpec key : row) {
                        assertFalse(",".equals(key.label()));
                    }
                }
            }
        }
    }

    @Test
    public void everyCandidateCommitsItsOwnCharacter() {
        SoftwareKeySpec period = KOREAN.findById("touch.text.period");
        for (int index = 0; index < EXPECTED.size(); index++) {
            ProjectKeyEvent event = period.longPressEvent(index);
            assertEquals(
                SemanticInput.text(EXPECTED.get(index)),
                event.semanticInput()
            );
            assertEquals(InputSource.SOFTWARE, event.source());
        }
    }

    @Test
    public void candidateIndexIsBoundsChecked() {
        SoftwareKeySpec period = KOREAN.findById("touch.text.period");
        try {
            period.longPressEvent(EXPECTED.size());
            org.junit.Assert.fail("expected an out-of-bounds candidate to be rejected");
        } catch (IndexOutOfBoundsException expected) {
            assertTrue(expected.getMessage().contains("long-press"));
        }
    }

    @Test
    public void keysWithoutAlternatesOpenNoPopup() {
        int bottomRow = KOREAN.rows().size() - 1;
        assertNull(LongPressPopup.open(KOREAN, 0, 0, WIDTH, HEIGHT));
        assertNull(LongPressPopup.open(KOREAN, bottomRow, 3, WIDTH, HEIGHT));
    }

    @Test
    public void aPopupWithNoViewOpensNothing() {
        assertNull(LongPressPopup.open(KOREAN, 2, 8, 0, HEIGHT));
        assertNull(LongPressPopup.open(KOREAN, 2, 8, WIDTH, 0));
    }

    @Test
    public void thePopupStaysInsideTheKeyboardAndShowsEveryCandidate() {
        LongPressPopup popup = periodPopup();
        assertNotNull(popup);
        assertEquals(EXPECTED.size(), popup.candidateCount());
        assertTrue("popup starts inside the view", popup.left() >= 0);
        assertTrue("popup ends inside the view", popup.right() <= WIDTH);
        assertTrue("popup sits above the held key", popup.top() >= 0);
        for (int index = 0; index < popup.candidateCount(); index++) {
            assertEquals(EXPECTED.get(index), popup.candidate(index));
        }
    }

    @Test
    public void everyCandidateCellSelectsItsOwnCandidate() {
        LongPressPopup popup = periodPopup();
        float y = popup.top() + 1.0f;
        for (int index = 0; index < popup.candidateCount(); index++) {
            float x = popup.cellLeft(index) + popup.cellWidth() * 0.5f;
            assertEquals(index, popup.indexAt(x, y));
        }
    }

    @Test
    public void slidingPastThePopupEdgesSelectsNothing() {
        LongPressPopup popup = periodPopup();
        float y = popup.top() + 1.0f;
        assertEquals(-1, popup.indexAt(popup.left() - 1.0f, y));
        assertEquals(-1, popup.indexAt(popup.right(), y));
        assertEquals(-1, popup.indexAt(popup.left() + 1.0f, popup.bottom() + 1.0f));
    }

    @Test
    public void slidingAboveThePopupKeepsTheSelection() {
        LongPressPopup popup = periodPopup();
        float x = popup.cellLeft(2) + popup.cellWidth() * 0.5f;
        assertEquals(2, popup.indexAt(x, popup.top() - 40.0f));
    }

    private static LongPressPopup periodPopup() {
        List<SoftwareKeySpec> row = KOREAN.rows().get(2);
        int keyIndex = -1;
        for (int index = 0; index < row.size(); index++) {
            if ("touch.text.period".equals(row.get(index).stableKeyId())) {
                keyIndex = index;
            }
        }
        assertTrue("the period key sits in the third row", keyIndex >= 0);
        return LongPressPopup.open(KOREAN, 2, keyIndex, WIDTH, HEIGHT);
    }
}
