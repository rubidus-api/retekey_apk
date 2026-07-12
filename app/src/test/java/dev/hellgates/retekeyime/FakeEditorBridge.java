package dev.hellgates.retekeyime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

final class FakeEditorBridge implements EditorBridge {
    private final List<String> trace = new ArrayList<>();
    private final Map<Integer, EditorCallResult> callResults = new HashMap<>();
    private final Map<Integer, Runnable> callHooks = new HashMap<>();
    private final Set<Integer> applyEffectBeforeFailure = new HashSet<>();
    private final Set<Integer> throwAtCall = new HashSet<>();
    private final Set<Integer> returnNullAtCall = new HashSet<>();
    private final StringBuilder modelText = new StringBuilder();
    private int callIndex;
    private EditorTextResult textBeforeCursor = EditorTextResult.value("");
    private boolean textBeforeCursorOverridden;
    private int selectionStart;
    private int selectionEnd;
    private int composingStart = -1;
    private int composingEnd = -1;

    void returnAt(int oneBasedCallIndex, EditorCallResult result) {
        callResults.put(oneBasedCallIndex, result);
    }

    void setTextBeforeCursor(EditorTextResult result) {
        textBeforeCursor = result;
        textBeforeCursorOverridden = true;
    }

    void runAt(int oneBasedCallIndex, Runnable hook) {
        callHooks.put(oneBasedCallIndex, hook);
    }

    void returnAfterEffectAt(int oneBasedCallIndex, EditorCallResult result) {
        returnAt(oneBasedCallIndex, result);
        applyEffectBeforeFailure.add(oneBasedCallIndex);
    }

    void throwAt(int oneBasedCallIndex) {
        throwAtCall.add(oneBasedCallIndex);
    }

    void returnNullAt(int oneBasedCallIndex) {
        returnNullAtCall.add(oneBasedCallIndex);
    }

    void setModel(String text, EditorBounds bounds) {
        if (text == null || bounds == null || !bounds.hasSelection()) {
            throw new IllegalArgumentException("model requires text and known selection");
        }
        if (bounds.selectionStart() > text.length() || bounds.selectionEnd() > text.length()) {
            throw new IllegalArgumentException("selection lies outside model text");
        }
        if (bounds.hasComposingRange() && bounds.composingEnd() > text.length()) {
            throw new IllegalArgumentException("composing range lies outside model text");
        }
        modelText.setLength(0);
        modelText.append(text);
        selectionStart = bounds.selectionStart();
        selectionEnd = bounds.selectionEnd();
        composingStart = bounds.composingStart();
        composingEnd = bounds.composingEnd();
    }

    String modelText() {
        return modelText.toString();
    }

    EditorBounds modelBounds() {
        return EditorBounds.of(
            selectionStart,
            selectionEnd,
            composingStart,
            composingEnd
        );
    }

    List<String> trace() {
        return trace;
    }

    int callCount() {
        return callIndex;
    }

    private EditorCallResult record(String call) {
        return record(call, () -> { });
    }

    private EditorCallResult record(String call, Runnable effect) {
        trace.add(call);
        callIndex++;
        if (throwAtCall.contains(callIndex)) {
            throw new IllegalStateException("fake editor runtime failure");
        }
        if (returnNullAtCall.contains(callIndex)) {
            return null;
        }
        EditorCallResult result = callResults.get(callIndex);
        if (result == null) {
            result = EditorCallResult.succeeded();
        }
        if (result.isSucceeded() || applyEffectBeforeFailure.contains(callIndex)) {
            effect.run();
        }
        Runnable hook = callHooks.get(callIndex);
        if (hook != null) {
            hook.run();
        }
        return result;
    }

    @Override
    public EditorCallResult beginBatchEdit() {
        return record("beginBatchEdit");
    }

    @Override
    public EditorCallResult endBatchEdit() {
        return record("endBatchEdit");
    }

    @Override
    public EditorCallResult commitText(String text, int newCursorPosition) {
        return record(
            "commitText:length=" + text.length() + ":cursor=" + newCursorPosition,
            () -> replace(text, false)
        );
    }

    @Override
    public EditorCallResult setComposingText(String text, int newCursorPosition) {
        return record(
            "setComposingText:length=" + text.length() + ":cursor=" + newCursorPosition,
            () -> replace(text, true)
        );
    }

    @Override
    public EditorCallResult finishComposingText() {
        return record("finishComposingText", () -> {
            composingStart = -1;
            composingEnd = -1;
        });
    }

    @Override
    public EditorCallResult deleteSurroundingTextInCodePoints(int before, int after) {
        return record(
            "deleteCodePoints:before=" + before + ":after=" + after,
            () -> deleteCodePoints(before, after)
        );
    }

    @Override
    public EditorTextResult getTextBeforeCursor(int maxUtf16Units, int flags) {
        record("getTextBeforeCursor:max=" + maxUtf16Units + ":flags=" + flags);
        if (textBeforeCursorOverridden) {
            return textBeforeCursor;
        }
        int cursor = Math.min(selectionStart, selectionEnd);
        int start = Math.max(0, cursor - maxUtf16Units);
        return EditorTextResult.value(modelText.substring(start, cursor));
    }

    @Override
    public EditorCallResult deleteSurroundingText(int beforeUtf16Units, int afterUtf16Units) {
        return record(
            "deleteUtf16:before=" + beforeUtf16Units + ":after=" + afterUtf16Units,
            () -> deleteUtf16(beforeUtf16Units, afterUtf16Units)
        );
    }

    @Override
    public EditorCallResult performEditorAction(int actionId) {
        return record("performEditorAction:id=" + actionId);
    }

    @Override
    public EditorCallResult sendRawKey(RawEditorKey key) {
        return record(
            "sendRawKey:kind=" + key.kind() + ":action=" + key.action(),
            () -> applyRawKey(key)
        );
    }

    private void replace(String replacement, boolean composing) {
        int start = composingStart >= 0
            ? composingStart
            : Math.min(selectionStart, selectionEnd);
        int end = composingEnd >= 0
            ? composingEnd
            : Math.max(selectionStart, selectionEnd);
        modelText.replace(start, end, replacement);
        int cursor = start + replacement.length();
        selectionStart = cursor;
        selectionEnd = cursor;
        composingStart = composing && !replacement.isEmpty() ? start : -1;
        composingEnd = composing && !replacement.isEmpty() ? cursor : -1;
    }

    private void deleteCodePoints(int before, int after) {
        if (selectionStart != selectionEnd) {
            return;
        }
        int cursor = selectionStart;
        try {
            int start = modelText.offsetByCodePoints(cursor, -before);
            int end = modelText.offsetByCodePoints(cursor, after);
            modelText.delete(start, end);
            selectionStart = start;
            selectionEnd = start;
            composingStart = -1;
            composingEnd = -1;
        } catch (IndexOutOfBoundsException ignored) {
            // The controllable fake models a target editor that reports success with no effect.
        }
    }

    private void deleteUtf16(int before, int after) {
        if (selectionStart != selectionEnd) {
            return;
        }
        int cursor = selectionStart;
        int start = Math.max(0, cursor - before);
        int end = Math.min(modelText.length(), cursor + after);
        modelText.delete(start, end);
        selectionStart = start;
        selectionEnd = start;
        composingStart = -1;
        composingEnd = -1;
    }

    private void applyRawKey(RawEditorKey key) {
        if (key.action() != RawEditorKey.Action.DOWN) {
            return;
        }
        if (key.kind() == RawEditorKey.Kind.DELETE) {
            deleteCodePoints(1, 0);
        } else {
            replace("\n", false);
        }
    }
}
