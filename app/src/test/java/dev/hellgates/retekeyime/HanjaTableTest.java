package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;

public final class HanjaTableTest {
    private static HanjaTable sample() {
        return HanjaTable.parse(Arrays.asList(
            "# a comment line",
            "",
            "가:佳,假,價,家",
            "가,고:賈",          // one candidate set indexed under two readings
            "학:學,鶴",
            "학교:學校",          // a word entry
            "교:校,敎",
            "bad line without colon",
            "빈: ",               // empty candidate list -> skipped
            "  :亂"               // empty key -> skipped
        ));
    }

    @Test
    public void indexesSingleSyllablesAndMergesMultiReadings() {
        HanjaTable t = sample();
        assertEquals(Arrays.asList("佳", "假", "價", "家", "賈"), t.candidates("가"));
        assertEquals(Arrays.asList("賈"), t.candidates("고"));
    }

    @Test
    public void wordEntriesAreLookedUpWhole() {
        assertEquals(Arrays.asList("學校"), sample().candidates("학교"));
    }

    @Test
    public void unknownAndMalformedReturnEmpty() {
        HanjaTable t = sample();
        assertTrue(t.candidates("없음").isEmpty());
        assertTrue(t.candidates("빈").isEmpty());
        assertTrue(t.candidates(null).isEmpty());
    }

    @Test
    public void longestSuffixPrefersTheWordThenTheSyllable() {
        HanjaTable t = sample();
        // "안녕학교" ends with the word "학교".
        HanjaTable.Match word = t.longestSuffixMatch("안녕학교", 8);
        assertEquals("학교", word.reading);
        assertEquals(2, word.length);
        assertEquals(Arrays.asList("學校"), word.candidates);
        // A trailing lone syllable still converts.
        HanjaTable.Match syllable = t.longestSuffixMatch("동네학", 8);
        assertEquals("학", syllable.reading);
        assertEquals(1, syllable.length);
    }

    @Test
    public void reverseIndexMapsHanjaAndWordsBackToReadings() {
        HanjaTable t = sample();
        // 賈 was listed under both 가 and 고, so both readings come back.
        assertEquals(Arrays.asList("가", "고"), t.readings("賈"));
        assertEquals(Arrays.asList("가"), t.readings("家"));
        // A Hanja word reverses to its word reading.
        assertEquals(Arrays.asList("학교"), t.readings("學校"));
        assertTrue(t.readings("龘").isEmpty());
    }

    @Test
    public void longestSuffixReverseMatchPrefersTheWord() {
        HanjaTable t = sample();
        HanjaTable.Match word = t.longestSuffixReverseMatch("서울學校", 8);
        assertEquals("學校", word.reading);
        assertEquals(2, word.length);
        assertEquals(Arrays.asList("학교"), word.candidates);
        HanjaTable.Match single = t.longestSuffixReverseMatch("字學", 8);
        assertEquals("學", single.reading);
        assertEquals(1, single.length);
    }

    @Test
    public void scriptHelpersClassifyHangulAndHanja() {
        assertTrue(HanjaTable.isHangul('가'));
        assertTrue(HanjaTable.isHangul('ㄱ'));
        assertFalse(HanjaTable.isHangul('家'));
        assertTrue(HanjaTable.isHanja('家'));
        assertTrue(HanjaTable.isHanja('學'));
        assertFalse(HanjaTable.isHanja('가'));
        assertFalse(HanjaTable.isHanja('A'));
    }

    @Test
    public void longestSuffixReturnsNullWhenNothingMatches() {
        assertNull(sample().longestSuffixMatch("xyz", 8));
        assertNull(sample().longestSuffixMatch("", 8));
    }
}
