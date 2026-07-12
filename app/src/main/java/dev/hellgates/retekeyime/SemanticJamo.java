package dev.hellgates.retekeyime;

import java.util.Objects;

public final class SemanticJamo {
    public enum Role {
        CONTEXTUAL_CONSONANT,
        VOWEL,
        DIRECT_INITIAL,
        DIRECT_MEDIAL,
        DIRECT_FINAL
    }

    private final Role role;
    private final int index;

    public SemanticJamo(Role role, int index) {
        if (role == null) {
            throw new IllegalArgumentException("jamo role must not be null");
        }
        validateIndex(role, index);
        this.role = role;
        this.index = index;
    }

    public static SemanticJamo contextualConsonant(int index) {
        return new SemanticJamo(Role.CONTEXTUAL_CONSONANT, index);
    }

    public static SemanticJamo vowel(int index) {
        return new SemanticJamo(Role.VOWEL, index);
    }

    public static SemanticJamo directInitial(int index) {
        return new SemanticJamo(Role.DIRECT_INITIAL, index);
    }

    public static SemanticJamo directMedial(int index) {
        return new SemanticJamo(Role.DIRECT_MEDIAL, index);
    }

    public static SemanticJamo directFinal(int index) {
        return new SemanticJamo(Role.DIRECT_FINAL, index);
    }

    private static void validateIndex(Role role, int index) {
        int minimum = role == Role.DIRECT_FINAL ? 1 : 0;
        int maximum;
        switch (role) {
            case CONTEXTUAL_CONSONANT:
            case DIRECT_INITIAL:
                maximum = 18;
                break;
            case VOWEL:
            case DIRECT_MEDIAL:
                maximum = 20;
                break;
            case DIRECT_FINAL:
                maximum = 27;
                break;
            default:
                throw new IllegalArgumentException("unsupported jamo role");
        }
        if (index < minimum || index > maximum) {
            throw new IllegalArgumentException("jamo index is outside its role domain");
        }
    }

    public Role role() {
        return role;
    }

    public int index() {
        return index;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SemanticJamo)) {
            return false;
        }
        SemanticJamo that = (SemanticJamo) other;
        return index == that.index && role == that.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, index);
    }

    @Override
    public String toString() {
        return "SemanticJamo{" + "role=" + role + ", index=" + index + '}';
    }
}
