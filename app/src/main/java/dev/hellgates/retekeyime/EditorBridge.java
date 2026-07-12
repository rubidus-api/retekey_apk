package dev.hellgates.retekeyime;

public interface EditorBridge {
    EditorCallResult beginBatchEdit();

    EditorCallResult endBatchEdit();

    EditorCallResult commitText(String text, int newCursorPosition);

    EditorCallResult setComposingText(String text, int newCursorPosition);

    EditorCallResult finishComposingText();

    EditorCallResult deleteSurroundingTextInCodePoints(int before, int after);

    EditorTextResult getTextBeforeCursor(int maxUtf16Units, int flags);

    EditorCallResult deleteSurroundingText(int beforeUtf16Units, int afterUtf16Units);

    EditorCallResult performEditorAction(int actionId);

    EditorCallResult sendRawKey(RawEditorKey key);
}
