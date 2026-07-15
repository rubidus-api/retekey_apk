package dev.hellgates.retekeyime;

/**
 * Hangul jamo tables and combination rules, ported from the Jamotong project's {@code layout.c}
 * and {@code fsm.c} (revision 90d6eb5, MIT; see THIRD_PARTY_NOTICES.md). Indices follow the
 * standard KS compatibility-jamo order: 19 choseong, 21 jungseong, 28 jongseong (index 0 = none).
 * This class is platform-neutral and holds no state.
 */
public final class HangulTables {
    public static final int HANGUL_BASE = 0xAC00;
    public static final int CHO_COUNT = 19;
    public static final int JUNG_COUNT = 21;
    public static final int JONG_COUNT = 28;

    private static final String[] CHO_JAMO = {
        "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ",
        "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    };

    // Jung compatibility jamo are contiguous from U+314F.
    private static final int JUNG_BASE = 0x314F;

    // Jong index 0 is the empty final; 1..27 are the compatibility jamo.
    private static final String[] JONG_JAMO = {
        "", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ",
        "ㄻ", "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ",
        "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    };

    // choseong index -> jongseong index, or -1 when the consonant cannot be a batchim (ㄸ ㅃ ㅉ).
    private static final int[] CHO_TO_JONG = {
        1, 2, 4, 7, -1, 8, 16, 17, -1, 19, 20, 21, 22, -1, 23, 24, 25, 26, 27
    };

    // jongseong index -> the choseong it becomes when it moves to the next syllable, or -1.
    private static final int[] JONG_TO_CHO = {
        -1, 0, 1, -1, 2, -1, -1, 3, 5, -1, -1, -1, -1, -1, -1, -1,
        6, 7, -1, 9, 10, 11, 12, 14, 15, 16, 17, 18
    };

    private HangulTables() {
    }

    public static String choJamo(int cho) {
        return CHO_JAMO[cho];
    }

    public static String jungJamo(int jung) {
        return new String(Character.toChars(JUNG_BASE + jung));
    }

    public static String jongJamo(int jong) {
        return JONG_JAMO[jong];
    }

    /** The composed syllable, or 0 when cho or jung is unset. */
    public static char compose(int cho, int jung, int jong) {
        if (cho < 0 || jung < 0) {
            return 0;
        }
        return (char) (HANGUL_BASE
            + (cho * JUNG_COUNT + jung) * JONG_COUNT
            + (jong < 0 ? 0 : jong));
    }

    /** The jongseong for a choseong, or -1 when it cannot be a batchim. */
    public static int choToJong(int cho) {
        return CHO_TO_JONG[cho];
    }

    /** A compound vowel from two jungseong, or -1 when they do not combine. */
    public static int combineJung(int first, int second) {
        if (first == 8 && second == 0) return 9;    // ㅗ + ㅏ = ㅘ
        if (first == 8 && second == 1) return 10;   // ㅗ + ㅐ = ㅙ
        if (first == 8 && second == 20) return 11;  // ㅗ + ㅣ = ㅚ
        if (first == 13 && second == 4) return 14;  // ㅜ + ㅓ = ㅝ
        if (first == 13 && second == 5) return 15;  // ㅜ + ㅔ = ㅞ
        if (first == 13 && second == 20) return 16; // ㅜ + ㅣ = ㅟ
        if (first == 18 && second == 20) return 19; // ㅡ + ㅣ = ㅢ
        return -1;
    }

    /** A compound final from a jongseong index and a following choseong index, or -1. */
    public static int combineJong(int jong, int cho) {
        if (jong == 1 && cho == 9) return 3;    // ㄱ + ㅅ = ㄳ
        if (jong == 4 && cho == 12) return 5;   // ㄴ + ㅈ = ㄵ
        if (jong == 4 && cho == 18) return 6;   // ㄴ + ㅎ = ㄶ
        if (jong == 8 && cho == 0) return 9;    // ㄹ + ㄱ = ㄺ
        if (jong == 8 && cho == 6) return 10;   // ㄹ + ㅁ = ㄻ
        if (jong == 8 && cho == 7) return 11;   // ㄹ + ㅂ = ㄼ
        if (jong == 8 && cho == 9) return 12;   // ㄹ + ㅅ = ㄽ
        if (jong == 8 && cho == 16) return 13;  // ㄹ + ㅌ = ㄾ
        if (jong == 8 && cho == 17) return 14;  // ㄹ + ㅍ = ㄿ
        if (jong == 8 && cho == 18) return 15;  // ㄹ + ㅎ = ㅀ
        if (jong == 17 && cho == 9) return 18;  // ㅂ + ㅅ = ㅄ
        return -1;
    }

    /**
     * Splits a final when a vowel follows, for the batchim-moves-to-the-next-syllable rule. Returns
     * a two-element array {jongThatStays, choThatMoves}. A compound final keeps its first part and
     * moves its second; a single final leaves no batchim (0) and moves its whole consonant.
     */
    public static int[] splitJong(int jong) {
        switch (jong) {
            case 3: return new int[]{1, 9};    // ㄳ -> ㄱ + ㅅ
            case 5: return new int[]{4, 12};   // ㄵ -> ㄴ + ㅈ
            case 6: return new int[]{4, 18};   // ㄶ -> ㄴ + ㅎ
            case 9: return new int[]{8, 0};    // ㄺ -> ㄹ + ㄱ
            case 10: return new int[]{8, 6};   // ㄻ -> ㄹ + ㅁ
            case 11: return new int[]{8, 7};   // ㄼ -> ㄹ + ㅂ
            case 12: return new int[]{8, 9};   // ㄽ -> ㄹ + ㅅ
            case 13: return new int[]{8, 16};  // ㄾ -> ㄹ + ㅌ
            case 14: return new int[]{8, 17};  // ㄿ -> ㄹ + ㅍ
            case 15: return new int[]{8, 18};  // ㅀ -> ㄹ + ㅎ
            case 18: return new int[]{17, 9};  // ㅄ -> ㅂ + ㅅ
            default: return new int[]{0, JONG_TO_CHO[jong]};
        }
    }

    /** The two jungseong a compound vowel decomposes into, or null when it is not compound. */
    public static int[] splitJung(int jung) {
        switch (jung) {
            case 9: return new int[]{8, 0};    // ㅘ -> ㅗ + ㅏ
            case 10: return new int[]{8, 1};   // ㅙ -> ㅗ + ㅐ
            case 11: return new int[]{8, 20};  // ㅚ -> ㅗ + ㅣ
            case 14: return new int[]{13, 4};  // ㅝ -> ㅜ + ㅓ
            case 15: return new int[]{13, 5};  // ㅞ -> ㅜ + ㅔ
            case 16: return new int[]{13, 20}; // ㅟ -> ㅜ + ㅣ
            case 19: return new int[]{18, 20}; // ㅢ -> ㅡ + ㅣ
            default: return null;
        }
    }
}
