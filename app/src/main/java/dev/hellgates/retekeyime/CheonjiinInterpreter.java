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

    /**
     * The element run each vowel is spelled with, so a drag can put the run exactly where the taps
     * would have left it. That is what lets a dragged vowel go on combining: drag ㅗ and the run is
     * "du", so a following ㅣ still spells ㅚ.
     */
    private static final Map<Integer, String> VOWEL_RUNS = new HashMap<>();

    static {
        for (Map.Entry<String, Integer> entry : VOWELS.entrySet()) {
            String existing = VOWEL_RUNS.get(entry.getValue());
            // The shortest spelling wins, so the run left behind is the one taps would have built.
            if (existing == null || entry.getKey().length() < existing.length()) {
                VOWEL_RUNS.put(entry.getValue(), entry.getKey());
            }
        }
    }

    /** A drag off a key, in the direction the finger went. */
    public enum Flick {
        LEFT,
        RIGHT,
        UP,
        DOWN
    }

    /**
     * What a drag off each vowel key spells. The direction points at the letter: ㅓ leans left and
     * ㅏ leans right, ㅗ is up and ㅜ is down, and the compounds follow the vowel they are built
     * from — ㅔ from ㅓ, ㅐ from ㅏ, ㅚ from ㅗ, ㅟ from ㅜ.
     */
    private static final Map<String, Integer> VOWEL_FLICKS = new HashMap<>();

    static {
        flick(Key.DOT, Flick.LEFT, 4);      // ㅓ
        flick(Key.DOT, Flick.RIGHT, 0);     // ㅏ
        flick(Key.DOT, Flick.UP, 8);        // ㅗ
        flick(Key.DOT, Flick.DOWN, 13);     // ㅜ
        flick(Key.I, Flick.LEFT, 5);        // ㅔ
        flick(Key.I, Flick.RIGHT, 1);       // ㅐ
        flick(Key.I, Flick.UP, 3);          // ㅒ
        flick(Key.I, Flick.DOWN, 7);        // ㅖ
        flick(Key.EU, Flick.LEFT, 14);      // ㅝ
        flick(Key.EU, Flick.RIGHT, 9);      // ㅘ
        flick(Key.EU, Flick.UP, 11);        // ㅚ
        flick(Key.EU, Flick.DOWN, 16);      // ㅟ
    }

    private static void flick(Key key, Flick direction, int vowel) {
        VOWEL_FLICKS.put(key.name() + ":" + direction.name(), vowel);
    }

    private Key consonantKey;
    private int consonantTap;
    private String vowelRun = "";
    private boolean vowelOnScreen;

    /**
     * Ends the multi-tap grouping without ending the syllable: the next press of the same key
     * types that key's first letter instead of cycling to its next one. This is what a pause does
     * on a phone — it is how 삶 can be followed by ㅇ, which is the same key as the ㅁ before it.
     *
     * <p>The vowel run is deliberately left alone. Vowel elements are spelled with different keys
     * and belong to the syllable, not to a burst of taps, so a slow ㅣ ㆍ must still be ㅏ.
     */
    public void endMultiTap() {
        consonantKey = null;
        consonantTap = 0;
    }

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

    /**
     * A drag off a key, which types its letter at once — no waiting for a tap count, and no long
     * press. On a consonant it reaches the letters that would otherwise take two or three taps:
     * right for the aspirate (ㄱ → ㅋ), left for the tense one (ㄱ → ㄲ). On a vowel it types the
     * whole vowel the direction points at, and leaves the element run where the taps would have,
     * so the result still combines with whatever is typed next.
     */
    public List<SemanticInput> flick(Key key, Flick direction) {
        if (key == null || direction == null) {
            throw new IllegalArgumentException("key and direction must not be null");
        }
        if (isVowelKey(key)) {
            Integer vowel = VOWEL_FLICKS.get(key.name() + ":" + direction.name());
            return vowel == null ? Collections.emptyList() : flickVowel(vowel);
        }
        int[] group = CONSONANTS.get(key);
        int slot = consonantSlot(direction);
        if (slot < 0 || slot >= group.length) {
            // Nothing is above a consonant, and a group with no tense letter has nothing below
            // it either. Either way this drag types nothing at all.
            return Collections.emptyList();
        }
        int target = group[slot];
        vowelRun = "";
        vowelOnScreen = false;
        consonantKey = key;
        consonantTap = indexOf(group, target);
        return addOf(SemanticInput.jamo(SemanticJamo.contextualConsonant(target)));
    }

    /**
     * The letter a drag off {@code key} would type, for the guide the keyboard shows under a held
     * finger. A consonant has nothing above or below it, so those directions answer with the
     * letter a plain tap gives — which is what dragging that way actually types.
     */
    public static String flickLabel(Key key, Flick direction) {
        if (key == null || direction == null) {
            return null;
        }
        if (isVowelKey(key)) {
            Integer vowel = VOWEL_FLICKS.get(key.name() + ":" + direction.name());
            return vowel == null ? null : HangulTables.jungJamo(vowel);
        }
        int[] group = CONSONANTS.get(key);
        int slot = consonantSlot(direction);
        if (group == null || slot < 0 || slot >= group.length) {
            return null;
        }
        return HangulTables.choJamo(group[slot]);
    }

    /**
     * Which letter of a consonant group each way points at: left is the plain one the key already
     * types, right the aspirate beside it, down the tense one. Nothing is above a consonant — the
     * digit is reached by holding the key, not by dragging — so up answers -1 and types nothing.
     */
    private static int consonantSlot(Flick direction) {
        switch (direction) {
            case LEFT:
                return 0;
            case RIGHT:
                return 1;
            case DOWN:
                return 2;
            default:
                return -1;
        }
    }

    /** Whether {@code key} is one of the three vowel elements. */
    public static boolean isVowelElement(Key key) {
        return isVowelKey(key);
    }

    private static int indexOf(int[] group, int value) {
        for (int i = 0; i < group.length; i++) {
            if (group[i] == value) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Types a dragged vowel. When it can join the vowel already on screen it does — dragging ㅗ and
     * then ㅏ spells ㅘ, exactly as the taps behind them would — and otherwise it starts a vowel of
     * its own.
     */
    private List<SemanticInput> flickVowel(int vowel) {
        String run = VOWEL_RUNS.get(vowel);
        Integer combined = vowelOnScreen ? VOWELS.get(vowelRun + run) : null;
        List<SemanticInput> edits;
        if (combined != null) {
            edits = replaceVowel(VOWELS.get(vowelRun), combined);
            vowelRun = vowelRun + run;
        } else {
            edits = addOf(SemanticInput.jamo(SemanticJamo.vowel(vowel)));
            vowelRun = run;
        }
        vowelOnScreen = true;
        consonantKey = null;
        consonantTap = 0;
        return edits;
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
