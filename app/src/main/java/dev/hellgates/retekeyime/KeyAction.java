package dev.hellgates.retekeyime;

import java.util.Objects;

public final class KeyAction {
    public enum Kind {
        COMMIT_TEXT,
        SET_COMPOSING_TEXT,
        FINISH_COMPOSING,
        DELETE_BACKWARD,
        PERFORM_EDITOR_ACTION,
        RAW_ENTER
    }

    private static final KeyAction DELETE_BACKWARD =
        new KeyAction(Kind.DELETE_BACKWARD, "", 0);
    private static final KeyAction RAW_ENTER = new KeyAction(Kind.RAW_ENTER, "", 0);

    private final Kind kind;
    private final String text;
    private final int actionId;

    private KeyAction(Kind kind, String text, int actionId) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.text = Objects.requireNonNull(text, "text");
        this.actionId = actionId;
    }

    public static KeyAction commitText(String text) {
        return new KeyAction(Kind.COMMIT_TEXT, text, 0);
    }

    public static KeyAction setComposingText(String text) {
        return new KeyAction(Kind.SET_COMPOSING_TEXT, text, 0);
    }

    public static KeyAction finishComposing() {
        return new KeyAction(Kind.FINISH_COMPOSING, "", 0);
    }

    public static KeyAction deleteBackward() {
        return DELETE_BACKWARD;
    }

    public static KeyAction performEditorAction(int actionId) {
        return new KeyAction(Kind.PERFORM_EDITOR_ACTION, "", actionId);
    }

    public static KeyAction rawEnter() {
        return RAW_ENTER;
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
            || kind == Kind.RAW_ENTER;
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
        return kind == that.kind && text.equals(that.text) && actionId == that.actionId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, text, actionId);
    }

    @Override
    public String toString() {
        return "KeyAction{" + "kind=" + kind + ", textLength=" + text.length() + '}';
    }
}
