package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** The bar's slot list: stored as words, repaired rather than trusted. */
public final class ActionBarSlotsTest {
    @Test
    public void aStoredBarRoundTrips() {
        List<BarAction> bar = Arrays.asList(BarAction.PASTE, BarAction.SELECT_WORD, BarAction.END);
        assertEquals(bar, ActionBarSlots.parse(ActionBarSlots.format(bar)));
        assertEquals("paste,word,end", ActionBarSlots.format(bar));
    }

    @Test
    public void anActionThisBuildDoesNotKnowIsDropped() {
        // A bar written by a later build, opened by this one: it loses what it cannot do.
        assertEquals(Arrays.asList(BarAction.COPY, BarAction.PASTE),
            ActionBarSlots.parse("copy,macro:3,paste"));
    }

    @Test
    public void repeatsCollapseAndBlanksAreIgnored() {
        assertEquals(Arrays.asList(BarAction.CUT, BarAction.COPY),
            ActionBarSlots.parse(" cut , copy ,cut,, "));
    }

    @Test
    public void anUnreadableBarFallsBackToTheDefault() {
        assertEquals(ActionBarSlots.DEFAULT, ActionBarSlots.parse(null));
        assertEquals(ActionBarSlots.DEFAULT, ActionBarSlots.parse(""));
        assertEquals(ActionBarSlots.DEFAULT, ActionBarSlots.parse("nothing,whatever"));
    }

    @Test
    public void theBarIsOffUntilItIsAskedFor() {
        assertFalse(ActionBarSlots.DEFAULT_ENABLED);
    }

    @Test
    public void everyActionHasAStoredWordAndALabel() {
        for (BarAction action : BarAction.values()) {
            assertEquals(action, BarAction.parse(action.stored()));
            assertFalse(action.name(), action.label().isEmpty());
        }
    }

    @Test
    public void movingASlotIsTheSameWhicheverWayItIsMoved() {
        // The arrows and the drag share this, so a slot cannot land somewhere different depending
        // on how it was picked up.
        List<BarAction> bar = Arrays.asList(
            BarAction.SELECT_WORD, BarAction.COPY, BarAction.PASTE, BarAction.LEFT);
        assertEquals(Arrays.asList(
            BarAction.COPY, BarAction.SELECT_WORD, BarAction.PASTE, BarAction.LEFT),
            ActionBarSlots.moved(bar, 0, 1));
        assertEquals("dragged to the end",
            Arrays.asList(BarAction.COPY, BarAction.PASTE, BarAction.LEFT, BarAction.SELECT_WORD),
            ActionBarSlots.moved(bar, 0, 3));
        assertEquals("dragged to the front",
            Arrays.asList(BarAction.LEFT, BarAction.SELECT_WORD, BarAction.COPY, BarAction.PASTE),
            ActionBarSlots.moved(bar, 3, 0));
    }

    @Test
    public void anImpossibleMoveLeavesTheBarAlone() {
        List<BarAction> bar = Arrays.asList(BarAction.COPY, BarAction.PASTE);
        assertSame(bar, ActionBarSlots.moved(bar, 1, 1));
        assertSame("a slot that is not on the bar", bar, ActionBarSlots.moved(bar, -1, 0));
        assertSame("past the end", bar, ActionBarSlots.moved(bar, 0, 2));
    }

    @Test
    public void thereIsAnOrderToGoBackTo() {
        // What "Default order" puts back. It has to be a real bar, not an empty one.
        assertFalse(ActionBarSlots.DEFAULT.isEmpty());
        assertEquals(ActionBarSlots.DEFAULT,
            ActionBarSlots.parse(ActionBarSlots.format(ActionBarSlots.DEFAULT)));
    }
}
