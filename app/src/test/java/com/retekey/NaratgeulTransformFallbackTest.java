package com.retekey;

import static org.junit.Assert.assertEquals;

import com.retekey.SemanticInput.Transform;
import java.util.List;
import org.junit.Test;

/**
 * 획추가/쌍자음 after the interpreter's run has been lost — a session restart, a hardware key, a
 * layer switch — arrive as {@link SemanticInput.Kind#TRANSFORM} and are resolved against what is
 * actually on screen: the composing syllable when there is one, and the character before the
 * cursor when there is not. The letter the user is looking at transforms either way.
 */
public final class NaratgeulTransformFallbackTest {

    /** Tier one: the composing syllable's batchim transforms, no interpreter memory needed. */
    @Test
    public void strokeActsOnTheComposingBatchim() {
        Editor editor = new Editor("");
        editor.jamo(SemanticJamo.contextualConsonant(0));   // ㄱ
        editor.jamo(SemanticJamo.vowel(0));                 // ㅏ
        editor.jamo(SemanticJamo.contextualConsonant(3));   // ㄷ → 갇
        editor.transform(Transform.STROKE);                 // ㄷ → ㅌ
        assertEquals("같", editor.text());
    }

    /** Tier one still re-opens a syllable the consonant just closed, the way the live run does. */
    @Test
    public void strokeStillAssemblesACompoundFinalAcrossACommit() {
        Editor editor = new Editor("");
        editor.jamo(SemanticJamo.contextualConsonant(6));   // ㅁ
        editor.jamo(SemanticJamo.vowel(0));                 // ㅏ
        editor.jamo(SemanticJamo.contextualConsonant(2));   // ㄴ → 만
        editor.jamo(SemanticJamo.contextualConsonant(11));  // ㅇ commits 만, composes ㅇ
        editor.transform(Transform.STROKE);                 // ㅇ → ㅎ, and 만 re-opens
        assertEquals("많", editor.text());
    }

    @Test
    public void strokeIotatesTheComposingVowel() {
        Editor editor = new Editor("");
        editor.jamo(SemanticJamo.contextualConsonant(0));   // ㄱ
        editor.jamo(SemanticJamo.vowel(0));                 // ㅏ
        editor.transform(Transform.STROKE);
        assertEquals("갸", editor.text());
        editor.transform(Transform.TWIN);                   // a vowel has no double
        assertEquals("갸", editor.text());
    }

    /** Tier two: nothing composing, and the character before the cursor transforms instead. */
    @Test
    public void strokeActsOnTheCommittedSyllableBeforeTheCursor() {
        Editor editor = new Editor("각");
        editor.transform(Transform.STROKE);                 // batchim ㄱ → ㅋ
        assertEquals("갘", editor.text());
    }

    @Test
    public void twinActsOnACommittedBareJamo() {
        Editor editor = new Editor("ㅅ");
        editor.transform(Transform.TWIN);
        assertEquals("ㅆ", editor.text());
    }

    @Test
    public void strokeIotatesACommittedOpenSyllable() {
        Editor editor = new Editor("가");
        editor.transform(Transform.STROKE);
        assertEquals("갸", editor.text());
    }

    /** A double that cannot be a batchim splits the syllable, exactly as it does live. */
    @Test
    public void twinOnACommittedBatchimSplitsWhenTheDoubleCannotStay() {
        Editor editor = new Editor("갇");
        editor.transform(Transform.TWIN);                   // ㄷ → ㄸ, no ㄸ batchim
        assertEquals("가ㄸ", editor.text());
        editor.jamo(SemanticJamo.vowel(0));                 // and typing continues: ㅏ
        assertEquals("가따", editor.text());
    }

    /** A compound final's tail transforms; here ㅎ of 많 gives back ㅇ, which cannot rejoin ㄴ. */
    @Test
    public void strokeUnwindsACommittedCompoundFinal() {
        Editor editor = new Editor("많");
        editor.transform(Transform.STROKE);                 // ㅎ → ㅇ
        assertEquals("만ㅇ", editor.text());
    }

    /** The recovered syllable is composing again: the next vowel moves its batchim on. */
    @Test
    public void aRecoveredSyllableKeepsComposing() {
        Editor editor = new Editor("각");
        editor.transform(Transform.STROKE);                 // 갘 composing
        editor.jamo(SemanticJamo.vowel(0));                 // ㅏ — batchim moves
        assertEquals("가카", editor.text());
    }

    @Test
    public void aTransformWithNothingToActOnDoesNothing() {
        Editor empty = new Editor("");
        empty.transform(Transform.STROKE);
        assertEquals("", empty.text());

        Editor latin = new Editor("z");
        latin.transform(Transform.STROKE);
        assertEquals("z", latin.text());

        Editor rieul = new Editor("라");
        rieul.transform(Transform.TWIN);                    // ㄹ has no double, ㅏ neither
        assertEquals("라", rieul.text());
    }

    /** Repeated presses keep cycling, each resolved fresh from the screen. */
    @Test
    public void repeatedTransformsCycle() {
        Editor editor = new Editor("각");
        editor.transform(Transform.STROKE);
        assertEquals("갘", editor.text());
        editor.transform(Transform.STROKE);
        assertEquals("각", editor.text());
        editor.transform(Transform.TWIN);
        assertEquals("갂", editor.text());
    }

    /** Committed text plus composing region, with the processor reading its own text back. */
    private static final class Editor {
        private final HangulInputProcessor processor;
        private final StringBuilder committed = new StringBuilder();
        private String composing = "";

        Editor(String initial) {
            committed.append(initial);
            processor = new HangulInputProcessor(
                EditorProfile::unsupported,
                () -> {
                    String text = text();
                    return text.isEmpty() ? null : text.substring(text.length() - 1);
                }
            );
        }

        void jamo(SemanticJamo jamo) {
            apply(processor.process(SemanticInput.jamo(jamo)));
        }

        void transform(Transform kind) {
            apply(processor.process(SemanticInput.transform(kind)));
        }

        private void apply(DispatchResult result) {
            for (KeyAction action : result.actions()) {
                switch (action.kind()) {
                    case COMMIT_TEXT:
                        composing = "";
                        committed.append(action.text());
                        break;
                    case SET_COMPOSING_TEXT:
                        composing = action.text();
                        break;
                    case DELETE_BACKWARD:
                        if (!composing.isEmpty()) {
                            composing = composing.substring(0, composing.length() - 1);
                        } else if (committed.length() > 0) {
                            committed.deleteCharAt(committed.length() - 1);
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        String text() {
            return committed.toString() + composing;
        }
    }
}
