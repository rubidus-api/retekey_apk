package com.retekey;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * What a terminal or remote-desktop composer may keep believing after a plan (issue #7).
 */
public final class MaterializedCompositionPolicyTest {
    @Test
    public void aCleanDispatchLeavesTheRecordStanding() {
        assertFalse(MaterializedCompositionPolicy.shouldForgetWhatWasWritten(
            true, ExecutionResult.Outcome.DISPATCHED));
    }

    @Test
    public void aPlanThatAskedTheEditorForNothingChangesNothing() {
        // Forgetting here would make the next syllable commit itself beside the one on screen.
        assertFalse(MaterializedCompositionPolicy.shouldForgetWhatWasWritten(
            true, ExecutionResult.Outcome.NO_EDITOR_ACTIONS));
    }

    @Test
    public void anythingElseMeansTheScreenDidNotMove() {
        for (ExecutionResult.Outcome outcome : new ExecutionResult.Outcome[] {
            ExecutionResult.Outcome.NOT_DISPATCHED,
            ExecutionResult.Outcome.UNCERTAIN,
            ExecutionResult.Outcome.CONFIRMED_NO_EFFECT}) {
            assertTrue(outcome.toString(),
                MaterializedCompositionPolicy.shouldForgetWhatWasWritten(true, outcome));
        }
        assertTrue("nothing ran at all",
            MaterializedCompositionPolicy.shouldForgetWhatWasWritten(true, null));
    }

    @Test
    public void anOrdinaryEditorKeepsItsOwnComposingRegionAndNeedsNoneOfThis() {
        assertFalse(MaterializedCompositionPolicy.shouldForgetWhatWasWritten(
            false, ExecutionResult.Outcome.NOT_DISPATCHED));
        assertFalse(MaterializedCompositionPolicy.shouldForgetWhatWasWritten(false, null));
    }
}
