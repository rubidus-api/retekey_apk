package com.retekey;

import android.view.KeyEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The modifiers held down on a physical keyboard right now. The action bar is pressed with a
 * finger while the other hand may be holding Ctrl or Shift on a keyboard; an arrow tapped on the
 * bar then has to go out as Ctrl+arrow, the way it would from the keyboard's own arrow key.
 *
 * <p>Kept from the modifier keys' own down and up events rather than from the meta state of the
 * last event, so each side of a pair counts on its own: letting go of left Shift while right Shift
 * is still down leaves Shift held.
 *
 * <p>Held by the keyboard that holds it, not by key code alone (review finding R17). Two keyboards
 * can hold the same key, and one's key-up must not let go of the other's; an unplugged keyboard
 * takes its own keys with it. A key-up that never arrives — the keyboard was unplugged mid-chord,
 * the window changed — would otherwise leave a modifier held for ever, so the next ordinary key
 * from that keyboard is believed about what it carries.
 */
final class HeldHardwareModifiers {
    /** One key, on one keyboard. */
    private static final class Held {
        final int deviceId;
        final int keyCode;

        Held(int deviceId, int keyCode) {
            this.deviceId = deviceId;
            this.keyCode = keyCode;
        }
    }

    private final List<Held> down = new ArrayList<>(4);

    /** Records a key event from one keyboard; returns true when it was a modifier key's. */
    boolean onKey(int deviceId, int keyCode, boolean isDown) {
        if (modifierOf(keyCode) == null) {
            return false;
        }
        forget(deviceId, keyCode);
        if (isDown) {
            down.add(new Held(deviceId, keyCode));
        }
        return true;
    }

    /**
     * What an ordinary key from this keyboard says about the modifiers it carries: anything this
     * keyboard is believed to hold, and which the event does not carry, was let go of unheard.
     * A modifier's own event is not asked — its meta state is the platform's business.
     */
    void reconcile(int deviceId, int keyCode, int metaState) {
        if (modifierOf(keyCode) != null) {
            return;
        }
        for (int i = down.size() - 1; i >= 0; i--) {
            Held held = down.get(i);
            if (held.deviceId == deviceId && (metaState & metaMaskOf(held.keyCode)) == 0) {
                down.remove(i);
            }
        }
    }

    /** Forgets what one keyboard held: it was unplugged, or switched away from. */
    void forgetDevice(int deviceId) {
        for (int i = down.size() - 1; i >= 0; i--) {
            if (down.get(i).deviceId == deviceId) {
                down.remove(i);
            }
        }
    }

    /** Forgets everything — a field left with a key still down, or a session ending. */
    void clear() {
        down.clear();
    }

    Set<KeyModifier> held() {
        Set<KeyModifier> mods = EnumSet.noneOf(KeyModifier.class);
        for (Held key : down) {
            KeyModifier mod = modifierOf(key.keyCode);
            if (mod != null) {
                mods.add(mod);
            }
        }
        return mods;
    }

    private void forget(int deviceId, int keyCode) {
        for (int i = down.size() - 1; i >= 0; i--) {
            Held held = down.get(i);
            if (held.deviceId == deviceId && held.keyCode == keyCode) {
                down.remove(i);
            }
        }
    }

    /** The meta-state bit for one side of one modifier, which an event carries while it is down. */
    private static int metaMaskOf(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CTRL_LEFT:
                return KeyEvent.META_CTRL_LEFT_ON;
            case KeyEvent.KEYCODE_CTRL_RIGHT:
                return KeyEvent.META_CTRL_RIGHT_ON;
            case KeyEvent.KEYCODE_SHIFT_LEFT:
                return KeyEvent.META_SHIFT_LEFT_ON;
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                return KeyEvent.META_SHIFT_RIGHT_ON;
            case KeyEvent.KEYCODE_ALT_LEFT:
                return KeyEvent.META_ALT_LEFT_ON;
            case KeyEvent.KEYCODE_ALT_RIGHT:
                return KeyEvent.META_ALT_RIGHT_ON;
            case KeyEvent.KEYCODE_META_LEFT:
                return KeyEvent.META_META_LEFT_ON;
            case KeyEvent.KEYCODE_META_RIGHT:
                return KeyEvent.META_META_RIGHT_ON;
            default:
                return 0;
        }
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
