package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import dev.hellgates.retekeyime.NaratgeulInterpreter.Key;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** The 나랏글 automaton: a consonant block plus the stroke and doubling keys. */
public final class NaratgeulInterpreterTest {
    private final NaratgeulInterpreter interpreter = new NaratgeulInterpreter();

    @Test
    public void theBlockKeysTypeTheirOwnConsonant() {
        assertEquals(consonant(0), interpreter.press(Key.GIYEOK));
        assertEquals(consonant(2), interpreter.press(Key.NIEUN));
        assertEquals(consonant(5), interpreter.press(Key.RIEUL));
        assertEquals(consonant(6), interpreter.press(Key.MIEUM));
        assertEquals(consonant(9), interpreter.press(Key.SIOT));
        assertEquals(consonant(11), interpreter.press(Key.IEUNG));
    }

    @Test
    public void strokeWalksItsChain() {
        interpreter.press(Key.NIEUN);
        assertEquals(replaced(3), interpreter.press(Key.STROKE));    // ㄴ → ㄷ
        assertEquals(replaced(16), interpreter.press(Key.STROKE));   // ㄷ → ㅌ
        assertEquals(replaced(2), interpreter.press(Key.STROKE));    // ㅌ → ㄴ
    }

    @Test
    public void sibilantAndGlottalChainsFollowTheirOwnStrokes() {
        interpreter.press(Key.SIOT);
        assertEquals(replaced(12), interpreter.press(Key.STROKE));   // ㅅ → ㅈ
        assertEquals(replaced(14), interpreter.press(Key.STROKE));   // ㅈ → ㅊ
        assertEquals(replaced(9), interpreter.press(Key.STROKE));    // ㅊ → ㅅ

        interpreter.press(Key.IEUNG);
        assertEquals(replaced(18), interpreter.press(Key.STROKE));   // ㅇ → ㅎ
        assertEquals(replaced(11), interpreter.press(Key.STROKE));   // ㅎ → ㅇ
    }

    @Test
    public void theLiquidHasNoStrokeOfItsOwn() {
        interpreter.press(Key.RIEUL);

        // ㄹ is not on any stroke chain, so the key answers nothing rather than inventing a letter.
        assertTrue(interpreter.press(Key.STROKE).isEmpty());
    }

    @Test
    public void theLabialChainWalksFromTheNasal() {
        interpreter.press(Key.MIEUM);
        assertEquals(replaced(7), interpreter.press(Key.STROKE));    // ㅁ → ㅂ
        assertEquals(replaced(17), interpreter.press(Key.STROKE));   // ㅂ → ㅍ
        assertEquals(replaced(6), interpreter.press(Key.STROKE));    // ㅍ → ㅁ
    }

    @Test
    public void doublingTurnsAConsonantTenseAndBack() {
        interpreter.press(Key.SIOT);
        assertEquals(replaced(10), interpreter.press(Key.TWIN));     // ㅅ → ㅆ
        assertEquals(replaced(9), interpreter.press(Key.TWIN));      // ㅆ → ㅅ
    }

    @Test
    public void strokeAndDoublingCompose() {
        interpreter.press(Key.NIEUN);
        interpreter.press(Key.STROKE);                               // ㄷ
        assertEquals(replaced(4), interpreter.press(Key.TWIN));      // ㄸ
    }

    @Test
    public void aTransformWithNothingToActOnTypesNothing() {
        assertTrue(interpreter.press(Key.STROKE).isEmpty());
        assertTrue(interpreter.press(Key.TWIN).isEmpty());
    }

    @Test
    public void aConsonantThatCannotDoubleIsLeftAlone() {
        interpreter.press(Key.IEUNG);                                // ㅇ
        assertTrue(interpreter.press(Key.TWIN).isEmpty());
    }

    @Test
    public void aStrokeCanBeDoubledAfterwards() {
        interpreter.press(Key.SIOT);
        interpreter.press(Key.STROKE);                               // ㅈ
        assertEquals(replaced(13), interpreter.press(Key.TWIN));     // ㅉ
    }

    @Test
    public void aVowelEndsTheConsonantSoAStrokeActsOnTheVowelInstead() {
        interpreter.press(Key.GIYEOK);
        interpreter.press(Key.A);

        // Not ㄱ → ㅋ: the stroke reaches the vowel just typed, and iotates it.
        assertEquals(replacedVowel(2), interpreter.press(Key.STROKE));   // ㅏ → ㅑ
    }

    @Test
    public void aStrokeIotatesAVowel() {
        assertEquals(vowel(8), interpreter.press(Key.O));                // ㅗ
        assertEquals(replacedVowel(12), interpreter.press(Key.STROKE));  // ㅛ
        assertEquals(replacedVowel(8), interpreter.press(Key.STROKE));   // back to ㅗ
    }

    @Test
    public void theRoundKeyPressedTwiceReachesTheOtherRoundVowel() {
        assertEquals(vowel(8), interpreter.press(Key.O));                // ㅗ
        assertEquals(replacedVowel(13), interpreter.press(Key.O));       // ㅜ
        assertEquals(replacedVowel(17), interpreter.press(Key.O));       // ㅠ
    }

    @Test
    public void aVowelWithNoStrokeOfItsOwnIsLeftAlone() {
        interpreter.press(Key.EU);                                       // ㅡ
        assertTrue(interpreter.press(Key.STROKE).isEmpty());

        interpreter.reset();
        interpreter.press(Key.I);                                        // ㅣ
        assertTrue(interpreter.press(Key.STROKE).isEmpty());
    }

    @Test
    public void vowelsCombineTheWayAPenWould() {
        assertEquals(vowel(0), interpreter.press(Key.A));            // ㅏ
        assertEquals(replacedVowel(1), interpreter.press(Key.I));    // ㅏㅣ = ㅐ
    }

    @Test
    public void theFlatKeyPressedTwiceReachesTheOtherFlatVowel() {
        assertEquals(vowel(0), interpreter.press(Key.A));            // ㅏ
        assertEquals(replacedVowel(4), interpreter.press(Key.A));    // ㅓ
        assertEquals(replacedVowel(6), interpreter.press(Key.A));    // ㅕ
        assertEquals(replacedVowel(7), interpreter.press(Key.I));    // ㅖ
    }

    @Test
    public void theRoundVowelsBuildTheCompounds() {
        assertEquals(vowel(8), interpreter.press(Key.O));            // ㅗ
        assertEquals(replacedVowel(9), interpreter.press(Key.A));    // ㅘ
        // ㅘ is compound, so replacing it deletes twice before the new vowel.
        assertEquals(replacedCompoundVowel(10), interpreter.press(Key.I));   // ㅙ
    }

    @Test
    public void theDownStrokePairAlsoMakesTheUVowels() {
        assertEquals(vowel(18), interpreter.press(Key.EU));          // ㅡ
        assertEquals(replacedVowel(13), interpreter.press(Key.O));   // ㅜ
        assertEquals(replacedVowel(17), interpreter.press(Key.O));   // ㅠ
    }

    @Test
    public void aPairThatSpellsNothingStartsAFreshVowel() {
        interpreter.press(Key.I);                                    // ㅣ
        assertEquals(vowel(8), interpreter.press(Key.O));            // ㅣㅗ is nothing, so ㅗ
    }

    @Test
    public void resetClearsWhatTheTransformKeysActOn() {
        interpreter.press(Key.GIYEOK);
        interpreter.reset();

        assertTrue(interpreter.press(Key.STROKE).isEmpty());
    }

    private static List<SemanticInput> consonant(int index) {
        return Collections.singletonList(
            SemanticInput.jamo(SemanticJamo.contextualConsonant(index)));
    }

    private static List<SemanticInput> vowel(int index) {
        return Collections.singletonList(SemanticInput.jamo(SemanticJamo.vowel(index)));
    }

    private static List<SemanticInput> replaced(int index) {
        return Arrays.asList(
            SemanticInput.deleteBackward(),
            SemanticInput.jamo(SemanticJamo.contextualConsonant(index)));
    }

    /** Replacing a compound vowel takes two deletes: the composer decomposes it before removing. */
    private static List<SemanticInput> replacedCompoundVowel(int index) {
        return Arrays.asList(
            SemanticInput.deleteBackward(),
            SemanticInput.deleteBackward(),
            SemanticInput.jamo(SemanticJamo.vowel(index)));
    }

    private static List<SemanticInput> replacedVowel(int index) {
        return Arrays.asList(
            SemanticInput.deleteBackward(),
            SemanticInput.jamo(SemanticJamo.vowel(index)));
    }
}
