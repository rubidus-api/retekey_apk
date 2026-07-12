package dev.hellgates.retekeyime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public final class DispatchResultTest {
    @Test
    public void handledWithoutActionsIsDistinctFromDelegation() {
        DispatchResult handled = DispatchResult.handled();
        DispatchResult delegated = DispatchResult.delegate();

        Assert.assertEquals(DispatchResult.Disposition.HANDLED, handled.disposition());
        Assert.assertEquals(DispatchResult.Disposition.DELEGATE, delegated.disposition());
        Assert.assertTrue(handled.actions().isEmpty());
        Assert.assertTrue(delegated.actions().isEmpty());
        Assert.assertNotEquals(handled, delegated);
    }

    @Test
    public void orderedActionsAreDefensivelyCopiedAndImmutable() {
        List<KeyAction> source = new ArrayList<>(Arrays.asList(
            KeyAction.commitText("before"),
            KeyAction.setComposingText("after")
        ));

        DispatchResult result = DispatchResult.handled(source);
        source.clear();

        Assert.assertEquals(Arrays.asList(
            KeyAction.commitText("before"),
            KeyAction.setComposingText("after")
        ), result.actions());
        Assert.assertThrows(
            UnsupportedOperationException.class,
            () -> result.actions().add(KeyAction.deleteBackward())
        );
    }

    @Test
    public void delegationMayFollowOrderedFlushActions() {
        DispatchResult result = DispatchResult.delegate(Arrays.asList(
            KeyAction.setComposingText("pending"),
            KeyAction.finishComposing()
        ));

        Assert.assertEquals(DispatchResult.Disposition.DELEGATE, result.disposition());
        Assert.assertEquals(Arrays.asList(
            KeyAction.setComposingText("pending"),
            KeyAction.finishComposing()
        ), result.actions());
    }
}
