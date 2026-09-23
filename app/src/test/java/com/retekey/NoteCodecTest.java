package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** Notes survive being written down and read back — including the ones with awkward bodies. */
public final class NoteCodecTest {
    private static Note note(String id, String stamp, String title, String body) {
        return new Note(id, stamp, title, body);
    }

    @Test
    public void aNoteComesBackAsItWentIn() {
        List<Note> notes = Arrays.asList(
            note("a", "20260713-1448", "Shopping", "milk\nbread"),
            note("b", "20260714-0900", "", ""));
        assertEquals(notes, NoteCodec.decode(NoteCodec.encode(notes)));
    }

    /**
     * A body is where every separator someone can type will turn up — newlines, tabs, a line that
     * looks like another note's header, and the record separator the first form was built on.
     */
    @Test
    public void aBodyMayContainAnything() {
        Note awkward = note("a", "20260713-1448", "odd\ttitle\u001Fand a unit separator",
            "line\nline\ttab\n\n20260101-0000\tlooks like a header\u001Eand a record one\nend");
        List<Note> back = NoteCodec.decode(NoteCodec.encode(Collections.singletonList(awkward)));
        assertEquals(1, back.size());
        assertEquals(awkward.body(), back.get(0).body());
        assertEquals(awkward.title(), back.get(0).title());
        assertEquals("20260713-1448", back.get(0).stamp());
    }

    @Test
    public void nothingInNothingOut() {
        assertTrue(NoteCodec.decode(null).isEmpty());
        assertTrue(NoteCodec.decode("").isEmpty());
        assertTrue(NoteCodec.decode(NoteCodec.encode(Collections.<Note>emptyList())).isEmpty());
    }

    /** Notes written by the first form are still read, and are given ids of their own. */
    @Test
    public void theFirstFormIsStillRead() {
        String stored = "20260713-1448\tShopping\nmilk\nbread"
            + "\u001E20260713-1448\tSecond in the same minute\nother";
        List<Note> notes = NoteCodec.decode(stored);
        assertEquals(2, notes.size());
        assertEquals("milk\nbread", notes.get(0).body());
        assertEquals("Second in the same minute", notes.get(1).title());
        assertEquals(notes.get(0).stamp(), notes.get(1).stamp());
        assertNotEquals(notes.get(0).id(), notes.get(1).id());
        // The same stored text always gives the same ids, so a second read is not new notes.
        assertEquals(notes, NoteCodec.decode(stored));
        assertFalse(NoteCodec.isCurrentForm(stored));
        assertTrue(NoteCodec.isCurrentForm(NoteCodec.encode(notes)));
    }

    @Test
    public void aFirstFormRecordWithoutAStampIsDropped() {
        assertTrue(NoteCodec.decode("\tno stamp\nbody").isEmpty());
    }

    @Test
    public void aNoteWithNoBodyKeepsItsTitle() {
        List<Note> back = NoteCodec.decode(NoteCodec.encode(
            Collections.singletonList(note("a", "20260713-1448", "title only", ""))));
        assertEquals("title only", back.get(0).title());
        assertEquals("", back.get(0).body());
    }
}
