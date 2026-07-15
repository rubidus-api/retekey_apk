package dev.hellgates.retekeyime;

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

    private RawEditorKey(RawKey key, Set<KeyModifier> modifiers, Action action) {
        this.key = Objects.requireNonNull(key, "key");
        this.action = Objects.requireNonNull(action, "action");
        this.modifiers = modifiers.isEmpty()
            ? Collections.emptySet()
            : Collections.unmodifiableSet(EnumSet.copyOf(modifiers));
    }

    public static RawEditorKey of(RawKey key, Action action) {
        return new RawEditorKey(key, Collections.emptySet(), action);
    }

    public static RawEditorKey of(RawKey key, Set<KeyModifier> modifiers, Action action) {
        return new RawEditorKey(key, modifiers, action);
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
