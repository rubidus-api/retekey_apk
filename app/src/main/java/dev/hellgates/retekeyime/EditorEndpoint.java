package dev.hellgates.retekeyime;

import java.util.Objects;

public final class EditorEndpoint {
    private final long generation;
    private final EditorBridge bridge;
    private final Fn.BooleanSupplier current;

    private EditorEndpoint(
        long generation,
        EditorBridge bridge,
        Fn.BooleanSupplier current
    ) {
        if (generation < 1) {
            throw new IllegalArgumentException("generation must be positive");
        }
        this.generation = generation;
        this.bridge = Objects.requireNonNull(bridge, "bridge");
        this.current = Objects.requireNonNull(current, "current");
    }

    public static EditorEndpoint of(long generation, EditorBridge bridge) {
        return new EditorEndpoint(generation, bridge, () -> true);
    }

    public long generation() {
        return generation;
    }

    public EditorBridge bridge() {
        return bridge;
    }

    public boolean isCurrent() {
        try {
            return current.getAsBoolean();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    EditorEndpoint guardedBy(Fn.BooleanSupplier additionalGuard) {
        Objects.requireNonNull(additionalGuard, "additionalGuard");
        return new EditorEndpoint(
            generation,
            bridge,
            () -> isCurrent() && additionalGuard.getAsBoolean()
        );
    }
}
