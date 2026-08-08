package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * The code-point pad: a readout and two rows of keys. It is a small floating panel, not a
 * keyboard, so it holds nothing it does not use — no filler cells spacing sixteen digits out
 * across a keyboard's worth of grid.
 */
public final class UnicodePadTest {
    private static final KeyboardLayout pad = KeyboardLayouts.unicodeEntry();

    @Test
    public void aReadoutAndTwoRowsOfKeys() {
        assertEquals(3, pad.rows().size());
        assertEquals(1, pad.rows().get(0).size());
        assertEquals(KeyboardLayouts.UNICODE_DISPLAY_ID, pad.rows().get(0).get(0).stableKeyId());
    }

    @Test
    public void theDigitsAreOnTheFirstRowAndTheLettersOnTheSecond() {
        assertEquals("1234567890", labels(pad.rows().get(1)).replace(" ", ""));
        assertTrue(labels(pad.rows().get(2)).startsWith("ABCDEF"));
    }

    @Test
    public void theThreeWaysOutSayWhatTheyDoInWords() {
        String bottom = labels(pad.rows().get(2));
        for (String word : new String[] {"Bksp", "Cancel", "OK"}) {
            assertTrue(word + " is on the pad", bottom.contains(word));
        }
    }

    @Test
    public void everyRowFillsTheGridExactly() {
        for (List<SoftwareKeySpec> row : pad.rows()) {
            int columns = 0;
            for (SoftwareKeySpec key : row) {
                columns += key.columnSpan();
            }
            assertEquals(labels(row), pad.columns(), columns);
        }
    }

    @Test
    public void nothingOnItIsAFillerCell() {
        // Every key on the second row does something; the row before it is all digits.
        List<String> dead = new ArrayList<>();
        for (int row = 1; row < pad.rows().size(); row++) {
            for (SoftwareKeySpec key : pad.rows().get(row)) {
                if (!key.enabled() && !key.isControl()) {
                    dead.add(key.stableKeyId());
                }
            }
        }
        assertEquals(dead.toString(), 0, dead.size());
    }

    private static String labels(List<SoftwareKeySpec> row) {
        StringBuilder text = new StringBuilder();
        for (SoftwareKeySpec key : row) {
            text.append(key.label());
        }
        return text.toString();
    }
}
