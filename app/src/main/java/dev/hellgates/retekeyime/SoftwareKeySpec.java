package dev.hellgates.retekeyime;

public final class SoftwareKeySpec {
    private final String stableKeyId;
    private final String label;
    private final SemanticInput semanticInput;

    private SoftwareKeySpec(String stableKeyId, String label, SemanticInput semanticInput) {
        if (stableKeyId == null || stableKeyId.isEmpty()) {
            throw new IllegalArgumentException("stable key id must not be empty");
        }
        if (label == null || label.isEmpty()) {
            throw new IllegalArgumentException("key label must not be empty");
        }
        this.stableKeyId = stableKeyId;
        this.label = label;
        this.semanticInput = semanticInput;
    }

    public static SoftwareKeySpec enabled(
        String stableKeyId,
        String label,
        SemanticInput semanticInput
    ) {
        if (semanticInput == null) {
            throw new IllegalArgumentException("enabled key requires semantic input");
        }
        return new SoftwareKeySpec(stableKeyId, label, semanticInput);
    }

    public static SoftwareKeySpec disabled(String stableKeyId, String label) {
        return new SoftwareKeySpec(stableKeyId, label, null);
    }

    public String stableKeyId() {
        return stableKeyId;
    }

    public String label() {
        return label;
    }

    public boolean enabled() {
        return semanticInput != null;
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
            '}';
    }
}
