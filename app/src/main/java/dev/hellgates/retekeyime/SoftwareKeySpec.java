package dev.hellgates.retekeyime;

public final class SoftwareKeySpec {
    private final String stableKeyId;
    private final String label;
    private final SemanticInput semanticInput;
    private final ControlKey control;
    private final int columnSpan;

    private SoftwareKeySpec(
        String stableKeyId,
        String label,
        SemanticInput semanticInput,
        ControlKey control,
        int columnSpan
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
    }

    public static SoftwareKeySpec enabled(
        String stableKeyId,
        String label,
        SemanticInput semanticInput
    ) {
        if (semanticInput == null) {
            throw new IllegalArgumentException("enabled key requires semantic input");
        }
        return new SoftwareKeySpec(stableKeyId, label, semanticInput, null, 1);
    }

    public static SoftwareKeySpec control(String stableKeyId, String label, ControlKey control) {
        if (control == null) {
            throw new IllegalArgumentException("control key requires a control command");
        }
        return new SoftwareKeySpec(stableKeyId, label, null, control, 1);
    }

    public static SoftwareKeySpec disabled(String stableKeyId, String label) {
        return new SoftwareKeySpec(stableKeyId, label, null, null, 1);
    }

    public SoftwareKeySpec withColumnSpan(int newColumnSpan) {
        return new SoftwareKeySpec(stableKeyId, label, semanticInput, control, newColumnSpan);
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
