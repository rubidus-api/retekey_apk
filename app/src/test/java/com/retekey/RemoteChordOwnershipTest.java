package com.retekey;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

/**
 * A remote-desktop chord presses real modifier keys around the key (manual chapter 15a), so it
 * owns every modifier it pressed until it has let it go again. Whatever goes wrong on the way —
 * a refused or throwing press, the editor changing under it — each modifier pressed is released,
 * in reverse, on the connection it was pressed on; nothing pressed after a failure; nothing sent
 * to whichever editor replaced it; and the result says a key may have been left down.
 * (Review finding R01.)
 */
public final class RemoteChordOwnershipTest {
    private static final EditorBounds CURSOR = EditorBounds.of(1, 1, -1, -1);
    private static final EditorCapabilities REMOTE =
        EditorCapabilities.richText(false, false).withDeleteByKeyEvents();

    private static final String CTRL_DOWN = "sendRawKey:key=CTRL_LEFT:modifiers=[CTRL]:hw:action=DOWN";
    private static final String CTRL_UP = "sendRawKey:key=CTRL_LEFT:modifiers=[]:hw:action=UP";
    private static final String C_DOWN = "sendRawKey:key=C:modifiers=[CTRL]:hw:action=DOWN";
    private static final String C_UP = "sendRawKey:key=C:modifiers=[CTRL]:hw:action=UP";
    private static final String SHIFT_DOWN =
        "sendRawKey:key=SHIFT_LEFT:modifiers=[CTRL, SHIFT]:hw:action=DOWN";
    private static final String SHIFT_UP = "sendRawKey:key=SHIFT_LEFT:modifiers=[CTRL]:hw:action=UP";
    private static final String RIGHT_DOWN =
        "sendRawKey:key=RIGHT:modifiers=[CTRL, SHIFT]:hw:action=DOWN";
    private static final String RIGHT_UP =
        "sendRawKey:key=RIGHT:modifiers=[CTRL, SHIFT]:hw:action=UP";

    private static final KeyAction CTRL_C = KeyAction.rawKey(RawKey.C, EnumSet.of(KeyModifier.CTRL));
    private static final KeyAction CTRL_SHIFT_RIGHT =
        KeyAction.rawKey(RawKey.RIGHT, EnumSet.of(KeyModifier.CTRL, KeyModifier.SHIFT));

    @Test
    public void aCleanChordIsFourKeysAndDispatched() {
        FakeEditorBridge bridge = new FakeEditorBridge();

        ExecutionResult result = execute(bridge, new AtomicBoolean(true), CTRL_C);

        Assert.assertEquals(Arrays.asList(CTRL_DOWN, C_DOWN, C_UP, CTRL_UP), bridge.trace());
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
        Assert.assertEquals(4, result.operationCount());
    }

    /** R01a: the session ends right after Ctrl goes down. Ctrl comes up; C is never sent. */
    @Test
    public void aSessionEndingAfterTheModifierReleasesItAndSendsNoKey() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        AtomicBoolean current = new AtomicBoolean(true);
        bridge.runAt(1, () -> current.set(false));

        ExecutionResult result = execute(bridge, current, CTRL_C);

        Assert.assertEquals(Arrays.asList(CTRL_DOWN, CTRL_UP), bridge.trace());
        assertUncertain(result, ExecutionResult.Reason.SESSION_CHANGED_DURING_EXECUTION);
        Assert.assertEquals(0, result.dispatchedMutationCount());
    }

    /** R01b: a refused Ctrl must not turn Ctrl+C into a bare C, reported as a shortcut. */
    @Test
    public void aRefusedModifierIsReleasedAndTheKeyIsNotSent() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.returnAt(1, EditorCallResult.rejected());

        ExecutionResult result = execute(bridge, new AtomicBoolean(true), CTRL_C);

        Assert.assertEquals(Arrays.asList(CTRL_DOWN, CTRL_UP), bridge.trace());
        assertUncertain(result, ExecutionResult.Reason.OPERATION_FALSE);
        Assert.assertEquals(0, result.failedOperationIndex());
    }

    @Test
    public void aThrowingModifierIsReleasedAndTheKeyIsNotSent() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.throwAt(1);

        ExecutionResult result = execute(bridge, new AtomicBoolean(true), CTRL_C);

        Assert.assertEquals(Arrays.asList(CTRL_DOWN, CTRL_UP), bridge.trace());
        assertUncertain(result, ExecutionResult.Reason.OPERATION_RUNTIME_FAILURE);
    }

    /** Only what was pressed is released: Shift failed, so Shift and Ctrl come up — nothing else. */
    @Test
    public void aSecondModifierFailingReleasesBothAndSendsNoKey() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.returnAt(2, EditorCallResult.rejected());

        ExecutionResult result = execute(bridge, new AtomicBoolean(true), CTRL_SHIFT_RIGHT);

        Assert.assertEquals(
            Arrays.asList(CTRL_DOWN, SHIFT_DOWN, SHIFT_UP, CTRL_UP), bridge.trace());
        assertUncertain(result, ExecutionResult.Reason.OPERATION_FALSE);
        Assert.assertEquals(1, result.failedOperationIndex());
    }

    @Test
    public void aSessionEndingBetweenModifiersPressesNoMore() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        AtomicBoolean current = new AtomicBoolean(true);
        bridge.runAt(1, () -> current.set(false));

        ExecutionResult result = execute(bridge, current, CTRL_SHIFT_RIGHT);

        Assert.assertEquals(Arrays.asList(CTRL_DOWN, CTRL_UP), bridge.trace());
        assertUncertain(result, ExecutionResult.Reason.SESSION_CHANGED_DURING_EXECUTION);
    }

    /** Stale before anything was pressed: nothing was sent, so nothing needs letting go. */
    @Test
    public void aSessionEndingBeforeTheFirstModifierSendsNothing() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        AtomicInteger checks = new AtomicInteger();

        TransitionPlan<String> plan = plan(CTRL_C);
        ExecutionResult result = new CheckedEditorExecutor().execute(
            plan,
            ExecutionContext.active(1, 0, CURSOR, REMOTE),
            // Current for the executor's own check on resolving, gone by the first key.
            () -> EditorEndpoint.of(1, bridge).guardedBy(() -> checks.incrementAndGet() <= 1));

        Assert.assertEquals(Collections.emptyList(), bridge.trace());
        Assert.assertEquals(ExecutionResult.Outcome.NOT_DISPATCHED, result.outcome());
        Assert.assertEquals(
            ExecutionResult.Reason.SESSION_CHANGED_DURING_EXECUTION, result.reason());
        Assert.assertFalse(result.remoteMutationMayHaveOccurred());
    }

    /** The key itself refused: its up still goes, and so does Ctrl's. */
    @Test
    public void aRefusedKeyStillReleasesEverything() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.returnAt(2, EditorCallResult.rejected());

        ExecutionResult result = execute(bridge, new AtomicBoolean(true), CTRL_C);

        Assert.assertEquals(Arrays.asList(CTRL_DOWN, C_DOWN, C_UP, CTRL_UP), bridge.trace());
        assertUncertain(result, ExecutionResult.Reason.OPERATION_FALSE);
        Assert.assertEquals(1, result.failedOperationIndex());
    }

    /** Stale just before the key: Ctrl comes up, C is not sent. */
    @Test
    public void aSessionEndingBeforeTheKeyReleasesTheModifier() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        AtomicBoolean current = new AtomicBoolean(true);
        bridge.runAt(2, () -> current.set(false));

        ExecutionResult result = execute(bridge, current, CTRL_SHIFT_RIGHT);

        Assert.assertEquals(
            Arrays.asList(CTRL_DOWN, SHIFT_DOWN, SHIFT_UP, CTRL_UP), bridge.trace());
        assertUncertain(result, ExecutionResult.Reason.SESSION_CHANGED_DURING_EXECUTION);
    }

    /** Once C went down on this connection, its up and Ctrl's belong to the same connection. */
    @Test
    public void aSessionEndingAfterTheKeyWentDownFinishesTheChordWhereItStarted() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        AtomicBoolean current = new AtomicBoolean(true);
        bridge.runAt(2, () -> current.set(false));

        ExecutionResult result = execute(bridge, current, CTRL_C);

        Assert.assertEquals(Arrays.asList(CTRL_DOWN, C_DOWN, C_UP, CTRL_UP), bridge.trace());
        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
    }

    /** A release that fails may leave Ctrl down on the far side: that is not a clean dispatch. */
    @Test
    public void aRefusedReleaseIsReportedAsCleanup() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.returnAt(4, EditorCallResult.rejected());

        ExecutionResult result = execute(bridge, new AtomicBoolean(true), CTRL_C);

        Assert.assertEquals(Arrays.asList(CTRL_DOWN, C_DOWN, C_UP, CTRL_UP), bridge.trace());
        assertUncertain(result, ExecutionResult.Reason.OPERATION_FALSE);
        Assert.assertEquals(ExecutionResult.Reason.OPERATION_FALSE, result.cleanupReason());
        Assert.assertEquals(3, result.cleanupOperationIndex());
        Assert.assertEquals(1, result.dispatchedMutationCount());
    }

    /** One release throwing does not stop the next one. */
    @Test
    public void aThrowingReleaseDoesNotSkipTheOthers() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        bridge.throwAt(5);

        ExecutionResult result = execute(bridge, new AtomicBoolean(true), CTRL_SHIFT_RIGHT);

        Assert.assertEquals(
            Arrays.asList(CTRL_DOWN, SHIFT_DOWN, RIGHT_DOWN, RIGHT_UP, SHIFT_UP, CTRL_UP),
            bridge.trace());
        assertUncertain(result, ExecutionResult.Reason.OPERATION_RUNTIME_FAILURE);
        Assert.assertEquals(
            ExecutionResult.Reason.OPERATION_RUNTIME_FAILURE, result.cleanupReason());
        Assert.assertEquals(4, result.cleanupOperationIndex());
    }

    /** Whatever happens to the old editor, the one that replaced it hears nothing of the chord. */
    @Test
    public void theEditorThatReplacedTheSessionIsNeverWrittenTo() {
        FakeEditorBridge old = new FakeEditorBridge();
        FakeEditorBridge replacement = new FakeEditorBridge();
        AtomicBoolean current = new AtomicBoolean(true);
        AtomicInteger resolves = new AtomicInteger();
        old.runAt(1, () -> current.set(false));

        new CheckedEditorExecutor().execute(
            plan(CTRL_C),
            ExecutionContext.active(1, 0, CURSOR, REMOTE),
            () -> resolves.incrementAndGet() == 1
                ? EditorEndpoint.of(1, old).guardedBy(current::get)
                : EditorEndpoint.of(1, replacement));

        Assert.assertEquals(Arrays.asList(CTRL_DOWN, CTRL_UP), old.trace());
        Assert.assertEquals(Collections.emptyList(), replacement.trace());
    }

    /**
     * A syllable written ahead of a key has landed even if the key then cannot go: the result
     * must keep that, or the composer would still think the syllable is in progress.
     */
    @Test
    public void aKeyLostAfterAWriteStillReportsTheWrite() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        AtomicBoolean current = new AtomicBoolean(true);
        TransitionPlan<String> plan = plan(
            KeyAction.commitText("가"), KeyAction.rawEnter());
        // begin, commit, end — then the session is gone before Enter.
        bridge.runAt(3, () -> current.set(false));

        ExecutionResult result = new CheckedEditorExecutor().execute(
            plan,
            ExecutionContext.active(1, 0, CURSOR, EditorCapabilities.richText(false, false)),
            () -> EditorEndpoint.of(1, bridge).guardedBy(current::get));

        Assert.assertTrue(bridge.trace().contains("commitText:length=1:cursor=1"));
        Assert.assertFalse(bridge.trace().toString().contains("ENTER"));
        Assert.assertEquals(ExecutionResult.Outcome.UNCERTAIN, result.outcome());
        Assert.assertEquals(
            ExecutionResult.StateEffect.RESET_DESYNCHRONIZED, result.stateEffect());
        Assert.assertTrue(result.remoteMutationMayHaveOccurred());
        Assert.assertEquals(1, result.dispatchedMutationCount());
    }

    @Test
    public void aWriteThenAKeyThatBothLandIsDispatched() {
        FakeEditorBridge bridge = new FakeEditorBridge();

        ExecutionResult result = new CheckedEditorExecutor().execute(
            plan(KeyAction.commitText("가"), KeyAction.rawEnter()),
            ExecutionContext.active(1, 0, CURSOR, EditorCapabilities.richText(false, false)),
            () -> EditorEndpoint.of(1, bridge));

        Assert.assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
        Assert.assertTrue(bridge.trace().toString().contains("key=ENTER"));
    }

    private static void assertUncertain(ExecutionResult result, ExecutionResult.Reason reason) {
        Assert.assertEquals(ExecutionResult.Outcome.UNCERTAIN, result.outcome());
        Assert.assertEquals(reason, result.reason());
        Assert.assertEquals(
            ExecutionResult.StateEffect.RESET_DESYNCHRONIZED, result.stateEffect());
        // A modifier may be down on the far side: the service must not hand the event on.
        Assert.assertTrue(result.remoteMutationMayHaveOccurred());
    }

    private static TransitionPlan<String> plan(KeyAction... actions) {
        return TransitionPlan.of(
            1, 0, DispatchResult.Disposition.HANDLED, "state", CURSOR, Arrays.asList(actions));
    }

    private static ExecutionResult execute(
        FakeEditorBridge bridge,
        AtomicBoolean current,
        KeyAction action
    ) {
        List<KeyAction> actions = Collections.singletonList(action);
        return new CheckedEditorExecutor().execute(
            TransitionPlan.of(1, 0, DispatchResult.Disposition.HANDLED, "state", CURSOR, actions),
            ExecutionContext.active(1, 0, CURSOR, REMOTE),
            () -> EditorEndpoint.of(1, bridge).guardedBy(current::get));
    }
}
