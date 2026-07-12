package dev.hellgates.retekeyime;

import android.view.KeyEvent;

public final class KeyEventNormalizer {
    private KeyEventNormalizer() {
    }

    public static ProjectKeyEvent fromAndroid(int keyCode, KeyEvent event) {
        return fromAndroid(keyCode, event, HardwareSemanticMapper.none());
    }

    public static ProjectKeyEvent fromAndroid(
        int keyCode,
        KeyEvent event,
        HardwareSemanticMapper mapper
    ) {
        if (event == null || mapper == null) {
            throw new IllegalArgumentException("event and mapper are required");
        }

        InputAction action = actionFromAndroid(event.getAction());
        int rawMetaState = event.getMetaState();
        String stableKeyId = stableKeyId(keyCode);
        SemanticInput mappedInput = keyCode == KeyEvent.KEYCODE_DEL
            ? SemanticInput.deleteBackward()
            : mapper.map(stableKeyId, event.isShiftPressed());

        RawHardwareKeyEvent raw = RawHardwareKeyEvent.builder(action, keyCode)
            .stableKeyId(stableKeyId)
            .scanCode(event.getScanCode())
            .deviceId(event.getDeviceId())
            .deviceSource(event.getSource())
            .unicodeValue(event.getUnicodeChar())
            .characters(multipleCharacters(event, action))
            .shift(event.isShiftPressed())
            .ctrl(event.isCtrlPressed())
            .alt(event.isAltPressed())
            .meta(event.isMetaPressed())
            .capsLock(event.isCapsLockOn())
            .function(event.isFunctionPressed())
            .sym((rawMetaState & KeyEvent.META_SYM_ON) != 0)
            .rawMetaState(rawMetaState)
            .repeatCount(event.getRepeatCount())
            .canceled(event.isCanceled())
            .mappedInput(mappedInput)
            .build();
        return HardwareEventNormalizer.normalize(raw);
    }

    @SuppressWarnings("deprecation")
    private static InputAction actionFromAndroid(int action) {
        switch (action) {
            case KeyEvent.ACTION_DOWN:
                return InputAction.DOWN;
            case KeyEvent.ACTION_UP:
                return InputAction.UP;
            case KeyEvent.ACTION_MULTIPLE:
                return InputAction.MULTIPLE;
            default:
                return InputAction.UNKNOWN;
        }
    }

    @SuppressWarnings("deprecation")
    private static String multipleCharacters(KeyEvent event, InputAction action) {
        if (action != InputAction.MULTIPLE) {
            return "";
        }
        String characters = event.getCharacters();
        return characters == null ? "" : characters;
    }

    private static String stableKeyId(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            return "hardware.edit.backspace";
        }
        if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
            char letter = (char) ('a' + keyCode - KeyEvent.KEYCODE_A);
            return "hardware.key." + letter;
        }
        return "hardware.keycode." + keyCode;
    }
}
