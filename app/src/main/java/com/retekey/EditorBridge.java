package com.retekey;

public interface EditorBridge {
    EditorCallResult beginBatchEdit();

    EditorCallResult endBatchEdit();

    EditorCallResult commitText(String text, int newCursorPosition);

    EditorCallResult setComposingText(String text, int newCursorPosition);

    EditorCallResult finishComposingText();

    /**
     * Marks the text between {@code start} and {@code end} as the composing region — how a
     * syllable already written is taken back to be rewritten, without deleting anything.
     */
    EditorCallResult setComposingRegion(int start, int end);

    EditorCallResult deleteSurroundingTextInCodePoints(int before, int after);

    EditorTextResult getTextBeforeCursor(int maxUtf16Units, int flags);

    EditorCallResult deleteSurroundingText(int beforeUtf16Units, int afterUtf16Units);

    EditorCallResult performEditorAction(int actionId);

    EditorCallResult sendRawKey(RawEditorKey key);
}
