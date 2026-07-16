package dev.hellgates.retekeyime;

import java.util.Collections;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

public final class InputSessionControllerTest {
    private static final EditorCapabilities RICH = EditorCapabilities.richText(false, false);
    private static final EditorBounds START = EditorBounds.of(0, 0, -1, -1);
    private static final EditorBounds ONE = EditorBounds.of(1, 1, -1, -1);
    private static final EditorBounds TWO = EditorBounds.of(2, 2, -1, -1);

    @Test
    public void restartCreatesNewGenerationAndRejectsOldPlanWithoutConnection() {
        InputSessionController<String> controller = new InputSessionController<>(4);
        long first = controller.start("initial-1", START, RICH);
        TransitionPlan<String> stale = controller.plan(
            DispatchResult.handled(KeyAction.commitText("x")),
            "next-1",
            ONE
        );
        long second = controller.start("initial-2", START, RICH);
        AtomicInteger resolves = new AtomicInteger();

        ExecutionResult result = controller.execute(stale, () -> {
            resolves.incrementAndGet();
            return EditorEndpoint.of(second, new FakeEditorBridge());
        });

        Assert.assertEquals(first + 1, second);
        Assert.assertEquals(0, resolves.get());
        Assert.assertEquals(ExecutionResult.Reason.STALE_GENERATION, result.reason());
        Assert.assertEquals("initial-2", controller.currentState());
    }

    @Test
    public void currentPlanIsConsumedEvenWhenConnectionIsMissing() {
        InputSessionController<String> controller = new InputSessionController<>(4);
        controller.start("initial", START, RICH);
        TransitionPlan<String> plan = controller.plan(
            DispatchResult.handled(KeyAction.commitText("x")),
            "next",
            ONE
        );
        AtomicInteger replayResolves = new AtomicInteger();

        ExecutionResult missing = controller.execute(plan, () -> null);
        ExecutionResult replay = controller.execute(plan, () -> {
            replayResolves.incrementAndGet();
            return EditorEndpoint.of(controller.generation(), new FakeEditorBridge());
        });

        Assert.assertEquals(ExecutionResult.Reason.NO_CONNECTION, missing.reason());
        Assert.assertEquals(1, controller.revision());
        Assert.assertEquals(ExecutionResult.Reason.STALE_REVISION, replay.reason());
        Assert.assertEquals(0, replayResolves.get());
    }

    @Test
    public void richEditorWithoutBoundsStaysStatelessAndNeverDesynchronizes() {
        // A terminal may present as a rich-text editor that never reports a selection (Termius with
        // Korean). Commits while bounds are unknown must also be stateless, or the session
        // accumulates and stops input after a few characters.
        InputSessionController<String> controller = new InputSessionController<>(4);
        long generation = controller.start("neutral", EditorBounds.unknown(), RICH);
        for (int i = 0; i < 50; i++) {
            ExecutionResult r = controller.execute(
                controller.plan(
                    DispatchResult.handled(KeyAction.commitText("가")),
                    "s" + i,
                    EditorBounds.unknown()
                ),
                () -> EditorEndpoint.of(generation, new FakeEditorBridge())
            );
            Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, r.outcome());
            Assert.assertEquals(0, controller.pendingExpectationCount());
        }
        Assert.assertEquals(SynchronizationState.WAITING_FOR_BOUNDS, controller.syncState());
        // Once the editor finally reports a selection, the session adopts it (as external movement,
        // since nothing was pending) and becomes synced — never desynchronized.
        Assert.assertEquals(
            SelectionReconcileResult.EXTERNAL_MOVEMENT,
            controller.updateSelection(generation, ONE));
        Assert.assertEquals(SynchronizationState.SYNCED, controller.syncState());
    }

    @Test
    public void rawKeyTerminalCommitsStayStatelessAndNeverDesynchronize() {
        // A terminal (RAW_KEY / TYPE_NULL) never reports a selection; repeated commits must not
        // accumulate pending expectations or drift into AWAITING_CONFIRMATION and desynchronize.
        EditorCapabilities rawKey = EditorCapabilities.rawKey();
        InputSessionController<String> controller = new InputSessionController<>(4);
        long generation = controller.start("neutral", EditorBounds.unknown(), rawKey);
        for (int i = 0; i < 50; i++) {
            ExecutionResult r = controller.execute(
                controller.plan(
                    DispatchResult.handled(KeyAction.commitText("x")),
                    "s" + i,
                    EditorBounds.unknown()
                ),
                () -> EditorEndpoint.of(generation, new FakeEditorBridge())
            );
            Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, r.outcome());
            Assert.assertEquals(0, controller.pendingExpectationCount());
        }
        Assert.assertEquals(SynchronizationState.WAITING_FOR_BOUNDS, controller.syncState());
        // The terminal keeps reporting an unknown selection; that must not desynchronize the session.
        Assert.assertEquals(
            SelectionReconcileResult.WAITING_FOR_BOUNDS,
            controller.updateSelection(generation, EditorBounds.unknown()));
        // Input still works afterwards.
        ExecutionResult after = controller.execute(
            controller.plan(
                DispatchResult.handled(KeyAction.commitText("y")), "after", EditorBounds.unknown()),
            () -> EditorEndpoint.of(generation, new FakeEditorBridge()));
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, after.outcome());
    }

    @Test
    public void dispatchedPlanAdoptsTentativeStateUntilMatchingFeedback() {
        InputSessionController<String> controller = new InputSessionController<>(4);
        long generation = controller.start("initial", START, RICH);
        FakeEditorBridge bridge = new FakeEditorBridge();

        ExecutionResult result = controller.execute(
            controller.plan(
                DispatchResult.handled(KeyAction.commitText("x")),
                "next",
                ONE
            ),
            () -> EditorEndpoint.of(generation, bridge)
        );

        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
        Assert.assertEquals("next", controller.currentState());
        Assert.assertEquals(SynchronizationState.AWAITING_CONFIRMATION, controller.syncState());
        Assert.assertEquals(1, controller.pendingExpectationCount());
        Assert.assertEquals(
            SelectionReconcileResult.MATCHED,
            controller.updateSelection(generation, ONE)
        );
        Assert.assertEquals(SynchronizationState.SYNCED, controller.syncState());
        Assert.assertEquals(0, controller.pendingExpectationCount());
    }

    @Test
    public void newestFeedbackCoalescesEarlierExpectationsAndLateDuplicateIsIgnored() {
        InputSessionController<String> controller = new InputSessionController<>(4);
        long generation = controller.start("initial", START, RICH);
        executeSuccess(controller, generation, "one", ONE);
        executeSuccess(controller, generation, "two", TWO);

        Assert.assertEquals(2, controller.pendingExpectationCount());
        Assert.assertEquals(
            SelectionReconcileResult.COALESCED,
            controller.updateSelection(generation, TWO)
        );
        Assert.assertEquals(SynchronizationState.SYNCED, controller.syncState());
        Assert.assertEquals(
            SelectionReconcileResult.DUPLICATE_OR_DELAYED,
            controller.updateSelection(generation, ONE)
        );
        Assert.assertEquals("two", controller.currentState());
    }

    @Test
    public void duplicateOfLastConfirmedBoundsIsIgnoredWhileNewExpectationIsPending() {
        InputSessionController<String> controller = new InputSessionController<>(4);
        long generation = controller.start("neutral", START, RICH);
        executeSuccess(controller, generation, "one", ONE);

        Assert.assertEquals(
            SelectionReconcileResult.DUPLICATE_OR_DELAYED,
            controller.updateSelection(generation, START)
        );
        Assert.assertEquals(SynchronizationState.AWAITING_CONFIRMATION, controller.syncState());
        Assert.assertEquals("one", controller.currentState());
        Assert.assertEquals(1, controller.pendingExpectationCount());
    }

    @Test
    public void identicalExpectedBoundsAreCoalescedByOneCallback() {
        InputSessionController<String> controller = new InputSessionController<>(4);
        long generation = controller.start("neutral", START, RICH);
        executeSuccess(controller, generation, "one", ONE);
        executeSuccess(controller, generation, "two", ONE);

        Assert.assertEquals(2, controller.pendingExpectationCount());
        Assert.assertEquals(
            SelectionReconcileResult.COALESCED,
            controller.updateSelection(generation, ONE)
        );
        Assert.assertEquals(0, controller.pendingExpectationCount());
        Assert.assertEquals(SynchronizationState.SYNCED, controller.syncState());
    }

    @Test
    public void unknownInitialBoundsWaitUntilFirstValidFeedback() {
        InputSessionController<String> controller = new InputSessionController<>(2);
        long generation = controller.start("neutral", EditorBounds.unknown(), RICH);

        Assert.assertEquals(
            SynchronizationState.WAITING_FOR_BOUNDS,
            controller.syncState()
        );
        Assert.assertEquals(
            SelectionReconcileResult.WAITING_FOR_BOUNDS,
            controller.updateSelection(generation, EditorBounds.unknown())
        );
        ExecutionResult actionless = controller.execute(
            controller.plan(DispatchResult.handled(), "still-neutral", EditorBounds.unknown()),
            () -> {
                throw new AssertionError("actionless plan must not resolve an editor");
            }
        );
        Assert.assertEquals(ExecutionResult.Outcome.NO_EDITOR_ACTIONS, actionless.outcome());
        Assert.assertEquals(
            SynchronizationState.WAITING_FOR_BOUNDS,
            controller.syncState()
        );
        Assert.assertEquals(
            SelectionReconcileResult.EXTERNAL_MOVEMENT,
            controller.updateSelection(generation, START)
        );
        Assert.assertEquals(SynchronizationState.SYNCED, controller.syncState());
    }

    @Test
    public void unsupportedCapabilityNeverBecomesSyncedFromSelectionFeedback() {
        InputSessionController<String> controller = new InputSessionController<>(2);
        long generation = controller.start(
            "neutral",
            EditorBounds.unknown(),
            EditorCapabilities.unsupported()
        );

        Assert.assertEquals(
            SelectionReconcileResult.UNSUPPORTED,
            controller.updateSelection(generation, START)
        );
        Assert.assertEquals(SynchronizationState.UNSUPPORTED, controller.syncState());
    }

    @Test
    public void contradictoryFeedbackAdoptsTheEditorAndResyncs() {
        InputSessionController<String> controller = new InputSessionController<>(4);
        long generation = controller.start("neutral", START, RICH);
        FakeEditorBridge bridge = new FakeEditorBridge();
        executeSuccess(controller, generation, "tentative", ONE, bridge);
        int callsBeforeFeedback = bridge.callCount();

        SelectionReconcileResult reconcile = controller.updateSelection(
            generation,
            EditorBounds.of(9, 9, -1, -1)
        );

        // The editor is authoritative: adopt its reported selection and resync rather than
        // desynchronizing (which would freeze the keyboard). No editor call is replayed.
        Assert.assertEquals(SelectionReconcileResult.EXTERNAL_MOVEMENT, reconcile);
        Assert.assertEquals(SynchronizationState.SYNCED, controller.syncState());
        Assert.assertEquals("tentative", controller.currentState());
        Assert.assertEquals(0, controller.pendingExpectationCount());
        Assert.assertEquals(callsBeforeFeedback, bridge.callCount());
    }

    @Test
    public void fullPendingLedgerDropsBacklogAndKeepsAcceptingInput() {
        InputSessionController<String> controller = new InputSessionController<>(1);
        long generation = controller.start("neutral", START, RICH);
        executeSuccess(controller, generation, "one", ONE);
        TransitionPlan<String> overflow = controller.plan(
            DispatchResult.handled(KeyAction.commitText("y")),
            "two",
            TWO
        );
        AtomicInteger resolves = new AtomicInteger();

        ExecutionResult result = controller.execute(overflow, () -> {
            resolves.incrementAndGet();
            return EditorEndpoint.of(generation, new FakeEditorBridge());
        });

        // The stale backlog is dropped and the new commit still dispatches — never a permanent
        // freeze when an editor rarely confirms its selection.
        Assert.assertEquals(1, resolves.get());
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
        Assert.assertNotEquals(SynchronizationState.DESYNCHRONIZED, controller.syncState());
        Assert.assertEquals("two", controller.currentState());
    }

    @Test
    public void feedbackDuringEditorCallIsDeferredThenReconciled() {
        InputSessionController<String> controller = new InputSessionController<>(2);
        long generation = controller.start("neutral", START, RICH);
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.runAt(2, () -> Assert.assertEquals(
            SelectionReconcileResult.DEFERRED,
            controller.updateSelection(generation, ONE)
        ));

        ExecutionResult result = controller.execute(
            controller.plan(
                DispatchResult.handled(KeyAction.commitText("x")),
                "one",
                ONE
            ),
            () -> EditorEndpoint.of(generation, bridge)
        );

        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
        Assert.assertEquals(SynchronizationState.SYNCED, controller.syncState());
        Assert.assertEquals(0, controller.pendingExpectationCount());
        Assert.assertEquals("one", controller.currentState());
    }

    @Test
    public void intermediateCallbacksInsideMultiActionPlanCoalesceToFinalBounds() {
        InputSessionController<String> controller = new InputSessionController<>(3);
        long generation = controller.start("neutral", START, RICH);
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.runAt(2, () -> Assert.assertEquals(
            SelectionReconcileResult.DEFERRED,
            controller.updateSelection(generation, ONE)
        ));
        bridge.runAt(3, () -> Assert.assertEquals(
            SelectionReconcileResult.DEFERRED,
            controller.updateSelection(generation, TWO)
        ));

        ExecutionResult result = controller.execute(
            controller.plan(
                DispatchResult.handled(
                    KeyAction.commitText("a"),
                    KeyAction.setComposingText("b")
                ),
                "composed",
                TWO
            ),
            () -> EditorEndpoint.of(generation, bridge)
        );

        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
        Assert.assertEquals(SynchronizationState.SYNCED, controller.syncState());
        Assert.assertEquals(0, controller.pendingExpectationCount());
        Assert.assertEquals("composed", controller.currentState());
    }

    @Test
    public void delayedIntermediateCallbackWaitsForFinalBoundsWithoutDesync() {
        InputSessionController<String> controller = new InputSessionController<>(3);
        long generation = controller.start("neutral", START, RICH);
        java.util.List<KeyAction> actions = Arrays.asList(
            KeyAction.commitText("a"),
            KeyAction.setComposingText("b")
        );
        EditorExpectation expectation = EditorBoundsPredictor.expectationAfter(
            START,
            actions
        );

        ExecutionResult result = controller.execute(
            controller.planWithExpectation(
                DispatchResult.handled(actions),
                "composed",
                expectation
            ),
            () -> EditorEndpoint.of(generation, new FakeEditorBridge())
        );

        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
        Assert.assertEquals(
            SelectionReconcileResult.INTERMEDIATE,
            controller.updateSelection(generation, ONE)
        );
        Assert.assertEquals(
            SynchronizationState.AWAITING_CONFIRMATION,
            controller.syncState()
        );
        EditorBounds finalBounds = EditorBounds.of(2, 2, 1, 2);
        Assert.assertEquals(
            SelectionReconcileResult.MATCHED,
            controller.updateSelection(generation, finalBounds)
        );
        Assert.assertEquals(SynchronizationState.SYNCED, controller.syncState());
    }

    @Test
    public void intermediateAndOlderDuplicateCanPrecedeFinalBounds() {
        InputSessionController<String> controller = new InputSessionController<>(3);
        long generation = controller.start("neutral", START, RICH);
        java.util.List<KeyAction> actions = Arrays.asList(
            KeyAction.commitText("a"),
            KeyAction.setComposingText("b")
        );
        EditorExpectation expectation = EditorBoundsPredictor.expectationAfter(START, actions);

        controller.execute(
            controller.planWithExpectation(
                DispatchResult.handled(actions),
                "composed",
                expectation
            ),
            () -> EditorEndpoint.of(generation, new FakeEditorBridge())
        );

        Assert.assertEquals(
            SelectionReconcileResult.INTERMEDIATE,
            controller.updateSelection(generation, ONE)
        );
        Assert.assertEquals(
            SelectionReconcileResult.DUPLICATE_OR_DELAYED,
            controller.updateSelection(generation, START)
        );
        EditorBounds finalBounds = EditorBounds.of(2, 2, 1, 2);
        Assert.assertEquals(
            SelectionReconcileResult.MATCHED,
            controller.updateSelection(generation, finalBounds)
        );
        Assert.assertEquals(SynchronizationState.SYNCED, controller.syncState());
        Assert.assertEquals("composed", controller.currentState());
    }

    @Test
    public void intermediateArrivingAfterFinalBoundsIsRetiredFeedback() {
        InputSessionController<String> controller = new InputSessionController<>(3);
        long generation = controller.start("neutral", START, RICH);
        java.util.List<KeyAction> actions = Arrays.asList(
            KeyAction.commitText("a"),
            KeyAction.setComposingText("b")
        );
        EditorExpectation expectation = EditorBoundsPredictor.expectationAfter(START, actions);
        EditorBounds finalBounds = EditorBounds.of(2, 2, 1, 2);

        controller.execute(
            controller.planWithExpectation(
                DispatchResult.handled(actions),
                "composed",
                expectation
            ),
            () -> EditorEndpoint.of(generation, new FakeEditorBridge())
        );
        Assert.assertEquals(
            SelectionReconcileResult.MATCHED,
            controller.updateSelection(generation, finalBounds)
        );

        Assert.assertEquals(
            SelectionReconcileResult.DUPLICATE_OR_DELAYED,
            controller.updateSelection(generation, ONE)
        );
        Assert.assertEquals(SynchronizationState.SYNCED, controller.syncState());
        Assert.assertEquals("composed", controller.currentState());
        Assert.assertEquals(finalBounds, controller.workingBounds());
    }

    @Test
    public void unobservedFinalAlternativeRemainsAnExternalMovement() {
        InputSessionController<String> controller = new InputSessionController<>(3);
        EditorBounds cursorTwo = EditorBounds.of(2, 2, -1, -1);
        EditorBounds cursorOne = EditorBounds.of(1, 1, -1, -1);
        EditorBounds cursorZero = EditorBounds.of(0, 0, -1, -1);
        long generation = controller.start("neutral", cursorTwo, RICH);
        java.util.List<KeyAction> actions = Collections.singletonList(
            KeyAction.deleteBackward()
        );

        controller.execute(
            controller.planWithExpectation(
                DispatchResult.handled(actions),
                "after-delete",
                EditorBoundsPredictor.expectationAfter(cursorTwo, actions)
            ),
            () -> EditorEndpoint.of(generation, new FakeEditorBridge())
        );
        Assert.assertEquals(
            SelectionReconcileResult.MATCHED,
            controller.updateSelection(generation, cursorOne)
        );

        Assert.assertEquals(
            SelectionReconcileResult.EXTERNAL_MOVEMENT,
            controller.updateSelection(generation, cursorZero)
        );
        Assert.assertEquals(cursorZero, controller.workingBounds());
        Assert.assertEquals("neutral", controller.currentState());
        Assert.assertEquals(SynchronizationState.SYNCED, controller.syncState());
    }

    @Test
    public void deferredFeedbackOverflowConservativelyDesynchronizes() {
        InputSessionController<String> controller = new InputSessionController<>(1);
        long generation = controller.start("neutral", START, RICH);
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.runAt(2, () -> {
            Assert.assertEquals(
                SelectionReconcileResult.DEFERRED,
                controller.updateSelection(generation, ONE)
            );
            Assert.assertEquals(
                SelectionReconcileResult.DEFERRED_OVERFLOW,
                controller.updateSelection(generation, TWO)
            );
        });

        ExecutionResult result = controller.execute(
            controller.plan(
                DispatchResult.handled(KeyAction.commitText("x")),
                "one",
                ONE
            ),
            () -> EditorEndpoint.of(generation, bridge)
        );

        Assert.assertEquals(SynchronizationState.DESYNCHRONIZED, controller.syncState());
        Assert.assertEquals(ExecutionResult.Outcome.UNCERTAIN, result.outcome());
        Assert.assertEquals(ExecutionResult.Reason.LEDGER_OVERFLOW, result.reason());
        Assert.assertTrue(result.remoteMutationMayHaveOccurred());
        Assert.assertEquals("neutral", controller.currentState());
        Assert.assertEquals(0, controller.pendingExpectationCount());
    }

    @Test
    public void stoppedAndDesynchronizedSessionsRejectFurtherMutation() {
        InputSessionController<String> stopped = new InputSessionController<>(2);
        long stoppedGeneration = stopped.start("neutral", START, RICH);
        stopped.stopAccepting();
        AtomicInteger stoppedResolves = new AtomicInteger();
        ExecutionResult stoppedResult = stopped.execute(
            stopped.plan(
                DispatchResult.handled(KeyAction.commitText("x")),
                "next",
                ONE
            ),
            () -> {
                stoppedResolves.incrementAndGet();
                return EditorEndpoint.of(stoppedGeneration, new FakeEditorBridge());
            }
        );

        Assert.assertEquals(ExecutionResult.Reason.SESSION_STOPPED, stoppedResult.reason());
        Assert.assertEquals(0, stoppedResolves.get());
    }

    @Test
    public void controllerDiagnosticsDoNotExposeEngineState() {
        InputSessionController<String> controller = new InputSessionController<>(2);
        controller.start("private-engine-state", START, RICH);

        Assert.assertFalse(controller.toString().contains("private-engine-state"));
    }

    @Test
    public void unpredictableDeleteBlocksLaterMutationUntilAnyValidFeedback() {
        InputSessionController<String> controller = new InputSessionController<>(3);
        EditorBounds cursorTwo = EditorBounds.of(2, 2, -1, -1);
        long generation = controller.start("neutral", cursorTwo, RICH);
        FakeEditorBridge deleteBridge = new FakeEditorBridge();
        TransitionPlan<String> delete = controller.planWithExpectation(
            DispatchResult.handled(KeyAction.deleteBackward()),
            "after-delete",
            EditorBoundsPredictor.expectationAfter(
                cursorTwo,
                Collections.singletonList(KeyAction.deleteBackward())
            )
        );

        ExecutionResult deleted = controller.execute(
            delete,
            () -> EditorEndpoint.of(generation, deleteBridge)
        );

        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, deleted.outcome());
        Assert.assertEquals(EditorBounds.unknown(), controller.workingBounds());
        AtomicInteger blockedResolves = new AtomicInteger();
        ExecutionResult blocked = controller.execute(
            controller.plan(
                // Deletion still requires a known selection, so it stays blocked here.
                DispatchResult.handled(KeyAction.deleteBackward()),
                "must-not-adopt",
                EditorBounds.unknown()
            ),
            () -> {
                blockedResolves.incrementAndGet();
                return EditorEndpoint.of(generation, new FakeEditorBridge());
            }
        );
        Assert.assertEquals(ExecutionResult.Reason.INVALID_SELECTION, blocked.reason());
        Assert.assertEquals(0, blockedResolves.get());

        Assert.assertEquals(
            SelectionReconcileResult.MATCHED,
            controller.updateSelection(generation, EditorBounds.of(1, 1, -1, -1))
        );
        Assert.assertEquals(EditorBounds.of(1, 1, -1, -1), controller.workingBounds());
        Assert.assertEquals(SynchronizationState.SYNCED, controller.syncState());
    }

    @Test
    public void generationChangeDuringEditorCallCannotAdoptOldPlan() {
        InputSessionController<String> controller = new InputSessionController<>(3);
        long firstGeneration = controller.start("old-neutral", START, RICH);
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.runAt(2, () -> controller.start("new-neutral", START, RICH));

        ExecutionResult result = controller.execute(
            controller.plan(
                DispatchResult.handled(KeyAction.commitText("x")),
                "old-proposed",
                ONE
            ),
            () -> EditorEndpoint.of(firstGeneration, bridge)
        );

        Assert.assertEquals(
            ExecutionResult.Reason.SESSION_CHANGED_DURING_EXECUTION,
            result.reason()
        );
        Assert.assertEquals(ExecutionResult.Outcome.UNCERTAIN, result.outcome());
        Assert.assertEquals(firstGeneration + 1, controller.generation());
        Assert.assertEquals(0, controller.revision());
        Assert.assertEquals("new-neutral", controller.currentState());
        Assert.assertEquals(SynchronizationState.SYNCED, controller.syncState());
    }

    @Test
    public void generationChangeStopsRemainingActionsButStillEndsBatch() {
        InputSessionController<String> controller = new InputSessionController<>(3);
        long firstGeneration = controller.start("old-neutral", START, RICH);
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.runAt(2, () -> controller.start("new-neutral", START, RICH));
        TransitionPlan<String> plan = controller.plan(
            DispatchResult.handled(
                KeyAction.commitText("first"),
                KeyAction.setComposingText("must-not-run"),
                KeyAction.finishComposing()
            ),
            "old-proposed",
            ONE
        );

        ExecutionResult result = controller.execute(
            plan,
            () -> EditorEndpoint.of(firstGeneration, bridge)
        );

        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit",
            "commitText:length=5:cursor=1",
            "endBatchEdit"
        ), bridge.trace());
        Assert.assertEquals(
            ExecutionResult.Reason.SESSION_CHANGED_DURING_EXECUTION,
            result.reason()
        );
        Assert.assertEquals("new-neutral", controller.currentState());
    }

    @Test
    public void stopDuringEditorCallCannotAdoptOldPlan() {
        InputSessionController<String> controller = new InputSessionController<>(3);
        long generation = controller.start("neutral", START, RICH);
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.runAt(2, controller::stopAccepting);

        ExecutionResult result = controller.execute(
            controller.plan(
                DispatchResult.handled(KeyAction.commitText("x")),
                "old-proposed",
                ONE
            ),
            () -> EditorEndpoint.of(generation, bridge)
        );

        Assert.assertEquals(
            ExecutionResult.Reason.SESSION_CHANGED_DURING_EXECUTION,
            result.reason()
        );
        Assert.assertEquals("neutral", controller.currentState());
        Assert.assertEquals(SynchronizationState.STOPPED, controller.syncState());
    }

    @Test
    public void nestedExecutionIsRejectedWithoutResolvingAnotherConnection() {
        InputSessionController<String> controller = new InputSessionController<>(3);
        long generation = controller.start("neutral", START, RICH);
        FakeEditorBridge bridge = new FakeEditorBridge();
        AtomicInteger nestedResolves = new AtomicInteger();
        java.util.concurrent.atomic.AtomicReference<ExecutionResult> nested =
            new java.util.concurrent.atomic.AtomicReference<>();
        bridge.runAt(2, () -> nested.set(controller.execute(
            controller.plan(
                DispatchResult.handled(KeyAction.commitText("nested")),
                "nested-proposed",
                ONE
            ),
            () -> {
                nestedResolves.incrementAndGet();
                return EditorEndpoint.of(generation, new FakeEditorBridge());
            }
        )));

        controller.execute(
            controller.plan(
                DispatchResult.handled(KeyAction.commitText("outer")),
                "outer-proposed",
                ONE
            ),
            () -> EditorEndpoint.of(generation, bridge)
        );

        Assert.assertEquals(ExecutionResult.Reason.REENTRANT_EXECUTION, nested.get().reason());
        Assert.assertEquals(0, nestedResolves.get());
        Assert.assertEquals("outer-proposed", controller.currentState());
    }

    @Test
    public void tentativeCursorZeroNeverSuppressesFollowingDeleteAsConfirmedNoEffect() {
        InputSessionController<String> controller = new InputSessionController<>(3);
        EditorBounds cursorOne = EditorBounds.of(1, 1, -1, -1);
        long generation = controller.start("neutral", cursorOne, RICH);
        executeSuccess(controller, generation, "tentative-zero", START);
        FakeEditorBridge deleteBridge = new FakeEditorBridge();

        ExecutionResult result = controller.execute(
            controller.plan(
                DispatchResult.handled(KeyAction.deleteBackward()),
                "after-delete",
                START
            ),
            () -> EditorEndpoint.of(generation, deleteBridge)
        );

        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit",
            "deleteCodePoints:before=1:after=0",
            "endBatchEdit"
        ), deleteBridge.trace());
    }

    private static void executeSuccess(
        InputSessionController<String> controller,
        long generation,
        String proposed,
        EditorBounds expected
    ) {
        executeSuccess(controller, generation, proposed, expected, new FakeEditorBridge());
    }

    private static void executeSuccess(
        InputSessionController<String> controller,
        long generation,
        String proposed,
        EditorBounds expected,
        FakeEditorBridge bridge
    ) {
        ExecutionResult result = controller.execute(
            controller.plan(
                DispatchResult.handled(KeyAction.commitText("x")),
                proposed,
                expected
            ),
            () -> EditorEndpoint.of(generation, bridge)
        );
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
    }
}
