package dev.hellgates.retekeyime;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public final class EditorBoundsPredictorTest {
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
