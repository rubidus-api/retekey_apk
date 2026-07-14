package dev.hellgates.retekeyime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SoftwareKeySpec {
    private final String stableKeyId;
    private final String label;
    private final SemanticInput semanticInput;
    private final ControlKey control;
    private final int columnSpan;
    private final List<String> longPressTexts;

    private SoftwareKeySpec(
        String stableKeyId,
        String label,
        SemanticInput semanticInput,
        ControlKey control,
        int columnSpan,
        List<String> longPressTexts
    ) {
        if (stableKeyId == null || stableKeyId.isEmpty()) {
            throw new IllegalArgumentException("stable key id must not be empty");
        }
        if (label == null || label.isEmpty()) {
            throw new IllegalArgumentException("key label must not be empty");
        }
        if (columnSpan < 1) {
            throw new IllegalArgumentException("column span must be at least one column");
        }
        if (semanticInput != null && control != null) {
            throw new IllegalArgumentException("a key is either semantic or view-local, not both");
        }
        this.stableKeyId = stableKeyId;
        this.label = label;
        this.semanticInput = semanticInput;
        this.control = control;
        this.columnSpan = columnSpan;
        this.longPressTexts = longPressTexts;
    }

    public static SoftwareKeySpec enabled(
        String stableKeyId,
        String label,
        SemanticInput semanticInput
    ) {
        if (semanticInput == null) {
            throw new IllegalArgumentException("enabled key requires semantic input");
        }
        return new SoftwareKeySpec(
            stableKeyId,
            label,
            semanticInput,
            null,
            1,
            Collections.emptyList()
        );
    }

    public static SoftwareKeySpec control(String stableKeyId, String label, ControlKey control) {
        if (control == null) {
            throw new IllegalArgumentException("control key requires a control command");
        }
        return new SoftwareKeySpec(stableKeyId, label, null, control, 1, Collections.emptyList());
    }

    public static SoftwareKeySpec disabled(String stableKeyId, String label) {
        return new SoftwareKeySpec(stableKeyId, label, null, null, 1, Collections.emptyList());
    }

    public SoftwareKeySpec withColumnSpan(int newColumnSpan) {
        return new SoftwareKeySpec(
            stableKeyId,
            label,
            semanticInput,
            control,
            newColumnSpan,
            longPressTexts
        );
    }

    /**
     * Alternate characters reachable by holding the key. They are plain text, in the order the
     * popup shows them, and only an enabled text key can carry them.
     */
    public SoftwareKeySpec withLongPress(String... texts) {
        if (!enabled()) {
            throw new IllegalStateException("only an enabled key can carry long-press characters");
        }
        if (texts == null || texts.length == 0) {
            throw new IllegalArgumentException("long-press characters must not be empty");
        }
        List<String> candidates = new ArrayList<>(texts.length);
        for (String text : texts) {
            // Reuse the semantic-input guard: a candidate must be committable text.
            SemanticInput.text(text);
            candidates.add(text);
        }
        return new SoftwareKeySpec(
            stableKeyId,
            label,
            semanticInput,
            control,
            columnSpan,
            Collections.unmodifiableList(candidates)
        );
    }

    public List<String> longPressTexts() {
        return longPressTexts;
    }

    public boolean hasLongPress() {
        return !longPressTexts.isEmpty();
    }

    /** The event for the alternate character at {@code index} of the long-press popup. */
    public ProjectKeyEvent longPressEvent(int index) {
        if (index < 0 || index >= longPressTexts.size()) {
            throw new IndexOutOfBoundsException("no long-press candidate at " + index);
        }
        String text = longPressTexts.get(index);
        return ProjectKeyEvent.softwareDown(
            stableKeyId + ".long." + index,
            SemanticInput.text(text)
        );
    }

    public String stableKeyId() {
        return stableKeyId;
    }

    public String label() {
        return label;
    }

    public int columnSpan() {
        return columnSpan;
    }

    public boolean enabled() {
        return semanticInput != null;
    }

    public boolean isControl() {
        return control != null;
    }

    public ControlKey control() {
        return control;
    }

    public SemanticInput semanticInput() {
        return semanticInput;
    }

    public ProjectKeyEvent pressEvent() {
        if (!enabled()) {
            throw new IllegalStateException("disabled key cannot emit input");
        }
        return ProjectKeyEvent.softwareDown(stableKeyId, semanticInput);
    }

    @Override
    public String toString() {
        return "SoftwareKeySpec{" +
            "stableKeyId='" + stableKeyId + '\'' +
            ", enabled=" + enabled() +
            ", control=" + control +
            ", columnSpan=" + columnSpan +
            '}';
    }
}
