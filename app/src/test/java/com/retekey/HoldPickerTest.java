package com.retekey;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** The strip a held key raises: out from under the finger, and towards the side with room. */
public final class HoldPickerTest {
    private static final int COLUMNS = 10;

    @Test
    public void aMiddleKeyPutsItsCandidatesOnTheRowAboveRunningRight() {
        HoldPicker picker = HoldPicker.place(COLUMNS, 2, 4, 1, 4);
        assertEquals(1, picker.stripRow);
        assertEquals(4, picker.stripStart);
        assertEquals(+1, picker.direction);
        assertEquals(4, picker.columnOf(0));
        assertEquals(7, picker.columnOf(3));
    }

    @Test
    public void aKeyAtTheRightEdgeRunsLeftInstead() {
        // Column 9, four candidates: the right has one column of room, the left has ten.
        HoldPicker picker = HoldPicker.place(COLUMNS, 1, 9, 1, 4);
        assertEquals(0, picker.stripRow);
        assertEquals(9, picker.stripStart);
        assertEquals(-1, picker.direction);
        assertEquals(6, picker.columnOf(3));
    }

    @Test
    public void aKeyAtTheLeftEdgeRunsRight() {
        HoldPicker picker = HoldPicker.place(COLUMNS, 2, 0, 1, 5);
        assertEquals(0, picker.stripStart);
        assertEquals(+1, picker.direction);
    }

    @Test
    public void aTopRowKeyStaysOnItsRowButStartsBesideTheFinger() {
        HoldPicker left = HoldPicker.place(COLUMNS, 0, 2, 1, 3);
        assertEquals(0, left.stripRow);
        assertEquals("starts one column to the right of the key", 3, left.stripStart);
        assertEquals(+1, left.direction);

        HoldPicker right = HoldPicker.place(COLUMNS, 0, 9, 1, 3);
        assertEquals("starts one column to the left of the key", 8, right.stripStart);
        assertEquals(-1, right.direction);
    }

    @Test
    public void aWideKeyAnchorsAtItsOwnEdge() {
        // Space-like key spanning columns 4-6 on the bottom row: above it, candidate 0 over its
        // left edge running right; if it had to run left, from its right edge.
        HoldPicker picker = HoldPicker.place(COLUMNS, 3, 4, 3, 3);
        assertEquals(4, picker.stripStart);
        assertEquals(+1, picker.direction);
        HoldPicker crowded = HoldPicker.place(COLUMNS, 3, 4, 3, 7);
        assertEquals("right has 6, left has 7: left from the key's right edge",
            6, crowded.stripStart);
        assertEquals(-1, crowded.direction);
        assertEquals(0, crowded.columnOf(6));
    }

    @Test
    public void whenNeitherSideHasRoomTheStripSlidesInsideTheRoomierSide() {
        // Column 5, eight candidates: right room 5, left room 6 -> runs left, must end at column 0.
        HoldPicker picker = HoldPicker.place(COLUMNS, 2, 5, 1, 8);
        assertEquals(-1, picker.direction);
        assertEquals(7, picker.stripStart);
        assertEquals(0, picker.columnOf(7));
        // Column 4, eight candidates: right room 6, left room 5 -> runs right, starts at column 2.
        HoldPicker other = HoldPicker.place(COLUMNS, 2, 4, 1, 8);
        assertEquals(+1, other.direction);
        assertEquals(2, other.stripStart);
        assertEquals(9, other.columnOf(7));
    }

    @Test
    public void moreCandidatesThanColumnsAreCutToTheColumns() {
        HoldPicker picker = HoldPicker.place(COLUMNS, 2, 4, 1, 14);
        assertEquals(COLUMNS, picker.count);
    }

    @Test
    public void theFingerSelectsByTheColumnItIsOver() {
        HoldPicker picker = HoldPicker.place(COLUMNS, 2, 4, 1, 4); // columns 4..7, running right
        assertEquals("not moved: the first", 0, picker.indexAt(9, false));
        assertEquals(0, picker.indexAt(4, true));
        assertEquals(2, picker.indexAt(6, true));
        assertEquals("past the far end: the last", 3, picker.indexAt(9, true));
        assertEquals("behind the near end: the first", 0, picker.indexAt(1, true));

        HoldPicker leftward = HoldPicker.place(COLUMNS, 1, 9, 1, 4); // columns 9..6, running left
        assertEquals(1, leftward.indexAt(8, true));
        assertEquals(3, leftward.indexAt(0, true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void aPickerWithNothingToPickIsAMistake() {
        HoldPicker.place(COLUMNS, 1, 1, 1, 0);
    }
}
