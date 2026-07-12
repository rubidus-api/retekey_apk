package dev.hellgates.retekeyime;

import java.util.Objects;

public final class SemanticInput {
    public enum Kind {
        TEXT,
        JAMO,
        DELETE_BACKWARD,
        FLUSH
    }

    private static final SemanticInput DELETE_BACKWARD =
        new SemanticInput(Kind.DELETE_BACKWARD, "", null);
    private static final SemanticInput FLUSH = new SemanticInput(Kind.FLUSH, "", null);

    private final Kind kind;
    private final String text;
    private final SemanticJamo jamo;

    private SemanticInput(Kind kind, String text, SemanticJamo jamo) {
        this.kind = kind;
        this.text = text;
        this.jamo = jamo;
    }

    public static SemanticInput text(String text) {
        if (text == null || text.isEmpty() || !UnicodeScalar.isWellFormed(text)) {
            throw new IllegalArgumentException("text input must be non-empty well-formed Unicode");
        }
        return new SemanticInput(Kind.TEXT, text, null);
    }

    public static SemanticInput jamo(SemanticJamo jamo) {
        if (jamo == null) {
            throw new IllegalArgumentException("jamo must not be null");
        }
        return new SemanticInput(Kind.JAMO, "", jamo);
    }

    public static SemanticInput deleteBackward() {
        return DELETE_BACKWARD;
    }

    public static SemanticInput flush() {
        return FLUSH;
    }

    public Kind kind() {
        return kind;
    }

    public String text() {
        return text;
    }

    public SemanticJamo jamo() {
        return jamo;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SemanticInput)) {
            return false;
        }
        SemanticInput that = (SemanticInput) other;
        return kind == that.kind && text.equals(that.text) && Objects.equals(jamo, that.jamo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, text, jamo);
    }

    @Override
    public String toString() {
        return "SemanticInput{" +
            "kind=" + kind +
            ", textLength=" + text.length() +
            ", jamo=" + jamo +
            '}';
    }
}
