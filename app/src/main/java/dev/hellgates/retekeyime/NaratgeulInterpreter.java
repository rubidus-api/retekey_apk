package dev.hellgates.retekeyime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The 나랏글 12-key mode: a six-key consonant block, three vowel keys, and two transformation keys
 * that act on what was just typed rather than typing anything themselves.
 *
 * <p>획추가 adds a stroke — ㄱ becomes ㅋ, ㄴ becomes ㄷ then ㅌ, ㅅ becomes ㅎ — and 쌍자음 doubles
 * a consonant that can be doubled. Vowels are spelled the same way a pen would: ㅏ and ㅣ and ㅗ and
 * ㅡ combine into the compound vowels, so ㅏ then ㅣ is ㅐ and ㅗ then ㅏ is ㅘ.
 *
 * <p>Like {@link CheonjiinInterpreter} this answers each press with the edits the composer should
 * see — a new jamo, or a backspace and the jamo that replaces it — and is Android-free so the
 * tables are unit-tested directly.
 */
public final class NaratgeulInterpreter {
    /** The keys that carry Hangul or transform it. */
    public enum Key {
        GIYEOK,
        NIEUN,
        RIEUL,
        MIEUM,
        SIOT,
        IEUNG,
        A,
        O,
        I,
        EU,
        /** 획추가: adds a stroke to the consonant just typed. */
        STROKE,
        /** 쌍자음: doubles the consonant just typed. */
        TWIN
    }

    /** Stroke chains over the standard 19 initial indices; each key steps to the next. */
    private static final Map<Integer, Integer> STROKE = new HashMap<>();
    /** Doubling, over the same indices. */
    private static final Map<Integer, Integer> TWIN = new HashMap<>();

    static {
        STROKE.put(0, 15);      // ㄱ → ㅋ
        STROKE.put(15, 0);      // ㅋ → ㄱ
        STROKE.put(2, 3);       // ㄴ → ㄷ
        STROKE.put(3, 16);      // ㄷ → ㅌ
        STROKE.put(16, 2);      // ㅌ → ㄴ
        STROKE.put(5, 2);       // ㄹ → ㄴ, so the block's two nasal chains meet
        STROKE.put(6, 7);       // ㅁ → ㅂ
        STROKE.put(7, 17);      // ㅂ → ㅍ
        STROKE.put(17, 6);      // ㅍ → ㅁ
        STROKE.put(9, 18);      // ㅅ → ㅎ
        STROKE.put(18, 9);      // ㅎ → ㅅ
        STROKE.put(11, 12);     // ㅇ → ㅈ
        STROKE.put(12, 14);     // ㅈ → ㅊ
        STROKE.put(14, 11);     // ㅊ → ㅇ

        TWIN.put(0, 1);         // ㄱ ㄲ
        TWIN.put(1, 0);
        TWIN.put(3, 4);         // ㄷ ㄸ
        TWIN.put(4, 3);
        TWIN.put(7, 8);         // ㅂ ㅃ
        TWIN.put(8, 7);
        TWIN.put(9, 10);        // ㅅ ㅆ
        TWIN.put(10, 9);
        TWIN.put(12, 13);       // ㅈ ㅉ
        TWIN.put(13, 12);
    }

    /** Which consonant each block key starts from. */
    private static final Map<Key, Integer> CONSONANTS = new HashMap<>();

    static {
        CONSONANTS.put(Key.GIYEOK, 0);   // ㄱ
        CONSONANTS.put(Key.NIEUN, 2);    // ㄴ
        CONSONANTS.put(Key.RIEUL, 5);    // ㄹ
        CONSONANTS.put(Key.MIEUM, 6);    // ㅁ
        CONSONANTS.put(Key.SIOT, 9);     // ㅅ
        CONSONANTS.put(Key.IEUNG, 11);   // ㅇ
    }

    /** Which vowel each vowel key starts from, over the standard 21 medial indices. */
    private static final Map<Key, Integer> VOWELS = new HashMap<>();

    static {
        VOWELS.put(Key.A, 0);            // ㅏ
        VOWELS.put(Key.O, 8);            // ㅗ
        VOWELS.put(Key.I, 20);           // ㅣ
        VOWELS.put(Key.EU, 18);          // ㅡ
    }

    /** Vowel already on screen, plus the key pressed, to the vowel they spell together. */
    private static final Map<String, Integer> COMBINE = new HashMap<>();

    static {
        combine(0, Key.I, 1);            // ㅏㅣ = ㅐ
        combine(0, Key.A, 2);            // ㅏㅏ = ㅑ
        combine(2, Key.I, 3);            // ㅑㅣ = ㅒ
        combine(4, Key.I, 5);            // ㅓㅣ = ㅔ
        combine(6, Key.I, 7);            // ㅕㅣ = ㅖ
        combine(8, Key.A, 9);            // ㅗㅏ = ㅘ
        combine(9, Key.I, 10);           // ㅘㅣ = ㅙ
        combine(8, Key.I, 11);           // ㅗㅣ = ㅚ
        combine(8, Key.O, 12);           // ㅗㅗ = ㅛ
        combine(13, Key.I, 16);          // ㅜㅣ = ㅟ
        combine(14, Key.I, 15);          // ㅝㅣ = ㅞ
        combine(18, Key.I, 19);          // ㅡㅣ = ㅢ
        combine(18, Key.O, 13);          // ㅡㅗ = ㅜ, the down-stroke pair
        combine(13, Key.O, 17);          // ㅜㅗ = ㅠ
        combine(20, Key.A, 4);           // ㅣㅏ = ㅓ
        combine(4, Key.A, 6);            // ㅓㅏ = ㅕ
        combine(13, Key.A, 14);          // ㅜㅏ = ㅝ
    }

    private static void combine(int current, Key key, int result) {
        COMBINE.put(current + ":" + key.name(), result);
    }

    private int lastConsonant = -1;
    private int lastVowel = -1;

    /** Forgets what was last typed; the transformation keys then have nothing to act on. */
    public void reset() {
        lastConsonant = -1;
        lastVowel = -1;
    }

    /** The edits one key press produces, in order. */
    public List<SemanticInput> press(Key key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (key == Key.STROKE) {
            return transform(STROKE);
        }
        if (key == Key.TWIN) {
            return transform(TWIN);
        }
        Integer consonant = CONSONANTS.get(key);
        if (consonant != null) {
            lastConsonant = consonant;
            lastVowel = -1;
            return addOf(SemanticInput.jamo(SemanticJamo.contextualConsonant(consonant)));
        }
        return pressVowel(key);
    }

    private List<SemanticInput> pressVowel(Key key) {
        Integer combined = lastVowel < 0 ? null : COMBINE.get(lastVowel + ":" + key.name());
        if (combined != null) {
            lastVowel = combined;
            lastConsonant = -1;
            return replaceWith(SemanticInput.jamo(SemanticJamo.vowel(combined)));
        }
        int vowel = VOWELS.get(key);
        lastVowel = vowel;
        lastConsonant = -1;
        return addOf(SemanticInput.jamo(SemanticJamo.vowel(vowel)));
    }

    /**
     * Applies a transformation to the consonant just typed. With nothing to act on, or a consonant
     * the table has no entry for, the press does nothing rather than typing a stray letter.
     */
    private List<SemanticInput> transform(Map<Integer, Integer> table) {
        if (lastConsonant < 0) {
            return Collections.emptyList();
        }
        Integer next = table.get(lastConsonant);
        if (next == null) {
            return Collections.emptyList();
        }
        lastConsonant = next;
        return replaceWith(SemanticInput.jamo(SemanticJamo.contextualConsonant(next)));
    }

    private static List<SemanticInput> addOf(SemanticInput input) {
        return Collections.singletonList(input);
    }

    private static List<SemanticInput> replaceWith(SemanticInput input) {
        return new ArrayList<>(Arrays.asList(SemanticInput.deleteBackward(), input));
    }
}
