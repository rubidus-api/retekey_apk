package com.retekey;

/**
 * The 나랏글 transformation tables: what 획추가 and 쌍자음 turn each letter into. Indices are the
 * standard 19 choseong and 21 jungseong. Shared by {@link NaratgeulInterpreter}, which acts on the
 * letter it remembers typing, and by {@link HangulInputProcessor}, which acts on the letter
 * actually on screen when that memory has been lost. Platform-neutral, stateless.
 */
public final class NaratgeulTransforms {
    // Each block key walks the letters its own strokes build, and returns to its start.
    private static final int[] STROKE = new int[19];
    // Doubling, where a doubled letter exists; a second press takes it back.
    private static final int[] TWIN = new int[19];
    // 획추가 on a vowel iotates it: the same gesture, one more stroke.
    private static final int[] VOWEL_STROKE = new int[21];

    static {
        java.util.Arrays.fill(STROKE, -1);
        java.util.Arrays.fill(TWIN, -1);
        java.util.Arrays.fill(VOWEL_STROKE, -1);

        STROKE[0] = 15;     // ㄱ → ㅋ
        STROKE[15] = 0;     // ㅋ → ㄱ
        STROKE[2] = 3;      // ㄴ → ㄷ
        STROKE[3] = 16;     // ㄷ → ㅌ
        STROKE[16] = 2;     // ㅌ → ㄴ
        STROKE[6] = 7;      // ㅁ → ㅂ
        STROKE[7] = 17;     // ㅂ → ㅍ
        STROKE[17] = 6;     // ㅍ → ㅁ
        STROKE[9] = 12;     // ㅅ → ㅈ
        STROKE[12] = 14;    // ㅈ → ㅊ
        STROKE[14] = 9;     // ㅊ → ㅅ
        STROKE[11] = 18;    // ㅇ → ㅎ
        STROKE[18] = 11;    // ㅎ → ㅇ
        // ㄹ has no stroke of its own; the key answers nothing rather than inventing a letter.

        TWIN[0] = 1;        // ㄱ ㄲ
        TWIN[1] = 0;
        TWIN[3] = 4;        // ㄷ ㄸ
        TWIN[4] = 3;
        TWIN[7] = 8;        // ㅂ ㅃ
        TWIN[8] = 7;
        TWIN[9] = 10;       // ㅅ ㅆ
        TWIN[10] = 9;
        TWIN[12] = 13;      // ㅈ ㅉ
        TWIN[13] = 12;

        VOWEL_STROKE[0] = 2;      // ㅏ → ㅑ
        VOWEL_STROKE[2] = 0;      // ㅑ → ㅏ
        VOWEL_STROKE[4] = 6;      // ㅓ → ㅕ
        VOWEL_STROKE[6] = 4;      // ㅕ → ㅓ
        VOWEL_STROKE[8] = 12;     // ㅗ → ㅛ
        VOWEL_STROKE[12] = 8;     // ㅛ → ㅗ
        VOWEL_STROKE[13] = 17;    // ㅜ → ㅠ
        VOWEL_STROKE[17] = 13;    // ㅠ → ㅜ
    }

    private NaratgeulTransforms() {
    }

    /** The choseong 획추가 turns this one into, or -1 when it has no stroke. */
    public static int strokeOf(int cho) {
        return STROKE[cho];
    }

    /** The choseong 쌍자음 turns this one into, or -1 when it has no double. */
    public static int twinOf(int cho) {
        return TWIN[cho];
    }

    /** The jungseong 획추가 iotates this one into, or -1 when it has no iotated form. */
    public static int vowelStrokeOf(int jung) {
        return VOWEL_STROKE[jung];
    }
}
