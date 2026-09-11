package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.text.InputType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * A physical keyboard typing Korean into a terminal — the half of issue #7 the first fix left.
 *
 * <p>The keys go the way the service sends them: a hardware event, normalised with the Dubeolsik
 * mapper, through the dispatcher and the Hangul processor, and the resulting plan executed whole
 * against a terminal-shaped editor.
 */
public final class TerminalHardwareKeyTest {
    private static final CheckedEditorExecutor EXECUTOR = new CheckedEditorExecutor();
    private static final EditorBounds CURSOR = EditorBounds.unknown();

    @Test
    public void aTerminalTakesMappedKeysThroughTheComposerAndPassesTheRest() {
        assertFalse("English on QWERTY: the plain passthrough a terminal always had",
            TerminalHardwareKeys.throughComposer(true, false));
        assertTrue("Korean, Colemak, Persian: the mapped keys are typed",
            TerminalHardwareKeys.throughComposer(true, true));
        assertTrue("an ordinary editor is unchanged",
            TerminalHardwareKeys.throughComposer(false, false));
    }

    @Test
    public void onlyAKeyThatTypesSomethingEndsTheSyllable() {
        assertTrue(TerminalHardwareKeys.endSyllableFirst(true, true, true, false));
        assertFalse("Shift on its own, held for the next letter",
            TerminalHardwareKeys.endSyllableFirst(true, true, true, true));
        assertFalse("a release or a repeat", TerminalHardwareKeys.endSyllableFirst(true, true, false, false));
        assertFalse("nothing being built", TerminalHardwareKeys.endSyllableFirst(true, false, true, false));
        assertFalse("an ordinary editor keeps its composing region",
            TerminalHardwareKeys.endSyllableFirst(false, true, true, false));
    }

    @Test
    public void physicalKeysBuildASyllableInATerminal() {
        Rig rig = new Rig();
        assertEquals(Arrays.asList("commit:ㄱ"), rig.press("r"));
        assertEquals(Arrays.asList("backspace", "commit:가"), rig.press("k"));
        assertEquals(Arrays.asList("backspace", "commit:간"), rig.press("s"));
    }

    @Test
    public void physicalBackspaceTakesTheSyllableApartInATerminal() {
        Rig rig = new Rig();
        rig.press("r");
        rig.press("k");
        assertEquals(Arrays.asList("backspace", "commit:ㄱ"), rig.backspace());
        assertEquals(Arrays.asList("backspace"), rig.backspace());
        // Nothing composing: a real backspace key for the character before it.
        assertEquals(Arrays.asList("backspace"), rig.backspace());
    }

    @Test
    public void aKeyPassedThroughEndsTheSyllableSoTheNextOneDoesNotEatIt() {
        // 가, then Space from the physical keyboard (no mapper claims it, so it goes to the
        // terminal as it is), then ㄴ. Without ending 가 first, ㄴ would join it — 간 — and take
        // back one character to redraw it: the Space.
        Rig hazard = new Rig();
        hazard.press("r");
        hazard.press("k");
        assertTrue("Space is not the composer's", hazard.passesThrough("space"));
        assertEquals("the hazard the service must prevent",
            Arrays.asList("backspace", "commit:간"), hazard.press("s"));

        Rig rig = new Rig();
        rig.press("r");
        rig.press("k");
        assertTrue(rig.passesThrough("space"));
        assertEquals("ending the syllable writes nothing: it is already there",
            new ArrayList<String>(), rig.endSyllable());
        assertEquals(Arrays.asList("commit:ㄴ"), rig.press("s"));
    }

    @Test
    public void aRedrawReachesATerminalAsTextOnly() {
        // The take-back and the syllable that replaces it must travel one channel. A backspace
        // key event goes through the view's input queue while committed text is written at once,
        // so the commit overtook the key and Termux garbled every longer word (0 of 11 eight-
        // syllable words intact under the emulator). The take-back is DEL, committed as text.
        Rig rig = new Rig();
        rig.press("r");
        rig.bridge.trace().clear();
        rig.press("k");
        assertEquals(
            Arrays.asList("beginBatchEdit", "commitText:length=1:cursor=1",
                "commitText:length=1:cursor=1", "endBatchEdit"),
            rig.bridge.trace());
    }

    /** A terminal, the dispatcher in front of it, and a record of what reaches it. */
    private static final class Rig {
        private final EditorProfile profile = AndroidEditorProfileClassifier.classify(
            termux(InputType.TYPE_NULL), 33);
        private final HangulInputProcessor processor = new HangulInputProcessor(() -> profile);
        private final InputDispatcher dispatcher = new InputDispatcher(processor);
        private final FakeEditorBridge bridge = new FakeEditorBridge();

        List<String> press(String letter) {
            String id = "hardware.key." + letter;
            return run(dispatcher.dispatch(hardwareDown(id,
                DubeolsikHardwareMapper.INSTANCE.map(id, false))));
        }

        List<String> backspace() {
            return run(dispatcher.dispatch(hardwareDown("hardware.edit.backspace",
                SemanticInput.deleteBackward())));
        }

        boolean passesThrough(String name) {
            String id = "hardware.key." + name;
            DispatchResult result = dispatcher.dispatch(hardwareDown(id,
                DubeolsikHardwareMapper.INSTANCE.map(id, false)));
            return !result.isHandled() && result.actions().isEmpty();
        }

        /** What the service does before a passed-through key (endSyllableBeforeDelegating). */
        List<String> endSyllable() {
            return run(dispatcher.dispatch(
                ProjectKeyEvent.softwareDown("hardware.flush", SemanticInput.flush())));
        }

        private List<String> run(DispatchResult result) {
            List<String> seen = new ArrayList<>();
            if (result.actions().isEmpty()) {
                return seen;
            }
            TransitionPlan<String> plan = TransitionPlan.of(
                1, 0, DispatchResult.Disposition.HANDLED, "state", CURSOR, result.actions());
            ExecutionResult outcome = EXECUTOR.execute(
                plan, ExecutionContext.active(1, 0, CURSOR, profile.capabilities()),
                () -> EditorEndpoint.of(1, bridge));
            if (outcome.outcome() != ExecutionResult.Outcome.DISPATCHED) {
                seen.add("refused:" + outcome.reason());
                return seen;
            }
            for (KeyAction action : result.actions()) {
                switch (action.kind()) {
                    case COMMIT_TEXT:
                        seen.add("commit:" + action.text());
                        break;
                    case DELETE_BACKWARD:
                    case DELETE_RECENT:
                        seen.add("backspace");
                        break;
                    default:
                        seen.add(action.kind().toString());
                }
            }
            return seen;
        }
    }

    private static ProjectKeyEvent hardwareDown(String stableKeyId, SemanticInput mapped) {
        return HardwareEventNormalizer.normalize(
            RawHardwareKeyEvent.builder(InputAction.DOWN, stableKeyId.hashCode() & 0xff | 1)
                .stableKeyId(stableKeyId)
                .deviceId(3)
                .mappedInput(mapped)
                .build());
    }

    private static android.view.inputmethod.EditorInfo termux(int inputType) {
        android.view.inputmethod.EditorInfo info = new android.view.inputmethod.EditorInfo();
        info.inputType = inputType;
        info.packageName = "com.termux";
        return info;
    }
}
