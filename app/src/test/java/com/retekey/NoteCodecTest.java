package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** Notes survive being written down and read back — including the ones with awkward bodies. */
public final class NoteCodecTest {

    @Test
    public void aNoteComesBackAsItWentIn() {
        List<Note> notes = Arrays.asList(
            new Note("20260713-1448", "Shopping", "milk\nbread"),
            new Note("20260714-0900", "", ""));
        assertEquals(notes, NoteCodec.decode(NoteCodec.encode(notes)));
    }

    /**
     * A body is exactly where every separator someone can type will turn up — newlines, tabs, and
     * lines that look like another note's header.
     */
    @Test
    public void aBodyMayContainAnythingTypable() {
        Note awkward = new Note("20260713-1448", "odd\ttitle",
            "line\nline\ttab\n\n20260101-0000\tlooks like a header\nend");
        List<Note> back = NoteCodec.decode(NoteCodec.encode(Collections.singletonList(awkward)));
        assertEquals(1, back.size());
        assertEquals(awkward.body(), back.get(0).body());
        assertEquals("20260713-1448", back.get(0).stamp());
    }

    @Test
    public void nothingInNothingOut() {
        assertTrue(NoteCodec.decode(null).isEmpty());
        assertTrue(NoteCodec.decode("").isEmpty());
        assertEquals("", NoteCodec.encode(Collections.<Note>emptyList()));
    }

    @Test
    public void aRecordWithoutAStampIsDropped() {
        assertTrue(NoteCodec.decode("\tno stamp\nbody").isEmpty());
    }

    @Test
    public void aNoteWithNoBodyKeepsItsTitle() {
        List<Note> back = NoteCodec.decode(NoteCodec.encode(
            Collections.singletonList(new Note("20260713-1448", "title only", ""))));
        assertEquals("title only", back.get(0).title());
        assertEquals("", back.get(0).body());
    }
}
