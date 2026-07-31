package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import dev.hellgates.retekeyime.CheonjiinInterpreter.Key;
import java.util.List;
import org.junit.Test;

/**
 * The 천지인 automaton, read as the edits it hands the composer. A press either adds a jamo or
 * replaces the one on screen — backspace, then its successor.
 */
public final class CheonjiinInterpreterTest {
    private final CheonjiinInterpreter interpreter = new CheonjiinInterpreter();

    @Test
    public void aConsonantKeyCyclesThroughItsGroup() {
        assertEquals(consonant(0), interpreter.press(Key.GIYEOK));          // ㄱ
        assertEquals(replaced(15), interpreter.press(Key.GIYEOK));          // ㅋ
        assertEquals(replaced(1), interpreter.press(Key.GIYEOK));           // ㄲ
        assertEquals(replaced(0), interpreter.press(Key.GIYEOK));           // back to ㄱ
    }

    @Test
    public void aDifferentConsonantStartsAfresh() {
        interpreter.press(Key.GIYEOK);
        interpreter.press(Key.GIYEOK);

        assertEquals(consonant(2), interpreter.press(Key.NIEUN));           // ㄴ, added not replaced
        assertEquals(replaced(5), interpreter.press(Key.NIEUN));            // ㄹ
    }

    @Test
    public void aTwoLetterGroupHasOnlyTwoStops() {
        assertEquals(consonant(11), interpreter.press(Key.IEUNG));          // ㅇ
        assertEquals(replaced(6), interpreter.press(Key.IEUNG));            // ㅁ
        assertEquals(replaced(11), interpreter.press(Key.IEUNG));           // ㅇ again
    }

    @Test
    public void theLoneHeavenElementWaitsForWhatCompletesIt() {
        assertTrue("ㆍ alone is not a vowel yet", interpreter.press(Key.DOT).isEmpty());
        assertEquals(vowel(4), interpreter.press(Key.I));                   // ㆍㅣ = ㅓ
    }

    @Test
    public void theSimpleVowelsSpellOut() {
        assertEquals(vowel(20), interpreter.press(Key.I));                  // ㅣ
        assertEquals(replacedVowel(0), interpreter.press(Key.DOT));         // ㅣㆍ = ㅏ
        assertEquals(replacedVowel(2), interpreter.press(Key.DOT));         // ㅣㆍㆍ = ㅑ
    }

    @Test
    public void theEarthElementBuildsItsOwnFamily() {
        assertEquals(vowel(18), interpreter.press(Key.EU));                 // ㅡ
        assertEquals(replacedVowel(13), interpreter.press(Key.DOT));        // ㅡㆍ = ㅜ
        assertEquals(replacedVowel(17), interpreter.press(Key.DOT));        // ㅡㆍㆍ = ㅠ
        assertEquals(replacedVowel(14), interpreter.press(Key.I));          // ㅡㆍㆍㅣ = ㅝ
        assertEquals(replacedVowel(15), interpreter.press(Key.I));          // ㅞ
    }

    @Test
    public void compoundVowelsKeepGrowingFromTheirRun() {
        interpreter.press(Key.DOT);
        assertEquals(vowel(8), interpreter.press(Key.EU));                  // ㆍㅡ = ㅗ
        assertEquals(replacedVowel(11), interpreter.press(Key.I));          // ㆍㅡㅣ = ㅚ
        assertEquals(replacedVowel(9), interpreter.press(Key.DOT));         // ㆍㅡㅣㆍ = ㅘ
        assertEquals(replacedVowel(10), interpreter.press(Key.I));          // ㅙ
    }

    @Test
    public void aRunThatCannotGrowStartsANewVowel() {
        interpreter.press(Key.I);
        interpreter.press(Key.DOT);
        interpreter.press(Key.I);                                           // ㅐ
        // ㅐ has nowhere left to go, so ㅡ is a vowel of its own rather than a correction.
        assertEquals(vowel(18), interpreter.press(Key.EU));
    }

    @Test
    public void aConsonantEndsTheVowelRun() {
        interpreter.press(Key.I);
        interpreter.press(Key.DOT);                                         // ㅏ
        interpreter.press(Key.GIYEOK);                                      // ㄱ

        // The ㆍ after a consonant starts a new run, it does not resume ㅏ's.
        assertTrue(interpreter.press(Key.DOT).isEmpty());
        assertEquals(vowel(4), interpreter.press(Key.I));                   // ㅓ
    }

    @Test
    public void resetForgetsTheRunInProgress() {
        interpreter.press(Key.GIYEOK);
        interpreter.reset();

        assertEquals(consonant(0), interpreter.press(Key.GIYEOK));          // ㄱ again, not ㅋ
    }

    private static List<SemanticInput> consonant(int index) {
        return java.util.Collections.singletonList(
            SemanticInput.jamo(SemanticJamo.contextualConsonant(index)));
    }

    private static List<SemanticInput> vowel(int index) {
        return java.util.Collections.singletonList(
            SemanticInput.jamo(SemanticJamo.vowel(index)));
    }

    private static List<SemanticInput> replaced(int index) {
        return java.util.Arrays.asList(
            SemanticInput.deleteBackward(),
            SemanticInput.jamo(SemanticJamo.contextualConsonant(index)));
    }

    private static List<SemanticInput> replacedVowel(int index) {
        return java.util.Arrays.asList(
            SemanticInput.deleteBackward(),
            SemanticInput.jamo(SemanticJamo.vowel(index)));
    }
}
