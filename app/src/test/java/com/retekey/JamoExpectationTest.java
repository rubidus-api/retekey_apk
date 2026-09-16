package com.retekey;

import static org.junit.Assert.assertEquals;

import com.retekey.JamoExpectation.Kind;
import org.junit.Test;

public final class JamoExpectationTest {
    @Test
    public void aSyllableBeginsWithAConsonant() {
        assertEquals(Kind.CONSONANT, JamoExpectation.of(""));
        assertEquals(Kind.CONSONANT, JamoExpectation.of(null));
    }

    @Test
    public void aLoneConsonantWantsItsVowel() {
        assertEquals(Kind.VOWEL, JamoExpectation.of("ㄱ"));
        assertEquals(Kind.VOWEL, JamoExpectation.of("ㅎ"));
    }

    @Test
    public void aSyllableWithItsVowelCouldTakeEither() {
        assertEquals("ㅗ then ㅏ spells ㅘ; a final consonant is as likely",
            Kind.NONE, JamoExpectation.of("고"));
        assertEquals(Kind.NONE, JamoExpectation.of("한"));
        assertEquals("a lone vowel says nothing either", Kind.NONE, JamoExpectation.of("ㅏ"));
    }
}
