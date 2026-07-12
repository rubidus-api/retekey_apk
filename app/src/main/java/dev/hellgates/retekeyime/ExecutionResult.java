package dev.hellgates.retekeyime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ExecutionResult {
    public enum Outcome {
        NO_EDITOR_ACTIONS,
        DISPATCHED,
        CONFIRMED_NO_EFFECT,
        NOT_DISPATCHED,
        UNCERTAIN
    }

    public enum Reason {
        NONE,
        SESSION_STOPPED,
        SESSION_DESYNCHRONIZED,
        REENTRANT_EXECUTION,
        SESSION_CHANGED_DURING_EXECUTION,
        STALE_GENERATION,
        STALE_REVISION,
        UNSUPPORTED_EDITOR,
        INVALID_SELECTION,
        INVALID_EXPECTED_BOUNDS,
        SENSITIVE_OPERATION_PROHIBITED,
        NO_CONNECTION,
        CONNECTION_RESOLUTION_RUNTIME_FAILURE,
        ENDPOINT_GENERATION_MISMATCH,
        BATCH_BEGIN_FALSE,
        BATCH_BEGIN_RUNTIME_FAILURE,
        OPERATION_FALSE,
        OPERATION_RUNTIME_FAILURE,
        BATCH_END_FALSE,
        BATCH_END_RUNTIME_FAILURE,
        INVALID_SURROUNDING_TEXT,
        LEDGER_OVERFLOW
    }

    public enum StateEffect {
        ADOPT_PROPOSED_SYNCED,
        ADOPT_PROPOSED_AWAITING_CONFIRMATION,
        KEEP_CURRENT,
        RESET_DESYNCHRONIZED
    }

    private final Outcome outcome;
    private final Reason reason;
    private final Reason cleanupReason;
    private final StateEffect stateEffect;
    private final long generation;
    private final long baseRevision;
    private final EditorExpectation expectation;
    private final EditorBounds stateBounds;
    private final List<KeyAction.Kind> actionKinds;
    private final int failedActionIndex;
    private final int failedOperationIndex;
    private final int cleanupOperationIndex;
    private final int operationCount;
    private final KeyAction.Kind failedActionKind;
    private final int dispatchedMutationCount;
    private final boolean remoteMutationMayHaveOccurred;

    ExecutionResult(
        Outcome outcome,
        Reason reason,
        Reason cleanupReason,
        StateEffect stateEffect,
        TransitionPlan<?> plan,
        int failedActionIndex,
        int failedOperationIndex,
        int cleanupOperationIndex,
        int operationCount,
        KeyAction.Kind failedActionKind,
        int dispatchedMutationCount,
        boolean remoteMutationMayHaveOccurred
    ) {
        this.outcome = outcome;
        this.reason = reason;
        this.cleanupReason = cleanupReason;
        this.stateEffect = stateEffect;
        this.generation = plan.generation();
        this.baseRevision = plan.baseRevision();
        this.expectation = plan.expectation();
        this.stateBounds = EditorBounds.unknown();
        ArrayList<KeyAction.Kind> kinds = new ArrayList<>();
        for (KeyAction action : plan.actions()) {
            kinds.add(action.kind());
        }
        this.actionKinds = Collections.unmodifiableList(kinds);
        this.failedActionIndex = failedActionIndex;
        this.failedOperationIndex = failedOperationIndex;
        this.cleanupOperationIndex = cleanupOperationIndex;
        this.operationCount = operationCount;
        this.failedActionKind = failedActionKind;
        this.dispatchedMutationCount = dispatchedMutationCount;
        this.remoteMutationMayHaveOccurred = remoteMutationMayHaveOccurred;
    }

    private ExecutionResult(ExecutionResult source, EditorBounds stateBounds) {
        this.outcome = source.outcome;
        this.reason = source.reason;
        this.cleanupReason = source.cleanupReason;
        this.stateEffect = source.stateEffect;
        this.generation = source.generation;
        this.baseRevision = source.baseRevision;
        this.expectation = source.expectation;
        this.stateBounds = stateBounds;
        this.actionKinds = source.actionKinds;
        this.failedActionIndex = source.failedActionIndex;
        this.failedOperationIndex = source.failedOperationIndex;
        this.cleanupOperationIndex = source.cleanupOperationIndex;
        this.operationCount = source.operationCount;
        this.failedActionKind = source.failedActionKind;
        this.dispatchedMutationCount = source.dispatchedMutationCount;
        this.remoteMutationMayHaveOccurred = source.remoteMutationMayHaveOccurred;
    }

    ExecutionResult withStateBounds(EditorBounds stateBounds) {
        return new ExecutionResult(this, stateBounds);
    }

    public Outcome outcome() {
        return outcome;
    }

    public Reason reason() {
        return reason;
    }

    public Reason cleanupReason() {
        return cleanupReason;
    }

    public StateEffect stateEffect() {
        return stateEffect;
    }

    public long generation() {
        return generation;
    }

    public long baseRevision() {
        return baseRevision;
    }

    public EditorExpectation expectation() {
        return expectation;
    }

    public EditorBounds stateBounds() {
        return stateBounds;
    }

    public List<KeyAction.Kind> actionKinds() {
        return actionKinds;
    }

    public int failedActionIndex() {
        return failedActionIndex;
    }

    public int failedOperationIndex() {
        return failedOperationIndex;
    }

    public int cleanupOperationIndex() {
        return cleanupOperationIndex;
    }

    public int operationCount() {
        return operationCount;
    }

    public KeyAction.Kind failedActionKind() {
        return failedActionKind;
    }

    public int dispatchedMutationCount() {
        return dispatchedMutationCount;
    }

    public boolean remoteMutationMayHaveOccurred() {
        return remoteMutationMayHaveOccurred;
    }

    public boolean isFailure() {
        return outcome == Outcome.NOT_DISPATCHED || outcome == Outcome.UNCERTAIN;
    }

    @Override
    public String toString() {
        return "ExecutionResult{"
            + "outcome=" + outcome
            + ", reason=" + reason
            + ", cleanupReason=" + cleanupReason
            + ", stateEffect=" + stateEffect
            + ", generation=" + generation
            + ", baseRevision=" + baseRevision
            + ", expectation=" + expectation
            + ", stateBounds=" + stateBounds
            + ", actionKinds=" + actionKinds
            + ", failedActionIndex=" + failedActionIndex
            + ", failedOperationIndex=" + failedOperationIndex
            + ", cleanupOperationIndex=" + cleanupOperationIndex
            + ", operationCount=" + operationCount
            + ", failedActionKind=" + failedActionKind
            + ", dispatchedMutationCount=" + dispatchedMutationCount
            + ", remoteMutationMayHaveOccurred=" + remoteMutationMayHaveOccurred
            + '}';
    }
}
