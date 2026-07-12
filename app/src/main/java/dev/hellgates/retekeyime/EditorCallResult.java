package dev.hellgates.retekeyime;

public final class EditorCallResult {
    public enum Kind {
        SUCCEEDED,
        REJECTED,
        RUNTIME_FAILURE,
        STALE_SESSION
    }

    private static final EditorCallResult SUCCEEDED = new EditorCallResult(Kind.SUCCEEDED);
    private static final EditorCallResult REJECTED = new EditorCallResult(Kind.REJECTED);
    private static final EditorCallResult RUNTIME_FAILURE =
        new EditorCallResult(Kind.RUNTIME_FAILURE);
    private static final EditorCallResult STALE_SESSION =
        new EditorCallResult(Kind.STALE_SESSION);

    private final Kind kind;

    private EditorCallResult(Kind kind) {
        this.kind = kind;
    }

    public static EditorCallResult succeeded() {
        return SUCCEEDED;
    }

    public static EditorCallResult rejected() {
        return REJECTED;
    }

    public static EditorCallResult runtimeFailure() {
        return RUNTIME_FAILURE;
    }

    public static EditorCallResult staleSession() {
        return STALE_SESSION;
    }

    public Kind kind() {
        return kind;
    }

    public boolean isSucceeded() {
        return kind == Kind.SUCCEEDED;
    }

    public boolean isRejected() {
        return kind == Kind.REJECTED;
    }

    public boolean isStaleSession() {
        return kind == Kind.STALE_SESSION;
    }

    @Override
    public String toString() {
        return "EditorCallResult{" + "kind=" + kind + '}';
    }
}
