package dev.hellgates.retekeyime;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public final class SemanticContractTest {
    @Test
    public void semanticJamoUsesRoleAndIndexValueEquality() {
        SemanticJamo giyeok = SemanticJamo.contextualConsonant(0);

        Assert.assertEquals(
            SemanticInput.jamo(giyeok),
            SemanticInput.jamo(SemanticJamo.contextualConsonant(0))
        );
        Assert.assertNotEquals(
            SemanticInput.jamo(giyeok),
            SemanticInput.jamo(SemanticJamo.vowel(0))
        );
        Assert.assertNotEquals(
            SemanticInput.jamo(giyeok),
            SemanticInput.jamo(SemanticJamo.contextualConsonant(1))
        );
    }

    @Test
    public void semanticFactoriesRejectInvalidPayloads() {
        Assert.assertThrows(IllegalArgumentException.class, () -> SemanticInput.text(null));
        Assert.assertThrows(IllegalArgumentException.class, () -> SemanticInput.text(""));
        Assert.assertThrows(IllegalArgumentException.class, () -> SemanticInput.text("\ud800"));
        Assert.assertThrows(IllegalArgumentException.class, () -> SemanticInput.jamo(null));
        Assert.assertThrows(
            IllegalArgumentException.class,
            () -> new SemanticJamo(SemanticJamo.Role.VOWEL, -1)
        );
        Assert.assertThrows(IllegalArgumentException.class, () -> new SemanticJamo(null, 0));
        Assert.assertThrows(
            IllegalArgumentException.class,
            () -> DispatchResult.handled(Arrays.asList(KeyAction.deleteBackward(), null))
        );
        Assert.assertThrows(
            IllegalArgumentException.class,
            () -> DispatchResult.handled((List<KeyAction>) null)
        );
    }

    @Test
    public void diagnosticsDoNotExposeEnteredText() {
        String privateText = "do-not-log-this-text";

        Assert.assertFalse(SemanticInput.text(privateText).toString().contains(privateText));
        Assert.assertFalse(KeyAction.commitText(privateText).toString().contains(privateText));
        Assert.assertFalse(
            DispatchResult.handled(KeyAction.commitText(privateText))
                .toString()
                .contains(privateText)
        );
    }

    @Test
    public void semanticJamoIndicesUseBoundedUnicodeCompositionDomains() {
        Assert.assertThrows(
            IllegalArgumentException.class,
            () -> SemanticJamo.contextualConsonant(19)
        );
        Assert.assertThrows(IllegalArgumentException.class, () -> SemanticJamo.vowel(21));
        Assert.assertThrows(IllegalArgumentException.class, () -> SemanticJamo.directInitial(19));
        Assert.assertThrows(IllegalArgumentException.class, () -> SemanticJamo.directMedial(21));
        Assert.assertThrows(IllegalArgumentException.class, () -> SemanticJamo.directFinal(0));
        Assert.assertThrows(IllegalArgumentException.class, () -> SemanticJamo.directFinal(28));
    }

    @Test
    public void allDirectJamoRolesHaveVisibleScaffoldFallbacks() {
        ScaffoldInputProcessor processor = new ScaffoldInputProcessor();

        Assert.assertEquals(
            DispatchResult.handled(KeyAction.commitText("ㄱ")),
            processor.process(SemanticInput.jamo(SemanticJamo.directInitial(0)))
        );
        Assert.assertEquals(
            DispatchResult.handled(KeyAction.commitText("ㅏ")),
            processor.process(SemanticInput.jamo(SemanticJamo.directMedial(0)))
        );
        Assert.assertEquals(
            DispatchResult.handled(KeyAction.commitText("ㅎ")),
            processor.process(SemanticInput.jamo(SemanticJamo.directFinal(27)))
        );
    }
}
