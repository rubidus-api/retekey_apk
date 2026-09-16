package com.retekey;

/**
 * What kind of jamo the next key is likely to be, read from the syllable being built. Korean
 * spelling is strict enough to say so in two cases, and the keyboard uses them only to settle a
 * touch that landed on the line between a consonant key and a vowel key (TouchTargets).
 *
 * <ul>
 *   <li>Nothing composing: a syllable starts with a consonant — ㅇ where it is spoken as a vowel.</li>
 *   <li>One consonant composing: a vowel follows it; two consonants cannot begin a syllable.</li>
 *   <li>Anything else — a syllable with its vowel — could take either, because ㅗ ㅏ spells ㅘ and
 *       a final consonant is just as likely. Then the keyboard guesses nothing.</li>
 * </ul>
 *
 * <p>Android-free, so the rule is a unit test.
 */
public final class JamoExpectation {
    public enum Kind {
        NONE,
        CONSONANT,
        VOWEL
    }

    private JamoExpectation() {
    }

    /** The kind expected next, given what is on screen as composing text. */
    public static Kind of(String preedit) {
        if (preedit == null || preedit.isEmpty()) {
            return Kind.CONSONANT;
        }
        if (preedit.length() == 1 && isCompatibilityConsonant(preedit.charAt(0))) {
            return Kind.VOWEL;
        }
        return Kind.NONE;
    }

    private static boolean isCompatibilityConsonant(char c) {
        return c >= 0x3131 && c <= 0x314E;
    }
}
