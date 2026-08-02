package dev.hellgates.retekeyime;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import org.junit.Assert;
import org.junit.Test;

/**
 * The raw-key path: a RAW_KEY action reaches the editor as a down/up key event, in both rich and
 * TYPE_NULL editors, carrying any chorded modifiers.
 */
public final class RawKeyExecutionTest {
    private static final CheckedEditorExecutor EXECUTOR = new CheckedEditorExecutor();
    private static final EditorBounds CURSOR = EditorBounds.of(1, 1, -1, -1);
    private static final EditorCapabilities RICH = EditorCapabilities.richText(false, false);
    private static final EditorCapabilities TYPE_NULL = EditorCapabilities.rawKey();

    @Test
    public void aRawArrowSendsADownAndUpToARichEditor() {
        FakeEditorBridge bridge = new FakeEditorBridge();

        ExecutionResult result = execute(
            bridge,
            RICH,
            KeyAction.rawKey(RawKey.RIGHT, Collections.emptySet())
        );

        Assert.assertEquals(Arrays.asList(
            "sendRawKey:key=RIGHT:modifiers=[]:action=DOWN",
            "sendRawKey:key=RIGHT:modifiers=[]:action=UP"
        ), bridge.trace());
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
    }

    @Test
    public void aRawArrowAlsoWorksInATypeNullEditor() {
        FakeEditorBridge bridge = new FakeEditorBridge();

        ExecutionResult result = execute(
            bridge,
            TYPE_NULL,
            KeyAction.rawKey(RawKey.HOME, Collections.emptySet())
        );

        Assert.assertEquals(Arrays.asList(
            "sendRawKey:key=HOME:modifiers=[]:action=DOWN",
            "sendRawKey:key=HOME:modifiers=[]:action=UP"
        ), bridge.trace());
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
    }

    @Test
    public void aChordCarriesItsModifiers() {
        FakeEditorBridge bridge = new FakeEditorBridge();

        execute(
            bridge,
            RICH,
            KeyAction.rawKey(RawKey.RIGHT, EnumSet.of(KeyModifier.CTRL))
        );

        Assert.assertEquals(Arrays.asList(
            "sendRawKey:key=RIGHT:modifiers=[CTRL]:action=DOWN",
            "sendRawKey:key=RIGHT:modifiers=[CTRL]:action=UP"
        ), bridge.trace());
    }

    @Test
    public void theStatelessProcessorLowersRawInputToARawAction() {
        StatelessInputProcessor processor = new ScaffoldInputProcessor();

        DispatchResult result = processor.process(
            SemanticInput.rawKey(RawKey.F5, EnumSet.of(KeyModifier.ALT))
        );

        Assert.assertEquals(
            Collections.singletonList(KeyAction.rawKey(RawKey.F5, EnumSet.of(KeyModifier.ALT))),
            result.actions()
        );
    }

    @Test
    public void aHeldKeySendsTheDownHalfAndLeavesItDown() {
        FakeEditorBridge bridge = new FakeEditorBridge();

        ExecutionResult result = execute(
            bridge,
            RICH,
            KeyAction.rawKey(RawKey.TAB, Collections.emptySet(), RawKeyPhase.HOLD)
        );

        Assert.assertEquals(Collections.singletonList(
            "sendRawKey:key=TAB:modifiers=[]:action=DOWN"
        ), bridge.trace());
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
    }

    @Test
    public void releasingAHeldKeySendsOnlyTheUpHalf() {
        FakeEditorBridge bridge = new FakeEditorBridge();

        ExecutionResult result = execute(
            bridge,
            RICH,
            KeyAction.rawKey(RawKey.TAB, Collections.emptySet(), RawKeyPhase.RELEASE)
        );

        Assert.assertEquals(Collections.singletonList(
            "sendRawKey:key=TAB:modifiers=[]:action=UP"
        ), bridge.trace());
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
    }

    @Test
    public void aHeldKeyAlsoWorksInATypeNullEditor() {
        FakeEditorBridge bridge = new FakeEditorBridge();

        execute(
            bridge,
            TYPE_NULL,
            KeyAction.rawKey(RawKey.TAB, Collections.emptySet(), RawKeyPhase.HOLD)
        );

        Assert.assertEquals(Collections.singletonList(
            "sendRawKey:key=TAB:modifiers=[]:action=DOWN"
        ), bridge.trace());
    }

    @Test
    public void aRawKeyIsAWholeTapUnlessItSaysOtherwise() {
        Assert.assertEquals(
            RawKeyPhase.TAP,
            KeyAction.rawKey(RawKey.TAB, Collections.emptySet()).rawKeyPhase()
        );
        Assert.assertEquals(
            RawKeyPhase.TAP,
            SemanticInput.rawKey(RawKey.TAB).rawKeyPhase()
        );
    }

    @Test
    public void theProcessorCarriesTheHalfItWasGiven() {
        StatelessInputProcessor processor = new ScaffoldInputProcessor();

        DispatchResult result = processor.process(
            SemanticInput.rawKey(RawKey.TAB, Collections.emptySet(), RawKeyPhase.HOLD)
        );

        Assert.assertEquals(
            Collections.singletonList(
                KeyAction.rawKey(RawKey.TAB, Collections.emptySet(), RawKeyPhase.HOLD)),
            result.actions()
        );
    }

    @Test
    public void aHalfPressKeepsItsHalfWhenModifiersAreFoldedIn() {
        SemanticInput held = SemanticInput
            .rawKey(RawKey.TAB, Collections.emptySet(), RawKeyPhase.HOLD)
            .withModifiers(EnumSet.of(KeyModifier.CTRL));

        Assert.assertEquals(RawKeyPhase.HOLD, held.rawKeyPhase());
        Assert.assertEquals(EnumSet.of(KeyModifier.CTRL), held.modifiers());
    }

    private static ExecutionResult execute(
        FakeEditorBridge bridge,
        EditorCapabilities capabilities,
        KeyAction action
    ) {
        TransitionPlan<String> plan = TransitionPlan.of(
            1,
            0,
            DispatchResult.Disposition.HANDLED,
            "state",
            CURSOR,
            Collections.singletonList(action)
        );
        return EXECUTOR.execute(
            plan,
            ExecutionContext.active(1, 0, CURSOR, capabilities),
            () -> EditorEndpoint.of(1, bridge)
        );
    }
}
