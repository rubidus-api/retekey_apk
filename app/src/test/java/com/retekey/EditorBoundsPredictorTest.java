package com.retekey;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public final class EditorBoundsPredictorTest {
    /**
     * A raw key used to reach this predictor and throw, which the service caught and swallowed —
     * so Tab, Escape, the arrows, and every other raw key silently did nothing. Where the cursor
     * ends up after one is the editor's business; the honest answer is that it is unknown.
     */
    @Test
    public void aRawKeyLeavesTheCursorUnknownRatherThanThrowing() {
        Assert.assertEquals(
            EditorBounds.unknown(),
            EditorBoundsPredictor.after(
                EditorBounds.of(2, 2, -1, -1),
                Collections.singletonList(
                    KeyAction.rawKey(RawKey.TAB, Collections.emptySet()))
            )
        );
    }

    @Test
    public void aHeldRawKeyIsJustAsUnpredictable() {
        for (RawKeyPhase phase : RawKeyPhase.values()) {
            Assert.assertEquals(
                phase.toString(),
                EditorBounds.unknown(),
                EditorBoundsPredictor.after(
                    EditorBounds.of(2, 2, -1, -1),
                    Collections.singletonList(
                        KeyAction.rawKey(RawKey.TAB, Collections.emptySet(), phase))
                )
            );
        }
    }

    @Test
    public void commitReplacesSelectionAndExistingComposition() {
        Assert.assertEquals(
            EditorBounds.of(4, 4, -1, -1),
            EditorBoundsPredictor.after(
                EditorBounds.of(2, 5, -1, -1),
                Collections.singletonList(KeyAction.commitText("ab"))
            )
        );
        Assert.assertEquals(
            EditorBounds.of(3, 3, -1, -1),
            EditorBoundsPredictor.after(
                EditorBounds.of(4, 4, 1, 4),
                Collections.singletonList(KeyAction.commitText("xy"))
            )
        );
    }

    @Test
    public void composingReplacementPreservesItsStartAndCursorContract() {
        Assert.assertEquals(
            EditorBounds.of(4, 4, 1, 4),
            EditorBoundsPredictor.after(
                EditorBounds.of(3, 3, 1, 3),
                Collections.singletonList(KeyAction.setComposingText("abc"))
            )
        );
    }

    @Test
    public void commitThenNewPreeditIsPredictedInExactOrder() {
        Assert.assertEquals(
            EditorBounds.of(4, 4, 3, 4),
            EditorBoundsPredictor.after(
                EditorBounds.of(0, 0, -1, -1),
                Arrays.asList(
                    KeyAction.commitText("abc"),
                    KeyAction.setComposingText("가")
                )
            )
        );
    }

    @Test
    public void selectionDeleteIsExactButCollapsedCodePointDeleteIsWildcard() {
        Assert.assertEquals(
            EditorBounds.of(2, 2, -1, -1),
            EditorBoundsPredictor.after(
                EditorBounds.of(5, 2, -1, -1),
                Collections.singletonList(KeyAction.deleteBackward())
            )
        );
        Assert.assertEquals(
            EditorBounds.unknown(),
            EditorBoundsPredictor.after(
                EditorBounds.of(2, 2, -1, -1),
                Collections.singletonList(KeyAction.deleteBackward())
            )
        );
    }

    @Test
    public void focusChangingAndUnknownStartingBoundsStayWildcard() {
        Assert.assertEquals(
            EditorBounds.unknown(),
            EditorBoundsPredictor.after(
                EditorBounds.of(1, 1, -1, -1),
                Collections.singletonList(KeyAction.performEditorAction(6))
            )
        );
        Assert.assertEquals(
            EditorBounds.unknown(),
            EditorBoundsPredictor.after(
                EditorBounds.unknown(),
                Collections.singletonList(KeyAction.commitText("x"))
            )
        );
    }

    }
