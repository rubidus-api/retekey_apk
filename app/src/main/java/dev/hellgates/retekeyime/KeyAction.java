package dev.hellgates.retekeyime;

public final class KeyAction {
    public enum Kind {
        NOOP,
        COMMIT_TEXT,
        SET_COMPOSING_TEXT,
        FINISH_COMPOSING,
        DELETE_BACKWARD
    }

    private static final KeyAction NOOP = new KeyAction(Kind.NOOP, "");
    private static final KeyAction DELETE_BACKWARD = new KeyAction(Kind.DELETE_BACKWARD, "");

    private final Kind kind;
    private final String text;

    private KeyAction(Kind kind, String text) {
        this.kind = kind;
        this.text = text;
    }

    public static KeyAction noop() {
        return NOOP;
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
}
