package com.retekey;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * A raw key event to hand to the editor: a platform-neutral {@link RawKey}, the modifiers held with
 * it, and whether this is the down or the up half. The Android bridge turns it into a KeyEvent.
 */
public final class RawEditorKey {
    public enum Action {
        DOWN,
        UP
    }

    private final RawKey key;
    private final Set<KeyModifier> modifiers;
    private final Action action;
    private final boolean asHardware;

    private RawEditorKey(RawKey key, Set<KeyModifier> modifiers, Action action, boolean asHardware) {
        this.key = Objects.requireNonNull(key, "key");
        this.action = Objects.requireNonNull(action, "action");
        this.asHardware = asHardware;
        this.modifiers = modifiers.isEmpty()
            ? Collections.emptySet()
            : Collections.unmodifiableSet(EnumSet.copyOf(modifiers));
    }

    public static RawEditorKey of(RawKey key, Action action) {
        return new RawEditorKey(key, Collections.emptySet(), action, false);
    }

    public static RawEditorKey of(RawKey key, Set<KeyModifier> modifiers, Action action) {
        return new RawEditorKey(key, modifiers, action, false);
    }

    /**
     * The same key, dressed as a physical keyboard would send it: keyboard source, a real scan
     * code, and no soft-keyboard flag. A remote-desktop relay routes events by that shape — its
     * soft path handles each event alone (a lone Ctrl tap, a letter as typed text), while its
     * hardware path tracks modifier state and combines Ctrl with the letter.
     */
    public static RawEditorKey hardware(RawKey key, Set<KeyModifier> modifiers, Action action) {
        return new RawEditorKey(key, modifiers, action, true);
    }

    /** Whether the bridge should dress this event as a physical keyboard's. */
    public boolean asHardware() {
        return asHardware;
    }

    public RawKey key() {
        return key;
    }

    public Set<KeyModifier> modifiers() {
        return modifiers;
    }

    public Action action() {
        return action;
    }
}
