package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import org.junit.Test;

/**
 * The composer-to-editor-action lowering. The FSM itself is covered by {@link HangulComposerTest};
 * here we check that each composer result becomes the right commit / composing / raw actions.
 */
public final class HangulInputProcessorTest {
    private static final SemanticJamo GIYEOK = SemanticJamo.contextualConsonant(0);
    private static final SemanticJamo A = SemanticJamo.vowel(0);
    private static final EditorProfile RICH = EditorProfile.richText(
        false, false, false, false, false, 0, -1
    );

    private static HangulInputProcessor processor() {
        return new HangulInputProcessor(() -> RICH);
    }

    @Test
    public void aComposingJamoOnlySetsComposingText() {
        HangulInputProcessor processor = processor();
        assertEquals(
            Collections.singletonList(KeyAction.setComposingText("ㄱ")),
            processor.process(SemanticInput.jamo(GIYEOK)).actions()
        );
        assertEquals(
            Collections.singletonList(KeyAction.setComposingText("가")),
            processor.process(SemanticInput.jamo(A)).actions()
        );
        assertTrue(processor.isComposing());
    }

    @Test
    public void aClosedSyllableCommitsBeforeTheNewComposition() {
        HangulInputProcessor processor = processor();
        processor.process(SemanticInput.jamo(GIYEOK));
        processor.process(SemanticInput.jamo(A));       // 가, composing
        processor.process(SemanticInput.jamo(GIYEOK));  // 각, composing

        // 도깨비불: the new vowel commits 가 and starts a fresh 가.
        assertEquals(
            Arrays.asList(KeyAction.commitText("가"), KeyAction.setComposingText("가")),
            processor.process(SemanticInput.jamo(A)).actions()
        );
    }

    @Test
    public void textFlushesTheCompositionThenCommitsTheText() {
        HangulInputProcessor processor = processor();
        processor.process(SemanticInput.jamo(GIYEOK));
        processor.process(SemanticInput.jamo(A));       // 가, composing

        assertEquals(
            Arrays.asList(KeyAction.commitText("가"), KeyAction.commitText(" ")),
            processor.process(SemanticInput.text(" ")).actions()
        );
        assertFalse(processor.isComposing());
    }

    @Test
    public void textWithNoCompositionJustCommits() {
        assertEquals(
            Collections.singletonList(KeyAction.commitText("!")),
            processor().process(SemanticInput.text("!")).actions()
        );
    }

    @Test
    public void backspaceDecomposesTheComposition() {
        HangulInputProcessor processor = processor();
        processor.process(SemanticInput.jamo(GIYEOK));
        processor.process(SemanticInput.jamo(A));
        processor.process(SemanticInput.jamo(GIYEOK));  // 각

        assertEquals(
            Collections.singletonList(KeyAction.setComposingText("가")),
            processor.process(SemanticInput.deleteBackward()).actions()
        );
    }

    @Test
    public void backspaceWithNoCompositionDeletesInTheEditor() {
        assertEquals(
            Collections.singletonList(KeyAction.deleteBackward()),
            processor().process(SemanticInput.deleteBackward()).actions()
        );
    }

    @Test
    public void aRawKeyFlushesTheCompositionFirst() {
        HangulInputProcessor processor = processor();
        processor.process(SemanticInput.jamo(GIYEOK));
        processor.process(SemanticInput.jamo(A));       // 가, composing

        assertEquals(
            Arrays.asList(
                KeyAction.commitText("가"),
                KeyAction.rawKey(RawKey.RIGHT, EnumSet.noneOf(KeyModifier.class))
            ),
            processor.process(SemanticInput.rawKey(RawKey.RIGHT)).actions()
        );
    }

    @Test
    public void theEnterActionFlushesThenAppliesTheEditorAction() {
        EditorProfile search = EditorProfile.richText(false, false, false, false, false, 0, 3);
        HangulInputProcessor processor = new HangulInputProcessor(() -> search);
        processor.process(SemanticInput.jamo(GIYEOK));
        processor.process(SemanticInput.jamo(A));       // 가, composing

        assertEquals(
            Arrays.asList(KeyAction.commitText("가"), KeyAction.performEditorAction(3)),
            processor.process(SemanticInput.primaryAction()).actions()
        );
    }

    @Test
    public void resetDropsTheComposition() {
        HangulInputProcessor processor = processor();
        processor.process(SemanticInput.jamo(GIYEOK));
        assertTrue(processor.isComposing());
        processor.reset();
        assertFalse(processor.isComposing());
        // A jamo after reset starts fresh, not continuing the old syllable.
        assertEquals(
            Collections.singletonList(KeyAction.setComposingText("ㄱ")),
            processor.process(SemanticInput.jamo(GIYEOK)).actions()
        );
    }
}
