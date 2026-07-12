package dev.hellgates.retekeyime;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public final class FakeEditorBridgeModelTest {
    private static final CheckedEditorExecutor EXECUTOR = new CheckedEditorExecutor();
    private static final EditorCapabilities RICH = EditorCapabilities.richText(false, false);

    @Test
    public void modelsSelectionCompositionCommitAndFinishInUtf16Coordinates() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.setModel("abcd", EditorBounds.of(1, 3, -1, -1));

        Assert.assertTrue(bridge.setComposingText("가", 1).isSucceeded());
        Assert.assertEquals("a가d", bridge.modelText());
        Assert.assertEquals(EditorBounds.of(2, 2, 1, 2), bridge.modelBounds());
        Assert.assertTrue(bridge.setComposingText("xy", 1).isSucceeded());
        Assert.assertEquals("axyd", bridge.modelText());
        Assert.assertEquals(EditorBounds.of(3, 3, 1, 3), bridge.modelBounds());
        Assert.assertTrue(bridge.finishComposingText().isSucceeded());
        Assert.assertEquals(EditorBounds.of(3, 3, -1, -1), bridge.modelBounds());
    }

    @Test
    public void codePointDeletionDoesNotSplitSupplementaryCharacter() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        String text = "A\ud83d\ude00";
        bridge.setModel(text, EditorBounds.of(text.length(), text.length(), -1, -1));

        Assert.assertTrue(bridge.deleteSurroundingTextInCodePoints(1, 0).isSucceeded());

        Assert.assertEquals("A", bridge.modelText());
        Assert.assertEquals(EditorBounds.of(1, 1, -1, -1), bridge.modelBounds());
    }

    @Test
    public void emptyComposingTextDeletesPreeditAndFinishOnlyClearsSpan() {
        FakeEditorBridge empty = new FakeEditorBridge();
        empty.setModel("a가b", EditorBounds.of(2, 2, 1, 2));

        Assert.assertTrue(empty.setComposingText("", 1).isSucceeded());
        Assert.assertEquals("ab", empty.modelText());
        Assert.assertEquals(EditorBounds.of(1, 1, -1, -1), empty.modelBounds());

        FakeEditorBridge finish = new FakeEditorBridge();
        finish.setModel("a가b", EditorBounds.of(2, 2, 1, 2));
        Assert.assertTrue(finish.finishComposingText().isSucceeded());
        Assert.assertEquals("a가b", finish.modelText());
        Assert.assertEquals(EditorBounds.of(2, 2, -1, -1), finish.modelBounds());
    }

    @Test
    public void failureBeforeOrAfterEffectHasSameUncertainNoRetryResult() {
        for (boolean effectBeforeFailure : Arrays.asList(false, true)) {
            FakeEditorBridge bridge = new FakeEditorBridge();
            bridge.setModel("A", EditorBounds.of(1, 1, -1, -1));
            if (effectBeforeFailure) {
                bridge.returnAfterEffectAt(2, EditorCallResult.rejected());
            } else {
                bridge.returnAt(2, EditorCallResult.rejected());
            }

            ExecutionResult result = EXECUTOR.execute(
                TransitionPlan.of(
                    1,
                    0,
                    DispatchResult.Disposition.HANDLED,
                    "state",
                    EditorBounds.unknown(),
                    Collections.singletonList(KeyAction.deleteBackward())
                ),
                ExecutionContext.active(
                    1,
                    0,
                    EditorBounds.of(1, 1, -1, -1),
                    RICH
                ),
                () -> EditorEndpoint.of(1, bridge)
            );

            Assert.assertEquals(ExecutionResult.Outcome.UNCERTAIN, result.outcome());
            Assert.assertEquals(1, countDeleteCalls(bridge));
            Assert.assertEquals(effectBeforeFailure ? "" : "A", bridge.modelText());
        }
    }

    @Test
    public void partialSuccessNeverRunsOperationAfterFailureOrRollsBackModel() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.setModel("", EditorBounds.of(0, 0, -1, -1));
        bridge.returnAfterEffectAt(3, EditorCallResult.runtimeFailure());

        ExecutionResult result = EXECUTOR.execute(
            TransitionPlan.of(
                1,
                0,
                DispatchResult.Disposition.HANDLED,
                "state",
                EditorBounds.of(2, 2, -1, -1),
                Arrays.asList(
                    KeyAction.commitText("A"),
                    KeyAction.commitText("B"),
                    KeyAction.commitText("C")
                )
            ),
            ExecutionContext.active(1, 0, EditorBounds.of(0, 0, -1, -1), RICH),
            () -> EditorEndpoint.of(1, bridge)
        );

        Assert.assertEquals(ExecutionResult.Outcome.UNCERTAIN, result.outcome());
        Assert.assertEquals("AB", bridge.modelText());
        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit",
            "commitText:length=1:cursor=1",
            "commitText:length=1:cursor=1",
            "endBatchEdit"
        ), bridge.trace());
    }

    private static int countDeleteCalls(FakeEditorBridge bridge) {
        int count = 0;
        for (String call : bridge.trace()) {
            if (call.startsWith("delete")) {
                count++;
            }
        }
        return count;
    }
}
