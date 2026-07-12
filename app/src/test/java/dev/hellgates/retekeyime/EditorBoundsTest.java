package dev.hellgates.retekeyime;

import org.junit.Assert;
import org.junit.Test;

public final class EditorBoundsTest {
    @Test
    public void knownAndUnknownBoundsAreExplicitValues() {
        EditorBounds unknown = EditorBounds.unknown();
        EditorBounds known = EditorBounds.of(3, 5, -1, -1);

        Assert.assertFalse(unknown.hasSelection());
        Assert.assertFalse(unknown.hasComposingRange());
        Assert.assertTrue(known.hasSelection());
        Assert.assertFalse(known.hasComposingRange());
        Assert.assertEquals(3, known.selectionStart());
        Assert.assertEquals(5, known.selectionEnd());
        Assert.assertEquals(EditorBounds.of(3, 5, -1, -1), known);
    }

    @Test
    public void composingRangeIsPreservedIndependently() {
        EditorBounds bounds = EditorBounds.of(4, 4, 1, 4);

        Assert.assertTrue(bounds.hasComposingRange());
        Assert.assertEquals(1, bounds.composingStart());
        Assert.assertEquals(4, bounds.composingEnd());
    }

    @Test
    public void reversedSelectionPreservesAnchorAndFocus() {
        EditorBounds bounds = EditorBounds.of(5, 2, -1, -1);

        Assert.assertEquals(5, bounds.selectionStart());
        Assert.assertEquals(2, bounds.selectionEnd());
        Assert.assertTrue(bounds.hasSelectedText());
        Assert.assertEquals(2, bounds.selectionLowerBound());
        Assert.assertEquals(5, bounds.selectionUpperBound());
    }

    @Test
    public void rejectsPartialUnknownOrInvalidComposingBounds() {
        Assert.assertThrows(
            IllegalArgumentException.class,
            () -> EditorBounds.of(-1, 0, -1, -1)
        );
        Assert.assertThrows(
            IllegalArgumentException.class,
            () -> EditorBounds.of(0, 0, -1, 0)
        );
        Assert.assertThrows(
            IllegalArgumentException.class,
            () -> EditorBounds.of(0, 0, 4, 3)
        );
    }
}
