package dev.hellgates.retekeyime;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public final class InputDispatcherContractTest {
    private final InputDispatcher dispatcher = new InputDispatcher();

    @Test
    public void softwareAndHardwareTextProduceEquivalentResults() {
        ProjectKeyEvent software = ProjectKeyEvent.softwareDown(
            "touch.latin.text",
            SemanticInput.text("😀")
        );
        ProjectKeyEvent hardware = HardwareEventNormalizer.normalize(
            RawHardwareKeyEvent.builder(InputAction.DOWN, 29)
            .stableKeyId("hardware.key.a")
            .deviceId(7)
            .unicodeValue(0x1f600)
            .build()
        );

        Assert.assertEquals(dispatcher.dispatch(software), dispatcher.dispatch(hardware));
        Assert.assertEquals(
            DispatchResult.handled(KeyAction.commitText("😀")),
            dispatcher.dispatch(software)
        );
    }

    @Test
    public void softwareAndHardwareDeleteProduceEquivalentResults() {
        ProjectKeyEvent software = ProjectKeyEvent.softwareDown(
            "touch.edit.backspace",
            SemanticInput.deleteBackward()
        );
        ProjectKeyEvent hardware = HardwareEventNormalizer.normalize(
            RawHardwareKeyEvent.builder(InputAction.DOWN, 67)
            .stableKeyId("hardware.key.delete")
            .deviceId(7)
            .mappedInput(SemanticInput.deleteBackward())
            .build()
        );

        Assert.assertEquals(dispatcher.dispatch(software), dispatcher.dispatch(hardware));
        Assert.assertEquals(
            DispatchResult.handled(KeyAction.deleteBackward()),
            dispatcher.dispatch(software)
        );
    }

    @Test
    public void softwareAndHardwareJamoProduceEquivalentResults() {
        SemanticInput giyeok = SemanticInput.jamo(
            SemanticJamo.contextualConsonant(0)
        );
        SemanticInput mappedGiyeok = DubeolsikHardwareMapper.INSTANCE.map(
            "hardware.key.r",
            false
        );
        ProjectKeyEvent software = ScaffoldKeyboardLayout
            .findById("touch.ko2.giyeok")
            .pressEvent();
        ProjectKeyEvent hardware = HardwareEventNormalizer.normalize(
            RawHardwareKeyEvent.builder(InputAction.DOWN, 46)
            .stableKeyId("hardware.key.r")
            .deviceId(7)
            .unicodeValue('r')
            .mappedInput(mappedGiyeok)
            .build()
        );

        Assert.assertEquals(giyeok, mappedGiyeok);
        Assert.assertEquals(software.semanticInput(), hardware.semanticInput());
        Assert.assertNotEquals(SemanticInput.text("r"), hardware.semanticInput());
        Assert.assertEquals(dispatcher.dispatch(software), dispatcher.dispatch(hardware));
        Assert.assertEquals(
            DispatchResult.handled(KeyAction.commitText("ㄱ")),
            dispatcher.dispatch(software)
        );
    }

    @Test
    public void unknownHardwareInputDelegates() {
        ProjectKeyEvent unknown = ProjectKeyEvent.builder(
            InputSource.HARDWARE,
            InputAction.DOWN
        )
            .keyCode(999)
            .deviceId(7)
            .build();

        Assert.assertEquals(DispatchResult.delegate(), dispatcher.dispatch(unknown));
    }

    @Test
    public void flushWithoutCompositionIsHandledWithoutEditorAction() {
        ProjectKeyEvent flush = ProjectKeyEvent.softwareDown(
            "command.flush",
            SemanticInput.flush()
        );

        Assert.assertEquals(DispatchResult.handled(), dispatcher.dispatch(flush));
    }

    @Test
    public void processorActionsCanPrecedeDelegationWithoutTrackingTheKey() {
        SemanticInput[] seen = new SemanticInput[1];
        StatelessInputProcessor processor = input -> {
            seen[0] = input;
            return DispatchResult.delegate(Arrays.asList(
                KeyAction.setComposingText("pending"),
                KeyAction.finishComposing()
            ));
        };
        InputDispatcher delegatingDispatcher = new InputDispatcher(processor);
        ProjectKeyEvent flushThenDelegate = HardwareEventNormalizer.normalize(
            RawHardwareKeyEvent.builder(InputAction.DOWN, 62)
                .stableKeyId("hardware.key.space")
                .deviceId(7)
                .mappedInput(SemanticInput.flush())
                .build()
        );

        DispatchResult result = delegatingDispatcher.dispatch(flushThenDelegate);

        Assert.assertEquals(SemanticInput.flush(), seen[0]);
        Assert.assertEquals(DispatchResult.Disposition.DELEGATE, result.disposition());
        Assert.assertEquals(Arrays.asList(
            KeyAction.setComposingText("pending"),
            KeyAction.finishComposing()
        ), result.actions());
        Assert.assertEquals(
            DispatchResult.delegate(),
            delegatingDispatcher.dispatch(ProjectKeyEvent.builder(
                InputSource.HARDWARE,
                InputAction.UP
            ).keyCode(62).deviceId(7).build())
        );
    }

    @Test
    public void structuredConsonantAndVowelReachProcessorWithoutSourceData() {
        SemanticInput[] seen = new SemanticInput[2];
        int[] count = {0};
        InputDispatcher recordingDispatcher = new InputDispatcher(input -> {
            seen[count[0]++] = input;
            return DispatchResult.handled();
        });

        recordingDispatcher.dispatch(
            ScaffoldKeyboardLayout.findById("touch.ko2.giyeok").pressEvent()
        );
        recordingDispatcher.dispatch(HardwareEventNormalizer.normalize(
            RawHardwareKeyEvent.builder(InputAction.DOWN, 38)
                .stableKeyId("hardware.key.k")
                .deviceId(7)
                .unicodeValue('k')
                .mappedInput(DubeolsikHardwareMapper.INSTANCE.map("hardware.key.k", false))
                .build()
        ));

        Assert.assertEquals(2, count[0]);
        Assert.assertEquals(
            SemanticInput.jamo(SemanticJamo.contextualConsonant(0)),
            seen[0]
        );
        Assert.assertEquals(SemanticInput.jamo(SemanticJamo.vowel(0)), seen[1]);
    }

    @Test
    public void softwareInputCannotDelegateButKeepsProcessorActions() {
        InputDispatcher softwareDispatcher = new InputDispatcher(
            input -> DispatchResult.delegate(KeyAction.finishComposing())
        );

        Assert.assertEquals(
            DispatchResult.handled(KeyAction.finishComposing()),
            softwareDispatcher.dispatch(ProjectKeyEvent.softwareDown(
                "command.flush",
                SemanticInput.flush()
            ))
        );
    }
}
