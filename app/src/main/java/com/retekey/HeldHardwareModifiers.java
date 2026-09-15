package com.retekey;

import android.view.KeyEvent;
import java.util.EnumSet;
import java.util.Set;

/**
 * The modifiers held down on a physical keyboard right now. The action bar is pressed with a
 * finger while the other hand may be holding Ctrl or Shift on a keyboard; an arrow tapped on the
 * bar then has to go out as Ctrl+arrow, the way it would from the keyboard's own arrow key.
 *
 * <p>Kept from the modifier keys' own down and up events rather than from the meta state of the
 * last event, so each side of a pair counts on its own: letting go of left Shift while right Shift
 * is still down leaves Shift held.
 */
final class HeldHardwareModifiers {
    private final Set<Integer> down = new java.util.HashSet<>();

    /** Records a key event; returns true when it was a modifier key's. */
    boolean onKey(int keyCode, boolean isDown) {
        if (modifierOf(keyCode) == null) {
            return false;
        }
        if (isDown) {
            down.add(keyCode);
        } else {
            down.remove(keyCode);
        }
        return true;
    }

    /** Forgets everything — a keyboard unplugged, or a field left with a key still down. */
    void clear() {
        down.clear();
    }

    Set<KeyModifier> held() {
        Set<KeyModifier> mods = EnumSet.noneOf(KeyModifier.class);
        for (int keyCode : down) {
            KeyModifier mod = modifierOf(keyCode);
            if (mod != null) {
                mods.add(mod);
            }
        }
        return mods;
    }

    static KeyModifier modifierOf(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CTRL_LEFT:
            case KeyEvent.KEYCODE_CTRL_RIGHT:
                return KeyModifier.CTRL;
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                return KeyModifier.SHIFT;
            case KeyEvent.KEYCODE_ALT_LEFT:
            case KeyEvent.KEYCODE_ALT_RIGHT:
                return KeyModifier.ALT;
            case KeyEvent.KEYCODE_META_LEFT:
            case KeyEvent.KEYCODE_META_RIGHT:
                return KeyModifier.META;
            default:
                return null;
        }
    }
}
