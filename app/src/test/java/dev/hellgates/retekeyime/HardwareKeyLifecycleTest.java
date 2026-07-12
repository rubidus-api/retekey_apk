package dev.hellgates.retekeyime;

import org.junit.Assert;
import org.junit.Test;

public final class HardwareKeyLifecycleTest {
    @Test
    public void consumedDownConsumesOnlyItsMatchingUp() {
        InputDispatcher dispatcher = new InputDispatcher();

        Assert.assertEquals(
            DispatchResult.handled(KeyAction.commitText("a")),
            dispatcher.dispatch(textDown(7, 29))
        );
        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(keyUp(8, 29, false)));
        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(keyUp(7, 30, false)));
        Assert.assertEquals(DispatchResult.handled(), dispatcher.dispatch(keyUp(7, 29, false)));
        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(keyUp(7, 29, false)));
    }

    @Test
    public void trackedRepeatIsConsumedWithoutOutputAndUntrackedRepeatDelegates() {
        InputDispatcher dispatcher = new InputDispatcher();

        Assert.assertEquals(
            DispatchResult.handled(KeyAction.commitText("a")),
            dispatcher.dispatch(textDown(7, 29))
        );
        Assert.assertEquals(DispatchResult.handled(), dispatcher.dispatch(repeatDown(7, 29)));
        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(repeatDown(7, 30)));
        Assert.assertEquals(DispatchResult.handled(), dispatcher.dispatch(keyUp(7, 29, false)));
    }

    @Test
    public void canceledMatchingUpClearsTheConsumedIdentity() {
        InputDispatcher dispatcher = new InputDispatcher();

        dispatcher.dispatch(textDown(7, 29));

        Assert.assertEquals(DispatchResult.handled(), dispatcher.dispatch(keyUp(7, 29, true)));
        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(keyUp(7, 29, false)));
    }

    @Test
    public void canceledOrUnknownDownDoesNotLatchState() {
        InputDispatcher dispatcher = new InputDispatcher();
        ProjectKeyEvent canceled = HardwareEventNormalizer.normalize(
            RawHardwareKeyEvent.builder(InputAction.DOWN, 29)
                .deviceId(7)
                .unicodeValue('a')
                .canceled(true)
                .build()
        );
        ProjectKeyEvent unknown = ProjectKeyEvent.builder(InputSource.HARDWARE, InputAction.DOWN)
            .keyCode(30)
            .deviceId(7)
            .build();

        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(canceled));
        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(keyUp(7, 29, false)));
        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(unknown));
        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(keyUp(7, 30, false)));
    }

    @Test
    public void newInitialDownRecoversAStaleIdentityAndResetBoundsState() {
        InputDispatcher dispatcher = new InputDispatcher();

        Assert.assertTrue(dispatcher.dispatch(textDown(7, 29)).isHandled());
        Assert.assertTrue(dispatcher.dispatch(textDown(7, 29)).isHandled());
        Assert.assertEquals(DispatchResult.handled(), dispatcher.dispatch(keyUp(7, 29, false)));
        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(keyUp(7, 29, false)));

        Assert.assertTrue(dispatcher.dispatch(textDown(7, 29)).isHandled());
        dispatcher.reset();
        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(keyUp(7, 29, false)));
    }

    @Test
    public void canceledRepeatClearsATrackedIdentityWithoutOutput() {
        InputDispatcher dispatcher = new InputDispatcher();

        dispatcher.dispatch(textDown(7, 29));
        ProjectKeyEvent canceledRepeat = HardwareEventNormalizer.normalize(
            RawHardwareKeyEvent.builder(InputAction.DOWN, 29)
                .deviceId(7)
                .unicodeValue('a')
                .repeatCount(1)
                .canceled(true)
                .build()
        );

        Assert.assertEquals(DispatchResult.handled(), dispatcher.dispatch(canceledRepeat));
        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(keyUp(7, 29, false)));
    }

    @Test
    public void capacityDelegatesNewDownWithoutEvictingConsumedIdentities() {
        InputDispatcher dispatcher = new InputDispatcher();

        for (int keyCode = 1; keyCode <= 32; keyCode++) {
            Assert.assertTrue(dispatcher.dispatch(textDown(7, keyCode)).isHandled());
        }

        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(textDown(7, 33)));
        Assert.assertEquals(DispatchResult.handled(), dispatcher.dispatch(keyUp(7, 1, false)));
        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(keyUp(7, 33, false)));
        Assert.assertTrue(dispatcher.dispatch(textDown(7, 33)).isHandled());
        Assert.assertEquals(DispatchResult.handled(), dispatcher.dispatch(keyUp(7, 33, false)));
    }

    @Test
    public void capacityPreservesDelegateActionsFromStatelessProcessor() {
        InputDispatcher dispatcher = new InputDispatcher(input -> {
            if (input.kind() == SemanticInput.Kind.FLUSH) {
                return DispatchResult.delegate(KeyAction.finishComposing());
            }
            return DispatchResult.handled(KeyAction.commitText(input.text()));
        });
        for (int keyCode = 1; keyCode <= 32; keyCode++) {
            Assert.assertTrue(dispatcher.dispatch(textDown(7, keyCode)).isHandled());
        }
        ProjectKeyEvent flush = ProjectKeyEvent.builder(InputSource.HARDWARE, InputAction.DOWN)
            .keyCode(33)
            .deviceId(7)
            .semanticInput(SemanticInput.flush())
            .build();

        Assert.assertEquals(
            DispatchResult.delegate(KeyAction.finishComposing()),
            dispatcher.dispatch(flush)
        );
        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(keyUp(7, 33, false)));
    }

    @Test
    public void unknownKeyCodeZeroDelegatesWithoutOutputOrLatch() {
        InputDispatcher dispatcher = new InputDispatcher();

        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(textDown(7, 0)));
        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(keyUp(7, 0, false)));
    }

    @Test
    public void reservedHardwareKeyDownAndUpDelegateUnchanged() {
        InputDispatcher dispatcher = new InputDispatcher();
        ProjectKeyEvent reservedDown = ProjectKeyEvent.builder(
            InputSource.HARDWARE,
            InputAction.DOWN
        )
            .stableKeyId("hardware.keycode.4")
            .keyCode(4)
            .deviceId(7)
            .build();

        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(reservedDown));
        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(keyUp(7, 4, false)));
    }

    @Test
    public void trackedMultipleIsConsumedAndCanceledMultipleClearsIdentity() {
        InputDispatcher dispatcher = new InputDispatcher();

        dispatcher.dispatch(textDown(7, 29));
        Assert.assertEquals(
            DispatchResult.handled(),
            dispatcher.dispatch(keyMultiple(7, 29, false))
        );
        Assert.assertEquals(DispatchResult.handled(), dispatcher.dispatch(keyUp(7, 29, false)));

        dispatcher.dispatch(textDown(7, 29));
        Assert.assertEquals(
            DispatchResult.handled(),
            dispatcher.dispatch(keyMultiple(7, 29, true))
        );
        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(keyUp(7, 29, false)));
        Assert.assertEquals(
            DispatchResult.delegate(),
            dispatcher.dispatch(keyMultiple(7, 30, false))
        );
    }

    private static ProjectKeyEvent textDown(int deviceId, int keyCode) {
        return HardwareEventNormalizer.normalize(
            RawHardwareKeyEvent.builder(InputAction.DOWN, keyCode)
                .deviceId(deviceId)
                .unicodeValue('a')
                .build()
        );
    }

    private static ProjectKeyEvent repeatDown(int deviceId, int keyCode) {
        return HardwareEventNormalizer.normalize(
            RawHardwareKeyEvent.builder(InputAction.DOWN, keyCode)
                .deviceId(deviceId)
                .unicodeValue('a')
                .repeatCount(1)
                .build()
        );
    }

    private static ProjectKeyEvent keyUp(int deviceId, int keyCode, boolean canceled) {
        return HardwareEventNormalizer.normalize(
            RawHardwareKeyEvent.builder(InputAction.UP, keyCode)
                .deviceId(deviceId)
                .canceled(canceled)
                .build()
        );
    }

    private static ProjectKeyEvent keyMultiple(int deviceId, int keyCode, boolean canceled) {
        return HardwareEventNormalizer.normalize(
            RawHardwareKeyEvent.builder(InputAction.MULTIPLE, keyCode)
                .deviceId(deviceId)
                .characters("a")
                .canceled(canceled)
                .build()
        );
    }
}
