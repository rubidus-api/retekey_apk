package dev.hellgates.retekeyime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.Assert;
import org.junit.Test;

public final class TransitionPlanTest {
    @Test
    public void preservesGenerationDispositionStateBoundsAndOrderedActions() {
        TestState proposed = new TestState(2);
        EditorBounds expected = EditorBounds.of(3, 3, 1, 3);
        List<KeyAction> source = new ArrayList<>(Arrays.asList(
            KeyAction.commitText("before"),
            KeyAction.setComposingText("after")
        ));

        TransitionPlan<TestState> plan = TransitionPlan.of(
            7,
            11,
            DispatchResult.Disposition.HANDLED,
            proposed,
            expected,
            source
        );
        source.clear();

        Assert.assertEquals(7, plan.generation());
        Assert.assertEquals(11, plan.baseRevision());
        Assert.assertEquals(DispatchResult.Disposition.HANDLED, plan.disposition());
        Assert.assertEquals(proposed, plan.proposedState());
        Assert.assertEquals(expected, plan.expectedBounds());
        Assert.assertEquals(Arrays.asList(
            KeyAction.commitText("before"),
            KeyAction.setComposingText("after")
        ), plan.actions());
        Assert.assertThrows(
            UnsupportedOperationException.class,
            () -> plan.actions().add(KeyAction.finishComposing())
        );
    }

    @Test
    public void permitsDelegateWithActionsAndHandledWithoutActions() {
        TestState state = new TestState(1);

        TransitionPlan<TestState> delegated = TransitionPlan.of(
            1,
            0,
            DispatchResult.Disposition.DELEGATE,
            state,
            EditorBounds.unknown(),
            Arrays.asList(KeyAction.finishComposing())
        );
        TransitionPlan<TestState> handled = TransitionPlan.of(
            1,
            0,
            DispatchResult.Disposition.HANDLED,
            state,
            EditorBounds.unknown(),
            Arrays.asList()
        );

        Assert.assertEquals(DispatchResult.Disposition.DELEGATE, delegated.disposition());
        Assert.assertEquals(1, delegated.actions().size());
        Assert.assertEquals(DispatchResult.Disposition.HANDLED, handled.disposition());
        Assert.assertTrue(handled.actions().isEmpty());
    }

    @Test
    public void rejectsInvalidGenerationAndNullContractParts() {
        TestState state = new TestState(1);
        EditorBounds bounds = EditorBounds.unknown();

        Assert.assertThrows(
            IllegalArgumentException.class,
            () -> TransitionPlan.of(0, 0, DispatchResult.Disposition.HANDLED, state, bounds, Arrays.asList())
        );
        Assert.assertThrows(
            IllegalArgumentException.class,
            () -> TransitionPlan.of(1, -1, DispatchResult.Disposition.HANDLED, state, bounds, Arrays.asList())
        );
        Assert.assertThrows(
            IllegalArgumentException.class,
            () -> TransitionPlan.of(1, 0, null, state, bounds, Arrays.asList())
        );
        Assert.assertThrows(
            IllegalArgumentException.class,
            () -> TransitionPlan.of(1, 0, DispatchResult.Disposition.HANDLED, null, bounds, Arrays.asList())
        );
        Assert.assertThrows(
            IllegalArgumentException.class,
            () -> TransitionPlan.of(1, 0, DispatchResult.Disposition.HANDLED, state, null, Arrays.asList())
        );
        Assert.assertThrows(
            IllegalArgumentException.class,
            () -> TransitionPlan.of(
                1,
                0,
                DispatchResult.Disposition.HANDLED,
                state,
                bounds,
                Arrays.asList(KeyAction.finishComposing(), null)
            )
        );
    }

    @Test
    public void diagnosticsDoNotExposeStateOrEnteredText() {
        String privateText = "never-log-editor-text";
        TransitionPlan<TestState> plan = TransitionPlan.of(
            4,
            3,
            DispatchResult.Disposition.HANDLED,
            new TestState(9),
            EditorBounds.of(1, 1, -1, -1),
            Arrays.asList(KeyAction.commitText(privateText))
        );

        Assert.assertFalse(plan.toString().contains(privateText));
        Assert.assertFalse(plan.toString().contains("private-state"));
    }

    @Test
    public void rejectsAnyActionAfterFocusChangingAction() {
        Assert.assertThrows(
            IllegalArgumentException.class,
            () -> TransitionPlan.of(
                1,
                0,
                DispatchResult.Disposition.HANDLED,
                new TestState(1),
                EditorBounds.of(0, 0, -1, -1),
                Arrays.asList(
                    KeyAction.performEditorAction(7),
                    KeyAction.commitText("must-not-run")
                )
            )
        );
    }

    private static final class TestState {
        private final int revision;

        private TestState(int revision) {
            this.revision = revision;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof TestState && revision == ((TestState) other).revision;
        }

        @Override
        public int hashCode() {
            return Objects.hash(revision);
        }

        @Override
        public String toString() {
            return "private-state-" + revision;
        }
    }
}
