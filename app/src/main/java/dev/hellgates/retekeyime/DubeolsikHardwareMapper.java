package dev.hellgates.retekeyime;

/**
 * Standard 2-beolsik physical-letter mapping. Composition remains downstream.
 */
public final class DubeolsikHardwareMapper implements HardwareSemanticMapper {
    public static final DubeolsikHardwareMapper INSTANCE = new DubeolsikHardwareMapper();

    private DubeolsikHardwareMapper() {
    }

    @Override
    public SemanticInput map(String stableKeyId, boolean shift) {
        if (stableKeyId == null) {
            return null;
        }
        switch (stableKeyId) {
            case "hardware.key.q":
                return consonant(shift ? 8 : 7);
            case "hardware.key.w":
                return consonant(shift ? 13 : 12);
            case "hardware.key.e":
                return consonant(shift ? 4 : 3);
            case "hardware.key.r":
                return consonant(shift ? 1 : 0);
            case "hardware.key.t":
                return consonant(shift ? 10 : 9);
            case "hardware.key.y":
                return vowel(12);
            case "hardware.key.u":
                return vowel(6);
            case "hardware.key.i":
                return vowel(2);
            case "hardware.key.o":
                return vowel(shift ? 3 : 1);
            case "hardware.key.p":
                return vowel(shift ? 7 : 5);
            case "hardware.key.a":
                return consonant(6);
            case "hardware.key.s":
                return consonant(2);
            case "hardware.key.d":
                return consonant(11);
            case "hardware.key.f":
                return consonant(5);
            case "hardware.key.g":
                return consonant(18);
            case "hardware.key.h":
                return vowel(8);
            case "hardware.key.j":
                return vowel(4);
            case "hardware.key.k":
                return vowel(0);
            case "hardware.key.l":
                return vowel(20);
            case "hardware.key.z":
                return consonant(15);
            case "hardware.key.x":
                return consonant(16);
            case "hardware.key.c":
                return consonant(14);
            case "hardware.key.v":
                return consonant(17);
            case "hardware.key.b":
                return vowel(17);
            case "hardware.key.n":
                return vowel(13);
            case "hardware.key.m":
                return vowel(18);
            default:
                return null;
        }
    }

    private static SemanticInput consonant(int index) {
        return SemanticInput.jamo(SemanticJamo.contextualConsonant(index));
    }

    private static SemanticInput vowel(int index) {
        return SemanticInput.jamo(SemanticJamo.vowel(index));
    }
}
