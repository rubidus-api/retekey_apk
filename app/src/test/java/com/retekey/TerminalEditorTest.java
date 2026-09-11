package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.text.InputType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 * Typing into a terminal — issue #7, reported against Termux.
 *
 * <p>A terminal is a shape this keyboard's editor model did not have. It has no composing region
 * at all: whatever the IME marks as composing is invisible until it is committed. Termux offers
 * two of them, and the reporter found each broken in its own way.
 *
 * <ul>
 *   <li><b>enforce-char-based-input = true</b> — the view reports {@code TYPE_NULL}. Korean
 *       appeared only when a syllable closed, and backspace did nothing at all.</li>
 *   <li><b>false</b> — the view reports {@code TYPE_TEXT_VARIATION_VISIBLE_PASSWORD} with no
 *       suggestions. Backspace worked; Korean did nothing whatever.</li>
 * </ul>
 *
 * <p>The two editor kinds are written out here as Termux presents them, so the fix is checked
 * against the reported shapes rather than against a guess about them.
 */
public final class TerminalEditorTest {
    private static final CheckedEditorExecutor EXECUTOR = new CheckedEditorExecutor();
    // A terminal does not say where its cursor is; the keyboard is told -1 and must cope. Using
    // a known cursor here would have tested a phone that does not exist.
    private static final EditorBounds CURSOR = EditorBounds.unknown();

    /** Termux with enforce-char-based-input = true: the view reports TYPE_NULL. */
    private static EditorProfile charBased() {
        return AndroidEditorProfileClassifier.classify(termux(InputType.TYPE_NULL), 33);
    }

    /** Termux with enforce-char-based-input = false: an ordinary visible-password text field. */
    private static EditorProfile textBased() {
        return AndroidEditorProfileClassifier.classify(termux(
            InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS), 33);
    }

    private static android.view.inputmethod.EditorInfo termux(int inputType) {
        android.view.inputmethod.EditorInfo info = new android.view.inputmethod.EditorInfo();
        info.inputType = inputType;
        info.packageName = "com.termux";
        // Measured on Termux 0.118.3: the terminal view reports no cursor, in either mode. Its
        // own toolbar EditText does report one, and must not be taken for the terminal.
        info.initialSelStart = -1;
        info.initialSelEnd = -1;
        return info;
    }

    @Test
    public void aTerminalHasNoComposingRegionWhicheverModeItIsIn() {
        // Both are terminals, and a terminal cannot show composing text. Saying so once is what
        // lets one answer serve both: composition is materialised as commits.
        assertTrue(charBased().capabilities().deleteByKeyEvents());
        assertTrue(textBased().capabilities().deleteByKeyEvents());
    }

    @Test
    public void koreanReachesACharBasedTerminalAsItIsTyped() {
        // ㄱ, then ㅏ: the syllable must appear as it is built, not only when it closes. On a
        // terminal that means the ㄱ is taken back and 가 put in its place.
        assertEquals(
            java.util.Arrays.asList("commitText:ㄱ", "erase:DEL", "commitText:가"),
            typed(charBased()));
    }

    @Test
    public void backspaceReachesACharBasedTerminal() {
        FakeEditorBridge bridge = new FakeEditorBridge();
        ExecutionResult result = execute(bridge, charBased().capabilities(),
            KeyAction.deleteBackward());
        assertEquals(ExecutionResult.Outcome.DISPATCHED, result.outcome());
        assertTrue("a terminal is deleted with a key event: " + bridge.trace(),
            bridge.trace().toString().contains("BACKSPACE"));
    }

    @Test
    public void aVisiblePasswordFieldStillComposes() {
        // The reason Korean did nothing in the other mode: a field the keyboard treats as private
        // was one it refused to compose into. Nothing is hidden in a visible-password field — the
        // text is on the screen by definition — and in an ordinary login form with "show
        // password" ticked the refusal meant Korean could not be typed there at all.
        EditorCapabilities showPassword = AndroidEditorProfileClassifier.classifyFields(
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            0, false, 0, 33).capabilities();
        assertTrue("still private: never read, never remembered", showPassword.isSensitive());
        assertFalse("but it can be composed into",
            refuses(showPassword, KeyAction.setComposingText("가")));
    }

    @Test
    public void koreanReachesATextBasedTerminal() {
        assertEquals(
            java.util.Arrays.asList("commitText:ㄱ", "erase:DEL", "commitText:가"),
            typed(textBased()));
    }

    /** What the editor actually receives when ㄱ then ㅏ are typed, in order. */
    private static List<String> typed(EditorProfile profile) {
        HangulInputProcessor processor = new HangulInputProcessor(() -> profile);
        FakeEditorBridge bridge = new FakeEditorBridge();
        List<String> seen = new ArrayList<>();
        List<SemanticInput> keys = java.util.Arrays.asList(
            SemanticInput.jamo(SemanticJamo.contextualConsonant(0)),
            SemanticInput.jamo(SemanticJamo.vowel(0)));
        for (SemanticInput one : keys) {
            DispatchResult result = processor.process(one);
            // The service sends a dispatch result as ONE plan, not action by action; a terminal
            // used to refuse anything that was not a single action, which is what made a Korean
            // syllable — take back the jamo, commit the syllable — impossible there.
            bridge.trace().clear();
            ExecutionResult outcome = executePlan(bridge, profile.capabilities(),
                result.actions());
            if (outcome.outcome() != ExecutionResult.Outcome.DISPATCHED) {
                seen.add("refused:" + outcome.reason());
                continue;
            }
            for (KeyAction action : result.actions()) {
                if (action.kind() == KeyAction.Kind.COMMIT_TEXT) {
                    seen.add("commitText:" + action.text());
                } else if (action.kind() == KeyAction.Kind.SET_COMPOSING_TEXT) {
                    seen.add("setComposingText:" + action.text());
                } else if (action.kind() == KeyAction.Kind.DELETE_BACKWARD
                        || action.kind() == KeyAction.Kind.DELETE_RECENT) {
                    // A take-back in a terminal is the erase character committed as text, on the
                    // same channel as the syllable after it (see executeTerminalErase).
                    String calls = bridge.trace().toString();
                    seen.add(calls.contains("BACKSPACE") ? "sendRawKey:BACKSPACE"
                        : calls.contains("deleteSurrounding") ? "deleteSurrounding"
                        : "erase:DEL");
                }
            }
        }
        return seen;
    }

    private static boolean refuses(EditorCapabilities capabilities, KeyAction action) {
        return execute(new FakeEditorBridge(), capabilities, action).outcome()
            != ExecutionResult.Outcome.DISPATCHED;
    }

    private static ExecutionResult execute(
        FakeEditorBridge bridge, EditorCapabilities capabilities, KeyAction action) {
        return executePlan(bridge, capabilities, Collections.singletonList(action));
    }

    /** One plan, the way the service sends one. */
    private static ExecutionResult executePlan(
        FakeEditorBridge bridge, EditorCapabilities capabilities, List<KeyAction> actions) {
        TransitionPlan<String> plan = TransitionPlan.of(
            1, 0, DispatchResult.Disposition.HANDLED, "state", CURSOR, actions);
        return EXECUTOR.execute(
            plan, ExecutionContext.active(1, 0, CURSOR, capabilities),
            () -> EditorEndpoint.of(1, bridge));
    }
}
