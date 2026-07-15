package dev.hellgates.retekeyime;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public final class InputSessionController<S> {
    private static final int DEFAULT_LEDGER_CAPACITY = 32;

    private final int ledgerCapacity;
    private final CheckedEditorExecutor executor;
    private final Deque<DeferredSelection> deferredSelections = new ArrayDeque<>();

    private SelectionExpectationLedger ledger;
    private long generation;
    private long revision;
    private boolean accepting;
    private boolean executing;
    private boolean deferredOverflow;
    private EditorBounds confirmedBounds = EditorBounds.unknown();
    private EditorBounds workingBounds = EditorBounds.unknown();
    private EditorCapabilities capabilities = EditorCapabilities.unsupported();
    private SynchronizationState syncState = SynchronizationState.STOPPED;
    private S neutralState;
    private S currentState;

    public InputSessionController() {
        this(DEFAULT_LEDGER_CAPACITY);
    }

    public InputSessionController(int ledgerCapacity) {
        if (ledgerCapacity < 1 || ledgerCapacity > 64) {
            throw new IllegalArgumentException("ledgerCapacity must be between 1 and 64");
        }
        this.ledgerCapacity = ledgerCapacity;
        this.executor = new CheckedEditorExecutor();
        this.ledger = new SelectionExpectationLedger(ledgerCapacity);
    }

    public long start(
        S initialState,
        EditorBounds initialBounds,
        EditorCapabilities capabilities
    ) {
        if (generation == Long.MAX_VALUE) {
            throw new IllegalStateException("session generation exhausted");
        }
        this.generation++;
        this.revision = 0;
        this.accepting = true;
        this.executing = false;
        this.deferredOverflow = false;
        this.deferredSelections.clear();
        this.ledger = new SelectionExpectationLedger(ledgerCapacity);
        this.neutralState = Objects.requireNonNull(initialState, "initialState");
        this.currentState = initialState;
        this.confirmedBounds = Objects.requireNonNull(initialBounds, "initialBounds");
        this.workingBounds = initialBounds;
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        if (!capabilities.isSupported()) {
            this.syncState = SynchronizationState.UNSUPPORTED;
        } else if (!initialBounds.hasSelection()) {
            this.syncState = SynchronizationState.WAITING_FOR_BOUNDS;
        } else {
            this.syncState = SynchronizationState.SYNCED;
        }
        return generation;
    }

    public TransitionPlan<S> plan(
        DispatchResult dispatchResult,
        S proposedState,
        EditorBounds expectedBounds
    ) {
        requireStarted();
        Objects.requireNonNull(dispatchResult, "dispatchResult");
        return TransitionPlan.of(
            generation,
            revision,
            dispatchResult.disposition(),
            proposedState,
            expectedBounds,
            dispatchResult.actions()
        );
    }

    public TransitionPlan<S> planWithExpectation(
        DispatchResult dispatchResult,
        S proposedState,
        EditorExpectation expectation
    ) {
        requireStarted();
        Objects.requireNonNull(dispatchResult, "dispatchResult");
        return TransitionPlan.withExpectation(
            generation,
            revision,
            dispatchResult.disposition(),
            proposedState,
            expectation,
            dispatchResult.actions()
        );
    }

    public ExecutionResult execute(
        TransitionPlan<S> plan,
        EditorEndpointProvider endpointProvider
    ) {
        requireStarted();
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(endpointProvider, "endpointProvider");

        if (executing) {
            return localFailure(
                plan,
                ExecutionResult.Reason.REENTRANT_EXECUTION,
                ExecutionResult.StateEffect.KEEP_CURRENT
            );
        }

        if (plan.generation() != generation || plan.baseRevision() != revision) {
            return executor.execute(plan, contextFor(), endpointProvider);
        }
        if (!accepting) {
            return executor.execute(plan, contextFor(), endpointProvider);
        }

        long consumedRevision = revision;
        revision = incrementRevision(revision);
        long ownedGeneration = generation;
        long ownedRevision = revision;
        boolean boundsWereConfirmed = ledger.pendingCount() == 0
            && workingBounds.equals(confirmedBounds);

        if (syncState == SynchronizationState.DESYNCHRONIZED) {
            return localFailure(
                plan,
                ExecutionResult.Reason.SESSION_DESYNCHRONIZED,
                ExecutionResult.StateEffect.KEEP_CURRENT
            );
        }
        // A raw-key (TYPE_NULL terminal) editor never reports a selection to confirm against, so
        // its operations are fire-and-forget: they must not reserve ledger entries that could never
        // be retired, or the session would drift into AWAITING_CONFIRMATION and then desynchronize.
        boolean statelessEditor =
            capabilities.deletionMode() == EditorCapabilities.DeletionMode.RAW_KEY;
        boolean reserved = !plan.actions().isEmpty() && !statelessEditor;
        if (reserved && !ledger.hasCapacity()) {
            desynchronize();
            return localFailure(
                plan,
                ExecutionResult.Reason.LEDGER_OVERFLOW,
                ExecutionResult.StateEffect.RESET_DESYNCHRONIZED
            );
        }
        if (reserved) {
            ledger.reserve(consumedRevision, plan.expectation());
        }

        ExecutionResult result;
        executing = true;
        try {
            EditorEndpointProvider guardedProvider = () -> {
                EditorEndpoint endpoint = endpointProvider.resolve();
                return endpoint == null
                    ? null
                    : endpoint.guardedBy(
                        () -> ownsExecution(ownedGeneration, ownedRevision)
                    );
            };
            result = executor.execute(
                plan,
                ExecutionContext.active(
                    generation,
                    consumedRevision,
                    workingBounds,
                    capabilities,
                    boundsWereConfirmed
                ),
                guardedProvider
            );
        } finally {
            executing = false;
        }

        if (generation != ownedGeneration || revision != ownedRevision || !accepting) {
            return sessionChangedResult(plan, result);
        }

        applyExecutionResult(plan, consumedRevision, reserved, result);
        if (deferredOverflow) {
            desynchronize();
            deferredSelections.clear();
            deferredOverflow = false;
            return deferredOverflowResult(plan, result);
        }
        drainDeferredSelections(plan, result);
        return result;
    }

    public SelectionReconcileResult updateSelection(
        long updateGeneration,
        EditorBounds observedBounds
    ) {
        requireStarted();
        Objects.requireNonNull(observedBounds, "observedBounds");
        if (updateGeneration != generation) {
            return SelectionReconcileResult.STALE_GENERATION;
        }
        if (!accepting) {
            return SelectionReconcileResult.STOPPED;
        }
        if (executing) {
            if (deferredSelections.size() >= ledgerCapacity) {
                deferredOverflow = true;
                return SelectionReconcileResult.DEFERRED_OVERFLOW;
            }
            deferredSelections.addLast(new DeferredSelection(updateGeneration, observedBounds));
            return SelectionReconcileResult.DEFERRED;
        }
        return reconcileNow(updateGeneration, observedBounds);
    }

    public void stopAccepting() {
        if (generation == 0) {
            return;
        }
        accepting = false;
        executing = false;
        deferredOverflow = false;
        deferredSelections.clear();
        ledger.clear();
        syncState = SynchronizationState.STOPPED;
    }

    public void finish() {
        stopAccepting();
        currentState = neutralState;
        confirmedBounds = EditorBounds.unknown();
        workingBounds = EditorBounds.unknown();
        capabilities = EditorCapabilities.unsupported();
    }

    public long generation() {
        return generation;
    }

    public long revision() {
        return revision;
    }

    public S currentState() {
        requireStarted();
        return currentState;
    }

    public SynchronizationState syncState() {
        return syncState;
    }

    public int pendingExpectationCount() {
        return ledger.pendingCount();
    }

    public EditorBounds workingBounds() {
        return workingBounds;
    }

    private ExecutionContext contextFor() {
        return accepting
            ? ExecutionContext.active(generation, revision, workingBounds, capabilities)
            : ExecutionContext.stopped(generation, revision, workingBounds, capabilities);
    }

    private void applyExecutionResult(
        TransitionPlan<S> plan,
        long reservedRevision,
        boolean reserved,
        ExecutionResult result
    ) {
        switch (result.stateEffect()) {
            case ADOPT_PROPOSED_SYNCED:
                currentState = plan.proposedState();
                if (plan.expectation().workingBounds().hasSelection()) {
                    workingBounds = plan.expectation().workingBounds();
                }
                if (reserved) {
                    ledger.cancel(reservedRevision);
                }
                syncState = stateAfterNoEditorMutation();
                break;
            case ADOPT_PROPOSED_AWAITING_CONFIRMATION:
                currentState = plan.proposedState();
                workingBounds = plan.expectation().workingBounds();
                if (capabilities.deletionMode() == EditorCapabilities.DeletionMode.RAW_KEY) {
                    // Terminal-style editors never confirm, so settle into a clean neutral state
                    // (WAITING_FOR_BOUNDS) instead of awaiting a confirmation that never comes.
                    syncState = stateAfterNoEditorMutation();
                } else {
                    syncState = SynchronizationState.AWAITING_CONFIRMATION;
                }
                break;
            case KEEP_CURRENT:
                if (reserved) {
                    ledger.cancel(reservedRevision);
                }
                break;
            case RESET_DESYNCHRONIZED:
                desynchronize();
                break;
            default:
                throw new IllegalStateException("unsupported execution state effect");
        }
    }

    private void drainDeferredSelections(
        TransitionPlan<S> plan,
        ExecutionResult result
    ) {
        if (result.stateEffect()
            == ExecutionResult.StateEffect.ADOPT_PROPOSED_AWAITING_CONFIRMATION) {
            int index = 0;
            int finalMatchIndex = -1;
            for (DeferredSelection deferred : deferredSelections) {
                if (deferred.generation == generation
                    && plan.expectation().matches(deferred.bounds)) {
                    finalMatchIndex = index;
                }
                index++;
            }
            for (int discarded = 0; discarded < finalMatchIndex; discarded++) {
                deferredSelections.removeFirst();
            }
        }
        while (!deferredSelections.isEmpty()) {
            DeferredSelection deferred = deferredSelections.removeFirst();
            reconcileNow(deferred.generation, deferred.bounds);
        }
    }

    private SelectionReconcileResult reconcileNow(
        long updateGeneration,
        EditorBounds observedBounds
    ) {
        if (updateGeneration != generation) {
            return SelectionReconcileResult.STALE_GENERATION;
        }
        if (syncState == SynchronizationState.DESYNCHRONIZED) {
            return SelectionReconcileResult.DESYNCHRONIZED;
        }
        if (syncState == SynchronizationState.UNSUPPORTED) {
            return SelectionReconcileResult.UNSUPPORTED;
        }
        if (syncState == SynchronizationState.WAITING_FOR_BOUNDS) {
            if (!observedBounds.hasSelection()) {
                return SelectionReconcileResult.WAITING_FOR_BOUNDS;
            }
            confirmedBounds = observedBounds;
            workingBounds = observedBounds;
            syncState = SynchronizationState.SYNCED;
            return SelectionReconcileResult.EXTERNAL_MOVEMENT;
        }
        if (!observedBounds.hasSelection()) {
            desynchronize();
            return SelectionReconcileResult.CONTRADICTION;
        }
        if (ledger.pendingCount() > 0) {
            if (observedBounds.equals(confirmedBounds)) {
                return SelectionReconcileResult.DUPLICATE_OR_DELAYED;
            }
            SelectionReconcileResult result = ledger.reconcile(observedBounds);
            if (result == SelectionReconcileResult.CONTRADICTION) {
                desynchronize();
                return result;
            }
            if (result == SelectionReconcileResult.MATCHED
                || result == SelectionReconcileResult.COALESCED) {
                confirmedBounds = observedBounds;
                if (ledger.pendingCount() == 0) {
                    workingBounds = observedBounds;
                }
                syncState = ledger.pendingCount() == 0
                    ? SynchronizationState.SYNCED
                    : SynchronizationState.AWAITING_CONFIRMATION;
            } else if (result == SelectionReconcileResult.INTERMEDIATE) {
                syncState = SynchronizationState.AWAITING_CONFIRMATION;
            }
            return result;
        }
        if (observedBounds.equals(confirmedBounds) || ledger.isKnown(observedBounds)) {
            return SelectionReconcileResult.DUPLICATE_OR_DELAYED;
        }
        confirmedBounds = observedBounds;
        workingBounds = observedBounds;
        currentState = neutralState;
        ledger.clear();
        syncState = SynchronizationState.SYNCED;
        return SelectionReconcileResult.EXTERNAL_MOVEMENT;
    }

    private void desynchronize() {
        currentState = neutralState;
        workingBounds = EditorBounds.unknown();
        ledger.clear();
        syncState = SynchronizationState.DESYNCHRONIZED;
    }

    private SynchronizationState stateAfterNoEditorMutation() {
        if (!capabilities.isSupported()) {
            return SynchronizationState.UNSUPPORTED;
        }
        if (!workingBounds.hasSelection()) {
            return SynchronizationState.WAITING_FOR_BOUNDS;
        }
        return ledger.pendingCount() == 0
            ? SynchronizationState.SYNCED
            : SynchronizationState.AWAITING_CONFIRMATION;
    }

    private ExecutionResult localFailure(
        TransitionPlan<S> plan,
        ExecutionResult.Reason reason,
        ExecutionResult.StateEffect stateEffect
    ) {
        return new ExecutionResult(
            ExecutionResult.Outcome.NOT_DISPATCHED,
            reason,
            ExecutionResult.Reason.NONE,
            stateEffect,
            plan,
            -1,
            -1,
            -1,
            0,
            null,
            0,
            false
        ).withStateBounds(workingBounds);
    }

    private ExecutionResult deferredOverflowResult(
        TransitionPlan<S> plan,
        ExecutionResult original
    ) {
        boolean remoteMayHaveOccurred = original.remoteMutationMayHaveOccurred()
            || original.dispatchedMutationCount() > 0;
        return new ExecutionResult(
            remoteMayHaveOccurred
                ? ExecutionResult.Outcome.UNCERTAIN
                : ExecutionResult.Outcome.NOT_DISPATCHED,
            ExecutionResult.Reason.LEDGER_OVERFLOW,
            original.cleanupReason(),
            ExecutionResult.StateEffect.RESET_DESYNCHRONIZED,
            plan,
            original.failedActionIndex(),
            original.failedOperationIndex(),
            original.cleanupOperationIndex(),
            original.operationCount(),
            original.failedActionKind(),
            original.dispatchedMutationCount(),
            remoteMayHaveOccurred
        ).withStateBounds(original.stateBounds());
    }

    private ExecutionResult sessionChangedResult(
        TransitionPlan<S> plan,
        ExecutionResult original
    ) {
        boolean remoteMayHaveOccurred = original.remoteMutationMayHaveOccurred()
            || original.dispatchedMutationCount() > 0;
        return new ExecutionResult(
            remoteMayHaveOccurred
                ? ExecutionResult.Outcome.UNCERTAIN
                : ExecutionResult.Outcome.NOT_DISPATCHED,
            ExecutionResult.Reason.SESSION_CHANGED_DURING_EXECUTION,
            original.cleanupReason(),
            ExecutionResult.StateEffect.KEEP_CURRENT,
            plan,
            original.failedActionIndex(),
            original.failedOperationIndex(),
            original.cleanupOperationIndex(),
            original.operationCount(),
            original.failedActionKind(),
            original.dispatchedMutationCount(),
            remoteMayHaveOccurred
        ).withStateBounds(original.stateBounds());
    }

    private static long incrementRevision(long value) {
        if (value == Long.MAX_VALUE) {
            throw new IllegalStateException("session revision exhausted");
        }
        return value + 1;
    }

    private void requireStarted() {
        if (generation == 0) {
            throw new IllegalStateException("input session has not started");
        }
    }

    private boolean ownsExecution(long expectedGeneration, long expectedRevision) {
        return executing
            && accepting
            && generation == expectedGeneration
            && revision == expectedRevision;
    }

    @Override
    public String toString() {
        return "InputSessionController{"
            + "generation=" + generation
            + ", revision=" + revision
            + ", accepting=" + accepting
            + ", syncState=" + syncState
            + ", confirmedBounds=" + confirmedBounds
            + ", pendingExpectationCount=" + ledger.pendingCount()
            + '}';
    }

    private static final class DeferredSelection {
        private final long generation;
        private final EditorBounds bounds;

        private DeferredSelection(long generation, EditorBounds bounds) {
            this.generation = generation;
            this.bounds = bounds;
        }
    }
}
