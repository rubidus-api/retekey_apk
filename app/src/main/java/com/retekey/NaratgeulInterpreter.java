package com.retekey;

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
 * <p>획추가 adds a stroke — ㄱ becomes ㅋ, ㄴ becomes ㄷ then ㅌ, ㅅ becomes ㅈ then ㅊ, ㅇ becomes
 * ㅎ, and ㄹ has no stroke of its own. It does the same to a vowel, iotating it: ㅏ becomes ㅑ, ㅗ
 * becomes ㅛ. 쌍자음 doubles
 * a consonant that can be doubled. Vowels are spelled the same way a pen would: ㅏ and ㅣ and ㅗ and
 * ㅡ combine into the compound vowels, so ㅏ then ㅣ is ㅐ and ㅗ then ㅏ is ㅘ.
 *
 * <p>A vowel key pressed twice reaches its pair — ㅏ then ㅓ, ㅗ then ㅜ — and a third press
 * iotates that, so ㅏㅏㅏ is ㅕ and ㅗㅗㅗ is ㅠ. 획추가 iotates whatever vowel is showing, which is
 * the short way to ㅑ and ㅛ.
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

    // The stroke, twin and vowel-stroke chains live in NaratgeulTransforms, shared with the
    // processor-side fallback that acts when this interpreter's memory has been lost.

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
        combine(0, Key.A, 4);            // ㅏㅏ = ㅓ, the flat pair
        combine(2, Key.I, 3);            // ㅑㅣ = ㅒ
        combine(4, Key.I, 5);            // ㅓㅣ = ㅔ
        combine(6, Key.I, 7);            // ㅕㅣ = ㅖ
        combine(8, Key.A, 9);            // ㅗㅏ = ㅘ
        combine(9, Key.I, 10);           // ㅘㅣ = ㅙ
        combine(8, Key.I, 11);           // ㅗㅣ = ㅚ
        combine(8, Key.O, 13);           // ㅗㅗ = ㅜ, the round pair
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
            return lastConsonant < 0 && lastVowel >= 0
                ? strokeVowel()
                : transform(NaratgeulTransforms::strokeOf);
        }
        if (key == Key.TWIN) {
            return transform(NaratgeulTransforms::twinOf);
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
            int previous = lastVowel;
            lastVowel = combined;
            lastConsonant = -1;
            return replaceVowel(previous, combined);
        }
        int vowel = VOWELS.get(key);
        lastVowel = vowel;
        lastConsonant = -1;
        return addOf(SemanticInput.jamo(SemanticJamo.vowel(vowel)));
    }

    /** Adds the iotating stroke to the vowel just typed; nothing when it has none. */
    private List<SemanticInput> strokeVowel() {
        int next = NaratgeulTransforms.vowelStrokeOf(lastVowel);
        if (next < 0) {
            return Collections.emptyList();
        }
        int previous = lastVowel;
        lastVowel = next;
        return replaceVowel(previous, next);
    }

    /**
     * Applies a transformation to the consonant just typed. With nothing to act on, or a consonant
     * the table has no entry for, the press does nothing rather than typing a stray letter.
     */
    private List<SemanticInput> transform(java.util.function.IntUnaryOperator table) {
        if (lastConsonant < 0) {
            return Collections.emptyList();
        }
        int next = table.applyAsInt(lastConsonant);
        if (next < 0) {
            return Collections.emptyList();
        }
        lastConsonant = next;
        return replaceWith(SemanticInput.jamo(SemanticJamo.contextualConsonant(next)));
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
    private static List<SemanticInput> replaceVowel(int current, int replacement) {
        List<SemanticInput> edits = new ArrayList<>(3);
        edits.add(SemanticInput.deleteForCorrection());
        if (HangulTables.isCompoundMedial(current)) {
            edits.add(SemanticInput.deleteForCorrection());
        }
        edits.add(SemanticInput.jamo(SemanticJamo.vowel(replacement)));
        return edits;
    }
}
