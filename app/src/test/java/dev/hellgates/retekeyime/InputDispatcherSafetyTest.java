package dev.hellgates.retekeyime;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public final class InputDispatcherSafetyTest {
    @Test
    public void unsafeAndDelegatedPathsNeverInvokeProcessor() {
        int[] calls = {0};
        InputDispatcher dispatcher = new InputDispatcher(input -> {
            calls[0]++;
            throw new AssertionError("processor must not be called");
        });

        for (ProjectKeyEvent event : Arrays.asList(
            hardwareDown().ctrl(true).semanticInput(SemanticInput.text("a")).build(),
            hardwareDown().alt(true).semanticInput(SemanticInput.text("a")).build(),
            hardwareDown().meta(true).semanticInput(SemanticInput.text("a")).build(),
            hardwareDown().function(true).semanticInput(SemanticInput.text("a")).build(),
            hardwareDown().sym(true).semanticInput(SemanticInput.text("a")).build(),
            hardwareDown().combiningAccentCodePoint(0x60)
                .semanticInput(SemanticInput.text("a")).build(),
            hardwareDown().canceled(true).semanticInput(SemanticInput.text("a")).build(),
            hardwareDown().repeatCount(1).semanticInput(SemanticInput.text("a")).build(),
            ProjectKeyEvent.builder(InputSource.HARDWARE, InputAction.UP)
                .keyCode(29).deviceId(7).semanticInput(SemanticInput.text("a")).build(),
            ProjectKeyEvent.builder(InputSource.HARDWARE, InputAction.MULTIPLE)
                .keyCode(29).deviceId(7).semanticInput(SemanticInput.text("a")).build(),
            hardwareDown().build()
        )) {
            DispatchResult result = dispatcher.dispatch(event);
            Assert.assertEquals(DispatchResult.delegate(), result);
        }

        Assert.assertEquals(0, calls[0]);
    }

    private static ProjectKeyEvent.Builder hardwareDown() {
        return ProjectKeyEvent.builder(InputSource.HARDWARE, InputAction.DOWN)
            .keyCode(29)
            .deviceId(7);
    }
}
