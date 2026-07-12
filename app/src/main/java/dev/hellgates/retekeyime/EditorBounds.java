package dev.hellgates.retekeyime;

import java.util.Objects;

public final class EditorBounds {
    private static final int UNKNOWN = -1;
    private static final EditorBounds UNKNOWN_BOUNDS =
        new EditorBounds(UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN);

    private final int selectionStart;
    private final int selectionEnd;
    private final int composingStart;
    private final int composingEnd;

    private EditorBounds(
        int selectionStart,
        int selectionEnd,
        int composingStart,
        int composingEnd
    ) {
        requireValidSelection(selectionStart, selectionEnd);
        requireValidOrderedRange("composing", composingStart, composingEnd);
        this.selectionStart = selectionStart;
        this.selectionEnd = selectionEnd;
        this.composingStart = composingStart;
        this.composingEnd = composingEnd;
    }

    public static EditorBounds unknown() {
        return UNKNOWN_BOUNDS;
    }

    public static EditorBounds of(
        int selectionStart,
        int selectionEnd,
        int composingStart,
        int composingEnd
    ) {
        if (selectionStart == UNKNOWN
            && selectionEnd == UNKNOWN
            && composingStart == UNKNOWN
            && composingEnd == UNKNOWN) {
            return UNKNOWN_BOUNDS;
        }
        return new EditorBounds(selectionStart, selectionEnd, composingStart, composingEnd);
    }

    private static void requireValidSelection(int start, int end) {
        boolean unknown = start == UNKNOWN && end == UNKNOWN;
        boolean known = start >= 0 && end >= 0;
        if (!unknown && !known) {
            throw new IllegalArgumentException("selection must be known or fully unknown");
        }
    }

    private static void requireValidOrderedRange(String name, int start, int end) {
        boolean unknown = start == UNKNOWN && end == UNKNOWN;
        boolean known = start >= 0 && end >= start;
        if (!unknown && !known) {
            throw new IllegalArgumentException(name + " range must be ordered or fully unknown");
        }
    }

    public boolean hasSelection() {
        return selectionStart != UNKNOWN;
    }

    public boolean hasComposingRange() {
        return composingStart != UNKNOWN;
    }

    public boolean hasSelectedText() {
        return hasSelection() && selectionStart != selectionEnd;
    }

    public int selectionLowerBound() {
        return hasSelection() ? Math.min(selectionStart, selectionEnd) : UNKNOWN;
    }

    public int selectionUpperBound() {
        return hasSelection() ? Math.max(selectionStart, selectionEnd) : UNKNOWN;
    }

    public int selectionStart() {
        return selectionStart;
    }

    public int selectionEnd() {
        return selectionEnd;
    }

    public int composingStart() {
        return composingStart;
    }

    public int composingEnd() {
        return composingEnd;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EditorBounds)) {
            return false;
        }
        EditorBounds that = (EditorBounds) other;
        return selectionStart == that.selectionStart
            && selectionEnd == that.selectionEnd
            && composingStart == that.composingStart
            && composingEnd == that.composingEnd;
    }

    @Override
    public int hashCode() {
        return Objects.hash(selectionStart, selectionEnd, composingStart, composingEnd);
    }

    @Override
    public String toString() {
        return "EditorBounds{"
            + "selectionStart=" + selectionStart
            + ", selectionEnd=" + selectionEnd
            + ", composingStart=" + composingStart
            + ", composingEnd=" + composingEnd
            + '}';
    }
}
