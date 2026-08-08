package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/**
 * The consonant-plus-Hanja special characters. The rows are the ones a Korean IME has always
 * offered, so these tests pin the identities people actually remember: ㅁ the symbols, ㅅ the
 * Greek, ㅇ the circled numbers, ㄹ the units.
 */
public final class SpecialCharTableTest {

    @Test
    public void everyConsonantWithARowAnswersWithIt() {
        for (char consonant : "ㄱㄴㄷㄹㅁㅂㅅㅇㅈㅊㅋㅌㅍㅎ".toCharArray()) {
            assertTrue("row for " + consonant, SpecialCharTable.hasCandidates(consonant));
            assertFalse("row for " + consonant, SpecialCharTable.candidatesFor(consonant).isEmpty());
        }
    }

    @Test
    public void theRowsAreTheOnesPeopleRemember() {
        assertTrue(SpecialCharTable.candidatesFor('ㅁ').contains("※"));
        assertTrue(SpecialCharTable.candidatesFor('ㅁ').contains("★"));
        assertTrue(SpecialCharTable.candidatesFor('ㅅ').contains("Α"));
        assertTrue(SpecialCharTable.candidatesFor('ㅅ').contains("π"));
        assertTrue(SpecialCharTable.candidatesFor('ㅇ').contains("①"));
        assertTrue(SpecialCharTable.candidatesFor('ㅈ').contains("Ⅰ"));
        assertTrue(SpecialCharTable.candidatesFor('ㄹ').contains("㎞"));
        assertTrue(SpecialCharTable.candidatesFor('ㄴ').contains("《"));
        assertTrue(SpecialCharTable.candidatesFor('ㄷ').contains("±"));
        assertTrue(SpecialCharTable.candidatesFor('ㅎ').contains("Я".substring(0, 0) + "А"));
    }

    /** A tense consonant borrows its plain partner's row, as the old IMEs do. */
    @Test
    public void tenseConsonantsShareTheirPartnersRow() {
        assertEquals(SpecialCharTable.candidatesFor('ㄱ'), SpecialCharTable.candidatesFor('ㄲ'));
        assertEquals(SpecialCharTable.candidatesFor('ㄷ'), SpecialCharTable.candidatesFor('ㄸ'));
        assertEquals(SpecialCharTable.candidatesFor('ㅂ'), SpecialCharTable.candidatesFor('ㅃ'));
        assertEquals(SpecialCharTable.candidatesFor('ㅅ'), SpecialCharTable.candidatesFor('ㅆ'));
        assertEquals(SpecialCharTable.candidatesFor('ㅈ'), SpecialCharTable.candidatesFor('ㅉ'));
    }

    @Test
    public void anythingThatIsNotAConsonantHasNoRow() {
        for (char other : "ㅏㅑㅓㅕ가A1 !".toCharArray()) {
            assertFalse("no row for " + other, SpecialCharTable.hasCandidates(other));
            assertTrue(SpecialCharTable.candidatesFor(other).isEmpty());
        }
    }

    @Test
    public void everyCandidateIsOneCharacterAndHasACodePointLabel() {
        for (char consonant : "ㄱㄴㄷㄹㅁㅂㅅㅇㅈㅊㅋㅌㅍㅎ".toCharArray()) {
            List<String> row = SpecialCharTable.candidatesFor(consonant);
            for (String candidate : row) {
                assertFalse(candidate.isEmpty());
                assertEquals("one code point per candidate: " + candidate,
                    1, candidate.codePointCount(0, candidate.length()));
                assertTrue(UnicodeEntry.labelOf(candidate).startsWith("U+"));
            }
        }
    }
}
