package com.retekey;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The rule that ends a composition when the user moves the cursor away from it. The positive
 * cases are taps and clicks landing outside the editor-reported composing region; everything our
 * own edits produce — cursor at the span's end, reports without a span — must leave the
 * composition alone, or typing itself would reset the composer (the v0.1.11 thrash).
 */
public final class CursorMovePolicyTest {

    @Test
    public void aTapOutsideTheComposingRegionAbandonsIt() {
        // Composing chars 5..6, cursor jumps to 0.
        assertTrue(CursorMovePolicy.shouldAbandonComposition(true, 0, 0, 5, 6));
        // And to far after it.
        assertTrue(CursorMovePolicy.shouldAbandonComposition(true, 20, 20, 5, 6));
        // A selection dragged across other text counts too.
        assertTrue(CursorMovePolicy.shouldAbandonComposition(true, 0, 3, 5, 6));
        // A selection reaching out of the region counts even when it touches it.
        assertTrue(CursorMovePolicy.shouldAbandonComposition(true, 5, 9, 5, 6));
    }

    @Test
    public void ourOwnEditsNeverAbandon() {
        // setComposingText leaves the cursor at the end of the span.
        assertFalse(CursorMovePolicy.shouldAbandonComposition(true, 6, 6, 5, 6));
        // A commit's intermediate report carries no composing region.
        assertFalse(CursorMovePolicy.shouldAbandonComposition(true, 6, 6, -1, -1));
    }

    @Test
    public void nothingComposingMeansNothingToAbandon() {
        assertFalse(CursorMovePolicy.shouldAbandonComposition(false, 0, 0, 5, 6));
    }

    @Test
    public void editorsThatCannotReportAreLeftAlone() {
        // Unknown selection (terminals): no verdict.
        assertFalse(CursorMovePolicy.shouldAbandonComposition(true, -1, -1, 5, 6));
        // No composing region reported while composing: resetting here would break editors that
        // never report one, one keystroke at a time.
        assertFalse(CursorMovePolicy.shouldAbandonComposition(true, 0, 0, -1, -1));
        // A malformed region is no region.
        assertFalse(CursorMovePolicy.shouldAbandonComposition(true, 0, 0, 6, 5));
    }

    @Test
    public void withoutARegionTheTextBeforeTheCursorDecides() {
        // Our own edit: the cursor sits right after the preedit.
        assertFalse(CursorMovePolicy.shouldAbandonWithoutRegion(true, 6, 6, "간", "간"));
        // A tap elsewhere: something else sits before the cursor.
        assertTrue(CursorMovePolicy.shouldAbandonWithoutRegion(true, 2, 2, "간", "다"));
        // A tap to the start of the document: nothing sits before the cursor.
        assertTrue(CursorMovePolicy.shouldAbandonWithoutRegion(true, 0, 0, "간", ""));
        // A range selection is never something composing produces.
        assertTrue(CursorMovePolicy.shouldAbandonWithoutRegion(true, 2, 5, "간", "간"));
    }

    @Test
    public void withoutARegionUncertaintyLeavesTheCompositionAlone() {
        // The editor cannot say what is before the cursor.
        assertFalse(CursorMovePolicy.shouldAbandonWithoutRegion(true, 6, 6, "간", null));
        // Unknown selection.
        assertFalse(CursorMovePolicy.shouldAbandonWithoutRegion(true, -1, -1, "간", "다"));
        // Nothing composing, or no preedit to compare.
        assertFalse(CursorMovePolicy.shouldAbandonWithoutRegion(false, 2, 2, "간", "다"));
        assertFalse(CursorMovePolicy.shouldAbandonWithoutRegion(true, 2, 2, "", "다"));
    }

    @Test
    public void aCursorInsideTheRegionKeepsComposing() {
        // Tapping into the middle of one's own preedit continues it rather than settling it.
        assertFalse(CursorMovePolicy.shouldAbandonComposition(true, 5, 5, 5, 6));
        assertFalse(CursorMovePolicy.shouldAbandonComposition(true, 5, 6, 5, 6));
    }
}
