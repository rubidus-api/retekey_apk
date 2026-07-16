package dev.hellgates.retekeyime;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

public final class CheckedEditorExecutorTest {
    private static final CheckedEditorExecutor EXECUTOR = new CheckedEditorExecutor();
    private static final EditorBounds CURSOR = EditorBounds.of(2, 2, -1, -1);
    private static final EditorCapabilities RICH = EditorCapabilities.richText(false, false);

    @Test
    public void dispatchesOrderedMutationsInsideOneBalancedBatch() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        AtomicInteger resolves = new AtomicInteger();
        TransitionPlan<String> plan = plan(
            2,
            3,
            Arrays.asList(KeyAction.commitText("x"), KeyAction.setComposingText("가"))
        );

        ExecutionResult result = EXECUTOR.execute(
            plan,
            ExecutionContext.active(2, 3, CURSOR, RICH),
            () -> {
                resolves.incrementAndGet();
                return EditorEndpoint.of(2, bridge);
            }
        );

        Assert.assertEquals(1, resolves.get());
        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit",
            "commitText:length=1:cursor=1",
            "setComposingText:length=1:cursor=1",
            "endBatchEdit"
        ), bridge.trace());
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
        Assert.assertEquals(ExecutionResult.Reason.NONE, result.reason());
        Assert.assertEquals(
            ExecutionResult.StateEffect.ADOPT_PROPOSED_AWAITING_CONFIRMATION,
            result.stateEffect()
        );
        Assert.assertEquals(2, result.dispatchedMutationCount());
        Assert.assertFalse(result.remoteMutationMayHaveOccurred() && result.isFailure());
    }

    @Test
    public void falseOrRuntimeBatchBeginStillEndsExactlyOnceAndRunsNoAction() {
        for (EditorCallResult failure : Arrays.asList(
            EditorCallResult.rejected(),
            EditorCallResult.runtimeFailure()
        )) {
            FakeEditorBridge bridge = new FakeEditorBridge();
            bridge.returnAt(1, failure);

            ExecutionResult result = EXECUTOR.execute(
                plan(1, 0, Collections.singletonList(KeyAction.commitText("secret"))),
                ExecutionContext.active(1, 0, CURSOR, RICH),
                () -> EditorEndpoint.of(1, bridge)
            );

            Assert.assertEquals(
                Arrays.asList("beginBatchEdit", "endBatchEdit"),
                bridge.trace()
            );
            Assert.assertEquals(
                failure.isRejected()
                    ? ExecutionResult.Outcome.NOT_DISPATCHED
                    : ExecutionResult.Outcome.UNCERTAIN,
                result.outcome()
            );
            Assert.assertEquals(
                failure.isRejected()
                    ? ExecutionResult.Reason.BATCH_BEGIN_FALSE
                    : ExecutionResult.Reason.BATCH_BEGIN_RUNTIME_FAILURE,
                result.reason()
            );
            Assert.assertEquals(0, result.failedOperationIndex());
            Assert.assertEquals(-1, result.cleanupOperationIndex());
            Assert.assertEquals(2, result.operationCount());
            Assert.assertEquals(
                failure.isRejected()
                    ? ExecutionResult.StateEffect.KEEP_CURRENT
                    : ExecutionResult.StateEffect.RESET_DESYNCHRONIZED,
                result.stateEffect()
            );
            Assert.assertEquals(!failure.isRejected(), result.remoteMutationMayHaveOccurred());
        }
    }

    @Test
    public void stopsAfterFirstFailedMutationAndReportsUnknownRemoteState() {
        for (EditorCallResult failure : Arrays.asList(
            EditorCallResult.rejected(),
            EditorCallResult.runtimeFailure()
        )) {
            FakeEditorBridge bridge = new FakeEditorBridge();
            bridge.returnAt(3, failure);

            ExecutionResult result = EXECUTOR.execute(
                plan(1, 0, Arrays.asList(
                    KeyAction.commitText("first"),
                    KeyAction.setComposingText("second"),
                    KeyAction.finishComposing()
                )),
                ExecutionContext.active(1, 0, CURSOR, RICH),
                () -> EditorEndpoint.of(1, bridge)
            );

            Assert.assertEquals(Arrays.asList(
                "beginBatchEdit",
                "commitText:length=5:cursor=1",
                "setComposingText:length=6:cursor=1",
                "endBatchEdit"
            ), bridge.trace());
            Assert.assertEquals(ExecutionResult.Outcome.UNCERTAIN, result.outcome());
            Assert.assertEquals(2, result.failedOperationIndex());
            Assert.assertEquals(-1, result.cleanupOperationIndex());
            Assert.assertEquals(4, result.operationCount());
            Assert.assertEquals(
                failure.isRejected()
                    ? ExecutionResult.Reason.OPERATION_FALSE
                    : ExecutionResult.Reason.OPERATION_RUNTIME_FAILURE,
                result.reason()
            );
            Assert.assertEquals(1, result.failedActionIndex());
            Assert.assertEquals(1, result.dispatchedMutationCount());
            Assert.assertTrue(result.remoteMutationMayHaveOccurred());
            Assert.assertEquals(
                ExecutionResult.StateEffect.RESET_DESYNCHRONIZED,
                result.stateEffect()
            );
        }
    }

    @Test
    public void failedBatchEndInvalidatesOtherwiseSuccessfulDispatch() {
        for (EditorCallResult failure : Arrays.asList(
            EditorCallResult.rejected(),
            EditorCallResult.runtimeFailure()
        )) {
            FakeEditorBridge bridge = new FakeEditorBridge();
            bridge.returnAt(3, failure);

            ExecutionResult result = EXECUTOR.execute(
                plan(1, 0, Collections.singletonList(KeyAction.commitText("x"))),
                ExecutionContext.active(1, 0, CURSOR, RICH),
                () -> EditorEndpoint.of(1, bridge)
            );

            Assert.assertEquals(Arrays.asList(
                "beginBatchEdit",
                "commitText:length=1:cursor=1",
                "endBatchEdit"
            ), bridge.trace());
            Assert.assertEquals(
                failure.isRejected()
                    ? ExecutionResult.Reason.BATCH_END_FALSE
                    : ExecutionResult.Reason.BATCH_END_RUNTIME_FAILURE,
                result.reason()
            );
            Assert.assertEquals(ExecutionResult.Outcome.UNCERTAIN, result.outcome());
            Assert.assertEquals(2, result.failedOperationIndex());
            Assert.assertEquals(2, result.cleanupOperationIndex());
            Assert.assertEquals(3, result.operationCount());
            Assert.assertEquals(
                ExecutionResult.StateEffect.RESET_DESYNCHRONIZED,
                result.stateEffect()
            );
        }
    }

    @Test
    public void primaryOperationFailureIsNotHiddenByCleanupFailure() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.returnAt(2, EditorCallResult.rejected());
        bridge.returnAt(3, EditorCallResult.runtimeFailure());

        ExecutionResult result = EXECUTOR.execute(
            plan(1, 0, Collections.singletonList(KeyAction.commitText("x"))),
            ExecutionContext.active(1, 0, CURSOR, RICH),
            () -> EditorEndpoint.of(1, bridge)
        );

        Assert.assertEquals(ExecutionResult.Reason.OPERATION_FALSE, result.reason());
        Assert.assertEquals(
            ExecutionResult.Reason.BATCH_END_RUNTIME_FAILURE,
            result.cleanupReason()
        );
    }

    @Test
    public void connectionResolverRuntimeFailureIsContained() {
        ExecutionResult result = EXECUTOR.execute(
            plan(1, 0, Collections.singletonList(KeyAction.commitText("private"))),
            ExecutionContext.active(1, 0, CURSOR, RICH),
            () -> {
                throw new IllegalStateException("private exception detail");
            }
        );

        Assert.assertEquals(
            ExecutionResult.Reason.CONNECTION_RESOLUTION_RUNTIME_FAILURE,
            result.reason()
        );
        Assert.assertFalse(result.toString().contains("private"));
    }

    @Test
    public void actualThrowOrNullEditorResultIsContainedAndBatchIsEnded() {
        for (boolean actualThrow : Arrays.asList(false, true)) {
            FakeEditorBridge bridge = new FakeEditorBridge();
            String privateText = "never-log-editor-content";
            if (actualThrow) {
                bridge.throwAt(2);
            } else {
                bridge.returnNullAt(2);
            }

            ExecutionResult result = EXECUTOR.execute(
                plan(1, 0, Arrays.asList(
                    KeyAction.commitText(privateText),
                    KeyAction.finishComposing()
                )),
                ExecutionContext.active(1, 0, CURSOR, RICH),
                () -> EditorEndpoint.of(1, bridge)
            );

            Assert.assertEquals(Arrays.asList(
                "beginBatchEdit",
                "commitText:length=" + privateText.length() + ":cursor=1",
                "endBatchEdit"
            ), bridge.trace());
            Assert.assertEquals(
                ExecutionResult.Reason.OPERATION_RUNTIME_FAILURE,
                result.reason()
            );
            Assert.assertEquals(
                Arrays.asList(KeyAction.Kind.COMMIT_TEXT, KeyAction.Kind.FINISH_COMPOSING),
                result.actionKinds()
            );
            Assert.assertEquals(CURSOR, result.stateBounds());
            Assert.assertFalse(result.toString().contains(privateText));
        }
    }

    @Test
    public void falseAndRuntimeFailpointsStopAtEveryMutationPosition() {
        for (EditorCallResult failure : Arrays.asList(
            EditorCallResult.rejected(),
            EditorCallResult.runtimeFailure()
        )) {
            for (int failingCall = 2; failingCall <= 4; failingCall++) {
                FakeEditorBridge bridge = new FakeEditorBridge();
                bridge.returnAt(failingCall, failure);

                ExecutionResult result = EXECUTOR.execute(
                    plan(1, 0, Arrays.asList(
                        KeyAction.commitText("a"),
                        KeyAction.setComposingText("b"),
                        KeyAction.finishComposing()
                    )),
                    ExecutionContext.active(1, 0, CURSOR, RICH),
                    () -> EditorEndpoint.of(1, bridge)
                );

                Assert.assertEquals(failingCall + 1, bridge.callCount());
                Assert.assertEquals("endBatchEdit", bridge.trace().get(bridge.trace().size() - 1));
                Assert.assertEquals(failingCall - 1, result.failedOperationIndex());
                Assert.assertEquals(failingCall + 1, result.operationCount());
                Assert.assertEquals(ExecutionResult.Outcome.UNCERTAIN, result.outcome());
            }
        }
    }

    @Test
    public void executorTracesEmptyComposingAndFinishSemantics() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.setModel("a가b", EditorBounds.of(2, 2, 1, 2));

        ExecutionResult result = EXECUTOR.execute(
            plan(1, 0, Arrays.asList(
                KeyAction.setComposingText(""),
                KeyAction.finishComposing()
            )),
            ExecutionContext.active(
                1,
                0,
                EditorBounds.of(2, 2, 1, 2),
                RICH
            ),
            () -> EditorEndpoint.of(1, bridge)
        );

        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit",
            "setComposingText:length=0:cursor=1",
            "finishComposingText",
            "endBatchEdit"
        ), bridge.trace());
        Assert.assertEquals("ab", bridge.modelText());
        Assert.assertEquals(EditorBounds.of(1, 1, -1, -1), bridge.modelBounds());
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
    }

    @Test
    public void laterActionsUseBoundsPredictedFromEarlierMutations() {
        EditorBounds selected = EditorBounds.of(1, 3, -1, -1);
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.setModel("abcd", selected);

        ExecutionResult result = EXECUTOR.execute(
            plan(1, 0, Arrays.asList(
                KeyAction.commitText("x"),
                KeyAction.deleteBackward()
            )),
            ExecutionContext.active(1, 0, selected, RICH),
            () -> EditorEndpoint.of(1, bridge)
        );

        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit",
            "commitText:length=1:cursor=1",
            "deleteCodePoints:before=1:after=0",
            "endBatchEdit"
        ), bridge.trace());
        Assert.assertEquals("ad", bridge.modelText());
        Assert.assertEquals(EditorBounds.of(1, 1, -1, -1), bridge.modelBounds());
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
    }

    @Test
    public void preflightFailuresNeverResolveOrMutateAnEditor() {
        assertPreflightFailure(
            ExecutionContext.stopped(4, 2, CURSOR, RICH),
            plan(4, 2, Collections.singletonList(KeyAction.commitText("x"))),
            ExecutionResult.Reason.SESSION_STOPPED
        );
        assertPreflightFailure(
            ExecutionContext.active(4, 2, CURSOR, RICH),
            plan(3, 2, Collections.singletonList(KeyAction.commitText("x"))),
            ExecutionResult.Reason.STALE_GENERATION
        );
        assertPreflightFailure(
            ExecutionContext.active(4, 2, CURSOR, RICH),
            plan(4, 1, Collections.singletonList(KeyAction.commitText("x"))),
            ExecutionResult.Reason.STALE_REVISION
        );
        assertPreflightFailure(
            ExecutionContext.active(4, 2, CURSOR, EditorCapabilities.unsupported()),
            plan(4, 2, Collections.singletonList(KeyAction.commitText("x"))),
            ExecutionResult.Reason.UNSUPPORTED_EDITOR
        );
    }

    @Test
    public void nullOrWrongGenerationEndpointIsExplicitAndNeverCalled() {
        TransitionPlan<String> plan = plan(
            6,
            1,
            Collections.singletonList(KeyAction.commitText("x"))
        );
        AtomicInteger nullResolves = new AtomicInteger();

        ExecutionResult missing = EXECUTOR.execute(
            plan,
            ExecutionContext.active(6, 1, CURSOR, RICH),
            () -> {
                nullResolves.incrementAndGet();
                return null;
            }
        );
        FakeEditorBridge staleBridge = new FakeEditorBridge();
        ExecutionResult stale = EXECUTOR.execute(
            plan,
            ExecutionContext.active(6, 1, CURSOR, RICH),
            () -> EditorEndpoint.of(5, staleBridge)
        );

        Assert.assertEquals(1, nullResolves.get());
        Assert.assertEquals(ExecutionResult.Reason.NO_CONNECTION, missing.reason());
        Assert.assertEquals(ExecutionResult.Reason.ENDPOINT_GENERATION_MISMATCH, stale.reason());
        Assert.assertTrue(staleBridge.trace().isEmpty());
    }

    @Test
    public void actionlessPlanAdoptsWithoutResolvingConnection() {
        AtomicInteger resolves = new AtomicInteger();

        ExecutionResult result = EXECUTOR.execute(
            plan(2, 7, Collections.emptyList()),
            ExecutionContext.active(2, 7, EditorBounds.unknown(), RICH),
            () -> {
                resolves.incrementAndGet();
                return null;
            }
        );

        Assert.assertEquals(0, resolves.get());
        Assert.assertEquals(ExecutionResult.Outcome.NO_EDITOR_ACTIONS, result.outcome());
        Assert.assertEquals(
            ExecutionResult.StateEffect.ADOPT_PROPOSED_SYNCED,
            result.stateEffect()
        );
    }

    @Test
    public void contextIndependentFocusActionWorksWithoutSelection() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        TransitionPlan<String> plan = TransitionPlan.of(
            1,
            0,
            DispatchResult.Disposition.HANDLED,
            "state",
            EditorBounds.unknown(),
            Collections.singletonList(KeyAction.performEditorAction(6))
        );

        ExecutionResult result = EXECUTOR.execute(
            plan,
            ExecutionContext.active(1, 0, EditorBounds.unknown(), RICH),
            () -> EditorEndpoint.of(1, bridge)
        );

        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
        Assert.assertEquals(
            Collections.singletonList("performEditorAction:id=6"),
            bridge.trace()
        );
    }

    @Test
    public void sensitiveEditorRejectsComposingBeforeResolvingConnection() {
        AtomicInteger resolves = new AtomicInteger();
        TransitionPlan<String> plan = plan(
            1,
            0,
            Collections.singletonList(KeyAction.setComposingText("private"))
        );

        ExecutionResult result = EXECUTOR.execute(
            plan,
            ExecutionContext.active(
                1,
                0,
                CURSOR,
                EditorCapabilities.richText(true, false)
            ),
            () -> {
                resolves.incrementAndGet();
                return EditorEndpoint.of(1, new FakeEditorBridge());
            }
        );

        Assert.assertEquals(0, resolves.get());
        Assert.assertEquals(
            ExecutionResult.Reason.SENSITIVE_OPERATION_PROHIBITED,
            result.reason()
        );
    }

    @Test
    public void commitTextSucceedsWithUnknownSelection() {
        // Terminals (e.g. Termius) never report a selection; committing text must still work.
        FakeEditorBridge bridge = new FakeEditorBridge();
        ExecutionResult result = EXECUTOR.execute(
            plan(1, 0, Collections.singletonList(KeyAction.commitText("hi"))),
            ExecutionContext.active(1, 0, EditorBounds.unknown(), RICH),
            () -> EditorEndpoint.of(1, bridge)
        );

        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
        Assert.assertTrue(
            "the commit reached the editor",
            bridge.trace().contains("commitText:length=2:cursor=1"));
    }

    @Test
    public void commitTextReachesRawKeyTerminalEditor() {
        // TYPE_NULL editors (RAW_KEY caps, e.g. a terminal) use key events for deletion but still
        // receive plain committed text through the ordinary commit path.
        FakeEditorBridge bridge = new FakeEditorBridge();
        ExecutionResult result = EXECUTOR.execute(
            plan(1, 0, Collections.singletonList(KeyAction.commitText("hi"))),
            ExecutionContext.active(1, 0, EditorBounds.unknown(), EditorCapabilities.rawKey()),
            () -> EditorEndpoint.of(1, bridge)
        );

        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
        Assert.assertTrue(
            "the commit reached the terminal editor",
            bridge.trace().contains("commitText:length=2:cursor=1"));
    }

    @Test
    public void deletionWithUnknownSelectionStillDispatches() {
        // deleteSurroundingText is cursor-relative, so backspace works even when the IME does not
        // know the selection (terminals reporting -1). It must not be refused.
        FakeEditorBridge bridge = new FakeEditorBridge();
        TransitionPlan<String> plan = TransitionPlan.of(
            1,
            0,
            DispatchResult.Disposition.HANDLED,
            "state",
            EditorBounds.unknown(),
            Collections.singletonList(KeyAction.deleteBackward())
        );

        ExecutionResult result = EXECUTOR.execute(
            plan,
            ExecutionContext.active(1, 0, EditorBounds.unknown(), RICH),
            () -> EditorEndpoint.of(1, bridge)
        );

        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
    }

    private static void assertPreflightFailure(
        ExecutionContext context,
        TransitionPlan<String> plan,
        ExecutionResult.Reason expected
    ) {
        AtomicInteger resolves = new AtomicInteger();
        ExecutionResult result = EXECUTOR.execute(plan, context, () -> {
            resolves.incrementAndGet();
            return EditorEndpoint.of(context.generation(), new FakeEditorBridge());
        });

        Assert.assertEquals(0, resolves.get());
        Assert.assertEquals(expected, result.reason());
        Assert.assertEquals(ExecutionResult.Outcome.NOT_DISPATCHED, result.outcome());
    }

    private static TransitionPlan<String> plan(
        long generation,
        long revision,
        java.util.List<KeyAction> actions
    ) {
        return TransitionPlan.of(
            generation,
            revision,
            DispatchResult.Disposition.HANDLED,
            "state",
            CURSOR,
            actions
        );
    }
}
