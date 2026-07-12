package dev.hellgates.retekeyime;

import java.util.Objects;

public final class KeyAction {
    public enum Kind {
        COMMIT_TEXT,
        SET_COMPOSING_TEXT,
        FINISH_COMPOSING,
        DELETE_BACKWARD
    }

    private static final KeyAction DELETE_BACKWARD = new KeyAction(Kind.DELETE_BACKWARD, "");

    private final Kind kind;
    private final String text;

    private KeyAction(Kind kind, String text) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.text = Objects.requireNonNull(text, "text");
    }

    public static KeyAction commitText(String text) {
        return new KeyAction(Kind.COMMIT_TEXT, text);
    }

    public static KeyAction setComposingText(String text) {
        return new KeyAction(Kind.SET_COMPOSING_TEXT, text);
    }

    public static KeyAction finishComposing() {
        return new KeyAction(Kind.FINISH_COMPOSING, "");
    }

    public static KeyAction deleteBackward() {
        return DELETE_BACKWARD;
    }

    public Kind kind() {
        return kind;
    }

    public String text() {
        return text;
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
        return kind == that.kind && text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, text);
    }

    @Override
    public String toString() {
        return "KeyAction{" + "kind=" + kind + ", textLength=" + text.length() + '}';
    }
}
