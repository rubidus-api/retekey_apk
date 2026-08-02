package dev.hellgates.retekeyime;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class SemanticInput {
    public enum Kind {
        TEXT,
        JAMO,
        DELETE_BACKWARD,
        FLUSH,
        PRIMARY_ACTION,
        RAW_KEY
    }

    private static final SemanticInput DELETE_BACKWARD =
        new SemanticInput(Kind.DELETE_BACKWARD, "", null, null, Collections.emptySet(), false);
    private static final SemanticInput DELETE_FOR_CORRECTION =
        new SemanticInput(Kind.DELETE_BACKWARD, "", null, null, Collections.emptySet(), true);
    private static final SemanticInput FLUSH =
        new SemanticInput(Kind.FLUSH, "", null, null, Collections.emptySet(), false);
    private static final SemanticInput PRIMARY_ACTION =
        new SemanticInput(Kind.PRIMARY_ACTION, "", null, null, Collections.emptySet(), false);

    private final Kind kind;
    private final String text;
    private final SemanticJamo jamo;
    private final RawKey rawKey;
    private final Set<KeyModifier> modifiers;
    private final boolean correction;
    private final RawKeyPhase rawKeyPhase;

    private SemanticInput(
        Kind kind,
        String text,
        SemanticJamo jamo,
        RawKey rawKey,
        Set<KeyModifier> modifiers,
        boolean correction
    ) {
        this(kind, text, jamo, rawKey, modifiers, correction, RawKeyPhase.TAP);
    }

    private SemanticInput(
        Kind kind,
        String text,
        SemanticJamo jamo,
        RawKey rawKey,
        Set<KeyModifier> modifiers,
        boolean correction,
        RawKeyPhase rawKeyPhase
    ) {
        this.rawKeyPhase = Objects.requireNonNull(rawKeyPhase, "rawKeyPhase");
        this.kind = kind;
        this.text = text;
        this.jamo = jamo;
        this.rawKey = rawKey;
        this.modifiers = modifiers.isEmpty()
            ? Collections.emptySet()
            : Collections.unmodifiableSet(EnumSet.copyOf(modifiers));
        this.correction = correction;
    }

    public static SemanticInput text(String text) {
        if (text == null || text.isEmpty() || !UnicodeScalar.isWellFormed(text)) {
            throw new IllegalArgumentException("text input must be non-empty well-formed Unicode");
        }
        return new SemanticInput(Kind.TEXT, text, null, null, Collections.emptySet(), false);
    }

    public static SemanticInput jamo(SemanticJamo jamo) {
        if (jamo == null) {
            throw new IllegalArgumentException("jamo must not be null");
        }
        return new SemanticInput(Kind.JAMO, "", jamo, null, Collections.emptySet(), false);
    }

    /** A hardware key, optionally chorded with modifiers, delivered to the editor as a key event. */
    public static SemanticInput rawKey(RawKey rawKey) {
        return rawKey(rawKey, Collections.emptySet());
    }

    public static SemanticInput rawKey(RawKey rawKey, Set<KeyModifier> modifiers) {
        return rawKey(rawKey, modifiers, RawKeyPhase.TAP);
    }

    /**
     * A raw key sent as one half of a press. {@link RawKeyPhase#HOLD} presses it and leaves it
     * down; {@link RawKeyPhase#RELEASE} lets it up. Used by keys that latch themselves down.
     */
    public static SemanticInput rawKey(
        RawKey rawKey,
        Set<KeyModifier> modifiers,
        RawKeyPhase phase
    ) {
        if (rawKey == null) {
            throw new IllegalArgumentException("raw key must not be null");
        }
        if (modifiers == null) {
            throw new IllegalArgumentException("modifiers must not be null");
        }
        return new SemanticInput(Kind.RAW_KEY, "", null, rawKey, modifiers, false, phase);
    }

    /** The same raw key with an added set of modifiers folded in. */
    public SemanticInput withModifiers(Set<KeyModifier> extra) {
        if (kind != Kind.RAW_KEY) {
            throw new IllegalStateException("only a raw key carries modifiers");
        }
        if (extra.isEmpty()) {
            return this;
        }
        EnumSet<KeyModifier> merged = EnumSet.noneOf(KeyModifier.class);
        merged.addAll(modifiers);
        merged.addAll(extra);
        return new SemanticInput(Kind.RAW_KEY, "", null, rawKey, merged, false, rawKeyPhase);
    }

    public static SemanticInput deleteBackward() {
        return DELETE_BACKWARD;
    }

    /**
     * A delete the keyboard issues to take back a letter it typed itself, on the way to typing the
     * letter that replaces it — the 12-key automata do this whenever a key transforms what is on
     * screen. It deletes exactly what an ordinary backspace deletes, and additionally lets the
     * Hangul composer re-open the syllable it closed a moment ago, so a consonant reached by 획추가
     * or 쌍자음 can still become the tail of a compound final. A user's own backspace must stay
     * {@link #deleteBackward()}: it means "undo what I see", not "I am mid-letter".
     */
    public static SemanticInput deleteForCorrection() {
        return DELETE_FOR_CORRECTION;
    }

    /** Whether this delete is the keyboard correcting its own output rather than the user's. */
    public boolean isCorrection() {
        return correction;
    }

    public static SemanticInput flush() {
        return FLUSH;
    }

    /**
     * The editor's primary action: Enter, or whatever action the focused editor requests.
     * The editor profile, not the layout, decides what it means.
     */
    public static SemanticInput primaryAction() {
        return PRIMARY_ACTION;
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

    public RawKey rawKey() {
        return rawKey;
    }

    public Set<KeyModifier> modifiers() {
        return modifiers;
    }

    /** Which half of a press this raw key is; {@link RawKeyPhase#TAP} for everything else. */
    public RawKeyPhase rawKeyPhase() {
        return rawKeyPhase;
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
        return kind == that.kind
            && correction == that.correction
            && text.equals(that.text)
            && Objects.equals(jamo, that.jamo)
            && rawKey == that.rawKey
            && rawKeyPhase == that.rawKeyPhase
            && modifiers.equals(that.modifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, text, jamo, rawKey, modifiers, correction, rawKeyPhase);
    }

    @Override
    public String toString() {
        return "SemanticInput{" +
            "kind=" + kind +
            ", textLength=" + text.length() +
            ", jamo=" + jamo +
            ", rawKey=" + rawKey +
            ", modifiers=" + modifiers +
            ", rawKeyPhase=" + rawKeyPhase +
            ", correction=" + correction +
            '}';
    }
}
