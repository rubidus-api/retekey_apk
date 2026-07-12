package dev.hellgates.retekeyime;

import java.util.Objects;

public final class ExecutionContext {
    private final boolean accepting;
    private final long generation;
    private final long revision;
    private final EditorBounds bounds;
    private final EditorCapabilities capabilities;
    private final boolean boundsConfirmed;

    private ExecutionContext(
        boolean accepting,
        long generation,
        long revision,
        EditorBounds bounds,
        EditorCapabilities capabilities,
        boolean boundsConfirmed
    ) {
        if (generation < 1) {
            throw new IllegalArgumentException("generation must be positive");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }
        this.accepting = accepting;
        this.generation = generation;
        this.revision = revision;
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.boundsConfirmed = boundsConfirmed;
    }

    public static ExecutionContext active(
        long generation,
        long revision,
        EditorBounds bounds,
        EditorCapabilities capabilities
    ) {
        return active(generation, revision, bounds, capabilities, true);
    }

    public static ExecutionContext active(
        long generation,
        long revision,
        EditorBounds bounds,
        EditorCapabilities capabilities,
        boolean boundsConfirmed
    ) {
        return new ExecutionContext(
            true,
            generation,
            revision,
            bounds,
            capabilities,
            boundsConfirmed
        );
    }

    public static ExecutionContext stopped(
        long generation,
        long revision,
        EditorBounds bounds,
        EditorCapabilities capabilities
    ) {
        return new ExecutionContext(false, generation, revision, bounds, capabilities, false);
    }

    public boolean isAccepting() {
        return accepting;
    }

    public long generation() {
        return generation;
    }

    public long revision() {
        return revision;
    }

    public EditorBounds bounds() {
        return bounds;
    }

    public EditorCapabilities capabilities() {
        return capabilities;
    }

    public boolean areBoundsConfirmed() {
        return boundsConfirmed;
    }
}
