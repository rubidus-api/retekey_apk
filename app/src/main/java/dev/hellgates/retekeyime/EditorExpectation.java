package dev.hellgates.retekeyime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EditorExpectation {
    private static final int MAX_ALTERNATIVES = 4;
    private static final EditorExpectation UNCONFIRMABLE =
        new EditorExpectation(Collections.emptyList(), Collections.emptyList());

    private final List<EditorBounds> alternatives;
    private final List<EditorBounds> intermediateBounds;

    private EditorExpectation(
        List<EditorBounds> alternatives,
        List<EditorBounds> intermediateBounds
    ) {
        this.alternatives = alternatives;
        this.intermediateBounds = intermediateBounds;
    }

    public static EditorExpectation exact(EditorBounds bounds) {
        if (bounds == null || !bounds.hasSelection()) {
            throw new IllegalArgumentException("exact expectation requires known selection");
        }
        return new EditorExpectation(
            Collections.singletonList(bounds),
            Collections.emptyList()
        );
    }

    public static EditorExpectation oneOf(List<EditorBounds> alternatives) {
        if (alternatives == null
            || alternatives.isEmpty()
            || alternatives.size() > MAX_ALTERNATIVES) {
            throw new IllegalArgumentException("expectation requires one to four alternatives");
        }
        ArrayList<EditorBounds> copy = new ArrayList<>(alternatives);
        if (copy.contains(null)) {
            throw new IllegalArgumentException("expectation alternatives must not contain null");
        }
        for (EditorBounds bounds : copy) {
            if (!bounds.hasSelection()) {
                throw new IllegalArgumentException("expectation alternatives must be known");
            }
        }
        for (int index = 0; index < copy.size(); index++) {
            if (copy.indexOf(copy.get(index)) != index) {
                throw new IllegalArgumentException("expectation alternatives must be distinct");
            }
        }
        return copy.size() == 1
            ? exact(copy.get(0))
            : new EditorExpectation(
                Collections.unmodifiableList(copy),
                Collections.emptyList()
            );
    }

    public static EditorExpectation unconfirmable() {
        return UNCONFIRMABLE;
    }

    public static EditorExpectation withIntermediateBounds(
        EditorExpectation finalExpectation,
        List<EditorBounds> intermediateBounds
    ) {
        if (finalExpectation == null || intermediateBounds == null) {
            throw new IllegalArgumentException("expectation parts must not be null");
        }
        ArrayList<EditorBounds> copy = new ArrayList<>(intermediateBounds);
        if (copy.contains(null)) {
            throw new IllegalArgumentException("intermediate bounds must not contain null");
        }
        for (EditorBounds bounds : copy) {
            if (!bounds.hasSelection()) {
                throw new IllegalArgumentException("intermediate bounds must be known");
            }
        }
        return copy.isEmpty()
            ? finalExpectation
            : new EditorExpectation(
                finalExpectation.alternatives,
                Collections.unmodifiableList(copy)
            );
    }

    public boolean isConfirmable() {
        return !alternatives.isEmpty();
    }

    public boolean matches(EditorBounds observed) {
        return observed != null && alternatives.contains(observed);
    }

    public boolean matchesIntermediate(EditorBounds observed) {
        return observed != null && intermediateBounds.contains(observed);
    }

    public EditorBounds workingBounds() {
        return alternatives.size() == 1 ? alternatives.get(0) : EditorBounds.unknown();
    }

    public List<EditorBounds> alternatives() {
        return alternatives;
    }

    public List<EditorBounds> intermediateBounds() {
        return intermediateBounds;
    }

    @Override
    public String toString() {
        return "EditorExpectation{"
            + "confirmable=" + isConfirmable()
            + ", alternativeCount=" + alternatives.size()
            + ", intermediateCount=" + intermediateBounds.size()
            + '}';
    }
}
