package dev.hellgates.retekeyime;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * A passive cursor cache in the AOSP LatinIME style. It optimistically records where the cursor
 * should be after each dispatched operation and lets {@link #updateSelection} overwrite that cache
 * with whatever the editor actually reports. The editor is authoritative: the cache is only a hint,
 * never a contract the editor must satisfy, so input is never refused and the session never latches
 * into a dead state. A generation guards against applying a plan built for a previous session.
 */
public final class InputSessionController<S> {
    private static final int DEFAULT_DEFERRED_CAPACITY = 32;

    private final int deferredCapacity;
    private final CheckedEditorExecutor executor;
    private final Deque<DeferredSelection> deferredSelections = new ArrayDeque<>();

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
        this(DEFAULT_DEFERRED_CAPACITY);
    }

    public InputSessionController(int deferredCapacity) {
        if (deferredCapacity < 1 || deferredCapacity > 64) {
            throw new IllegalArgumentException("deferredCapacity must be between 1 and 64");
        }
        this.deferredCapacity = deferredCapacity;
        this.executor = new CheckedEditorExecutor();
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
        boolean boundsWereConfirmed = workingBounds.equals(confirmedBounds);

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

        applyExecutionResult(plan, result);
        if (deferredOverflow) {
            dropToWaiting();
            deferredSelections.clear();
            deferredOverflow = false;
            return deferredOverflowResult(plan, result);
        }
        drainDeferredSelections();
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
            if (deferredSelections.size() >= deferredCapacity) {
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

    /** Always zero: the passive cache keeps no pending expectations. Kept for API compatibility. */
    public int pendingExpectationCount() {
        return 0;
    }

    public EditorBounds workingBounds() {
        return workingBounds;
    }

    private ExecutionContext contextFor() {
        return accepting
            ? ExecutionContext.active(generation, revision, workingBounds, capabilities)
            : ExecutionContext.stopped(generation, revision, workingBounds, capabilities);
    }

    private void applyExecutionResult(TransitionPlan<S> plan, ExecutionResult result) {
        switch (result.stateEffect()) {
            case ADOPT_PROPOSED_SYNCED:
                currentState = plan.proposedState();
                if (plan.expectedBounds().hasSelection()) {
                    workingBounds = plan.expectedBounds();
                }
                syncState = stateAfterNoEditorMutation();
                break;
            case ADOPT_PROPOSED_AWAITING_CONFIRMATION:
                // Optimistically cache the predicted cursor; the editor's next report is the truth.
                currentState = plan.proposedState();
                workingBounds = plan.expectedBounds();
                syncState = stateAfterNoEditorMutation();
                break;
            case KEEP_CURRENT:
                break;
            case RESET_DESYNCHRONIZED:
                // A failed editor call means the cursor is now unknown, not that the session is
                // dead: drop to waiting and keep accepting input.
                dropToWaiting();
                break;
            default:
                throw new IllegalStateException("unsupported execution state effect");
        }
    }

    private void drainDeferredSelections() {
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
        if (syncState == SynchronizationState.UNSUPPORTED) {
            return SelectionReconcileResult.UNSUPPORTED;
        }
        if (!observedBounds.hasSelection()) {
            // An unknown selection (routine in terminals and some webviews) is a waiting condition,
            // never a contradiction: keep the cursor unknown and keep accepting input.
            workingBounds = EditorBounds.unknown();
            syncState = SynchronizationState.WAITING_FOR_BOUNDS;
            return SelectionReconcileResult.WAITING_FOR_BOUNDS;
        }
        if (syncState == SynchronizationState.WAITING_FOR_BOUNDS) {
            confirmedBounds = observedBounds;
            workingBounds = observedBounds;
            syncState = SynchronizationState.SYNCED;
            return SelectionReconcileResult.EXTERNAL_MOVEMENT;
        }
        if (observedBounds.equals(confirmedBounds)) {
            return SelectionReconcileResult.DUPLICATE_OR_DELAYED;
        }
        // The editor is authoritative: adopt whatever it reports.
        confirmedBounds = observedBounds;
        workingBounds = observedBounds;
        currentState = neutralState;
        syncState = SynchronizationState.SYNCED;
        return SelectionReconcileResult.EXTERNAL_MOVEMENT;
    }

    /** Drops the cached cursor to unknown and keeps accepting input; never a latched failure. */
    private void dropToWaiting() {
        currentState = neutralState;
        workingBounds = EditorBounds.unknown();
        syncState = SynchronizationState.WAITING_FOR_BOUNDS;
    }

    private SynchronizationState stateAfterNoEditorMutation() {
        if (!capabilities.isSupported()) {
            return SynchronizationState.UNSUPPORTED;
        }
        if (!workingBounds.hasSelection()) {
            return SynchronizationState.WAITING_FOR_BOUNDS;
        }
        return SynchronizationState.SYNCED;
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
