package com.retekey;

import java.util.EnumSet;
import java.util.Set;

/**
 * What modifiers a raw key is sent with — an arrow, Home, Page Down, a function key.
 *
 * <p>The armed or locked Ctrl, Meta and Alt are obvious. Shift is here for a reason worth stating:
 * on a letter, the Shift key picks a layer and that is the whole of its meaning, so it never
 * travelled with a key press. A raw key has no layer to pick, so the only thing Shift can mean
 * there is the chord every keyboard makes with it — Shift+arrow selects text. Leaving it out was
 * why a locked Shift did nothing to the arrows, on the pad page and on the action bar alike.
 *
 * <p>Android-free, so the rule is a unit test rather than something to try on a phone.
 */
public final class RawKeyModifiers {
    private RawKeyModifiers() {
    }

    /**
     * The set to send with a raw key.
     *
     * @param latched the Ctrl/Meta/Alt (and right-hand Shift) a finger has armed or locked
     * @param shiftActive whether the layout's own Shift is armed or locked
     */
    public static Set<KeyModifier> of(Set<KeyModifier> latched, boolean shiftActive) {
        Set<KeyModifier> mods = EnumSet.noneOf(KeyModifier.class);
        if (latched != null) {
            mods.addAll(latched);
        }
        if (shiftActive) {
            mods.add(KeyModifier.SHIFT);
        }
        return mods;
    }
}
