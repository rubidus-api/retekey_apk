package dev.hellgates.retekeyime;

import android.view.KeyEvent;

public final class KeyEventNormalizer {
    private KeyEventNormalizer() {
    }

    public static ProjectKeyEvent fromAndroid(int keyCode, KeyEvent event) {
        int unicode = event.getUnicodeChar();
        String text = unicode == 0 ? "" : new String(Character.toChars(unicode));
        return new ProjectKeyEvent(
            keyCode,
            text,
            event.isShiftPressed(),
            event.isCtrlPressed(),
            event.isAltPressed(),
            event.isMetaPressed(),
            event.getRepeatCount()
        );
    }
}
