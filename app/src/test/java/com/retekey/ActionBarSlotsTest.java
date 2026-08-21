package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** The bar's slot list: stored as records, repaired rather than trusted. */
public final class ActionBarSlotsTest {
    @Test
    public void aStoredBarRoundTrips() {
        List<BarSlot> bar = Arrays.asList(
            BarSlot.of(BarAction.PASTE),
            BarSlot.text("ReteKey", "rk"),
            BarSlot.chord(RawKey.B, Collections.singleton(KeyModifier.CTRL), null));
        assertEquals(bar, ActionBarSlots.parse(ActionBarSlots.format(bar)));
    }

    @Test
    public void aBarWrittenBeforeCustomSlotsExistedIsStillRead() {
        // Every bar stored up to v0.1.106 was a comma-separated list of action words.
        assertEquals(
            Arrays.asList(BarSlot.of(BarAction.COPY), BarSlot.of(BarAction.PASTE)),
            ActionBarSlots.parse("copy,paste"));
    }

    @Test
    public void anActionThisBuildDoesNotKnowIsDropped() {
        assertEquals(Arrays.asList(BarSlot.of(BarAction.COPY), BarSlot.of(BarAction.PASTE)),
            ActionBarSlots.parse("copy,macro:3,paste"));
    }

    @Test
    public void anUnreadableBarFallsBackToTheDefault() {
        assertEquals(ActionBarSlots.defaults(), ActionBarSlots.parse(null));
        assertEquals(ActionBarSlots.defaults(), ActionBarSlots.parse(""));
        assertEquals(ActionBarSlots.defaults(), ActionBarSlots.parse("nothing,whatever"));
    }

    @Test
    public void textAndChordsSurviveBeingWrittenDown() {
        BarSlot text = BarSlot.text("hello\nthere", "greet");
        BarSlot chord = BarSlot.chord(RawKey.F5,
            new java.util.LinkedHashSet<>(Arrays.asList(KeyModifier.CTRL, KeyModifier.SHIFT)), "run");
        List<BarSlot> read = ActionBarSlots.parse(ActionBarSlots.format(Arrays.asList(text, chord)));
        assertEquals(2, read.size());
        assertEquals("hello\nthere", read.get(0).text());
        assertEquals("greet", read.get(0).customLabel());
        assertEquals(RawKey.F5, read.get(1).key());
        assertTrue(read.get(1).modifiers().contains(KeyModifier.CTRL));
        assertTrue(read.get(1).modifiers().contains(KeyModifier.SHIFT));
        assertEquals("Ctrl+Shift+F5", read.get(1).chordName());
    }

    @Test
    public void aSlotCarryingASeparatorIsDroppedRatherThanCorruptingTheNext() {
        List<BarSlot> bar = Arrays.asList(
            BarSlot.text("bad\u001Etext", null), BarSlot.of(BarAction.COPY));
        assertEquals(Arrays.asList(BarSlot.of(BarAction.COPY)),
            ActionBarSlots.parse(ActionBarSlots.format(bar)));
    }

    @Test
    public void movingASlotIsTheSameWhicheverWayItIsMoved() {
        List<BarSlot> bar = Arrays.asList(
            BarSlot.of(BarAction.SELECT_WORD), BarSlot.of(BarAction.COPY),
            BarSlot.of(BarAction.PASTE), BarSlot.of(BarAction.LEFT));
        assertEquals(Arrays.asList(
            BarSlot.of(BarAction.COPY), BarSlot.of(BarAction.SELECT_WORD),
            BarSlot.of(BarAction.PASTE), BarSlot.of(BarAction.LEFT)),
            ActionBarSlots.moved(bar, 0, 1));
        assertEquals("dragged to the front",
            Arrays.asList(BarSlot.of(BarAction.LEFT), BarSlot.of(BarAction.SELECT_WORD),
                BarSlot.of(BarAction.COPY), BarSlot.of(BarAction.PASTE)),
            ActionBarSlots.moved(bar, 3, 0));
    }

    @Test
    public void anImpossibleMoveLeavesTheBarAlone() {
        List<BarSlot> bar = Arrays.asList(BarSlot.of(BarAction.COPY), BarSlot.of(BarAction.PASTE));
        assertSame(bar, ActionBarSlots.moved(bar, 1, 1));
        assertSame(bar, ActionBarSlots.moved(bar, -1, 0));
        assertSame(bar, ActionBarSlots.moved(bar, 0, 2));
    }

    @Test
    public void aSlotSaysWhatItIs() {
        assertEquals("Copy", BarSlot.of(BarAction.COPY).label());
        assertEquals("rk", BarSlot.text("ReteKey", "rk").label());
        assertEquals("ReteKey", BarSlot.text("ReteKey", null).label());
        assertEquals("a very long…", BarSlot.text("a very long piece of text", null).label());
        assertEquals("Ctrl+B",
            BarSlot.chord(RawKey.B, Collections.singleton(KeyModifier.CTRL), null).label());
        assertEquals("Esc", BarSlot.chord(RawKey.ESCAPE,
            Collections.<KeyModifier>emptySet(), null).label());
    }

    @Test
    public void onlyAChordCanBeHeldDown() {
        // Holding text repeats it; holding a built-in action does it once. A chord is the only slot
        // that can be pressed and left down.
        assertTrue(BarSlot.chord(RawKey.ESCAPE, Collections.<KeyModifier>emptySet(), null)
            .canLatch());
        assertFalse(BarSlot.text("x", null).canLatch());
        assertFalse(BarSlot.of(BarAction.COPY).canLatch());
    }

    @Test
    public void theBarIsOffUntilItIsAskedFor() {
        assertFalse(ActionBarSlots.DEFAULT_ENABLED);
    }
}
