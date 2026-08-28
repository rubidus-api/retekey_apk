package com.retekey;

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
    public void aCorrectionDeleteTakesBackTheSyllableItClosed() {
        HangulInputProcessor processor = processor();
        processor.process(SemanticInput.jamo(SemanticJamo.contextualConsonant(6)));  // ㅁ
        processor.process(SemanticInput.jamo(SemanticJamo.vowel(0)));                // 마
        processor.process(SemanticInput.jamo(SemanticJamo.contextualConsonant(2)));  // 만
        processor.process(SemanticInput.jamo(SemanticJamo.contextualConsonant(11))); // 만 + ㅇ

        // The automaton asks for its ㅇ back: clear it, take 만 out of the editor, compose it again.
        assertEquals(
            Arrays.asList(
                KeyAction.setComposingText(""),
                KeyAction.deleteBackward(),
                KeyAction.setComposingText("만")
            ),
            processor.process(SemanticInput.deleteForCorrection()).actions()
        );
        assertEquals(
            Collections.singletonList(KeyAction.setComposingText("많")),
            processor.process(SemanticInput.jamo(SemanticJamo.contextualConsonant(18))).actions()
        );
    }

    @Test
    public void theUsersOwnBackspaceJustDeletes() {
        HangulInputProcessor processor = processor();
        processor.process(SemanticInput.jamo(SemanticJamo.contextualConsonant(6)));
        processor.process(SemanticInput.jamo(SemanticJamo.vowel(0)));
        processor.process(SemanticInput.jamo(SemanticJamo.contextualConsonant(2)));
        processor.process(SemanticInput.jamo(SemanticJamo.contextualConsonant(11)));

        // The ㅇ goes, and 만 stays committed where the user can see it.
        assertEquals(
            Collections.singletonList(KeyAction.setComposingText("")),
            processor.process(SemanticInput.deleteBackward()).actions()
        );
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

    // ---- the Latin composer (Vietnamese Telex) riding beside the Hangul one ----

    @Test
    public void withTelexTheLettersComposeInsteadOfCommitting() {
        HangulInputProcessor processor = processor();
        processor.setLatinComposer(new TelexComposer());
        assertEquals(Collections.singletonList(KeyAction.setComposingText("v")),
            processor.process(SemanticInput.text("v")).actions());
        processor.process(SemanticInput.text("i"));
        processor.process(SemanticInput.text("e"));
        processor.process(SemanticInput.text("e"));
        assertEquals(Collections.singletonList(KeyAction.setComposingText("việ")),
            processor.process(SemanticInput.text("j")).actions());
    }

    @Test
    public void aSpaceCommitsTheTelexWordAndThenTypesItself() {
        HangulInputProcessor processor = processor();
        processor.setLatinComposer(new TelexComposer());
        for (String key : new String[] {"V", "i", "e", "e", "j", "t"}) {
            processor.process(SemanticInput.text(key));
        }
        assertTrue(processor.isComposing());
        assertEquals("Việt", processor.composingText());
        assertEquals(
            Arrays.asList(KeyAction.commitText("Việt"), KeyAction.commitText(" ")),
            processor.process(SemanticInput.text(" ")).actions());
        assertFalse(processor.isComposing());
    }

    @Test
    public void backspaceInsideATelexWordDropsAKeystrokeNotACharacter() {
        HangulInputProcessor processor = processor();
        processor.setLatinComposer(new TelexComposer());
        for (String key : new String[] {"d", "d", "a", "a", "t", "s"}) {
            processor.process(SemanticInput.text(key));
        }
        assertEquals(Collections.singletonList(KeyAction.setComposingText("đât")),
            processor.process(SemanticInput.deleteBackward()).actions());
    }

    @Test
    public void withoutTelexTheLettersCommitAsBefore() {
        HangulInputProcessor processor = processor();
        assertEquals(Collections.singletonList(KeyAction.commitText("v")),
            processor.process(SemanticInput.text("v")).actions());
    }

    @Test
    public void aJamoCommitsTheTelexWordFirst() {
        HangulInputProcessor processor = processor();
        processor.setLatinComposer(new TelexComposer());
        processor.process(SemanticInput.text("a"));
        processor.process(SemanticInput.text("a"));
        assertEquals(
            Arrays.asList(KeyAction.commitText("â"), KeyAction.setComposingText("ㄱ")),
            processor.process(SemanticInput.jamo(GIYEOK)).actions());
    }

    // ---- remote-desktop editors: composition materialised as key-event deletes and commits ----

    private static HangulInputProcessor remoteDesktopProcessor() {
        EditorProfile remote = RICH.withDeleteByKeyEvents();
        return new HangulInputProcessor(() -> remote);
    }

    @Test
    public void aRemoteDesktopEditorGetsCommitsAndDeletesInsteadOfComposition() {
        // 않 on MS Remote Desktop: its dummy buffer cannot carry a composing region to the far
        // machine (the owner's report: 않 arrived as 안), so every update is a backspace key
        // event plus a commit — both of which it always forwards.
        HangulInputProcessor processor = remoteDesktopProcessor();
        assertEquals(Collections.singletonList(KeyAction.commitText("ㅇ")),
            processor.process(SemanticInput.jamo(SemanticJamo.contextualConsonant(11))).actions());
        assertEquals(Arrays.asList(KeyAction.deleteRecent(1), KeyAction.commitText("아")),
            processor.process(SemanticInput.jamo(SemanticJamo.vowel(0))).actions());
        assertEquals(Arrays.asList(KeyAction.deleteRecent(1), KeyAction.commitText("안")),
            processor.process(SemanticInput.jamo(SemanticJamo.contextualConsonant(2))).actions());
        assertEquals(Arrays.asList(KeyAction.deleteRecent(1), KeyAction.commitText("않")),
            processor.process(SemanticInput.jamo(SemanticJamo.contextualConsonant(18))).actions());
        // A space flushes: the syllable is already on screen, so only the space itself goes out.
        assertEquals(Collections.singletonList(KeyAction.commitText(" ")),
            processor.process(SemanticInput.text(" ")).actions());
    }

    @Test
    public void aRemoteDesktopBackspaceRetypesTheShorterSyllable() {
        HangulInputProcessor processor = remoteDesktopProcessor();
        processor.process(SemanticInput.jamo(SemanticJamo.contextualConsonant(11)));
        processor.process(SemanticInput.jamo(SemanticJamo.vowel(0)));
        processor.process(SemanticInput.jamo(SemanticJamo.contextualConsonant(2)));
        processor.process(SemanticInput.jamo(SemanticJamo.contextualConsonant(18)));
        assertEquals(Arrays.asList(KeyAction.deleteRecent(1), KeyAction.commitText("안")),
            processor.process(SemanticInput.deleteBackward()).actions());
        assertEquals(Arrays.asList(KeyAction.deleteRecent(1), KeyAction.commitText("아")),
            processor.process(SemanticInput.deleteBackward()).actions());
    }

    @Test
    public void aClosedSyllableOnRemoteDesktopCommitsOnlyTheNewLetter() {
        HangulInputProcessor processor = remoteDesktopProcessor();
        processor.process(SemanticInput.jamo(SemanticJamo.contextualConsonant(11)));
        processor.process(SemanticInput.jamo(SemanticJamo.vowel(0)));
        // 아 + ㄱ = 악; 악 + ㅏ = the ㄱ moves on: 아 stays, 가 composes.
        processor.process(SemanticInput.jamo(SemanticJamo.contextualConsonant(0)));
        assertEquals("the composing 악 is retyped as the closed 아 and the new 가",
            Arrays.asList(KeyAction.deleteRecent(1), KeyAction.commitText("아"),
                KeyAction.commitText("가")),
            processor.process(SemanticInput.jamo(SemanticJamo.vowel(0))).actions());
    }

    /** Applies a remote-desktop plan to a model buffer: commits append, deletes take one off. */
    private static void applyRemote(StringBuilder buffer, java.util.List<KeyAction> actions) {
        for (KeyAction action : actions) {
            switch (action.kind()) {
                case COMMIT_TEXT:
                    buffer.append(action.text());
                    break;
                case DELETE_BACKWARD:
                    if (buffer.length() > 0) {
                        buffer.deleteCharAt(buffer.length() - 1);
                    }
                    break;
                case DELETE_RECENT:
                    for (int i = 0; i < action.recentCount() && buffer.length() > 0; i++) {
                        buffer.deleteCharAt(buffer.length() - 1);
                    }
                    break;
                case SET_COMPOSING_TEXT:
                    org.junit.Assert.fail("a remote-desktop plan must never compose: " + actions);
                    break;
                default:
                    break;
            }
        }
    }

    private static String typeRemotely(SemanticInput... inputs) {
        HangulInputProcessor processor = remoteDesktopProcessor();
        StringBuilder buffer = new StringBuilder();
        for (SemanticInput input : inputs) {
            applyRemote(buffer, processor.process(input).actions());
        }
        applyRemote(buffer, processor.process(SemanticInput.flush()).actions());
        return buffer.toString();
    }

    @Test
    public void theEverydaySyllablesArriveIntactOnARemoteDesktop() {
        // The owner's report: 전 (Naratgeul's second ㅏ arrives as a correction delete plus ㅓ).
        assertEquals("전", typeRemotely(
            SemanticInput.jamo(SemanticJamo.contextualConsonant(12)),
            SemanticInput.jamo(SemanticJamo.vowel(0)),
            SemanticInput.deleteForCorrection(),
            SemanticInput.jamo(SemanticJamo.vowel(4)),
            SemanticInput.jamo(SemanticJamo.contextualConsonant(2))));
        // Compound vowels and double finals.
        assertEquals("와", typeRemotely(
            SemanticInput.jamo(SemanticJamo.contextualConsonant(11)),
            SemanticInput.jamo(SemanticJamo.vowel(8)),
            SemanticInput.jamo(SemanticJamo.vowel(0))));
        assertEquals("값", typeRemotely(
            SemanticInput.jamo(SemanticJamo.contextualConsonant(0)),
            SemanticInput.jamo(SemanticJamo.vowel(0)),
            SemanticInput.jamo(SemanticJamo.contextualConsonant(7)),
            SemanticInput.jamo(SemanticJamo.contextualConsonant(9))));
        assertEquals("많", typeRemotely(
            SemanticInput.jamo(SemanticJamo.contextualConsonant(6)),
            SemanticInput.jamo(SemanticJamo.vowel(0)),
            SemanticInput.jamo(SemanticJamo.contextualConsonant(2)),
            SemanticInput.jamo(SemanticJamo.contextualConsonant(18))));
        // The owner's report of 2026-08-28: 앉, the other ㄴ-compound final.
        assertEquals("앉", typeRemotely(
            SemanticInput.jamo(SemanticJamo.contextualConsonant(11)),
            SemanticInput.jamo(SemanticJamo.vowel(0)),
            SemanticInput.jamo(SemanticJamo.contextualConsonant(2)),
            SemanticInput.jamo(SemanticJamo.contextualConsonant(12))));
        assertEquals("뭐", typeRemotely(
            SemanticInput.jamo(SemanticJamo.contextualConsonant(6)),
            SemanticInput.jamo(SemanticJamo.vowel(13)),
            SemanticInput.jamo(SemanticJamo.vowel(4))));
        // A compound-vowel correction takes two deletes before the replacement (Naratgeul ㅘ→ㅝ).
        assertEquals("정말", typeRemotely(
            SemanticInput.jamo(SemanticJamo.contextualConsonant(12)),
            SemanticInput.jamo(SemanticJamo.vowel(0)),
            SemanticInput.deleteForCorrection(),
            SemanticInput.jamo(SemanticJamo.vowel(4)),
            SemanticInput.jamo(SemanticJamo.contextualConsonant(11)),
            SemanticInput.jamo(SemanticJamo.contextualConsonant(6)),
            SemanticInput.jamo(SemanticJamo.vowel(0)),
            SemanticInput.jamo(SemanticJamo.contextualConsonant(5))));
        // The reopened-syllable correction (the 만+ㅇ dance) still lands as 많.
        assertEquals("많", typeRemotely(
            SemanticInput.jamo(SemanticJamo.contextualConsonant(6)),
            SemanticInput.jamo(SemanticJamo.vowel(0)),
            SemanticInput.jamo(SemanticJamo.contextualConsonant(2)),
            SemanticInput.jamo(SemanticJamo.contextualConsonant(11)),
            SemanticInput.deleteForCorrection(),
            SemanticInput.jamo(SemanticJamo.contextualConsonant(18))));
    }
}
