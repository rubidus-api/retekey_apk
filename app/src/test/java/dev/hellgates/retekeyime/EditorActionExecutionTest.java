package dev.hellgates.retekeyime;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public final class EditorActionExecutionTest {
    private static final CheckedEditorExecutor EXECUTOR = new CheckedEditorExecutor();
    private static final EditorBounds CURSOR = EditorBounds.of(1, 1, -1, -1);
    private static final EditorCapabilities RICH = EditorCapabilities.richText(false, false);

    @Test
    public void actionOnlySkipsBatchAndUsesExactId() {
        FakeEditorBridge bridge = new FakeEditorBridge();

        ExecutionResult result = execute(
            bridge,
            RICH,
            Collections.singletonList(KeyAction.performEditorAction(7))
        );

        Assert.assertEquals(
            Collections.singletonList("performEditorAction:id=7"),
            bridge.trace()
        );
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
    }

    @Test
    public void standardActionUsesCapturedConnectionOutsideBatch() {
        FakeEditorBridge bridge = new FakeEditorBridge();

        ExecutionResult result = execute(
            bridge,
            RICH,
            Collections.singletonList(KeyAction.performEditorAction(6))
        );

        Assert.assertEquals(
            Collections.singletonList("performEditorAction:id=6"),
            bridge.trace()
        );
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
    }

    @Test
    public void mutationBatchFullyEndsBeforeFocusChangingAction() {
        FakeEditorBridge bridge = new FakeEditorBridge();

        ExecutionResult result = execute(
            bridge,
            RICH,
            Arrays.asList(
                KeyAction.commitText("x"),
                KeyAction.performEditorAction(6)
            )
        );

        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit",
            "commitText:length=1:cursor=1",
            "endBatchEdit",
            "performEditorAction:id=6"
        ), bridge.trace());
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
    }

    @Test
    public void batchCleanupFailurePreventsFocusAction() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.returnAt(3, EditorCallResult.rejected());

        ExecutionResult result = execute(
            bridge,
            RICH,
            Arrays.asList(
                KeyAction.commitText("x"),
                KeyAction.performEditorAction(6)
            )
        );

        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit",
            "commitText:length=1:cursor=1",
            "endBatchEdit"
        ), bridge.trace());
        Assert.assertEquals(ExecutionResult.Reason.BATCH_END_FALSE, result.reason());
    }

    @Test
    public void focusActionFalseOrRuntimeNeverTriggersFallbackCall() {
        for (EditorCallResult failure : Arrays.asList(
            EditorCallResult.rejected(),
            EditorCallResult.runtimeFailure()
        )) {
            FakeEditorBridge bridge = new FakeEditorBridge();
            bridge.returnAt(1, failure);

            ExecutionResult result = execute(
                bridge,
                RICH,
                Collections.singletonList(KeyAction.performEditorAction(2))
            );

            Assert.assertEquals(1, bridge.callCount());
            Assert.assertEquals(ExecutionResult.Outcome.UNCERTAIN, result.outcome());
            Assert.assertEquals(
                failure.isRejected()
                    ? ExecutionResult.Reason.OPERATION_FALSE
                    : ExecutionResult.Reason.OPERATION_RUNTIME_FAILURE,
                result.reason()
            );
        }
    }

    @Test
    public void focusActionFailureAfterMutationNeverReusesTheConnection() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.returnAt(4, EditorCallResult.rejected());

        ExecutionResult result = execute(
            bridge,
            RICH,
            Arrays.asList(
                KeyAction.commitText("x"),
                KeyAction.performEditorAction(2)
            )
        );

        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit",
            "commitText:length=1:cursor=1",
            "endBatchEdit",
            "performEditorAction:id=2"
        ), bridge.trace());
        Assert.assertEquals(ExecutionResult.Outcome.UNCERTAIN, result.outcome());
        Assert.assertEquals(1, result.dispatchedMutationCount());
        Assert.assertTrue(result.remoteMutationMayHaveOccurred());
        Assert.assertEquals(3, result.failedOperationIndex());
        Assert.assertEquals(4, result.operationCount());
    }

    @Test
    public void typeNullRawEnterUsesOnlyOneDownUpPairWithoutBatch() {
        FakeEditorBridge bridge = new FakeEditorBridge();

        ExecutionResult result = execute(
            bridge,
            EditorCapabilities.rawKey(),
            Collections.singletonList(KeyAction.rawEnter())
        );

        Assert.assertEquals(Arrays.asList(
            "sendRawKey:key=ENTER:modifiers=[]:action=DOWN",
            "sendRawKey:key=ENTER:modifiers=[]:action=UP"
        ), bridge.trace());
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
    }

    private static ExecutionResult execute(
        FakeEditorBridge bridge,
        EditorCapabilities capabilities,
        java.util.List<KeyAction> actions
    ) {
        TransitionPlan<String> plan = TransitionPlan.of(
            1,
            0,
            DispatchResult.Disposition.HANDLED,
            "state",
            CURSOR,
            actions
        );
        return EXECUTOR.execute(
            plan,
            ExecutionContext.active(1, 0, CURSOR, capabilities),
            () -> EditorEndpoint.of(1, bridge)
        );
    }
}
