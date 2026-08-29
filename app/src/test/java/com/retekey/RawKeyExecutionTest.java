package com.retekey;

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

    /**
     * ★ 원격데스크톱 릴레이는 **metaState 를 안 본다** — 저쪽 OS 의 수식 상태를 세우는 것은
     * 진짜 Ctrl 키의 down/up 이다. 그래서 그 프로파일에서는 코드를 **수식키로 감싼다**:
     * Ctrl down → C down/up → Ctrl up.
     * ★★ 그리고 전부 **물리 키보드 모양으로 입혀** 보낸다(사용자 실기 2026-08-29): 소프트
     * 모양의 이벤트는 릴레이가 하나씩 처리해 Ctrl 따로, 글자 따로 원격에 넣었다. 물리 Ctrl+A
     * 는 되므로, 키보드 source + 실제 스캔코드 + 소프트 플래그 제거로 하드웨어 경로 — 수식
     * 상태를 추적해 조합하는 경로 — 를 태우고, 글자에는 meta 도 싣는다.
     */
    @Test
    public void aRemoteDesktopChordIsFramedByRealModifierKeys() {
        FakeEditorBridge bridge = new FakeEditorBridge();

        execute(
            bridge,
            RICH.withDeleteByKeyEvents(),
            KeyAction.rawKey(RawKey.C, EnumSet.of(KeyModifier.CTRL))
        );

        Assert.assertEquals(Arrays.asList(
            "sendRawKey:key=CTRL_LEFT:modifiers=[CTRL]:hw:action=DOWN",
            "sendRawKey:key=C:modifiers=[CTRL]:hw:action=DOWN",
            "sendRawKey:key=C:modifiers=[CTRL]:hw:action=UP",
            "sendRawKey:key=CTRL_LEFT:modifiers=[]:hw:action=UP"
        ), bridge.trace());
    }

    /** 수식키가 여럿이면 **누른 역순으로** 놓는다 — 실제 손가락이 그렇게 한다. */
    @Test
    public void aRemoteDesktopChordReleasesModifiersInReverse() {
        FakeEditorBridge bridge = new FakeEditorBridge();

        execute(
            bridge,
            RICH.withDeleteByKeyEvents(),
            KeyAction.rawKey(RawKey.RIGHT, EnumSet.of(KeyModifier.CTRL, KeyModifier.SHIFT))
        );

        Assert.assertEquals(Arrays.asList(
            "sendRawKey:key=CTRL_LEFT:modifiers=[CTRL]:hw:action=DOWN",
            "sendRawKey:key=SHIFT_LEFT:modifiers=[CTRL, SHIFT]:hw:action=DOWN",
            "sendRawKey:key=RIGHT:modifiers=[CTRL, SHIFT]:hw:action=DOWN",
            "sendRawKey:key=RIGHT:modifiers=[CTRL, SHIFT]:hw:action=UP",
            "sendRawKey:key=SHIFT_LEFT:modifiers=[CTRL]:hw:action=UP",
            "sendRawKey:key=CTRL_LEFT:modifiers=[]:hw:action=UP"
        ), bridge.trace());
    }

    /** ★ 로컬(리치) 편집기는 **그대로** — metaState 하나면 TextView 가 읽는다. 두 벌로 안 보낸다. */
    @Test
    public void aLocalChordStillRidesOnMetaStateAlone() {
        FakeEditorBridge bridge = new FakeEditorBridge();

        execute(bridge, RICH, KeyAction.rawKey(RawKey.C, EnumSet.of(KeyModifier.CTRL)));

        Assert.assertEquals(Arrays.asList(
            "sendRawKey:key=C:modifiers=[CTRL]:action=DOWN",
            "sendRawKey:key=C:modifiers=[CTRL]:action=UP"
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
