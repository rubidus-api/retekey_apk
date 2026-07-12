package dev.hellgates.retekeyime;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public final class HardwareEventNormalizerTest {
    @Test
    public void preservesCompleteHardwareEventSnapshot() {
        RawHardwareKeyEvent raw = RawHardwareKeyEvent.builder(InputAction.DOWN, 29)
            .stableKeyId("hardware.key.a")
            .scanCode(30)
            .deviceId(7)
            .deviceSource(0x101)
            .unicodeValue('A')
            .shift(true)
            .ctrl(true)
            .alt(true)
            .meta(true)
            .capsLock(true)
            .function(true)
            .sym(true)
            .rawMetaState(0x12345678)
            .repeatCount(2)
            .canceled(true)
            .build();

        ProjectKeyEvent event = HardwareEventNormalizer.normalize(raw);

        Assert.assertEquals(InputSource.HARDWARE, event.source());
        Assert.assertEquals(InputAction.DOWN, event.action());
        Assert.assertEquals("hardware.key.a", event.stableKeyId());
        Assert.assertEquals(29, event.keyCode());
        Assert.assertEquals(30, event.scanCode());
        Assert.assertEquals(7, event.deviceId());
        Assert.assertEquals(0x101, event.deviceSource());
        Assert.assertEquals("A", event.text());
        Assert.assertTrue(event.shift());
        Assert.assertTrue(event.ctrl());
        Assert.assertTrue(event.alt());
        Assert.assertTrue(event.meta());
        Assert.assertTrue(event.capsLock());
        Assert.assertTrue(event.function());
        Assert.assertTrue(event.sym());
        Assert.assertEquals(0x12345678, event.rawMetaState());
        Assert.assertEquals(2, event.repeatCount());
        Assert.assertTrue(event.canceled());
        Assert.assertFalse(event.hasSemanticInput());
    }

    @Test
    public void supplementaryUnicodeTextIsPreservedExactly() {
        ProjectKeyEvent event = HardwareEventNormalizer.normalize(
            rawDown(0x1f600).build()
        );

        Assert.assertEquals("😀", event.text());
        Assert.assertEquals(SemanticInput.text("😀"), event.semanticInput());
    }

    @Test
    public void mappedJamoRemainsStructuredWhileRawTextIsPreserved() {
        SemanticInput giyeok = SemanticInput.jamo(SemanticJamo.contextualConsonant(0));
        ProjectKeyEvent event = HardwareEventNormalizer.normalize(
            rawDown('r').mappedInput(giyeok).build()
        );

        Assert.assertEquals("r", event.text());
        Assert.assertEquals(giyeok, event.semanticInput());
        Assert.assertNotEquals(SemanticInput.text("r"), event.semanticInput());
    }

    @Test
    public void deadKeyIsExplicitAndNeverBecomesText() {
        ProjectKeyEvent event = HardwareEventNormalizer.normalize(
            rawDown(0x80000000 | 0x0060).build()
        );

        Assert.assertEquals("", event.text());
        Assert.assertTrue(event.hasCombiningAccent());
        Assert.assertEquals(0x0060, event.combiningAccentCodePoint());
        Assert.assertFalse(event.hasSemanticInput());
        Assert.assertEquals(DispatchResult.delegate(), new InputDispatcher().dispatch(event));
    }

    @Test
    public void malformedDeadKeyFlagStillBlocksMappedInput() {
        SemanticInput giyeok = SemanticInput.jamo(SemanticJamo.contextualConsonant(0));
        ProjectKeyEvent flagOnly = HardwareEventNormalizer.normalize(
            rawDown(0x80000000).mappedInput(giyeok).build()
        );
        ProjectKeyEvent outOfRange = HardwareEventNormalizer.normalize(
            rawDown(0x80110000).mappedInput(giyeok).build()
        );

        for (ProjectKeyEvent event : Arrays.asList(flagOnly, outOfRange)) {
            Assert.assertTrue(event.hasCombiningAccent());
            Assert.assertEquals(0, event.combiningAccentCodePoint());
            Assert.assertFalse(event.hasSemanticInput());
            Assert.assertEquals(DispatchResult.delegate(), new InputDispatcher().dispatch(event));
        }
    }

    @Test
    public void accentPayloadCannotBeMarkedSafeByBuilderCallOrder() {
        ProjectKeyEvent event = ProjectKeyEvent.builder(InputSource.HARDWARE, InputAction.DOWN)
            .combiningAccentCodePoint(0x60)
            .deadKey(false)
            .build();

        Assert.assertTrue(event.hasDeadKey());
        Assert.assertEquals(0x60, event.combiningAccentCodePoint());
    }

    @Test
    public void invalidUnicodeScalarsNeverBecomeText() {
        ProjectKeyEvent surrogate = HardwareEventNormalizer.normalize(rawDown(0xd800).build());
        ProjectKeyEvent outOfRange = HardwareEventNormalizer.normalize(rawDown(0x110000).build());

        Assert.assertEquals("", surrogate.text());
        Assert.assertFalse(surrogate.hasSemanticInput());
        Assert.assertEquals("", outOfRange.text());
        Assert.assertFalse(outOfRange.hasSemanticInput());
    }

    @Test
    public void unsafeModifiersSuppressTextAndMappedDelete() {
        assertNoSemantic(rawDown('a').ctrl(true).build());
        assertNoSemantic(rawDown('a').alt(true).build());
        assertNoSemantic(rawDown('a').meta(true).build());
        assertNoSemantic(rawDown('a').function(true).build());
        assertNoSemantic(rawDown('a').sym(true).build());
        assertNoSemantic(rawDown(0).ctrl(true).mappedInput(SemanticInput.deleteBackward()).build());
    }

    @Test
    public void shiftAndCapsMayProduceTextButControlCharactersDoNot() {
        ProjectKeyEvent shifted = HardwareEventNormalizer.normalize(rawDown('A').shift(true).build());
        ProjectKeyEvent caps = HardwareEventNormalizer.normalize(rawDown('A').capsLock(true).build());
        ProjectKeyEvent control = HardwareEventNormalizer.normalize(rawDown('\n').build());

        Assert.assertEquals(SemanticInput.text("A"), shifted.semanticInput());
        Assert.assertEquals(SemanticInput.text("A"), caps.semanticInput());
        Assert.assertEquals("\n", control.text());
        Assert.assertFalse(control.hasSemanticInput());
    }

    @Test
    public void upRepeatAndCanceledEventsCarryNoSemanticInput() {
        assertNoSemantic(rawDown('a').repeatCount(1).build());
        assertNoSemantic(rawDown('a').canceled(true).build());
        assertNoSemantic(RawHardwareKeyEvent.builder(InputAction.UP, 29)
            .unicodeValue('a')
            .build());
        assertNoSemantic(RawHardwareKeyEvent.builder(InputAction.MULTIPLE, 29)
            .unicodeValue('a')
            .build());
    }

    @Test
    public void multipleActionCharactersArePreservedButDelegated() {
        ProjectKeyEvent event = HardwareEventNormalizer.normalize(
            RawHardwareKeyEvent.builder(InputAction.MULTIPLE, 0)
                .characters("A😀")
                .build()
        );

        Assert.assertEquals("A😀", event.text());
        Assert.assertFalse(event.hasSemanticInput());
        Assert.assertEquals(DispatchResult.delegate(), new InputDispatcher().dispatch(event));
    }

    private static RawHardwareKeyEvent.Builder rawDown(int unicodeValue) {
        return RawHardwareKeyEvent.builder(InputAction.DOWN, 29)
            .stableKeyId("hardware.key.a")
            .deviceId(7)
            .unicodeValue(unicodeValue);
    }

    private static void assertNoSemantic(RawHardwareKeyEvent raw) {
        Assert.assertFalse(HardwareEventNormalizer.normalize(raw).hasSemanticInput());
    }
}
