package com.retekey;

import android.os.SystemClock;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;
import java.util.Objects;
import com.retekey.core.EditorBridge;
import com.retekey.core.EditorCallResult;
import com.retekey.core.EditorTextResult;
import com.retekey.core.KeyModifier;
import com.retekey.core.RawEditorKey;
import com.retekey.core.RawKey;

public final class InputConnectionEditorBridge implements EditorBridge {
    private static final int RAW_KEY_FLAGS =
        KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE;

    private final InputConnection inputConnection;
    private final java.util.EnumMap<RawKey, Long> rawKeyDownTimes =
        new java.util.EnumMap<>(RawKey.class);

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
    public EditorCallResult setComposingRegion(int start, int end) {
        return booleanCall(() -> inputConnection.setComposingRegion(start, end));
    }

    @Override
    public EditorCallResult finishComposingText() {
        return booleanCall(inputConnection::finishComposingText);
    }

    @Override
    public EditorCallResult deleteSurroundingTextInCodePoints(int before, int after) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) {
            // Code-point deletion arrived in API 24. Below it the caller's UTF-16 fallback — the
            // one it already uses for the editors that mishandle code points — is the whole story.
            return EditorCallResult.rejected();
        }
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
            long downTime;
            if (key.action() == RawEditorKey.Action.DOWN) {
                rawKeyDownTimes.put(key.key(), eventTime);
                downTime = eventTime;
            } else {
                Long recorded = rawKeyDownTimes.remove(key.key());
                downTime = recorded == null ? eventTime : recorded;
            }
            int action = key.action() == RawEditorKey.Action.DOWN
                ? KeyEvent.ACTION_DOWN
                : KeyEvent.ACTION_UP;
            int metaState = metaStateFor(key.modifiers());
            KeyEvent event;
            if (key.asHardware()) {
                // Dressed as a physical keyboard's event: keyboard source, a real scan code,
                // no soft-keyboard flag. See RawEditorKey.hardware.
                event = new KeyEvent(
                    downTime,
                    eventTime,
                    action,
                    keyCodeFor(key.key()),
                    0,
                    metaState,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    scanCodeFor(key.key()),
                    KeyEvent.FLAG_KEEP_TOUCH_MODE,
                    android.view.InputDevice.SOURCE_KEYBOARD
                );
            } else {
                event = new KeyEvent(
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
            }
            boolean result = inputConnection.sendKeyEvent(event);
            return result ? EditorCallResult.succeeded() : EditorCallResult.rejected();
        } catch (RuntimeException ignored) {
            if (key.action() == RawEditorKey.Action.UP) {
                rawKeyDownTimes.remove(key.key());
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

    /** The Linux evdev scan code a physical keyboard reports for this key; 0 when unmapped. */
    static int scanCodeFor(RawKey key) {
        return key.scanCode();
    }

    static int keyCodeFor(RawKey key) {
        switch (key) {
            case CTRL_LEFT: return KeyEvent.KEYCODE_CTRL_LEFT;
            case SHIFT_LEFT: return KeyEvent.KEYCODE_SHIFT_LEFT;
            case ALT_LEFT: return KeyEvent.KEYCODE_ALT_LEFT;
            case META_LEFT: return KeyEvent.KEYCODE_META_LEFT;
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
            case CAPS_LOCK: return KeyEvent.KEYCODE_CAPS_LOCK;
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
            case SPACE: return KeyEvent.KEYCODE_SPACE;
            case PERIOD: return KeyEvent.KEYCODE_PERIOD;
            case CTRL_RIGHT: return KeyEvent.KEYCODE_CTRL_RIGHT;
            case ALT_RIGHT: return KeyEvent.KEYCODE_ALT_RIGHT;
            case GRAVE: return KeyEvent.KEYCODE_GRAVE;
            case MINUS: return KeyEvent.KEYCODE_MINUS;
            case EQUALS: return KeyEvent.KEYCODE_EQUALS;
            case LEFT_BRACKET: return KeyEvent.KEYCODE_LEFT_BRACKET;
            case RIGHT_BRACKET: return KeyEvent.KEYCODE_RIGHT_BRACKET;
            case BACKSLASH: return KeyEvent.KEYCODE_BACKSLASH;
            case SEMICOLON: return KeyEvent.KEYCODE_SEMICOLON;
            case APOSTROPHE: return KeyEvent.KEYCODE_APOSTROPHE;
            case COMMA: return KeyEvent.KEYCODE_COMMA;
            case SLASH: return KeyEvent.KEYCODE_SLASH;
            default:
                if (key.ordinal() >= RawKey.DIGIT_0.ordinal()
                    && key.ordinal() <= RawKey.DIGIT_9.ordinal()) {
                    // 0..9 are contiguous in both enums, so map by offset from zero.
                    return KeyEvent.KEYCODE_0 + (key.ordinal() - RawKey.DIGIT_0.ordinal());
                }
                if (key.ordinal() >= RawKey.A.ordinal() && key.ordinal() <= RawKey.Z.ordinal()) {
                    // A..Z are contiguous in both enums, so map by offset from A.
                    return KeyEvent.KEYCODE_A + (key.ordinal() - RawKey.A.ordinal());
                }
                return KeyEvent.KEYCODE_UNKNOWN;
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
