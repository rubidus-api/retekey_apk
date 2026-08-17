package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
}
