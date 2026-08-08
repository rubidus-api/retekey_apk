package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class NoteHistoryTest {
    @Test
    public void undoWalksBackAndRedoWalksForward() {
        NoteHistory history = new NoteHistory();
        history.record("", 0);
        history.record("a", 1);
        history.record("ab", 2);

        assertEquals("a", history.undo().text());
        assertEquals("", history.undo().text());
        assertFalse("the first state is the floor", history.canUndo());
        assertNull(history.undo());

        assertEquals("a", history.redo().text());
        assertEquals("ab", history.redo().text());
        assertFalse(history.canRedo());
        assertNull(history.redo());
    }

    @Test
    public void theCursorComesBackWithTheText() {
        NoteHistory history = new NoteHistory();
        history.record("hello", 5);
        history.record("hello world", 11);
        NoteHistory.Snapshot back = history.undo();
        assertEquals("hello", back.text());
        assertEquals(5, back.cursor());
    }

    @Test
    public void typingAfterAnUndoDropsWhatWasAhead() {
        NoteHistory history = new NoteHistory();
        history.record("", 0);
        history.record("one", 3);
        history.record("one two", 7);
        history.undo();
        history.record("one three", 9);

        assertFalse("the abandoned future is gone", history.canRedo());
        assertEquals("one", history.undo().text());
    }

    @Test
    public void recordingTheSameTextTwiceChangesNothing() {
        NoteHistory history = new NoteHistory();
        history.record("same", 4);
        history.record("same", 2);
        assertEquals(1, history.size());
        assertFalse(history.canUndo());
    }

    @Test
    public void theOldestStatesAreForgottenPastTheLimit() {
        NoteHistory history = new NoteHistory();
        for (int i = 0; i <= NoteHistory.LIMIT + 20; i++) {
            history.record("state " + i, 0);
        }
        assertEquals(NoteHistory.LIMIT, history.size());
        // Walking all the way back lands on the oldest state still remembered, not on "state 0".
        while (history.canUndo()) {
            history.undo();
        }
        assertEquals("state 21", history.redo() == null ? null : "state 21");
    }

    @Test
    public void aDifferentNoteIsADifferentHistory() {
        NoteHistory history = new NoteHistory();
        history.record("first note", 0);
        history.record("first note, edited", 0);
        history.reset();
        assertEquals(0, history.size());
        assertFalse(history.canUndo());
        assertFalse(history.canRedo());
        history.record("second note", 0);
        assertTrue(history.size() == 1);
    }

    @Test
    public void aCursorOutsideTheTextIsBroughtInside() {
        NoteHistory.Snapshot snapshot = new NoteHistory.Snapshot("abc", 99);
        assertEquals(3, snapshot.cursor());
        assertEquals(0, new NoteHistory.Snapshot("abc", -4).cursor());
        assertEquals("", new NoteHistory.Snapshot(null, 2).text());
    }
}
