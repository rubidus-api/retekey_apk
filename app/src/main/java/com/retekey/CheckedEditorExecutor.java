package com.retekey;

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
        // A raw Enter (KEYCODE_ENTER) and raw keys go out as key events on ANY editor — that is how
        // Enter reaches a terminal, and how a normal field sees a real Enter press.
        if (isSingleRawKey(plan.actions())
            || isSingleRawEnter(plan.actions())
            || (rawEditor && !isSingleCommitText(plan.actions()))) {
            return executeRawCompatibility(plan, endpoint, context.capabilities());
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
        // Deletion is never refused for an unknown selection: deleteSurroundingTextInCodePoints
        // deletes relative to the editor's own cursor, so it works whether or not the IME knows
        // the position. Refusing it here is what made backspace stop working in terminals once
        // they reported an unknown selection.
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
        return null;
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

    private static boolean isSingleRawEnter(List<KeyAction> actions) {
        return actions.size() == 1 && actions.get(0).kind() == KeyAction.Kind.RAW_ENTER;
    }

    private static boolean containsAction(List<KeyAction> actions, KeyAction.Kind kind) {
        for (KeyAction action : actions) {
            if (action.kind() == kind) {
                return true;
            }
        }
        return false;
    }


    /**
     * 이 코드를 감쌀 **진짜 수식키**들 — 원격데스크톱 프로파일에서만, 그리고 코드가 있을 때만.
     * 순서는 고정이다(Ctrl → Shift → Alt → Meta): 같은 코드가 언제나 같은 열로 나가야
     * 저쪽에서 재현된다.
     */
    private static java.util.List<RawKey> modifierFrame(
        EditorCapabilities capabilities,
        java.util.Set<KeyModifier> modifiers
    ) {
        if (capabilities == null || !capabilities.deleteByKeyEvents() || modifiers.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        java.util.List<RawKey> frame = new java.util.ArrayList<>(4);
        if (modifiers.contains(KeyModifier.CTRL)) {
            frame.add(RawKey.CTRL_LEFT);
        }
        if (modifiers.contains(KeyModifier.SHIFT)) {
            frame.add(RawKey.SHIFT_LEFT);
        }
        if (modifiers.contains(KeyModifier.ALT)) {
            frame.add(RawKey.ALT_LEFT);
        }
        if (modifiers.contains(KeyModifier.META)) {
            frame.add(RawKey.META_LEFT);
        }
        return frame;
    }

    private static KeyModifier modifierOf(RawKey key) {
        switch (key) {
            case CTRL_LEFT: return KeyModifier.CTRL;
            case SHIFT_LEFT: return KeyModifier.SHIFT;
            case ALT_LEFT: return KeyModifier.ALT;
            default: return KeyModifier.META;
        }
    }

    private static ExecutionResult executeRawCompatibility(
        TransitionPlan<?> plan,
        EditorEndpoint endpoint,
        EditorCapabilities capabilities
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
        RawKeyPhase phase = action.kind() == KeyAction.Kind.RAW_KEY
            ? action.rawKeyPhase()
            : RawKeyPhase.TAP;
        if (phase != RawKeyPhase.TAP) {
            return executeRawKeyHalf(plan, endpoint, action, rawKey, modifiers, phase);
        }
        // ★★★ **원격데스크톱에서는 수식키를 진짜로 누른다** (2026-08-29, 사용자 보고).
        //   릴레이 뒤에는 텍스트 뷰가 없고 저쪽은 **진짜 OS** 다: 릴레이는 KeyEvent 를 저쪽
        //   키 입력으로 옮기면서 **metaState 를 안 본다**. 그래서 Ctrl+C 가 `c` 로 도착했고,
        //   액션바의 잘라내기·복사와 터치 자판의 Ctrl+X/C/V/A 가 **원격에서만** 죽어 있었다.
        //   ⇒ 그 프로파일에서만 코드를 프레임으로 감싼다: Ctrl down → C down/up → Ctrl up.
        //   ☞ 로컬 편집기는 **그대로** 둔다 — 거기서는 metaState 하나면 TextView 가 읽고,
        //     수식키를 따로 보내면 다른 앱의 단축키까지 깨울 수 있다.
        java.util.List<RawKey> frame = modifierFrame(capabilities, modifiers);
        java.util.Set<KeyModifier> held = java.util.EnumSet.noneOf(KeyModifier.class);
        for (RawKey modifierKey : frame) {
            held.add(modifierOf(modifierKey));
            java.util.Set<KeyModifier> pressed = java.util.EnumSet.copyOf(held);
            EditorCallResult modifierDown = safeCall(() -> bridge.sendRawKey(RawEditorKey.of(
                modifierKey,
                pressed,
                RawEditorKey.Action.DOWN
            )));
            if (!modifierDown.isSucceeded()) {
                break;
            }
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
        // 누른 역순으로 놓는다 — 실제 손가락이 그렇게 하고, 저쪽 OS 도 그 순서를 기대한다.
        for (int i = frame.size() - 1; i >= 0; i--) {
            RawKey modifierKey = frame.get(i);
            held.remove(modifierOf(modifierKey));
            java.util.Set<KeyModifier> stillHeld = java.util.EnumSet.copyOf(held);
            safeCall(() -> bridge.sendRawKey(RawEditorKey.of(
                modifierKey,
                stillHeld,
                RawEditorKey.Action.UP
            )));
        }
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

    /**
     * Sends one half of a key press — the down that latches a key and leaves it held, or the up
     * that ends it. One editor call, so there is no half-done pair to unwind: it either lands or
     * it does not.
     */
    private static ExecutionResult executeRawKeyHalf(
        TransitionPlan<?> plan,
        EditorEndpoint endpoint,
        KeyAction action,
        RawKey rawKey,
        java.util.Set<KeyModifier> modifiers,
        RawKeyPhase phase
    ) {
        EditorBridge bridge = endpoint.bridge();
        RawEditorKey.Action half = phase == RawKeyPhase.HOLD
            ? RawEditorKey.Action.DOWN
            : RawEditorKey.Action.UP;
        EditorCallResult sent = guardedCall(endpoint, () -> bridge.sendRawKey(RawEditorKey.of(
            rawKey,
            modifiers,
            half
        )));
        if (sent.isStaleSession()) {
            return notDispatched(
                plan,
                ExecutionResult.Reason.SESSION_CHANGED_DURING_EXECUTION
            );
        }
        if (!sent.isSucceeded()) {
            return result(
                plan,
                ExecutionResult.Outcome.NOT_DISPATCHED,
                reasonForOperation(sent),
                ExecutionResult.Reason.NONE,
                ExecutionResult.StateEffect.KEEP_CURRENT,
                0,
                0,
                -1,
                1,
                action.kind(),
                0,
                false
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
            1,
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
            case DELETE_RECENT: {
                // Our own just-committed characters: the surrounding-text call is reliable here
                // on every editor, remote-desktop dummies included, and cannot be key-filtered.
                int count = action.recentCount();
                EditorCallResult recent = guardedCall(
                    endpoint,
                    () -> bridge.deleteSurroundingTextInCodePoints(count, 0)
                );
                return recent.isSucceeded()
                    ? ActionExecution.dispatched(1, 1)
                    : ActionExecution.failure(reasonForOperation(recent), 1,
                        !recent.isStaleSession());
            }
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
        if (capabilities.deleteByKeyEvents()) {
            // A remote-desktop editor relays over two pipes — text operations and key events —
            // and the pipes are not ordered against each other: a key-event backspace can land
            // after a text commit that followed it, eating the retyped syllable. When the relay's
            // buffer verifiably holds text, delete over the same text channel every commit takes,
            // so order is preserved; the key event stays the fallback for an unknown, empty, or
            // start-of-field buffer, for a sensitive field (never read), and for a selection,
            // which lives on the far side.
            int priorOperations = 0;
            if (!bounds.hasSelectedText()
                && !capabilities.isSensitive()
                && bounds.selectionStart() != 0) {
                EditorTextResult relayBefore = guardedTextCall(
                    endpoint,
                    () -> bridge.getTextBeforeCursor(1, 0)
                );
                priorOperations++;
                if (relayBefore.kind() == EditorTextResult.Kind.STALE_SESSION) {
                    return ActionExecution.failure(
                        ExecutionResult.Reason.SESSION_CHANGED_DURING_EXECUTION,
                        priorOperations,
                        false
                    );
                }
                if (relayBefore.hasValue() && !relayBefore.value().isEmpty()) {
                    EditorCallResult ordered = guardedCall(
                        endpoint,
                        () -> bridge.deleteSurroundingTextInCodePoints(1, 0)
                    );
                    priorOperations++;
                    if (ordered.isSucceeded()) {
                        return ActionExecution.dispatched(1, priorOperations);
                    }
                    if (ordered.isStaleSession()) {
                        return ActionExecution.failure(
                            ExecutionResult.Reason.SESSION_CHANGED_DURING_EXECUTION,
                            priorOperations,
                            false
                        );
                    }
                    // The text call was refused: the key event below still deletes remotely.
                }
            }
            return executeRawDeleteFallback(endpoint, priorOperations);
        }
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
            if (bounds.selectionStart() <= 0) {
                // The editor itself confirms what the bounds claimed: nothing sits before the
                // cursor, so a backspace is a no-op, not a failure.
                return ActionExecution.noEffect(2);
            }
            // Empty context contradicting a known nonzero cursor: do not guess.
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
