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

    public static EditorExpectation expectationAfter(
        EditorBounds initial,
        List<KeyAction> actions
    ) {
        if (initial == null || actions == null || actions.contains(null)) {
            throw new IllegalArgumentException("prediction arguments must not be null");
        }
        java.util.ArrayList<EditorBounds> intermediate = new java.util.ArrayList<>();
        EditorBounds prefix = initial;
        for (int index = 0; index + 1 < actions.size(); index++) {
            prefix = after(prefix, actions.get(index));
            if (!prefix.hasSelection()) {
                break;
            }
            intermediate.add(prefix);
        }
        EditorBounds predicted = after(initial, actions);
        EditorExpectation finalExpectation;
        if (predicted.hasSelection()) {
            finalExpectation = EditorExpectation.exact(predicted);
        } else if (initial.hasSelection()
            && actions.size() == 1
            && actions.get(0).kind() == KeyAction.Kind.DELETE_BACKWARD
            && !initial.hasComposingRange()
            && !initial.hasSelectedText()
            && initial.selectionStart() > 0) {
            int cursor = initial.selectionStart();
            EditorBounds oneUnit = EditorBounds.of(cursor - 1, cursor - 1, -1, -1);
            if (cursor == 1) {
                finalExpectation = EditorExpectation.exact(oneUnit);
            } else {
                finalExpectation = EditorExpectation.oneOf(java.util.Arrays.asList(
                    oneUnit,
                    EditorBounds.of(cursor - 2, cursor - 2, -1, -1)
                ));
            }
        } else {
            finalExpectation = EditorExpectation.unconfirmable();
        }
        return EditorExpectation.withIntermediateBounds(finalExpectation, intermediate);
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
