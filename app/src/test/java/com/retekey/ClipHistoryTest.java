package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/** What the keyboard remembers of what was copied through it — and what it refuses to remember. */
public final class ClipHistoryTest {
    @Test
    public void nothingIsRememberedFromASensitiveEditor() {
        ClipHistory history = ClipHistory.empty().record("hunter2", true);
        assertTrue("a password must not survive the keystroke", history.isEmpty());
    }

    @Test
    public void aClipIsRememberedNewestFirst() {
        ClipHistory history = ClipHistory.empty()
            .record("first", false)
            .record("second", false);
        assertEquals(texts("second", "first"), textsOf(history));
    }

    @Test
    public void blankClipsAreNotWorthKeeping() {
        assertTrue(ClipHistory.empty().record("   \n ", false).isEmpty());
        assertTrue(ClipHistory.empty().record(null, false).isEmpty());
    }

    @Test
    public void copyingTheSameThingTwiceMovesItRatherThanDuplicating() {
        ClipHistory history = ClipHistory.empty()
            .record("a", false)
            .record("b", false)
            .record("a", false);
        assertEquals(texts("a", "b"), textsOf(history));
    }

    @Test
    public void unpinnedClipsAgeOutAndPinnedOnesDoNot() {
        ClipHistory history = ClipHistory.empty().record("keep me", false)
            .setPinned("keep me", true);
        for (int i = 0; i < ClipHistory.UNPINNED_LIMIT + 5; i++) {
            history = history.record("clip " + i, false);
        }
        List<ClipHistory.Clip> clips = history.clips();
        assertEquals("pinned first", "keep me", clips.get(0).text);
        assertTrue(clips.get(0).pinned);
        assertEquals("one pinned plus the cap",
            ClipHistory.UNPINNED_LIMIT + 1, clips.size());
    }

    @Test
    public void aRepeatKeepsItsPin() {
        ClipHistory history = ClipHistory.empty()
            .record("note", false)
            .setPinned("note", true)
            .record("note", false);
        assertTrue(history.clips().get(0).pinned);
    }

    @Test
    public void somethingEnormousIsTruncatedRatherThanStored() {
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < ClipHistory.MAX_LENGTH + 500; i++) {
            huge.append('x');
        }
        ClipHistory history = ClipHistory.empty().record(huge, false);
        assertEquals(ClipHistory.MAX_LENGTH, history.clips().get(0).text.length());
    }

    @Test
    public void forgettingAndClearing() {
        ClipHistory history = ClipHistory.empty()
            .record("a", false)
            .record("b", false)
            .setPinned("a", true);
        assertEquals(texts("a"), textsOf(history.remove("b")));
        assertEquals("clearing keeps the pinned ones", texts("a"), textsOf(history.clearUnpinned()));
    }

    @Test
    public void aHistorySurvivesBeingWrittenDown() {
        ClipHistory history = ClipHistory.empty()
            .record("first line\nsecond line", false)
            .record("pinned", false)
            .setPinned("pinned", true);
        assertEquals(textsOf(history), textsOf(ClipCodec.decode(ClipCodec.encode(history.clips()))));
        assertTrue(ClipCodec.decode(ClipCodec.encode(history.clips())).clips().get(0).pinned);
    }

    @Test
    public void anUnreadableRecordIsDroppedRatherThanGuessedAt() {
        assertTrue(ClipCodec.decode(null).isEmpty());
        assertTrue(ClipCodec.decode("").isEmpty());
        assertTrue(ClipCodec.decode("no separator here").isEmpty());
    }

    private static List<String> texts(String... values) {
        return java.util.Arrays.asList(values);
    }

    private static List<String> textsOf(ClipHistory history) {
        List<String> out = new java.util.ArrayList<>();
        for (ClipHistory.Clip clip : history.clips()) {
            out.add(clip.text);
        }
        return out;
    }
}
