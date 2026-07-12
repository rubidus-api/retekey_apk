package dev.hellgates.retekeyime;

import java.util.Objects;

public final class EditorTextResult {
    public enum Kind {
        VALUE,
        NULL_VALUE,
        RUNTIME_FAILURE,
        STALE_SESSION
    }

    private static final EditorTextResult NULL_VALUE =
        new EditorTextResult(Kind.NULL_VALUE, null);
    private static final EditorTextResult RUNTIME_FAILURE =
        new EditorTextResult(Kind.RUNTIME_FAILURE, null);
    private static final EditorTextResult STALE_SESSION =
        new EditorTextResult(Kind.STALE_SESSION, null);

    private final Kind kind;
    private final String value;

    private EditorTextResult(Kind kind, String value) {
        this.kind = kind;
        this.value = value;
    }

    public static EditorTextResult value(String value) {
        return new EditorTextResult(Kind.VALUE, Objects.requireNonNull(value, "value"));
    }

    public static EditorTextResult nullValue() {
        return NULL_VALUE;
    }

    public static EditorTextResult runtimeFailure() {
        return RUNTIME_FAILURE;
    }

    public static EditorTextResult staleSession() {
        return STALE_SESSION;
    }

    public Kind kind() {
        return kind;
    }

    public boolean hasValue() {
        return kind == Kind.VALUE;
    }

    public String value() {
        if (!hasValue()) {
            throw new IllegalStateException("text result has no value");
        }
        return value;
    }

    @Override
    public String toString() {
        return "EditorTextResult{"
            + "kind=" + kind
            + ", valueLength=" + (value == null ? -1 : value.length())
            + '}';
    }
}
