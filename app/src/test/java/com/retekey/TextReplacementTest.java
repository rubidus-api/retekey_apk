package com.retekey;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

/**
 * The kana ゛゜小 key and a Hanja pick replace text the editor already holds: delete what is
 * before the cursor, commit what replaces it. The delete is a prerequisite — if it did not
 * happen, the commit must not either, or か becomes かが — and the batch opened for it is closed
 * whatever happens. (Review finding R02.)
 */
public final class TextReplacementTest {
    @Test
    public void aCleanReplacementDeletesThenCommitsInOneBatch() {
        FakeEditorBridge bridge = new FakeEditorBridge();

        TextReplacement.Outcome outcome = TextReplacement.replace(bridge, 1, "が");

        Assert.assertEquals(TextReplacement.Outcome.REPLACED, outcome);
        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit",
            "deleteUtf16:before=1:after=0",
            "commitText:length=1:cursor=1",
            "endBatchEdit"
        ), bridge.trace());
    }

    @Test
    public void aSelectionIsReplacedByTheCommitAlone() {
        FakeEditorBridge bridge = new FakeEditorBridge();

        TextReplacement.Outcome outcome = TextReplacement.replace(bridge, 0, "韓");

        Assert.assertEquals(TextReplacement.Outcome.REPLACED, outcome);
        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit", "commitText:length=1:cursor=1", "endBatchEdit"), bridge.trace());
    }

    @Test
    public void aRefusedDeleteCommitsNothing() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.returnAt(2, EditorCallResult.rejected());

        TextReplacement.Outcome outcome = TextReplacement.replace(bridge, 1, "が");

        Assert.assertEquals(TextReplacement.Outcome.NOTHING_WRITTEN, outcome);
        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit", "deleteUtf16:before=1:after=0", "endBatchEdit"),
            bridge.trace());
    }

    @Test
    public void aThrowingDeleteCommitsNothingAndStillClosesTheBatch() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.throwAt(2);

        TextReplacement.Outcome outcome = TextReplacement.replace(bridge, 1, "が");

        // A throw may have deleted: that is not "nothing happened".
        Assert.assertEquals(TextReplacement.Outcome.UNCERTAIN, outcome);
        Assert.assertEquals(Arrays.asList(
            "beginBatchEdit", "deleteUtf16:before=1:after=0", "endBatchEdit"),
            bridge.trace());
    }

    @Test
    public void aRefusedBatchWritesNothing() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.returnAt(1, EditorCallResult.rejected());

        TextReplacement.Outcome outcome = TextReplacement.replace(bridge, 1, "が");

        Assert.assertEquals(TextReplacement.Outcome.NOTHING_WRITTEN, outcome);
        Assert.assertEquals(Arrays.asList("beginBatchEdit", "endBatchEdit"), bridge.trace());
    }

    @Test
    public void aRefusedCommitAfterTheDeleteIsUncertain() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.returnAt(3, EditorCallResult.rejected());

        Assert.assertEquals(
            TextReplacement.Outcome.UNCERTAIN, TextReplacement.replace(bridge, 1, "が"));
        Assert.assertEquals("endBatchEdit", bridge.trace().get(bridge.trace().size() - 1));
    }

    @Test
    public void aThrowingBatchEndAfterGoodWritesIsUncertain() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.throwAt(4);

        Assert.assertEquals(
            TextReplacement.Outcome.UNCERTAIN, TextReplacement.replace(bridge, 1, "が"));
    }

    @Test
    public void theSourceIsStillThereWhenTheEditorShowsIt() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.setTextBeforeCursor(EditorTextResult.value("한자"));

        Assert.assertTrue(TextReplacement.stillBeforeCursor(bridge, "한자"));
        Assert.assertEquals(Arrays.asList("getTextBeforeCursor:max=2:flags=0"), bridge.trace());
    }

    /** The cursor moved, or the text changed, since the candidates were offered. */
    @Test
    public void theSourceIsGoneWhenTheEditorShowsSomethingElse() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.setTextBeforeCursor(EditorTextResult.value("가자"));

        Assert.assertFalse(TextReplacement.stillBeforeCursor(bridge, "한자"));
    }

    @Test
    public void anUnreadableEditorDoesNotConfirmTheSource() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.setTextBeforeCursor(EditorTextResult.runtimeFailure());

        Assert.assertFalse(TextReplacement.stillBeforeCursor(bridge, "한자"));
        Assert.assertFalse(TextReplacement.stillBeforeCursor(new FakeEditorBridge(), ""));
    }
}
