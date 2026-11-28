package com.retekey;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;
import com.retekey.core.CheckedEditorExecutor;
import com.retekey.core.DispatchResult;
import com.retekey.core.EditorBounds;
import com.retekey.core.EditorCallResult;
import com.retekey.core.EditorCapabilities;
import com.retekey.core.EditorEndpoint;
import com.retekey.core.EditorTextResult;
import com.retekey.core.ExecutionContext;
import com.retekey.core.ExecutionResult;
import com.retekey.core.KeyAction;
import com.retekey.core.TransitionPlan;

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
    public void aSensitiveEditorIsComposedIntoLikeAnyOther() {
        // It did not use to be: composing was refused outright in a password field, which meant
        // Korean could not be typed into one at all — nor into a terminal, which reports the
        // visible-password variation to turn suggestions off (issue #7). Being private is about
        // what the keyboard reads and remembers, not about whether it may compose.
        AtomicInteger resolves = new AtomicInteger();
        TransitionPlan<String> plan = plan(
            1,
            0,
            Collections.singletonList(KeyAction.setComposingText("가"))
        );

        ExecutionResult result = EXECUTOR.execute(
            plan,
            ExecutionContext.active(1, 0, CURSOR, EditorCapabilities.richText(true, false)),
            () -> {
                resolves.incrementAndGet();
                return EditorEndpoint.of(1, new FakeEditorBridge());
            }
        );

        Assert.assertEquals(1, resolves.get());
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
    }

    @Test
    public void anUnsupportedEditorIsRefusedBeforeTheConnectionIsResolved() {
        // The check that is still worth making early: nothing is asked of an editor that cannot
        // take it, and asking costs a connection lookup.
        AtomicInteger resolves = new AtomicInteger();
        TransitionPlan<String> plan = plan(
            1,
            0,
            Collections.singletonList(KeyAction.setComposingText("가"))
        );

        ExecutionResult result = EXECUTOR.execute(
            plan,
            ExecutionContext.active(1, 0, CURSOR, EditorCapabilities.unsupported()),
            () -> {
                resolves.incrementAndGet();
                return EditorEndpoint.of(1, new FakeEditorBridge());
            }
        );

        Assert.assertEquals(0, resolves.get());
        Assert.assertEquals(ExecutionResult.Reason.UNSUPPORTED_EDITOR, result.reason());
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

    @Test
    public void aRecomposeMarksTheRegionAndReplacesItWithoutDeleting() {
        // 나랏글's ㅈ is ㅅ with a stroke, and making it takes back the syllable the ㅅ closed. The
        // three-call way — clear, delete, compose — lost that syllable in an editor that ran the
        // calls out of order (owner's report, 2026-09-17). One region does it with no delete.
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.setModel("신ㅅ", EditorBounds.of(2, 2, 1, 2));

        ExecutionResult result = EXECUTOR.execute(
            plan(2, 3, Collections.singletonList(KeyAction.recomposePrevious(2, "신"))),
            ExecutionContext.active(2, 3, EditorBounds.of(2, 2, 1, 2), RICH),
            () -> EditorEndpoint.of(2, bridge)
        );

        Assert.assertFalse(result.isFailure());
        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit",
            "setComposingRegion:0..2",
            "setComposingText:length=1:cursor=1",
            "endBatchEdit"
        ), bridge.trace());
        Assert.assertEquals("신", bridge.modelText());
    }

    @Test
    public void anEditorThatWillNotMarkARegionStillGetsItsSyllableBack() {
        // The older way is the fallback, not the default: clear the composition, take back the one
        // character behind it, and compose the pair again.
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.setModel("신ㅅ", EditorBounds.of(2, 2, 1, 2));
        bridge.returnAt(2, EditorCallResult.rejected());

        ExecutionResult result = EXECUTOR.execute(
            plan(2, 3, Collections.singletonList(KeyAction.recomposePrevious(2, "신"))),
            ExecutionContext.active(2, 3, EditorBounds.of(2, 2, 1, 2), RICH),
            () -> EditorEndpoint.of(2, bridge)
        );

        Assert.assertFalse(result.isFailure());
        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit",
            "setComposingRegion:0..2",
            "setComposingText:length=0:cursor=1",
            "deleteCodePoints:before=1:after=0",
            "setComposingText:length=1:cursor=1",
            "endBatchEdit"
        ), bridge.trace());
        Assert.assertEquals("신", bridge.modelText());
    }

    @Test
    public void rawKeyTerminalExecutesMultiDeleteRecentAndRecomposeThroughRichPlan() {
        FakeEditorBridge deleteBridge = new FakeEditorBridge();
        ExecutionResult deleted = EXECUTOR.execute(
            plan(1, 0, Collections.singletonList(KeyAction.deleteRecent(3))),
            ExecutionContext.active(1, 0, EditorBounds.unknown(),
                EditorCapabilities.rawKey().asTerminal()),
            () -> EditorEndpoint.of(1, deleteBridge)
        );
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, deleted.outcome());
        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit",
            "commitText:length=3:cursor=1",
            "endBatchEdit"
        ), deleteBridge.trace());

        FakeEditorBridge recomposeBridge = new FakeEditorBridge();
        ExecutionResult recomposed = EXECUTOR.execute(
            plan(1, 0, Collections.singletonList(KeyAction.recomposePrevious(2, "재"))),
            ExecutionContext.active(1, 0, EditorBounds.unknown(),
                EditorCapabilities.rawKey().asTerminal()),
            () -> EditorEndpoint.of(1, recomposeBridge)
        );
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, recomposed.outcome());
        Assert.assertTrue(recomposed.dispatchedMutationCount() > 0);
    }

    @Test
    public void recomposeReportsMutationOnLateStaleAndDoesNotFallThroughOnRuntimeFailure() {
        // Primary path: setComposingRegion succeeds, then session goes stale before setComposingText.
        FakeEditorBridge staleBridge = new FakeEditorBridge();
        java.util.concurrent.atomic.AtomicBoolean current =
            new java.util.concurrent.atomic.AtomicBoolean(true);
        staleBridge.runAt(2, () -> current.set(false)); // after setComposingRegion
        ExecutionResult staleResult = EXECUTOR.execute(
            plan(1, 0, Collections.singletonList(KeyAction.recomposePrevious(1, "가"))),
            ExecutionContext.active(1, 0, EditorBounds.of(1, 1, -1, -1), RICH),
            () -> EditorEndpoint.of(1, staleBridge).guardedBy(current::get)
        );
        Assert.assertEquals(ExecutionResult.Outcome.UNCERTAIN, staleResult.outcome());
        Assert.assertTrue(staleResult.remoteMutationMayHaveOccurred());

        // Primary path runtime failure on setComposingRegion must not fall through to clearing.
        FakeEditorBridge throwBridge = new FakeEditorBridge();
        throwBridge.throwAt(2);
        ExecutionResult throwResult = EXECUTOR.execute(
            plan(1, 0, Collections.singletonList(KeyAction.recomposePrevious(1, "가"))),
            ExecutionContext.active(1, 0, EditorBounds.of(1, 1, -1, -1), RICH),
            () -> EditorEndpoint.of(1, throwBridge)
        );
        Assert.assertEquals(ExecutionResult.Outcome.UNCERTAIN, throwResult.outcome());
        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit",
            "setComposingRegion:0..1",
            "endBatchEdit"
        ), throwBridge.trace());

        // Fallback path falls back to deleteSurroundingText when code-point delete is rejected.
        FakeEditorBridge legacyBridge = new FakeEditorBridge();
        legacyBridge.returnAt(2, EditorCallResult.rejected()); // setComposingRegion
        legacyBridge.returnAt(4, EditorCallResult.rejected()); // deleteSurroundingTextInCodePoints
        ExecutionResult legacyResult = EXECUTOR.execute(
            plan(1, 0, Collections.singletonList(KeyAction.recomposePrevious(2, "재"))),
            ExecutionContext.active(1, 0, EditorBounds.of(2, 2, -1, -1),
                EditorCapabilities.richText(false, true)),
            () -> EditorEndpoint.of(1, legacyBridge)
        );
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, legacyResult.outcome());
        Assert.assertTrue(legacyBridge.trace().contains("deleteUtf16:before=1:after=0"));
    }

    @Test
    public void aDeleteRecentRefusedInCodePointsFallsBackToUtf16() {
        // Below API 24 the code-point delete does not exist, and a remote-desktop editor on the
        // legacy build refused every take-back of a syllable it had been given (RFC-0014 F6).
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.setModel("ㄱ", EditorBounds.of(1, 1, -1, -1));
        bridge.returnAt(2, EditorCallResult.rejected()); // deleteSurroundingTextInCodePoints

        ExecutionResult result = EXECUTOR.execute(
            plan(1, 0, Collections.singletonList(KeyAction.deleteRecent(1))),
            ExecutionContext.active(1, 0, EditorBounds.of(1, 1, -1, -1), RICH),
            () -> EditorEndpoint.of(1, bridge)
        );

        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
        Assert.assertTrue(bridge.trace().toString(),
            bridge.trace().contains("deleteUtf16:before=1:after=0"));
        Assert.assertEquals("", bridge.modelText());
    }

    @Test
    public void aColdRecomposeFallbackReplacesTheCharacterInsteadOfAddingOne() {
        // 나랏글's stroke on a committed 가 with nothing composing: one character before the cursor,
        // none of it composing. The fallback took one back for the composition it assumed, deleted
        // nothing, and typed 갸 after 가 (RFC-0014 F7).
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.setModel("가", EditorBounds.of(1, 1, -1, -1));
        bridge.returnAt(2, EditorCallResult.rejected()); // setComposingRegion

        ExecutionResult result = EXECUTOR.execute(
            plan(1, 0, Collections.singletonList(KeyAction.recomposePrevious(1, 0, "갸"))),
            ExecutionContext.active(1, 0, EditorBounds.of(1, 1, -1, -1), RICH),
            () -> EditorEndpoint.of(1, bridge)
        );

        Assert.assertFalse(result.isFailure());
        Assert.assertEquals("갸", bridge.modelText());
    }

    @Test
    public void focusActionStaleSessionWithNoPriorWritesIsNotDispatched() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        java.util.concurrent.atomic.AtomicInteger checks = new java.util.concurrent.atomic.AtomicInteger();
        ExecutionResult result = EXECUTOR.execute(
            plan(1, 0, Collections.singletonList(KeyAction.performEditorAction(6))),
            ExecutionContext.active(1, 0, CURSOR, RICH),
            () -> EditorEndpoint.of(1, bridge).guardedBy(() -> checks.incrementAndGet() <= 1)
        );
        Assert.assertEquals(ExecutionResult.Outcome.NOT_DISPATCHED, result.outcome());
        Assert.assertEquals(
            ExecutionResult.Reason.SESSION_CHANGED_DURING_EXECUTION, result.reason());
        Assert.assertFalse(result.remoteMutationMayHaveOccurred());
    }

    @Test
    public void remoteDesktopRichDeleteRuntimeFailureNeverDoubleDeletesWithBackspace() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.setTextBeforeCursor(EditorTextResult.value("a"));
        bridge.throwAt(3); // beginBatchEdit(1), getTextBeforeCursor(2), deleteCodePoints(3)
        ExecutionResult result = EXECUTOR.execute(
            plan(1, 0, Collections.singletonList(KeyAction.deleteBackward())),
            ExecutionContext.active(1, 0, CURSOR, RICH.withDeleteByKeyEvents()),
            () -> EditorEndpoint.of(1, bridge)
        );
        Assert.assertEquals(ExecutionResult.Outcome.UNCERTAIN, result.outcome());
        Assert.assertFalse(bridge.trace().toString().contains("BACKSPACE"));
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
