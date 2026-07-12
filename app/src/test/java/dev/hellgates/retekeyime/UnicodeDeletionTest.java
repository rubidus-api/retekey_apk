package dev.hellgates.retekeyime;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

public final class UnicodeDeletionTest {
    private static final CheckedEditorExecutor EXECUTOR = new CheckedEditorExecutor();

    @Test
    public void selectedTextUsesOneEmptyCommitForEitherSelectionDirection() {
        for (EditorBounds bounds : Arrays.asList(
            EditorBounds.of(2, 5, -1, -1),
            EditorBounds.of(5, 2, -1, -1)
        )) {
            FakeEditorBridge bridge = new FakeEditorBridge();

            ExecutionResult result = execute(
                bridge,
                bounds,
                EditorCapabilities.richText(false, false)
            );

            Assert.assertEquals(Arrays.asList(
                "beginBatchEdit",
                "commitText:length=0:cursor=1",
                "endBatchEdit"
            ), bridge.trace());
            Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
        }
    }

    @Test
    public void cursorAtStartIsConfirmedNoEffectWithoutResolvingConnection() {
        AtomicInteger resolves = new AtomicInteger();

        ExecutionResult result = EXECUTOR.execute(
            deletePlan(EditorBounds.of(0, 0, -1, -1)),
            ExecutionContext.active(
                1,
                0,
                EditorBounds.of(0, 0, -1, -1),
                EditorCapabilities.richText(false, false)
            ),
            () -> {
                resolves.incrementAndGet();
                return EditorEndpoint.of(1, new FakeEditorBridge());
            }
        );

        Assert.assertEquals(0, resolves.get());
        Assert.assertEquals(ExecutionResult.Outcome.CONFIRMED_NO_EFFECT, result.outcome());
        Assert.assertEquals(
            ExecutionResult.StateEffect.ADOPT_PROPOSED_SYNCED,
            result.stateEffect()
        );
    }

    @Test
    public void ordinaryRichDeletionUsesExactlyOneCodePointRequest() {
        FakeEditorBridge bridge = new FakeEditorBridge();

        ExecutionResult result = execute(
            bridge,
            EditorBounds.of(3, 3, -1, -1),
            EditorCapabilities.richText(false, false)
        );

        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit",
            "deleteCodePoints:before=1:after=0",
            "endBatchEdit"
        ), bridge.trace());
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
    }

    @Test
    public void legacyUnimplementedMethodFallsBackOnceForBmpOrSurrogatePair() {
        assertLegacyFallback("A", 1);
        assertLegacyFallback("\ud83d\ude00", 2);
    }

    @Test
    public void legacyFallbackDoesNotGuessFromMissingOrMalformedContext() {
        for (EditorTextResult context : Arrays.asList(
            EditorTextResult.nullValue(),
            EditorTextResult.value("\ud83d"),
            EditorTextResult.value("\ude00"),
            EditorTextResult.value("\ud83dA"),
            EditorTextResult.value("A\ude00"),
            EditorTextResult.runtimeFailure()
        )) {
            FakeEditorBridge bridge = new FakeEditorBridge();
            bridge.returnAt(2, EditorCallResult.rejected());
            bridge.setTextBeforeCursor(context);

            ExecutionResult result = execute(
                bridge,
                EditorBounds.of(2, 2, -1, -1),
                EditorCapabilities.richText(false, true)
            );

            Assert.assertEquals(Arrays.asList(
                "beginBatchEdit",
                "deleteCodePoints:before=1:after=0",
                "getTextBeforeCursor:max=2:flags=0",
                "endBatchEdit"
            ), bridge.trace());
            Assert.assertEquals(
                ExecutionResult.Reason.INVALID_SURROUNDING_TEXT,
                result.reason()
            );
            Assert.assertFalse(result.remoteMutationMayHaveOccurred());
        }
    }

    @Test
    public void emptyLegacyContextContradictsKnownNonzeroCursorWithoutSecondMutation() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.returnAt(2, EditorCallResult.rejected());
        bridge.setTextBeforeCursor(EditorTextResult.value(""));

        ExecutionResult result = execute(
            bridge,
            EditorBounds.of(2, 2, -1, -1),
            EditorCapabilities.richText(false, true)
        );

        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit",
            "deleteCodePoints:before=1:after=0",
            "getTextBeforeCursor:max=2:flags=0",
            "endBatchEdit"
        ), bridge.trace());
        Assert.assertEquals(ExecutionResult.Outcome.NOT_DISPATCHED, result.outcome());
        Assert.assertEquals(ExecutionResult.Reason.INVALID_SURROUNDING_TEXT, result.reason());
        Assert.assertFalse(result.remoteMutationMayHaveOccurred());
    }

    @Test
    public void currentOrSensitiveProfilesNeverReadOrFallbackAfterCodePointFalse() {
        for (EditorCapabilities capabilities : Arrays.asList(
            EditorCapabilities.richText(false, false),
            EditorCapabilities.richText(true, true)
        )) {
            FakeEditorBridge bridge = new FakeEditorBridge();
            bridge.returnAt(2, EditorCallResult.rejected());
            bridge.setTextBeforeCursor(EditorTextResult.value("A"));

            ExecutionResult result = execute(
                bridge,
                EditorBounds.of(2, 2, -1, -1),
                capabilities
            );

            Assert.assertEquals(Arrays.asList(
                "beginBatchEdit",
                "deleteCodePoints:before=1:after=0",
                "endBatchEdit"
            ), bridge.trace());
            Assert.assertEquals(ExecutionResult.Outcome.UNCERTAIN, result.outcome());
        }
    }

    @Test
    public void codePointRuntimeFailureNeverFallsBack() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.returnAt(2, EditorCallResult.runtimeFailure());
        bridge.setTextBeforeCursor(EditorTextResult.value("A"));

        ExecutionResult result = execute(
            bridge,
            EditorBounds.of(2, 2, -1, -1),
            EditorCapabilities.richText(false, true)
        );

        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit",
            "deleteCodePoints:before=1:after=0",
            "endBatchEdit"
        ), bridge.trace());
        Assert.assertEquals(
            ExecutionResult.Reason.OPERATION_RUNTIME_FAILURE,
            result.reason()
        );
    }

    @Test
    public void typeNullUsesOnlyOneRawDeleteDownUpSequenceWithoutBatch() {
        FakeEditorBridge bridge = new FakeEditorBridge();

        ExecutionResult result = execute(
            bridge,
            EditorBounds.of(2, 2, -1, -1),
            EditorCapabilities.rawKey()
        );

        Assert.assertEquals(Arrays.asList(
            "sendRawKey:kind=DELETE:action=DOWN",
            "sendRawKey:kind=DELETE:action=UP"
        ), bridge.trace());
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
    }

    @Test
    public void rawDeleteStopsAtFirstFailureAndNeverSendsASecondSequence() {
        for (EditorCallResult failure : Arrays.asList(
            EditorCallResult.rejected(),
            EditorCallResult.runtimeFailure()
        )) {
            for (int failedCall : Arrays.asList(1, 2)) {
                FakeEditorBridge bridge = new FakeEditorBridge();
                bridge.returnAt(failedCall, failure);

                ExecutionResult result = execute(
                    bridge,
                    EditorBounds.of(2, 2, -1, -1),
                    EditorCapabilities.rawKey()
                );

                Assert.assertEquals(2, bridge.trace().size());
                Assert.assertEquals("sendRawKey:kind=DELETE:action=UP", bridge.trace().get(1));
                Assert.assertEquals(ExecutionResult.Outcome.UNCERTAIN, result.outcome());
                Assert.assertEquals(
                    failure.isRejected()
                        ? ExecutionResult.Reason.OPERATION_FALSE
                        : ExecutionResult.Reason.OPERATION_RUNTIME_FAILURE,
                    result.reason()
                );
                Assert.assertTrue(result.remoteMutationMayHaveOccurred());
            }
        }
    }

    @Test
    public void legacyCodeUnitFallbackFailureNeverContinuesToRawDelete() {
        for (EditorCallResult failure : Arrays.asList(
            EditorCallResult.rejected(),
            EditorCallResult.runtimeFailure()
        )) {
            FakeEditorBridge bridge = new FakeEditorBridge();
            bridge.returnAt(2, EditorCallResult.rejected());
            bridge.returnAt(4, failure);
            bridge.setTextBeforeCursor(EditorTextResult.value("A"));

            ExecutionResult result = execute(
                bridge,
                EditorBounds.of(2, 2, -1, -1),
                EditorCapabilities.richText(false, true)
            );

            Assert.assertEquals(Arrays.asList(
                "beginBatchEdit",
                "deleteCodePoints:before=1:after=0",
                "getTextBeforeCursor:max=2:flags=0",
                "deleteUtf16:before=1:after=0",
                "endBatchEdit"
            ), bridge.trace());
            Assert.assertEquals(ExecutionResult.Outcome.UNCERTAIN, result.outcome());
        }
    }

    @Test
    public void explicitlyEnabledLegacyRawFallbackIsOnePairedAttempt() {
        for (EditorTextResult context : Arrays.asList(
            EditorTextResult.nullValue(),
            EditorTextResult.value("\ud83d")
        )) {
            FakeEditorBridge bridge = new FakeEditorBridge();
            bridge.returnAt(2, EditorCallResult.rejected());
            bridge.setTextBeforeCursor(context);

            ExecutionResult result = execute(
                bridge,
                EditorBounds.of(2, 2, -1, -1),
                EditorCapabilities.richText(false, true, true)
            );

            Assert.assertEquals(Arrays.asList(
                "beginBatchEdit",
                "deleteCodePoints:before=1:after=0",
                "getTextBeforeCursor:max=2:flags=0",
                "sendRawKey:kind=DELETE:action=DOWN",
                "sendRawKey:kind=DELETE:action=UP",
                "endBatchEdit"
            ), bridge.trace());
            Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
            Assert.assertEquals(1, result.dispatchedMutationCount());
        }
    }

    @Test
    public void sensitiveLegacyRawFallbackReadsNoSurroundingText() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.returnAt(2, EditorCallResult.rejected());

        ExecutionResult result = execute(
            bridge,
            EditorBounds.of(2, 2, -1, -1),
            EditorCapabilities.richText(true, true, true)
        );

        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit",
            "deleteCodePoints:before=1:after=0",
            "sendRawKey:kind=DELETE:action=DOWN",
            "sendRawKey:kind=DELETE:action=UP",
            "endBatchEdit"
        ), bridge.trace());
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
    }

    @Test
    public void legacyRawFallbackPreservesDownAndUpFailureIndices() {
        FakeEditorBridge downFailure = legacyRawFallbackBridge();
        downFailure.returnAt(4, EditorCallResult.rejected());
        downFailure.returnAt(5, EditorCallResult.runtimeFailure());

        ExecutionResult bothFailed = execute(
            downFailure,
            EditorBounds.of(2, 2, -1, -1),
            EditorCapabilities.richText(false, true, true)
        );

        Assert.assertEquals(3, bothFailed.failedOperationIndex());
        Assert.assertEquals(4, bothFailed.cleanupOperationIndex());
        Assert.assertEquals(ExecutionResult.Reason.OPERATION_FALSE, bothFailed.reason());
        Assert.assertEquals(
            ExecutionResult.Reason.OPERATION_RUNTIME_FAILURE,
            bothFailed.cleanupReason()
        );

        FakeEditorBridge upFailure = legacyRawFallbackBridge();
        upFailure.returnAt(5, EditorCallResult.rejected());
        ExecutionResult upFailed = execute(
            upFailure,
            EditorBounds.of(2, 2, -1, -1),
            EditorCapabilities.richText(false, true, true)
        );

        Assert.assertEquals(4, upFailed.failedOperationIndex());
        Assert.assertEquals(-1, upFailed.cleanupOperationIndex());
        Assert.assertEquals(1, upFailed.dispatchedMutationCount());
    }

    @Test
    public void selectedTextDeleteFailureNeverFallsThroughToCodePointOrRawDelete() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.returnAt(2, EditorCallResult.rejected());

        ExecutionResult result = execute(
            bridge,
            EditorBounds.of(2, 5, -1, -1),
            EditorCapabilities.richText(false, true)
        );

        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit",
            "commitText:length=0:cursor=1",
            "endBatchEdit"
        ), bridge.trace());
        Assert.assertEquals(ExecutionResult.Outcome.UNCERTAIN, result.outcome());
    }

    @Test
    public void textReadDiagnosticsNeverExposeEditorContent() {
        String content = "never-log-surrounding-text";

        Assert.assertFalse(EditorTextResult.value(content).toString().contains(content));
    }

    private static void assertLegacyFallback(String beforeCursor, int utf16Units) {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.returnAt(2, EditorCallResult.rejected());
        bridge.setTextBeforeCursor(EditorTextResult.value(beforeCursor));

        ExecutionResult result = execute(
            bridge,
            EditorBounds.of(4, 4, -1, -1),
            EditorCapabilities.richText(false, true)
        );

        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit",
            "deleteCodePoints:before=1:after=0",
            "getTextBeforeCursor:max=2:flags=0",
            "deleteUtf16:before=" + utf16Units + ":after=0",
            "endBatchEdit"
        ), bridge.trace());
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
        Assert.assertEquals(1, result.dispatchedMutationCount());
    }

    private static FakeEditorBridge legacyRawFallbackBridge() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.returnAt(2, EditorCallResult.rejected());
        bridge.setTextBeforeCursor(EditorTextResult.nullValue());
        return bridge;
    }

    private static ExecutionResult execute(
        FakeEditorBridge bridge,
        EditorBounds bounds,
        EditorCapabilities capabilities
    ) {
        return EXECUTOR.execute(
            deletePlan(bounds),
            ExecutionContext.active(1, 0, bounds, capabilities),
            () -> EditorEndpoint.of(1, bridge)
        );
    }

    private static TransitionPlan<String> deletePlan(EditorBounds expected) {
        return TransitionPlan.of(
            1,
            0,
            DispatchResult.Disposition.HANDLED,
            "state",
            expected,
            Collections.singletonList(KeyAction.deleteBackward())
        );
    }
}
