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
 * Building a syllable for an editor that never sees it — the answer to the whole take-back class
 * behind issue #7. A terminal has no composing region, so until now a syllable was drawn into it
 * as text and taken back with every jamo: a flickering cursor, an erase character that had to
 * reach the far side in the right order, and a cursor moved by anything the keyboard cannot see
 * (a click, Termux's own arrow buttons) dragging the syllable along with it.
 *
 * <p>Here the syllable lives on the keyboard's own strip instead, and only what closes is sent.
 * Nothing is ever taken back, so none of those can happen.
 */
public final class TerminalStripCompositionTest {
    private static EditorProfile terminalWithTheStrip() {
        return terminal().composingOffScreen();
    }

    private static EditorProfile terminal() {
        android.view.inputmethod.EditorInfo info = new android.view.inputmethod.EditorInfo();
        info.inputType = InputType.TYPE_NULL;
        info.packageName = "com.termux";
        info.initialSelStart = -1;
        info.initialSelEnd = -1;
        return AndroidEditorProfileClassifier.classify(info, 33);
    }

    private static SemanticInput consonant(int index) {
        return SemanticInput.jamo(SemanticJamo.contextualConsonant(index));
    }

    private static SemanticInput vowel(int index) {
        return SemanticInput.jamo(SemanticJamo.vowel(index));
    }

    @Test
    public void onlyAClosedSyllableReachesTheTerminal() {
        EditorProfile profile = terminalWithTheStrip();
        HangulInputProcessor processor = new HangulInputProcessor(() -> profile);
        List<String> strip = new ArrayList<>();
        List<String> editor = new ArrayList<>();
        // ㄱ ㅏ ㄴ ㅏ: 가 closes only when the ㅏ takes the ㄴ away from it.
        for (SemanticInput key : Arrays.asList(consonant(0), vowel(0), consonant(2), vowel(0))) {
            editor.addAll(whatTheEditorSees(processor.process(key)));
            strip.add(processor.composingText());
        }
        assertEquals("nothing is taken back, and only 가 is finished",
            Arrays.asList("commitText:가"), editor);
        assertEquals("the syllable is built on the strip",
            Arrays.asList("ㄱ", "가", "간", "나"), strip);
    }

    @Test
    public void backspaceInsideASyllableTouchesTheTerminalNotAtAll() {
        EditorProfile profile = terminalWithTheStrip();
        HangulInputProcessor processor = new HangulInputProcessor(() -> profile);
        processor.process(consonant(0));
        processor.process(vowel(0));
        List<String> editor = whatTheEditorSees(
            processor.process(SemanticInput.deleteBackward()));
        assertTrue("the strip loses the vowel; the terminal is not written to: " + editor,
            editor.isEmpty());
        assertEquals("ㄱ", processor.composingText());
    }

    @Test
    public void theSyllableIsWrittenWhenSomethingElseEndsIt() {
        EditorProfile profile = terminalWithTheStrip();
        HangulInputProcessor processor = new HangulInputProcessor(() -> profile);
        processor.process(consonant(0));
        processor.process(vowel(0));
        assertEquals(Arrays.asList("commitText:가"),
            whatTheEditorSees(processor.process(SemanticInput.flush())));
        assertEquals("", processor.composingText());
    }

    @Test
    public void backspaceWithNothingOnTheStripIsARealDelete() {
        EditorProfile profile = terminalWithTheStrip();
        HangulInputProcessor processor = new HangulInputProcessor(() -> profile);
        List<String> editor = whatTheEditorSees(
            processor.process(SemanticInput.deleteBackward()));
        assertEquals(Arrays.asList("deleteBackward"), editor);
    }

    @Test
    public void aTerminalIsTheOnlyEditorThisAppliesTo() {
        assertTrue(TerminalCompositionSettings.appliesTo(terminal(), true));
        assertFalse("the owner can have the old drawing back",
            TerminalCompositionSettings.appliesTo(terminal(), false));
        assertFalse("a remote desktop reports a cursor and composes in place",
            TerminalCompositionSettings.appliesTo(
                AndroidEditorProfileClassifier.classifyFields(
                    InputType.TYPE_CLASS_TEXT, 0, false, 0, 33).withDeleteByKeyEvents(), true));
        assertFalse("and it is never applied twice",
            TerminalCompositionSettings.appliesTo(terminalWithTheStrip(), true));
    }

    @Test
    public void theIdleClockIsNotNeededOnceNothingCanBeTakenBack() {
        // The 1.5 s settle exists only to drop a claim on text already written. With the strip
        // there is no such claim, so a syllable may wait as long as the user likes.
        EditorProfile profile = terminalWithTheStrip();
        boolean materialises = profile.capabilities().deleteByKeyEvents()
            && !profile.capabilities().composesOffScreen();
        assertFalse(IdleSyllableSettle.shouldArm(materialises, true));
    }

    /** The actions the editor would receive, in order — composing calls never reach it. */
    private static List<String> whatTheEditorSees(DispatchResult result) {
        List<String> seen = new ArrayList<>();
        for (KeyAction action : result.actions()) {
            switch (action.kind()) {
                case COMMIT_TEXT:
                    seen.add("commitText:" + action.text());
                    break;
                case SET_COMPOSING_TEXT:
                    seen.add("setComposingText:" + action.text());
                    break;
                case FINISH_COMPOSING:
                    seen.add("finishComposing");
                    break;
                case DELETE_BACKWARD:
                    seen.add("deleteBackward");
                    break;
                case DELETE_RECENT:
                    seen.add("deleteRecent:" + action.recentCount());
                    break;
                default:
                    seen.add(action.kind().toString());
            }
        }
        return seen;
    }
}
