package dev.hellgates.retekeyime;

import java.util.List;

public final class EditorBoundsPredictor {
    private EditorBoundsPredictor() {
    }

    public static EditorBounds after(EditorBounds initial, List<KeyAction> actions) {
        if (initial == null || actions == null || actions.contains(null)) {
            throw new IllegalArgumentException("prediction arguments must not be null");
        }
        EditorBounds current = initial;
        for (KeyAction action : actions) {
            current = after(current, action);
            if (!current.hasSelection()) {
                return EditorBounds.unknown();
            }
        }
        return current;
    }

static EditorBounds after(EditorBounds current, KeyAction action) {
        if (current == null || action == null) {
            throw new IllegalArgumentException("prediction arguments must not be null");
        }
        if (!current.hasSelection()) {
            return EditorBounds.unknown();
        }
        switch (action.kind()) {
            case COMMIT_TEXT:
                return replace(current, action.text(), false);
            case SET_COMPOSING_TEXT:
                return replace(current, action.text(), true);
            case FINISH_COMPOSING:
                return EditorBounds.of(
                    current.selectionStart(),
                    current.selectionEnd(),
                    -1,
                    -1
                );
            case DELETE_BACKWARD:
                if (current.hasComposingRange()) {
                    return EditorBounds.unknown();
                }
                if (current.hasSelectedText()) {
                    int cursor = current.selectionLowerBound();
                    return EditorBounds.of(cursor, cursor, -1, -1);
                }
                return current.selectionStart() == 0
                    ? EditorBounds.of(0, 0, -1, -1)
                    : EditorBounds.unknown();
            case PERFORM_EDITOR_ACTION:
            case RAW_ENTER:
                return EditorBounds.unknown();
            default:
                throw new IllegalStateException("unsupported action kind");
        }
    }

    private static EditorBounds replace(
        EditorBounds current,
        String text,
        boolean composing
    ) {
        int replacementStart = current.hasComposingRange()
            ? current.composingStart()
            : current.selectionLowerBound();
        int replacementEnd = safeAdd(replacementStart, text.length());
        if (replacementEnd < 0) {
            return EditorBounds.unknown();
        }
        return EditorBounds.of(
            replacementEnd,
            replacementEnd,
            composing && !text.isEmpty() ? replacementStart : -1,
            composing && !text.isEmpty() ? replacementEnd : -1
        );
    }

    private static int safeAdd(int left, int right) {
        long result = (long) left + right;
        return result > Integer.MAX_VALUE ? -1 : (int) result;
    }
}
