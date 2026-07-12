package dev.hellgates.retekeyime;

import android.os.SystemClock;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;
import java.util.Objects;

public final class InputConnectionEditorBridge implements EditorBridge {
    private static final int RAW_KEY_FLAGS =
        KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE;

    private final InputConnection inputConnection;
    private long rawKeyDownTime;

    public InputConnectionEditorBridge(InputConnection inputConnection) {
        this.inputConnection = Objects.requireNonNull(inputConnection, "inputConnection");
    }

    @Override
    public EditorCallResult beginBatchEdit() {
        return booleanCall(inputConnection::beginBatchEdit);
    }

    @Override
    public EditorCallResult endBatchEdit() {
        return booleanCall(inputConnection::endBatchEdit);
    }

    @Override
    public EditorCallResult commitText(String text, int newCursorPosition) {
        return booleanCall(() -> inputConnection.commitText(text, newCursorPosition));
    }

    @Override
    public EditorCallResult setComposingText(String text, int newCursorPosition) {
        return booleanCall(() -> inputConnection.setComposingText(text, newCursorPosition));
    }

    @Override
    public EditorCallResult finishComposingText() {
        return booleanCall(inputConnection::finishComposingText);
    }

    @Override
    public EditorCallResult deleteSurroundingTextInCodePoints(int before, int after) {
        return booleanCall(
            () -> inputConnection.deleteSurroundingTextInCodePoints(before, after)
        );
    }

    @Override
    public EditorTextResult getTextBeforeCursor(int maxUtf16Units, int flags) {
        try {
            CharSequence text = inputConnection.getTextBeforeCursor(maxUtf16Units, flags);
            return text == null
                ? EditorTextResult.nullValue()
                : EditorTextResult.value(text.toString());
        } catch (RuntimeException ignored) {
            return EditorTextResult.runtimeFailure();
        }
    }

    @Override
    public EditorCallResult deleteSurroundingText(
        int beforeUtf16Units,
        int afterUtf16Units
    ) {
        return booleanCall(
            () -> inputConnection.deleteSurroundingText(beforeUtf16Units, afterUtf16Units)
        );
    }

    @Override
    public EditorCallResult performEditorAction(int actionId) {
        return booleanCall(() -> inputConnection.performEditorAction(actionId));
    }

    @Override
    public EditorCallResult sendRawKey(RawEditorKey key) {
        try {
            long eventTime = SystemClock.uptimeMillis();
            if (key.action() == RawEditorKey.Action.DOWN) {
                rawKeyDownTime = eventTime;
            }
            long downTime = rawKeyDownTime == 0 ? eventTime : rawKeyDownTime;
            int action = key.action() == RawEditorKey.Action.DOWN
                ? KeyEvent.ACTION_DOWN
                : KeyEvent.ACTION_UP;
            int keyCode = key.kind() == RawEditorKey.Kind.DELETE
                ? KeyEvent.KEYCODE_DEL
                : KeyEvent.KEYCODE_ENTER;
            KeyEvent event = new KeyEvent(
                downTime,
                eventTime,
                action,
                keyCode,
                0,
                0,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                RAW_KEY_FLAGS
            );
            boolean result = inputConnection.sendKeyEvent(event);
            if (key.action() == RawEditorKey.Action.UP) {
                rawKeyDownTime = 0;
            }
            return result ? EditorCallResult.succeeded() : EditorCallResult.rejected();
        } catch (RuntimeException ignored) {
            if (key.action() == RawEditorKey.Action.UP) {
                rawKeyDownTime = 0;
            }
            return EditorCallResult.runtimeFailure();
        }
    }

    private static EditorCallResult booleanCall(BooleanEditorCall call) {
        try {
            return call.invoke()
                ? EditorCallResult.succeeded()
                : EditorCallResult.rejected();
        } catch (RuntimeException ignored) {
            return EditorCallResult.runtimeFailure();
        }
    }

    @FunctionalInterface
    private interface BooleanEditorCall {
        boolean invoke();
    }

}
