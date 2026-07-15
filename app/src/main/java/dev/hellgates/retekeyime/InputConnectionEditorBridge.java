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
            int metaState = metaStateFor(key.modifiers());
            KeyEvent event = new KeyEvent(
                downTime,
                eventTime,
                action,
                keyCodeFor(key.key()),
                0,
                metaState,
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

    static int metaStateFor(java.util.Set<KeyModifier> modifiers) {
        int meta = 0;
        for (KeyModifier modifier : modifiers) {
            switch (modifier) {
                case CTRL:
                    meta |= KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
                    break;
                case ALT:
                    meta |= KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON;
                    break;
                case SHIFT:
                    meta |= KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON;
                    break;
                case META:
                    meta |= KeyEvent.META_META_ON | KeyEvent.META_META_LEFT_ON;
                    break;
                default:
                    break;
            }
        }
        return meta;
    }

    static int keyCodeFor(RawKey key) {
        switch (key) {
            case ENTER: return KeyEvent.KEYCODE_ENTER;
            case BACKSPACE: return KeyEvent.KEYCODE_DEL;
            case ESCAPE: return KeyEvent.KEYCODE_ESCAPE;
            case TAB: return KeyEvent.KEYCODE_TAB;
            case FORWARD_DELETE: return KeyEvent.KEYCODE_FORWARD_DEL;
            case INSERT: return KeyEvent.KEYCODE_INSERT;
            case LEFT: return KeyEvent.KEYCODE_DPAD_LEFT;
            case RIGHT: return KeyEvent.KEYCODE_DPAD_RIGHT;
            case UP: return KeyEvent.KEYCODE_DPAD_UP;
            case DOWN: return KeyEvent.KEYCODE_DPAD_DOWN;
            case HOME: return KeyEvent.KEYCODE_MOVE_HOME;
            case END: return KeyEvent.KEYCODE_MOVE_END;
            case PAGE_UP: return KeyEvent.KEYCODE_PAGE_UP;
            case PAGE_DOWN: return KeyEvent.KEYCODE_PAGE_DOWN;
            case PRINT_SCREEN: return KeyEvent.KEYCODE_SYSRQ;
            case SCROLL_LOCK: return KeyEvent.KEYCODE_SCROLL_LOCK;
            case BREAK: return KeyEvent.KEYCODE_BREAK;
            case MENU: return KeyEvent.KEYCODE_MENU;
            case SEARCH: return KeyEvent.KEYCODE_SEARCH;
            case F1: return KeyEvent.KEYCODE_F1;
            case F2: return KeyEvent.KEYCODE_F2;
            case F3: return KeyEvent.KEYCODE_F3;
            case F4: return KeyEvent.KEYCODE_F4;
            case F5: return KeyEvent.KEYCODE_F5;
            case F6: return KeyEvent.KEYCODE_F6;
            case F7: return KeyEvent.KEYCODE_F7;
            case F8: return KeyEvent.KEYCODE_F8;
            case F9: return KeyEvent.KEYCODE_F9;
            case F10: return KeyEvent.KEYCODE_F10;
            case F11: return KeyEvent.KEYCODE_F11;
            case F12: return KeyEvent.KEYCODE_F12;
            default: return KeyEvent.KEYCODE_UNKNOWN;
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
