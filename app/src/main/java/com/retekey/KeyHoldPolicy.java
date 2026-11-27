package com.retekey;

import com.retekey.core.ControlKey;
import com.retekey.core.ModifierLatches;

/**
 * Decides what holding a key down means: opening its hold/flick UI, locking a modifier, repeating
 * a character/navigation key, or doing nothing. Android-free so hold vs repeat rules stay testable.
 */
final class KeyHoldPolicy {
    enum Action { HOLD, REPEAT, NONE }

    private KeyHoldPolicy() {
    }

    /** Whether dragging off this key emits a flick character instead of retargeting. */
    static boolean showsFlickGuide(SoftwareKeySpec key) {
        if (key == null) {
            return false;
        }
        return key.hasFlicks()
            || KanaFlick.of(key) != null
            || key.stableKeyId().startsWith("touch.cheonjiin.");
    }

    /** Whether holding an enabled key fires key repeats when long-press UI does not claim it. */
    static boolean repeatsOnHold(SoftwareKeySpec key) {
        if (key == null) {
            return false;
        }
        return key.enabled()
            && !key.isControl()
            && !key.hasLongPress()
            && !key.hasLongPressControl()
            && !showsFlickGuide(key);
    }

    static Action actionFor(SoftwareKeySpec key, boolean repeatEnabled) {
        if (key == null || (!key.enabled() && !key.isControl())) {
            return Action.NONE;
        }
        if (key.hasLongPress()
                || key.hasLongPressControl()
                || showsFlickGuide(key)
                || (key.isControl()
                    && (key.control() == ControlKey.SHIFT
                        || ModifierLatches.handles(key.control())))) {
            return Action.HOLD;
        }
        if (repeatEnabled && repeatsOnHold(key)) {
            return Action.REPEAT;
        }
        return Action.NONE;
    }
}
