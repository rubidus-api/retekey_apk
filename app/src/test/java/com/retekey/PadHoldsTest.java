package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/** The calculator set held under the 12-key pads, and the keypad digits beside it. */
public final class PadHoldsTest {
    @Test
    public void theSetIsTheOneTheOwnerChose() {
        // 1+ 2( 3) 4- 5= 6% 7/ 8e 9^ *! 0$ #@ — the e is the letter, for exponents.
        String[] expected = {"+", "(", ")", "-", "=", "%", "/", "e", "^", "!", "$", "@"};
        for (int cell = 0; cell < PadHolds.CELLS; cell++) {
            assertEquals("cell " + cell, expected[cell], PadHolds.symbol(cell));
        }
    }

    @Test
    public void everyCellHoldsSomethingDifferent() {
        Set<String> seen = new HashSet<>();
        for (int cell = 0; cell < PadHolds.CELLS; cell++) {
            assertFalse("cell " + cell + " repeats a symbol", !seen.add(PadHolds.symbol(cell)));
        }
    }

    @Test
    public void theDigitsAreThePhoneKeypadsOwn() {
        assertEquals("1", PadHolds.digit(0));
        assertEquals("9", PadHolds.digit(8));
        assertEquals("*", PadHolds.digit(9));
        assertEquals("0", PadHolds.digit(10));
        assertEquals("#", PadHolds.digit(11));
    }

    @Test
    public void thereIsNoThirteenthCell() {
        for (int cell : new int[] {-1, PadHolds.CELLS}) {
            try {
                PadHolds.symbol(cell);
                fail("cell " + cell + " should not exist");
            } catch (IndexOutOfBoundsException expected) {
                // The pad has twelve cells; asking for another is a bug, not a blank.
            }
        }
    }
}
