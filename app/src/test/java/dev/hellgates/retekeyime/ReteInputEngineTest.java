package dev.hellgates.retekeyime;

import org.junit.Assert;
import org.junit.Test;

public final class ReteInputEngineTest {
    @Test
    public void softHangulKeyCommitsLabelForInitialScaffold() {
        ReteInputEngine engine = new ReteInputEngine();

        KeyAction action = engine.onSoftKey("ㄱ");

        Assert.assertEquals(KeyAction.Kind.COMMIT_TEXT, action.kind());
        Assert.assertEquals("ㄱ", action.text());
    }

    @Test
    public void softBackspaceDeletesBackward() {
        ReteInputEngine engine = new ReteInputEngine();

        KeyAction action = engine.onSoftKey("⌫");

        Assert.assertEquals(KeyAction.Kind.DELETE_BACKWARD, action.kind());
    }
}
