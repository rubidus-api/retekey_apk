package dev.hellgates.retekeyime;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class KeyAction {
    public enum Kind {
        COMMIT_TEXT,
        SET_COMPOSING_TEXT,
        FINISH_COMPOSING,
        DELETE_BACKWARD,
        PERFORM_EDITOR_ACTION,
        RAW_ENTER,
        RAW_KEY
    }

    private static final KeyAction DELETE_BACKWARD =
        new KeyAction(Kind.DELETE_BACKWARD, "", 0, null, Collections.emptySet());
    private static final KeyAction RAW_ENTER =
        new KeyAction(Kind.RAW_ENTER, "", 0, null, Collections.emptySet());

    private final Kind kind;
    private final String text;
    private final int actionId;
    private final RawKey rawKey;
    private final Set<KeyModifier> modifiers;

    private KeyAction(
        Kind kind,
        String text,
        int actionId,
        RawKey rawKey,
        Set<KeyModifier> modifiers
    ) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.text = Objects.requireNonNull(text, "text");
        this.actionId = actionId;
        this.rawKey = rawKey;
        this.modifiers = modifiers.isEmpty()
            ? Collections.emptySet()
            : Collections.unmodifiableSet(EnumSet.copyOf(modifiers));
    }

    public static KeyAction commitText(String text) {
        return new KeyAction(Kind.COMMIT_TEXT, text, 0, null, Collections.emptySet());
    }

    public static KeyAction setComposingText(String text) {
        return new KeyAction(Kind.SET_COMPOSING_TEXT, text, 0, null, Collections.emptySet());
    }

    public static KeyAction finishComposing() {
        return new KeyAction(Kind.FINISH_COMPOSING, "", 0, null, Collections.emptySet());
    }

    public static KeyAction deleteBackward() {
        return DELETE_BACKWARD;
    }

    public static KeyAction performEditorAction(int actionId) {
        return new KeyAction(Kind.PERFORM_EDITOR_ACTION, "", actionId, null, Collections.emptySet());
    }

    public static KeyAction rawEnter() {
        return RAW_ENTER;
    }

    public static KeyAction rawKey(RawKey rawKey, Set<KeyModifier> modifiers) {
        if (rawKey == null) {
            throw new IllegalArgumentException("raw key must not be null");
        }
        return new KeyAction(Kind.RAW_KEY, "", 0, rawKey, modifiers);
    }

    public RawKey rawKey() {
        if (kind != Kind.RAW_KEY) {
            throw new IllegalStateException("action has no raw key");
        }
        return rawKey;
    }

    public Set<KeyModifier> modifiers() {
        return modifiers;
    }

    public Kind kind() {
        return kind;
    }

    public String text() {
        return text;
    }

    public int actionId() {
        if (kind != Kind.PERFORM_EDITOR_ACTION) {
            throw new IllegalStateException("action has no editor action id");
        }
        return actionId;
    }

    public boolean isTerminal() {
        return kind == Kind.PERFORM_EDITOR_ACTION
            || kind == Kind.RAW_ENTER
            || kind == Kind.RAW_KEY;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof KeyAction)) {
            return false;
        }
        KeyAction that = (KeyAction) other;
        return kind == that.kind
            && text.equals(that.text)
            && actionId == that.actionId
            && rawKey == that.rawKey
            && modifiers.equals(that.modifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, text, actionId, rawKey, modifiers);
    }

    @Override
    public String toString() {
        return "KeyAction{" + "kind=" + kind + ", textLength=" + text.length()
            + ", rawKey=" + rawKey + '}';
    }
}
