package dev.hellgates.retekeyime;

import java.util.Objects;

public final class RawEditorKey {
    public enum Kind {
        DELETE,
        ENTER
    }

    public enum Action {
        DOWN,
        UP
    }

    private final Kind kind;
    private final Action action;

    private RawEditorKey(Kind kind, Action action) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.action = Objects.requireNonNull(action, "action");
    }

    public static RawEditorKey of(Kind kind, Action action) {
        return new RawEditorKey(kind, action);
    }

    public Kind kind() {
        return kind;
    }

    public Action action() {
        return action;
    }
}
