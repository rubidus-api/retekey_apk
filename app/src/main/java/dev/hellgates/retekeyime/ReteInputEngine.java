package dev.hellgates.retekeyime;

public final class ReteInputEngine {
    public KeyAction onSoftKey(String label) {
        if ("⌫".equals(label)) {
            return KeyAction.deleteBackward();
        }
        if ("CTRL".equals(label) || "ALT".equals(label) || "한/영".equals(label)) {
            return KeyAction.noop();
        }
        if ("←".equals(label) || "↓".equals(label) || "↑".equals(label) || "→".equals(label)) {
            return KeyAction.noop();
        }
        return KeyAction.commitText(label);
    }

    public KeyAction onHardwareKey(ProjectKeyEvent event) {
        if (event.keyCode() == android.view.KeyEvent.KEYCODE_DEL) {
            return KeyAction.deleteBackward();
        }
        String text = event.text();
        if (text == null || text.isEmpty()) {
            return KeyAction.noop();
        }
        return KeyAction.commitText(text);
    }
}
