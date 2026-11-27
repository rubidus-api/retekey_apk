package com.retekey;

import java.util.Collections;
import java.util.Set;
import com.retekey.core.ControlKey;
import com.retekey.core.KeyModifier;
import com.retekey.core.LatchState;
import com.retekey.core.ModifierLatches;
import com.retekey.core.RawKeyModifiers;

/**
 * Coordinates Shift and the Ctrl/Alt/Meta/RShift modifier latches uniformly across touch, bar, and
 * hold transitions. Extracted from {@link ReteKeyboardView} so chord assembly, hold-to-lock/unlock,
 * multi-finger unwind, and one-shot consumption are unit-tested on the JVM.
 */
final class ModifierCoordinator {
    private final LatchState shiftLayer;
    private final ModifierLatches modifierLatches;

    ModifierCoordinator() {
        this(new LatchState(), new ModifierLatches());
    }

    ModifierCoordinator(LatchState shiftLayer, ModifierLatches modifierLatches) {
        this.shiftLayer = shiftLayer;
        this.modifierLatches = modifierLatches;
    }

    boolean shiftActive() {
        return shiftLayer.isActive();
    }

    boolean shiftLocked() {
        return shiftLayer.isLocked();
    }

    LatchState.State shiftState() {
        return shiftLayer.state();
    }

    boolean modifierActive(ControlKey control) {
        return modifierLatches.isActive(control);
    }

    boolean modifierLocked(ControlKey control) {
        return modifierLatches.isLocked(control);
    }

    LatchState.State stateOf(ControlKey control) {
        if (control == ControlKey.SHIFT) {
            return shiftLayer.state();
        }
        return modifierLatches.stateOf(control);
    }

    Set<KeyModifier> latchedModifiers() {
        return modifierLatches.active();
    }

    Set<KeyModifier> rawKeyModifiers() {
        return RawKeyModifiers.of(modifierLatches.active(), shiftLayer.isActive());
    }

    /**
     * Active modifiers for a letter key chord (Ctrl+B, Ctrl+Shift+Z, etc.). Returns empty when no
     * non-Shift modifier latch is active so plain shifted letters continue as text layers.
     */
    Set<KeyModifier> chordModifiersForLetter() {
        Set<KeyModifier> latched = modifierLatches.active();
        if (latched.isEmpty()) {
            return Collections.emptySet();
        }
        return RawKeyModifiers.of(latched, shiftLayer.isActive());
    }

    String signature() {
        return shiftLayer.isActive() + "|" + shiftLayer.isLocked() + "|"
            + modifierLatches.signature();
    }

    /** Arms a modifier on finger-down and returns its prior state for later hold/unwind. */
    LatchState.State press(ControlKey control) {
        LatchState.State before = stateOf(control);
        tap(control);
        return before;
    }

    void tap(ControlKey control) {
        if (control == ControlKey.SHIFT) {
            shiftLayer.tap();
        } else if (ModifierLatches.handles(control)) {
            modifierLatches.tap(control);
        }
    }

    /**
     * Toggles lock on long-press. When {@code beforePress} is non-null, restores it first so a
     * finger landing on an already-locked modifier unlocks cleanly instead of re-locking.
     */
    void hold(ControlKey control, LatchState.State beforePress) {
        if (control == ControlKey.SHIFT) {
            if (beforePress != null) {
                shiftLayer.restore(beforePress);
            }
            shiftLayer.toggleLock();
        } else if (ModifierLatches.handles(control)) {
            if (beforePress != null) {
                modifierLatches.restore(control, beforePress);
            }
            modifierLatches.hold(control);
        }
    }

    void setLockFromBar(ControlKey control, boolean down) {
        if (control == ControlKey.SHIFT) {
            if (shiftLayer.isLocked() != down) {
                shiftLayer.toggleLock();
            }
            return;
        }
        if (ModifierLatches.handles(control) && modifierLatches.isLocked(control) != down) {
            modifierLatches.hold(control);
        }
    }

    /** Spends ONLY {@code control}'s one-shot if a key was typed while that finger was down. */
    boolean releaseAfterTyped(ControlKey control, boolean anotherFingerHoldsSame) {
        if (control == null || anotherFingerHoldsSame) {
            return false;
        }
        if (control == ControlKey.SHIFT) {
            return shiftLayer.consumeOneShot();
        }
        return modifierLatches.consumeOneShot(control);
    }

    /** Unwinds a canceled or retargeted modifier press unless another finger owns the same key. */
    boolean unwind(
            ControlKey control,
            LatchState.State beforePress,
            boolean typedWhileHeld,
            boolean anotherFingerHoldsSame) {
        if (control == null) {
            return false;
        }
        if (typedWhileHeld) {
            return releaseAfterTyped(control, anotherFingerHoldsSame);
        }
        if (anotherFingerHoldsSame || beforePress == null) {
            return false;
        }
        if (control == ControlKey.SHIFT) {
            shiftLayer.restore(beforePress);
        } else {
            modifierLatches.restore(control, beforePress);
        }
        return true;
    }

    /**
     * Consumes active one-shot Shift and (when {@code includeLatches} is true) each unheld
     * Ctrl/Alt/Meta/RShift one-shot independently.
     */
    boolean consumeOneShots(boolean includeLatches, Set<ControlKey> heldFingers) {
        boolean changed = false;
        Set<ControlKey> held = heldFingers == null ? Collections.emptySet() : heldFingers;
        if (!held.contains(ControlKey.SHIFT)) {
            changed |= shiftLayer.consumeOneShot();
        }
        if (includeLatches) {
            for (ControlKey modifier : ModifierLatches.KEYS) {
                if (!held.contains(modifier)) {
                    changed |= modifierLatches.consumeOneShot(modifier);
                }
            }
        }
        return changed;
    }

    void tapShiftForPreview() {
        shiftLayer.tap();
    }

    void clearShift() {
        shiftLayer.clear();
    }

    void clearAll() {
        shiftLayer.clear();
        modifierLatches.clear();
    }
}
