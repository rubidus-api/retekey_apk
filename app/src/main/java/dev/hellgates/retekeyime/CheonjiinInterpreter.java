package dev.hellgates.retekeyime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The 천지인 12-key mode: seven grouped consonant keys and the three vowel elements ㅣ ㆍ ㅡ.
 *
 * <p>A phone key is not a jamo — what it means depends on what came before it. A consonant key
 * cycles through its group while it keeps being tapped (ㄱ → ㅋ → ㄲ → ㄱ), and the vowel keys build
 * a vowel out of elements, so ㅣ then ㆍ is ㅏ and one more ㆍ turns it into ㅑ. This class owns that
 * state and answers each press with the edits the composer below it should see: either a new jamo,
 * or a backspace followed by the jamo that replaces it.
 *
 * <p>Android-free, so the whole table is unit-tested directly. The keyboard view only maps a touch
 * to a {@link Key}.
 */
public final class CheonjiinInterpreter {
    /** The ten keys that carry Hangul; the rest of the page is the shared utility column. */
    public enum Key {
        /** ㄱ ㅋ ㄲ */
        GIYEOK,
        /** ㄴ ㄹ */
        NIEUN,
        /** ㄷ ㅌ ㄸ */
        DIGEUT,
        /** ㅂ ㅍ ㅃ */
        BIEUP,
        /** ㅅ ㅎ ㅆ */
        SIOT,
        /** ㅈ ㅊ ㅉ */
        JIEUT,
        /** ㅇ ㅁ */
        IEUNG,
        /** ㅣ, the "person" element. */
        I,
        /** ㆍ, the "heaven" element. */
        DOT,
        /** ㅡ, the "earth" element. */
        EU
    }

    // Indices into the standard 19 initials, in the order the shared jamo tables use.
    private static final Map<Key, int[]> CONSONANTS = new HashMap<>();

    static {
        CONSONANTS.put(Key.GIYEOK, new int[] {0, 15, 1});    // ㄱ ㅋ ㄲ
        CONSONANTS.put(Key.NIEUN, new int[] {2, 5});         // ㄴ ㄹ
        CONSONANTS.put(Key.DIGEUT, new int[] {3, 16, 4});    // ㄷ ㅌ ㄸ
        CONSONANTS.put(Key.BIEUP, new int[] {7, 17, 8});     // ㅂ ㅍ ㅃ
        CONSONANTS.put(Key.SIOT, new int[] {9, 18, 10});     // ㅅ ㅎ ㅆ
        CONSONANTS.put(Key.JIEUT, new int[] {12, 14, 13});   // ㅈ ㅊ ㅉ
        CONSONANTS.put(Key.IEUNG, new int[] {11, 6});        // ㅇ ㅁ
    }

    /**
     * Element sequences to the vowel they spell, using {@code i} for ㅣ, {@code d} for ㆍ and
     * {@code u} for ㅡ. Values are indices into the standard 21 medials. A sequence with no entry
     * is not a vowel yet (a lone ㆍ) or not a vowel at all, and ends the run.
     */
    private static final Map<String, Integer> VOWELS = new HashMap<>();

    static {
        VOWELS.put("i", 20);        // ㅣ
        VOWELS.put("u", 18);        // ㅡ
        VOWELS.put("id", 0);        // ㅏ
        VOWELS.put("idd", 2);       // ㅑ
        VOWELS.put("di", 4);        // ㅓ
        VOWELS.put("ddi", 6);       // ㅕ
        VOWELS.put("du", 8);        // ㅗ
        VOWELS.put("ddu", 12);      // ㅛ
        VOWELS.put("ud", 13);       // ㅜ
        VOWELS.put("udd", 17);      // ㅠ
        VOWELS.put("ui", 19);       // ㅢ
        VOWELS.put("idi", 1);       // ㅐ
        VOWELS.put("iddi", 3);      // ㅒ
        VOWELS.put("dii", 5);       // ㅔ
        VOWELS.put("ddii", 7);      // ㅖ
        VOWELS.put("dui", 11);      // ㅚ
        VOWELS.put("udi", 16);      // ㅟ
        VOWELS.put("duid", 9);      // ㅘ
        VOWELS.put("duidi", 10);    // ㅙ
        VOWELS.put("uddi", 14);     // ㅝ
        VOWELS.put("uddii", 15);    // ㅞ
    }

    private Key consonantKey;
    private int consonantTap;
    private String vowelRun = "";
    private boolean vowelOnScreen;

    /** Forgets the run in progress; the next press starts a fresh jamo. */
    public void reset() {
        consonantKey = null;
        consonantTap = 0;
        vowelRun = "";
        vowelOnScreen = false;
    }

    /**
     * The edits one key press produces, in order. Replacing the jamo already on screen is a
     * backspace followed by its successor, which is exactly what the tap that cycles ㄱ into ㅋ
     * means.
     */
    public List<SemanticInput> press(Key key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        return isVowelKey(key) ? pressVowel(key) : pressConsonant(key);
    }

    private static boolean isVowelKey(Key key) {
        return key == Key.I || key == Key.DOT || key == Key.EU;
    }

    private List<SemanticInput> pressConsonant(Key key) {
        int[] group = CONSONANTS.get(key);
        boolean cycling = key == consonantKey;
        // A consonant ends any vowel run; the next vowel element starts from nothing.
        vowelRun = "";
        vowelOnScreen = false;
        if (cycling) {
            consonantTap = (consonantTap + 1) % group.length;
            consonantKey = key;
            return replaceWith(SemanticInput.jamo(
                SemanticJamo.contextualConsonant(group[consonantTap])));
        }
        consonantKey = key;
        consonantTap = 0;
        return addOf(SemanticInput.jamo(SemanticJamo.contextualConsonant(group[0])));
    }

    private List<SemanticInput> pressVowel(Key key) {
        char element = element(key);
        String extended = vowelRun + element;
        Integer vowel = VOWELS.get(extended);
        if (vowel != null) {
            // The run grew into another vowel: swap the one on screen for it.
            List<SemanticInput> edits =
                vowelOnScreen
                    ? replaceVowel(VOWELS.get(vowelRun), vowel)
                    : addOf(SemanticInput.jamo(SemanticJamo.vowel(vowel)));
            vowelRun = extended;
            vowelOnScreen = true;
            consonantKey = null;
            consonantTap = 0;
            return edits;
        }
        if (extended.equals("d") || (!vowelOnScreen && VOWELS.get(String.valueOf(element)) == null)) {
            // A lone ㆍ is not a vowel yet; it waits for the element that completes it.
            vowelRun = extended;
            consonantKey = null;
            consonantTap = 0;
            return Collections.emptyList();
        }
        // The run cannot grow any further, so this press starts a new vowel of its own.
        vowelRun = String.valueOf(element);
        Integer fresh = VOWELS.get(vowelRun);
        consonantKey = null;
        consonantTap = 0;
        if (fresh == null) {
            vowelOnScreen = false;
            return Collections.emptyList();
        }
        vowelOnScreen = true;
        return addOf(SemanticInput.jamo(SemanticJamo.vowel(fresh)));
    }

    private static char element(Key key) {
        switch (key) {
            case I:
                return 'i';
            case DOT:
                return 'd';
            default:
                return 'u';
        }
    }

    private static List<SemanticInput> addOf(SemanticInput input) {
        return Collections.singletonList(input);
    }

    private static List<SemanticInput> replaceWith(SemanticInput input) {
        return new ArrayList<>(Arrays.asList(SemanticInput.deleteForCorrection(), input));
    }

    /**
     * Swaps the vowel on screen for another. A compound one takes two deletes, because the composer
     * decomposes it to the simple vowel it grew from before removing anything.
     */
    private static List<SemanticInput> replaceVowel(Integer current, int replacement) {
        List<SemanticInput> edits = new ArrayList<>(3);
        edits.add(SemanticInput.deleteForCorrection());
        if (current != null && HangulTables.isCompoundMedial(current)) {
            edits.add(SemanticInput.deleteForCorrection());
        }
        edits.add(SemanticInput.jamo(SemanticJamo.vowel(replacement)));
        return edits;
    }
}
