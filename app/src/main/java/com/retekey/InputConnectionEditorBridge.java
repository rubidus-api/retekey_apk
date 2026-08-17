package com.retekey;

import android.os.SystemClock;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;
import java.util.Objects;

public final class InputConnectionEditorBridge implements EditorBridge {
    private static final int RAW_KEY_FLAGS =
        KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE;
    /**
     * The same event with nothing on it that says "a keyboard drew this on a screen". Apps that
     * refuse soft-keyboard input — remote-desktop clients, some games — are usually looking at
     * exactly {@code FLAG_SOFT_KEYBOARD} and at a device id of {@code VIRTUAL_KEYBOARD}
     * (see RFC-0010).
     */
    private static final int HARDWARE_KEY_FLAGS = 0;

    private final InputConnection inputConnection;
    /** Whether raw keys are dressed as a physical keyboard's. Off unless the user asked for it. */
    private boolean hardwareDisguise;
    /** The device id to claim; resolved once, from a real keyboard where the device has one. */
    private int hardwareDeviceId = -1;
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
            if (key.action() == RawEditorKey.Action.DOWN) {
                rawKeyDownTime = eventTime;
            }
            long downTime = rawKeyDownTime == 0 ? eventTime : rawKeyDownTime;
            int action = key.action() == RawEditorKey.Action.DOWN
                ? KeyEvent.ACTION_DOWN
                : KeyEvent.ACTION_UP;
            int metaState = metaStateFor(key.modifiers());
            KeyEvent event = hardwareDisguise
                ? new KeyEvent(
                    downTime,
                    eventTime,
                    action,
                    keyCodeFor(key.key()),
                    0,
                    metaState,
                    hardwareDeviceId(),
                    0,
                    HARDWARE_KEY_FLAGS,
                    android.view.InputDevice.SOURCE_KEYBOARD)
                : new KeyEvent(
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

    /** Turns the physical-keyboard disguise on or off for the keys sent through this bridge. */
    public void setHardwareDisguise(boolean disguise) {
        this.hardwareDisguise = disguise;
    }

    /**
     * A device id that is not {@code VIRTUAL_KEYBOARD}. A real alphabetic keyboard's id where the
     * device has one attached — the most convincing answer — and otherwise the lowest id the device
     * reports, since anything is more hardware-looking than -1.
     */
    private int hardwareDeviceId() {
        if (hardwareDeviceId >= 0) {
            return hardwareDeviceId;
        }
        hardwareDeviceId = 0;
        try {
            for (int id : android.view.InputDevice.getDeviceIds()) {
                android.view.InputDevice device = android.view.InputDevice.getDevice(id);
                if (device == null || id < 0) {
                    continue;
                }
                boolean isKeyboard = (device.getSources() & android.view.InputDevice.SOURCE_KEYBOARD)
                    == android.view.InputDevice.SOURCE_KEYBOARD;
                // isVirtual() is API 16; below it there is nothing to exclude, and a device that
                // reports an alphabetic keyboard is the best answer available either way.
                boolean virtual = android.os.Build.VERSION.SDK_INT
                    >= android.os.Build.VERSION_CODES.JELLY_BEAN && device.isVirtual();
                if (isKeyboard
                    && device.getKeyboardType() == android.view.InputDevice.KEYBOARD_TYPE_ALPHABETIC
                    && !virtual) {
                    hardwareDeviceId = id;
                    break;
                }
            }
        } catch (RuntimeException ignored) {
            // No device list to read; 0 is still not the virtual keyboard's -1.
        }
        return hardwareDeviceId;
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
            case SPACE: return KeyEvent.KEYCODE_SPACE;
            case MINUS: return KeyEvent.KEYCODE_MINUS;
            case EQUALS: return KeyEvent.KEYCODE_EQUALS;
            case LEFT_BRACKET: return KeyEvent.KEYCODE_LEFT_BRACKET;
            case RIGHT_BRACKET: return KeyEvent.KEYCODE_RIGHT_BRACKET;
            case BACKSLASH: return KeyEvent.KEYCODE_BACKSLASH;
            case SEMICOLON: return KeyEvent.KEYCODE_SEMICOLON;
            case APOSTROPHE: return KeyEvent.KEYCODE_APOSTROPHE;
            case GRAVE: return KeyEvent.KEYCODE_GRAVE;
            case COMMA: return KeyEvent.KEYCODE_COMMA;
            case PERIOD: return KeyEvent.KEYCODE_PERIOD;
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
