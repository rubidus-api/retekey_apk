package com.retekey;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

/**
 * Ctrl, Meta and Alt, each holding the same three states Shift does: off, armed for exactly one
 * key, or locked. A tap arms, a hold locks, and a tap on a locked one clears it.
 *
 * <p>The distinction that matters is between armed and locked, and it is only visible when a key
 * is pressed: a chord takes every modifier that is active, and afterwards the armed ones are
 * spent while the locked ones stay. Nothing here knows about Android or about the view — what
 * reaches the editor is the modifier set {@link #active()} returns.
 */
public final class ModifierLatches {
    /** The modifiers a finger can arm or lock. Shift is the same idea but changes which layout
     * the keyboard draws, so it lives with the layout rather than here. */
    public static final Set<ControlKey> KEYS =
        Collections.unmodifiableSet(EnumSet.of(ControlKey.CTRL, ControlKey.META, ControlKey.ALT, ControlKey.RSHIFT));

    private final EnumMap<ControlKey, LatchState> latches = new EnumMap<>(ControlKey.class);

    public ModifierLatches() {
        for (ControlKey modifier : KEYS) {
            latches.put(modifier, new LatchState());
        }
    }

    /** Whether this control is one of the three, and so reacts to a tap and a hold. */
    public static boolean handles(ControlKey control) {
        return control != null && KEYS.contains(control);
    }

    /** A tap: arm for one key, cancel an arming, or clear a lock. */
    public void tap(ControlKey modifier) {
        latch(modifier).tap();
    }

    /** A hold: lock it down, or let it up if it already is. */
    public void hold(ControlKey modifier) {
        latch(modifier).toggleLock();
    }

    public boolean isActive(ControlKey modifier) {
        return handles(modifier) && latch(modifier).isActive();
    }

    public boolean isLocked(ControlKey modifier) {
        return handles(modifier) && latch(modifier).isLocked();
    }

    /** The modifiers a chord pressed right now should carry. */
    public Set<KeyModifier> active() {
        Set<KeyModifier> modifiers = EnumSet.noneOf(KeyModifier.class);
        for (ControlKey modifier : KEYS) {
            if (!latch(modifier).isActive()) {
                continue;
            }
            switch (modifier) {
                case CTRL:
                    modifiers.add(KeyModifier.CTRL);
                    break;
                case ALT:
                    modifiers.add(KeyModifier.ALT);
                    break;
                case META:
                    modifiers.add(KeyModifier.META);
                    break;
                case RSHIFT:
                    modifiers.add(KeyModifier.SHIFT);
                    break;
                default:
                    break;
            }
        }
        return modifiers;
    }

    /**
     * Spends the armed one-shots after a key press and says whether anything changed. A locked
     * modifier is not spent: staying is the whole of what locking it means.
     */
    public boolean consumeOneShots() {
        boolean changed = false;
        for (ControlKey modifier : KEYS) {
            changed |= latch(modifier).consumeOneShot();
        }
        return changed;
    }

    public void clear() {
        for (ControlKey modifier : KEYS) {
            latch(modifier).clear();
        }
    }

    /** A drawing-cache key: a bitmap drawn with one of these armed must not be reused without it. */
    public String signature() {
        StringBuilder signature = new StringBuilder();
        for (ControlKey modifier : KEYS) {
            signature.append(latch(modifier).state()).append(',');
        }
        return signature.toString();
    }

    private LatchState latch(ControlKey modifier) {
        LatchState latch = latches.get(modifier);
        if (latch == null) {
            throw new IllegalArgumentException("not a latching modifier: " + modifier);
        }
        return latch;
    }
}
