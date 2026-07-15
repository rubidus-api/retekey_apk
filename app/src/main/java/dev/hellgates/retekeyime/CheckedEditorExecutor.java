package dev.hellgates.retekeyime;

import java.util.List;

public final class CheckedEditorExecutor {
    public ExecutionResult execute(
        TransitionPlan<?> plan,
        ExecutionContext context,
        EditorEndpointProvider endpointProvider
    ) {
        if (plan == null || context == null || endpointProvider == null) {
            throw new IllegalArgumentException("execution arguments must not be null");
        }

        return executeInternal(plan, context, endpointProvider)
            .withStateBounds(context.bounds());
    }

    private ExecutionResult executeInternal(
        TransitionPlan<?> plan,
        ExecutionContext context,
        EditorEndpointProvider endpointProvider
    ) {
        ExecutionResult preflight = preflight(plan, context);
        if (preflight != null) {
            return preflight;
        }
        if (plan.actions().isEmpty()) {
            return actionless(plan);
        }
        if (isConfirmedCursorStart(plan, context)) {
            return confirmedNoEffect(plan);
        }

        EditorEndpoint endpoint;
        try {
            endpoint = endpointProvider.resolve();
        } catch (RuntimeException ignored) {
            return notDispatched(
                plan,
                ExecutionResult.Reason.CONNECTION_RESOLUTION_RUNTIME_FAILURE
            );
        }
        if (endpoint == null) {
            return notDispatched(plan, ExecutionResult.Reason.NO_CONNECTION);
        }
        if (endpoint.generation() != plan.generation()) {
            return notDispatched(
                plan,
                ExecutionResult.Reason.ENDPOINT_GENERATION_MISMATCH
            );
        }
        if (!endpoint.isCurrent()) {
            return notDispatched(
                plan,
                ExecutionResult.Reason.SESSION_CHANGED_DURING_EXECUTION
            );
        }

        boolean rawEditor = context.capabilities().deletionMode()
            == EditorCapabilities.DeletionMode.RAW_KEY;
        // A raw-key editor (a terminal like Termius reporting TYPE_NULL) uses key events for
        // deletion and enter, but plain committed text still goes through the ordinary commit path
        // so typed characters actually land.
        if (isSingleRawKey(plan.actions())
            || (rawEditor && !isSingleCommitText(plan.actions()))) {
            return executeRawCompatibility(plan, endpoint);
        }
        return executeRichPlan(plan, context, endpoint);
    }

    private static ExecutionResult preflight(TransitionPlan<?> plan, ExecutionContext context) {
        if (!context.isAccepting()) {
            return notDispatched(plan, ExecutionResult.Reason.SESSION_STOPPED);
        }
        if (plan.generation() != context.generation()) {
            return notDispatched(plan, ExecutionResult.Reason.STALE_GENERATION);
        }
        if (plan.baseRevision() != context.revision()) {
            return notDispatched(plan, ExecutionResult.Reason.STALE_REVISION);
        }
        if (!context.capabilities().isSupported()) {
            return notDispatched(plan, ExecutionResult.Reason.UNSUPPORTED_EDITOR);
        }
        if (requiresSelection(plan.actions()) && !context.bounds().hasSelection()) {
            return notDispatched(plan, ExecutionResult.Reason.INVALID_SELECTION);
        }
        if (context.capabilities().isSensitive()
            && containsAction(plan.actions(), KeyAction.Kind.SET_COMPOSING_TEXT)) {
            return notDispatched(
                plan,
                ExecutionResult.Reason.SENSITIVE_OPERATION_PROHIBITED
            );
        }
        if (context.capabilities().deletionMode()
            == EditorCapabilities.DeletionMode.RAW_KEY
            && !isSingleRawCompatibleAction(plan.actions())
            && !isSingleCommitText(plan.actions())) {
            return notDispatched(plan, ExecutionResult.Reason.UNSUPPORTED_EDITOR);
        }
        if (context.capabilities().deletionMode()
            == EditorCapabilities.DeletionMode.RICH_TEXT
            && containsRawEnter(plan.actions())) {
            return notDispatched(plan, ExecutionResult.Reason.UNSUPPORTED_EDITOR);
        }
        return null;
    }

    private static boolean isConfirmedCursorStart(
        TransitionPlan<?> plan,
        ExecutionContext context
    ) {
        return context.capabilities().deletionMode()
            == EditorCapabilities.DeletionMode.RICH_TEXT
            && context.areBoundsConfirmed()
            && isSingleDelete(plan.actions())
            && !context.bounds().hasSelectedText()
            && !context.bounds().hasComposingRange()
            && context.bounds().selectionStart() == 0;
    }

    private static boolean isSingleDelete(List<KeyAction> actions) {
        return actions.size() == 1 && actions.get(0).kind() == KeyAction.Kind.DELETE_BACKWARD;
    }

    private static boolean isSingleRawCompatibleAction(List<KeyAction> actions) {
        if (actions.size() != 1) {
            return false;
        }
        KeyAction.Kind kind = actions.get(0).kind();
        return kind == KeyAction.Kind.DELETE_BACKWARD
            || kind == KeyAction.Kind.RAW_ENTER
            || kind == KeyAction.Kind.RAW_KEY
            || kind == KeyAction.Kind.PERFORM_EDITOR_ACTION;
    }

    private static boolean isSingleCommitText(List<KeyAction> actions) {
        return actions.size() == 1 && actions.get(0).kind() == KeyAction.Kind.COMMIT_TEXT;
    }

    private static boolean isSingleRawKey(List<KeyAction> actions) {
        return actions.size() == 1 && actions.get(0).kind() == KeyAction.Kind.RAW_KEY;
    }

    private static boolean containsRawEnter(List<KeyAction> actions) {
        return containsAction(actions, KeyAction.Kind.RAW_ENTER);
    }

    private static boolean containsAction(List<KeyAction> actions, KeyAction.Kind kind) {
        for (KeyAction action : actions) {
            if (action.kind() == kind) {
                return true;
            }
        }
        return false;
    }

    private static boolean requiresSelection(List<KeyAction> actions) {
        // Only backward deletion needs a known selection: deleteSurroundingText depends on the
        // cursor position. Inserting or composing text lands at the editor's own cursor regardless
        // of what the IME knows, so those must not be blocked — otherwise editors that never report
        // a selection (terminals like Termius, some custom views) can never receive typed text.
        for (KeyAction action : actions) {
            if (action.kind() == KeyAction.Kind.DELETE_BACKWARD) {
                return true;
            }
        }
        return false;
    }

    private static ExecutionResult executeRawCompatibility(
        TransitionPlan<?> plan,
        EditorEndpoint endpoint
    ) {
        EditorBridge bridge = endpoint.bridge();
        KeyAction action = plan.actions().get(0);
        if (action.kind() == KeyAction.Kind.PERFORM_EDITOR_ACTION) {
            return executeFocusAction(plan, endpoint, action, 0, 0);
        }
        RawKey rawKey;
        java.util.Set<KeyModifier> modifiers;
        if (action.kind() == KeyAction.Kind.RAW_KEY) {
            rawKey = action.rawKey();
            modifiers = action.modifiers();
        } else {
            rawKey = action.kind() == KeyAction.Kind.RAW_ENTER
                ? RawKey.ENTER
                : RawKey.BACKSPACE;
            modifiers = java.util.Collections.emptySet();
        }
        EditorCallResult down = guardedCall(endpoint, () -> bridge.sendRawKey(RawEditorKey.of(
            rawKey,
            modifiers,
            RawEditorKey.Action.DOWN
        )));
        if (down.isStaleSession()) {
            return notDispatched(
                plan,
                ExecutionResult.Reason.SESSION_CHANGED_DURING_EXECUTION
            );
        }
        EditorCallResult up = safeCall(() -> bridge.sendRawKey(RawEditorKey.of(
            rawKey,
            modifiers,
            RawEditorKey.Action.UP
        )));
        if (!down.isSucceeded() || !up.isSucceeded()) {
            EditorCallResult primary = down.isSucceeded() ? up : down;
            ExecutionResult.Reason cleanupReason = !down.isSucceeded() && !up.isSucceeded()
                ? reasonForOperation(up)
                : ExecutionResult.Reason.NONE;
            return result(
                plan,
                ExecutionResult.Outcome.UNCERTAIN,
                reasonForOperation(primary),
                cleanupReason,
                ExecutionResult.StateEffect.RESET_DESYNCHRONIZED,
                0,
                down.isSucceeded() ? 1 : 0,
                !down.isSucceeded() && !up.isSucceeded() ? 1 : -1,
                2,
                action.kind(),
                down.isSucceeded() ? 1 : 0,
                true
            );
        }

        return result(
            plan,
            ExecutionResult.Outcome.DISPATCHED,
            ExecutionResult.Reason.NONE,
            ExecutionResult.Reason.NONE,
            ExecutionResult.StateEffect.ADOPT_PROPOSED_AWAITING_CONFIRMATION,
            -1,
            -1,
            -1,
            2,
            null,
            1,
            false
        );
    }

    private static ExecutionResult executeRichPlan(
        TransitionPlan<?> plan,
        ExecutionContext context,
        EditorEndpoint endpoint
    ) {
        List<KeyAction> actions = plan.actions();
        KeyAction last = actions.get(actions.size() - 1);
        boolean hasFocusAction = last.kind() == KeyAction.Kind.PERFORM_EDITOR_ACTION;
        int batchedActionCount = hasFocusAction ? actions.size() - 1 : actions.size();

        ExecutionResult prefixResult = null;
        if (batchedActionCount > 0) {
            prefixResult = executeBatched(plan, context, endpoint, batchedActionCount);
            if (prefixResult.isFailure()) {
                return prefixResult;
            }
        }
        if (!hasFocusAction) {
            return prefixResult;
        }
        return executeFocusAction(
            plan,
            endpoint,
            last,
            prefixResult == null ? 0 : prefixResult.dispatchedMutationCount(),
            prefixResult == null ? 0 : prefixResult.operationCount()
        );
    }

    private static ExecutionResult executeFocusAction(
        TransitionPlan<?> plan,
        EditorEndpoint endpoint,
        KeyAction action,
        int previouslyDispatched,
        int previousOperationCount
    ) {
        EditorCallResult call = guardedCall(
            endpoint,
            () -> endpoint.bridge().performEditorAction(action.actionId())
        );
        if (!call.isSucceeded()) {
            return uncertainOperationFailure(
                plan,
                call,
                plan.actions().size() - 1,
                previousOperationCount,
                action.kind(),
                previouslyDispatched,
                previousOperationCount + 1
            );
        }
        return result(
            plan,
            ExecutionResult.Outcome.DISPATCHED,
            ExecutionResult.Reason.NONE,
            ExecutionResult.Reason.NONE,
            ExecutionResult.StateEffect.ADOPT_PROPOSED_AWAITING_CONFIRMATION,
            -1,
            -1,
            -1,
            previousOperationCount + 1,
            null,
            previouslyDispatched + 1,
            false
        );
    }

    private static ExecutionResult executeBatched(
        TransitionPlan<?> plan,
        ExecutionContext context,
        EditorEndpoint endpoint,
        int actionCount
    ) {
        EditorBridge bridge = endpoint.bridge();
        List<KeyAction> actions = plan.actions();
        ExecutionResult.Reason primaryReason = ExecutionResult.Reason.NONE;
        ExecutionResult.Reason cleanupReason = ExecutionResult.Reason.NONE;
        int failedActionIndex = -1;
        int failedOperationIndex = -1;
        int cleanupOperationIndex = -1;
        KeyAction.Kind failedActionKind = null;
        int dispatched = 0;
        int operationIndex = 0;
        boolean confirmedNoEffect = false;
        boolean remoteMutationMayHaveOccurred = false;
        EditorBounds actionBounds = context.bounds();

        EditorCallResult begin = guardedCall(endpoint, bridge::beginBatchEdit);
        if (begin.isStaleSession()) {
            return notDispatched(
                plan,
                ExecutionResult.Reason.SESSION_CHANGED_DURING_EXECUTION
            );
        }
        operationIndex++;
        if (!begin.isSucceeded()) {
            primaryReason = begin.isRejected()
                ? ExecutionResult.Reason.BATCH_BEGIN_FALSE
                : ExecutionResult.Reason.BATCH_BEGIN_RUNTIME_FAILURE;
            failedOperationIndex = operationIndex - 1;
        }

        try {
            if (primaryReason == ExecutionResult.Reason.NONE) {
                for (int index = 0; index < actionCount; index++) {
                    KeyAction action = actions.get(index);
                    int actionStartOperation = operationIndex;
                    ActionExecution actionResult = executeAction(
                        endpoint,
                        action,
                        actionBounds,
                        context.capabilities()
                    );
                    operationIndex += actionResult.operationCount;
                    dispatched += actionResult.dispatchedMutationCount;
                    confirmedNoEffect |= actionResult.confirmedNoEffect;
                    if (!actionResult.succeeded) {
                        primaryReason = actionResult.reason;
                        failedActionIndex = index;
                        failedOperationIndex = actionStartOperation
                            + actionResult.failedOperationOffset;
                        failedActionKind = action.kind();
                        if (actionResult.cleanupReason != ExecutionResult.Reason.NONE) {
                            cleanupReason = actionResult.cleanupReason;
                            cleanupOperationIndex = actionStartOperation
                                + actionResult.cleanupOperationOffset;
                        }
                        remoteMutationMayHaveOccurred = actionResult.remoteMutationMayHaveOccurred
                            || dispatched > 0;
                        break;
                    }
                    actionBounds = EditorBoundsPredictor.after(actionBounds, action);
                }
            }
        } finally {
            EditorCallResult end = safeCall(bridge::endBatchEdit);
            operationIndex++;
            if (!end.isSucceeded()) {
                if (cleanupReason == ExecutionResult.Reason.NONE) {
                    cleanupReason = end.isRejected()
                        ? ExecutionResult.Reason.BATCH_END_FALSE
                        : ExecutionResult.Reason.BATCH_END_RUNTIME_FAILURE;
                    cleanupOperationIndex = operationIndex - 1;
                }
                if (primaryReason == ExecutionResult.Reason.NONE) {
                    failedOperationIndex = cleanupOperationIndex;
                }
            }
        }

        if (primaryReason == ExecutionResult.Reason.NONE
            && cleanupReason == ExecutionResult.Reason.NONE) {
            return result(
                plan,
                dispatched == 0 && confirmedNoEffect
                    ? ExecutionResult.Outcome.CONFIRMED_NO_EFFECT
                    : ExecutionResult.Outcome.DISPATCHED,
                ExecutionResult.Reason.NONE,
                ExecutionResult.Reason.NONE,
                dispatched == 0 && confirmedNoEffect
                    ? ExecutionResult.StateEffect.ADOPT_PROPOSED_SYNCED
                    : ExecutionResult.StateEffect.ADOPT_PROPOSED_AWAITING_CONFIRMATION,
                -1,
                -1,
                -1,
                operationIndex,
                null,
                dispatched,
                false
            );
        }

        ExecutionResult.Reason resultReason = primaryReason == ExecutionResult.Reason.NONE
            ? cleanupReason
            : primaryReason;
        remoteMutationMayHaveOccurred |= dispatched > 0;
        remoteMutationMayHaveOccurred |= primaryReason
            == ExecutionResult.Reason.BATCH_BEGIN_RUNTIME_FAILURE;
        remoteMutationMayHaveOccurred |= cleanupReason != ExecutionResult.Reason.NONE
            && begin.isSucceeded();
        return result(
            plan,
            remoteMutationMayHaveOccurred
                ? ExecutionResult.Outcome.UNCERTAIN
                : ExecutionResult.Outcome.NOT_DISPATCHED,
            resultReason,
            cleanupReason,
            remoteMutationMayHaveOccurred
                ? ExecutionResult.StateEffect.RESET_DESYNCHRONIZED
                : ExecutionResult.StateEffect.KEEP_CURRENT,
            failedActionIndex,
            failedOperationIndex,
            cleanupOperationIndex,
            operationIndex,
            failedActionKind,
            dispatched,
            remoteMutationMayHaveOccurred
        );
    }

    private static ActionExecution executeAction(
        EditorEndpoint endpoint,
        KeyAction action,
        EditorBounds bounds,
        EditorCapabilities capabilities
    ) {
        EditorBridge bridge = endpoint.bridge();
        switch (action.kind()) {
            case COMMIT_TEXT:
                return mutationCall(guardedCall(
                    endpoint,
                    () -> bridge.commitText(action.text(), 1)
                ));
            case SET_COMPOSING_TEXT:
                return mutationCall(guardedCall(
                    endpoint,
                    () -> bridge.setComposingText(action.text(), 1)
                ));
            case FINISH_COMPOSING:
                return mutationCall(guardedCall(endpoint, bridge::finishComposingText));
            case DELETE_BACKWARD:
                return executeRichDelete(endpoint, bounds, capabilities);
            case PERFORM_EDITOR_ACTION:
            case RAW_ENTER:
            case RAW_KEY:
                throw new IllegalStateException("terminal action reached batched executor");
            default:
                throw new IllegalStateException("unsupported editor action kind");
        }
    }

    private static ActionExecution executeRichDelete(
        EditorEndpoint endpoint,
        EditorBounds bounds,
        EditorCapabilities capabilities
    ) {
        EditorBridge bridge = endpoint.bridge();
        if (bounds.hasSelectedText()) {
            return mutationCall(guardedCall(endpoint, () -> bridge.commitText("", 1)));
        }

        EditorCallResult codePoint = guardedCall(
            endpoint,
            () -> bridge.deleteSurroundingTextInCodePoints(1, 0)
        );
        if (codePoint.isSucceeded()) {
            return ActionExecution.dispatched(1, 1);
        }
        if (!codePoint.isRejected()) {
            return ActionExecution.failure(
                reasonForOperation(codePoint),
                1,
                !codePoint.isStaleSession()
            );
        }
        if (!capabilities.allowLegacyCodeUnitFallback()
        ) {
            return ActionExecution.failure(
                ExecutionResult.Reason.OPERATION_FALSE,
                1,
                true
            );
        }

        if (capabilities.isSensitive()) {
            return capabilities.allowRawDeleteFallback()
                ? executeRawDeleteFallback(endpoint, 1)
                : ActionExecution.failure(
                    ExecutionResult.Reason.OPERATION_FALSE,
                    1,
                    true
                );
        }

        EditorTextResult textBefore = guardedTextCall(
            endpoint,
            () -> bridge.getTextBeforeCursor(2, 0)
        );
        if (textBefore.kind() == EditorTextResult.Kind.STALE_SESSION) {
            return ActionExecution.failure(
                ExecutionResult.Reason.SESSION_CHANGED_DURING_EXECUTION,
                1,
                false
            );
        }
        if (!textBefore.hasValue()) {
            return capabilities.allowRawDeleteFallback()
                ? executeRawDeleteFallback(endpoint, 2)
                : ActionExecution.failure(
                    ExecutionResult.Reason.INVALID_SURROUNDING_TEXT,
                    2,
                    false
                );
        }
        String text = textBefore.value();
        if (text.isEmpty()) {
            return capabilities.allowRawDeleteFallback()
                ? executeRawDeleteFallback(endpoint, 2)
                : ActionExecution.failure(
                    ExecutionResult.Reason.INVALID_SURROUNDING_TEXT,
                    2,
                    false
                );
        }
        if (!UnicodeScalar.isWellFormed(text)) {
            return capabilities.allowRawDeleteFallback()
                ? executeRawDeleteFallback(endpoint, 2)
                : ActionExecution.failure(
                    ExecutionResult.Reason.INVALID_SURROUNDING_TEXT,
                    2,
                    false
                );
        }
        int unitCount = safeTrailingCodePointUnits(text);
        if (unitCount == 0) {
            return capabilities.allowRawDeleteFallback()
                ? executeRawDeleteFallback(endpoint, 2)
                : ActionExecution.failure(
                    ExecutionResult.Reason.INVALID_SURROUNDING_TEXT,
                    2,
                    false
                );
        }

        EditorCallResult codeUnitDelete = guardedCall(
            endpoint,
            () -> bridge.deleteSurroundingText(unitCount, 0)
        );
        if (codeUnitDelete.isSucceeded()) {
            return ActionExecution.dispatched(1, 3);
        }
        return ActionExecution.failure(
            reasonForOperation(codeUnitDelete),
            3,
            !codeUnitDelete.isStaleSession()
        );
    }

    private static ActionExecution executeRawDeleteFallback(
        EditorEndpoint endpoint,
        int priorOperationCount
    ) {
        EditorBridge bridge = endpoint.bridge();
        EditorCallResult down = guardedCall(endpoint, () -> bridge.sendRawKey(RawEditorKey.of(
            RawKey.BACKSPACE,
            RawEditorKey.Action.DOWN
        )));
        if (down.isStaleSession()) {
            return ActionExecution.failure(
                ExecutionResult.Reason.SESSION_CHANGED_DURING_EXECUTION,
                priorOperationCount,
                false
            );
        }
        EditorCallResult up = safeCall(() -> bridge.sendRawKey(RawEditorKey.of(
            RawKey.BACKSPACE,
            RawEditorKey.Action.UP
        )));
        int operationCount = priorOperationCount + 2;
        if (down.isSucceeded() && up.isSucceeded()) {
            return ActionExecution.dispatched(1, operationCount);
        }
        EditorCallResult primary = down.isSucceeded() ? up : down;
        return ActionExecution.failureWithOffsets(
            reasonForOperation(primary),
            operationCount,
            true,
            down.isSucceeded() ? 1 : 0,
            down.isSucceeded() ? priorOperationCount + 1 : priorOperationCount,
            !down.isSucceeded() && !up.isSucceeded()
                ? reasonForOperation(up)
                : ExecutionResult.Reason.NONE,
            !down.isSucceeded() && !up.isSucceeded()
                ? priorOperationCount + 1
                : -1
        );
    }

    private static int safeTrailingCodePointUnits(String text) {
        if (text.length() > 2) {
            return 0;
        }
        char last = text.charAt(text.length() - 1);
        if (Character.isHighSurrogate(last)) {
            return 0;
        }
        if (!Character.isLowSurrogate(last)) {
            return 1;
        }
        if (text.length() == 2 && Character.isHighSurrogate(text.charAt(0))) {
            return 2;
        }
        return 0;
    }

    private static ActionExecution mutationCall(EditorCallResult callResult) {
        if (callResult.isSucceeded()) {
            return ActionExecution.dispatched(1, 1);
        }
        return ActionExecution.failure(
            reasonForOperation(callResult),
            1,
            !callResult.isStaleSession()
        );
    }

    private static ExecutionResult.Reason reasonForOperation(EditorCallResult callResult) {
        if (callResult.isStaleSession()) {
            return ExecutionResult.Reason.SESSION_CHANGED_DURING_EXECUTION;
        }
        return callResult.isRejected()
            ? ExecutionResult.Reason.OPERATION_FALSE
            : ExecutionResult.Reason.OPERATION_RUNTIME_FAILURE;
    }

    private static EditorCallResult safeCall(EditorCall call) {
        try {
            EditorCallResult result = call.invoke();
            return result == null ? EditorCallResult.runtimeFailure() : result;
        } catch (RuntimeException ignored) {
            return EditorCallResult.runtimeFailure();
        }
    }

    private static EditorCallResult guardedCall(
        EditorEndpoint endpoint,
        EditorCall call
    ) {
        return endpoint.isCurrent() ? safeCall(call) : EditorCallResult.staleSession();
    }

    private static EditorTextResult safeTextCall(EditorTextCall call) {
        try {
            EditorTextResult result = call.invoke();
            return result == null ? EditorTextResult.runtimeFailure() : result;
        } catch (RuntimeException ignored) {
            return EditorTextResult.runtimeFailure();
        }
    }

    private static EditorTextResult guardedTextCall(
        EditorEndpoint endpoint,
        EditorTextCall call
    ) {
        return endpoint.isCurrent() ? safeTextCall(call) : EditorTextResult.staleSession();
    }

    private static ExecutionResult actionless(TransitionPlan<?> plan) {
        return result(
            plan,
            ExecutionResult.Outcome.NO_EDITOR_ACTIONS,
            ExecutionResult.Reason.NONE,
            ExecutionResult.Reason.NONE,
            ExecutionResult.StateEffect.ADOPT_PROPOSED_SYNCED,
            -1,
            -1,
            -1,
            0,
            null,
            0,
            false
        );
    }

    private static ExecutionResult confirmedNoEffect(TransitionPlan<?> plan) {
        return result(
            plan,
            ExecutionResult.Outcome.CONFIRMED_NO_EFFECT,
            ExecutionResult.Reason.NONE,
            ExecutionResult.Reason.NONE,
            ExecutionResult.StateEffect.ADOPT_PROPOSED_SYNCED,
            -1,
            -1,
            -1,
            0,
            null,
            0,
            false
        );
    }

    private static ExecutionResult uncertainOperationFailure(
        TransitionPlan<?> plan,
        EditorCallResult callResult,
        int failedActionIndex,
        int failedOperationIndex,
        KeyAction.Kind failedActionKind,
        int dispatched,
        int operationCount
    ) {
        return result(
            plan,
            ExecutionResult.Outcome.UNCERTAIN,
            callResult.isRejected()
                ? ExecutionResult.Reason.OPERATION_FALSE
                : ExecutionResult.Reason.OPERATION_RUNTIME_FAILURE,
            ExecutionResult.Reason.NONE,
            ExecutionResult.StateEffect.RESET_DESYNCHRONIZED,
            failedActionIndex,
            failedOperationIndex,
            -1,
            operationCount,
            failedActionKind,
            dispatched,
            true
        );
    }

    private static ExecutionResult notDispatched(
        TransitionPlan<?> plan,
        ExecutionResult.Reason reason
    ) {
        return result(
            plan,
            ExecutionResult.Outcome.NOT_DISPATCHED,
            reason,
            ExecutionResult.Reason.NONE,
            ExecutionResult.StateEffect.KEEP_CURRENT,
            -1,
            -1,
            -1,
            0,
            null,
            0,
            false
        );
    }

    private static ExecutionResult result(
        TransitionPlan<?> plan,
        ExecutionResult.Outcome outcome,
        ExecutionResult.Reason reason,
        ExecutionResult.Reason cleanupReason,
        ExecutionResult.StateEffect stateEffect,
        int failedActionIndex,
        int failedOperationIndex,
        int cleanupOperationIndex,
        int operationCount,
        KeyAction.Kind failedActionKind,
        int dispatchedMutationCount,
        boolean remoteMutationMayHaveOccurred
    ) {
        return new ExecutionResult(
            outcome,
            reason,
            cleanupReason,
            stateEffect,
            plan,
            failedActionIndex,
            failedOperationIndex,
            cleanupOperationIndex,
            operationCount,
            failedActionKind,
            dispatchedMutationCount,
            remoteMutationMayHaveOccurred
        );
    }

    @FunctionalInterface
    private interface EditorCall {
        EditorCallResult invoke();
    }

    @FunctionalInterface
    private interface EditorTextCall {
        EditorTextResult invoke();
    }

    private static final class ActionExecution {
        private final boolean succeeded;
        private final boolean confirmedNoEffect;
        private final ExecutionResult.Reason reason;
        private final int operationCount;
        private final int dispatchedMutationCount;
        private final boolean remoteMutationMayHaveOccurred;
        private final int failedOperationOffset;
        private final ExecutionResult.Reason cleanupReason;
        private final int cleanupOperationOffset;

        private ActionExecution(
            boolean succeeded,
            boolean confirmedNoEffect,
            ExecutionResult.Reason reason,
            int operationCount,
            int dispatchedMutationCount,
            boolean remoteMutationMayHaveOccurred,
            int failedOperationOffset,
            ExecutionResult.Reason cleanupReason,
            int cleanupOperationOffset
        ) {
            this.succeeded = succeeded;
            this.confirmedNoEffect = confirmedNoEffect;
            this.reason = reason;
            this.operationCount = operationCount;
            this.dispatchedMutationCount = dispatchedMutationCount;
            this.remoteMutationMayHaveOccurred = remoteMutationMayHaveOccurred;
            this.failedOperationOffset = failedOperationOffset;
            this.cleanupReason = cleanupReason;
            this.cleanupOperationOffset = cleanupOperationOffset;
        }

        private static ActionExecution dispatched(int mutations, int operations) {
            return new ActionExecution(
                true,
                false,
                ExecutionResult.Reason.NONE,
                operations,
                mutations,
                false,
                -1,
                ExecutionResult.Reason.NONE,
                -1
            );
        }

        private static ActionExecution noEffect(int operations) {
            return new ActionExecution(
                true,
                true,
                ExecutionResult.Reason.NONE,
                operations,
                0,
                false,
                -1,
                ExecutionResult.Reason.NONE,
                -1
            );
        }

        private static ActionExecution failure(
            ExecutionResult.Reason reason,
            int operations,
            boolean remoteMutationMayHaveOccurred
        ) {
            return failure(reason, operations, remoteMutationMayHaveOccurred, 0);
        }

        private static ActionExecution failure(
            ExecutionResult.Reason reason,
            int operations,
            boolean remoteMutationMayHaveOccurred,
            int dispatchedMutationCount
        ) {
            return failureWithOffsets(
                reason,
                operations,
                remoteMutationMayHaveOccurred,
                dispatchedMutationCount,
                Math.max(0, operations - 1),
                ExecutionResult.Reason.NONE,
                -1
            );
        }

        private static ActionExecution failureWithOffsets(
            ExecutionResult.Reason reason,
            int operations,
            boolean remoteMutationMayHaveOccurred,
            int dispatchedMutationCount,
            int failedOperationOffset,
            ExecutionResult.Reason cleanupReason,
            int cleanupOperationOffset
        ) {
            return new ActionExecution(
                false,
                false,
                reason,
                operations,
                dispatchedMutationCount,
                remoteMutationMayHaveOccurred,
                failedOperationOffset,
                cleanupReason,
                cleanupOperationOffset
            );
        }
    }
}
