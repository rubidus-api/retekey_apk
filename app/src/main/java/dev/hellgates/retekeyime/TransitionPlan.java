package dev.hellgates.retekeyime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TransitionPlan<S> {
    private final long generation;
    private final long baseRevision;
    private final DispatchResult.Disposition disposition;
    private final S proposedState;
    private final EditorBounds expectedBounds;
    private final List<KeyAction> actions;

    private TransitionPlan(
        long generation,
        long baseRevision,
        DispatchResult.Disposition disposition,
        S proposedState,
        EditorBounds expectedBounds,
        List<KeyAction> actions
    ) {
        if (generation < 1) {
            throw new IllegalArgumentException("generation must be positive");
        }
        if (baseRevision < 0) {
            throw new IllegalArgumentException("baseRevision must not be negative");
        }
        if (disposition == null) {
            throw new IllegalArgumentException("disposition must not be null");
        }
        if (proposedState == null) {
            throw new IllegalArgumentException("proposedState must not be null");
        }
        if (expectedBounds == null) {
            throw new IllegalArgumentException("expectedBounds must not be null");
        }
        if (actions == null) {
            throw new IllegalArgumentException("actions must not be null");
        }
        ArrayList<KeyAction> copy = new ArrayList<>(actions);
        if (copy.contains(null)) {
            throw new IllegalArgumentException("actions must not contain null");
        }
        for (int index = 0; index + 1 < copy.size(); index++) {
            if (copy.get(index).isTerminal()) {
                throw new IllegalArgumentException("terminal editor action must be last");
            }
        }
        this.generation = generation;
        this.baseRevision = baseRevision;
        this.disposition = disposition;
        this.proposedState = proposedState;
        this.expectedBounds = expectedBounds;
        this.actions = Collections.unmodifiableList(copy);
    }

    public static <S> TransitionPlan<S> of(
        long generation,
        long baseRevision,
        DispatchResult.Disposition disposition,
        S proposedState,
        EditorBounds expectedBounds,
        List<KeyAction> actions
    ) {
        return new TransitionPlan<>(
            generation,
            baseRevision,
            disposition,
            proposedState,
            expectedBounds,
            actions
        );
    }

    public long generation() {
        return generation;
    }

    public DispatchResult.Disposition disposition() {
        return disposition;
    }

    public long baseRevision() {
        return baseRevision;
    }

    public S proposedState() {
        return proposedState;
    }

    /** The cursor the IME optimistically expects after this plan; a hint, never a contract. */
    public EditorBounds expectedBounds() {
        return expectedBounds;
    }

    public List<KeyAction> actions() {
        return actions;
    }

    @Override
    public String toString() {
        return "TransitionPlan{"
            + "generation=" + generation
            + ", baseRevision=" + baseRevision
            + ", disposition=" + disposition
            + ", expectedBounds=" + expectedBounds
            + ", actionCount=" + actions.size()
            + '}';
    }
}
