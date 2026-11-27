package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import com.retekey.core.WordBoundary;

/** The word the bar's Word button selects, read off the text either side of the cursor. */
public final class WordBoundaryTest {
    @Test
    public void aCursorInsideAWordReachesBothWays() {
        WordBoundary word = WordBoundary.of("the quick bro", "wn fox");
        assertEquals(3, word.before);
        assertEquals(2, word.after);
        assertFalse(word.isEmpty());
    }

    @Test
    public void hangulIsALetterLikeAnyOther() {
        WordBoundary word = WordBoundary.of("나는 키보드", "를 만든다");
        assertEquals("키보드".length(), word.before);
        assertEquals("를".length(), word.after);
    }

    @Test
    public void aCursorAtAWordsEdgeTakesTheWholeWord() {
        assertEquals(5, WordBoundary.of("hello", " world").before);
        assertEquals(0, WordBoundary.of("hello", " world").after);
        assertEquals(0, WordBoundary.of("hello ", "world").before);
        assertEquals(5, WordBoundary.of("hello ", "world").after);
    }

    @Test
    public void punctuationIsABoundary() {
        WordBoundary word = WordBoundary.of("(name", ").length");
        assertEquals(4, word.before);
        assertEquals(0, word.after);
    }

    @Test
    public void digitsAndUnderscoresAreWordCharacters() {
        WordBoundary word = WordBoundary.of("max_retry_", "3 = 5");
        assertEquals("max_retry_".length(), word.before);
        assertEquals(1, word.after);
    }

    @Test
    public void aCursorInWhitespaceSelectsNothing() {
        assertTrue(WordBoundary.of("one ", " two").isEmpty());
        assertTrue(WordBoundary.of("", "").isEmpty());
        assertTrue(WordBoundary.of(null, null).isEmpty());
    }

    @Test
    public void aCombiningMarkStaysWithItsLetter() {
        // e + combining acute: the selection must not stop between them.
        WordBoundary word = WordBoundary.of("café", " au lait");
        assertEquals(5, word.before);
    }

    @Test
    public void supplementaryPlaneLettersBelongToWords() {
        // CJK Extension B character (U+20000, 2 UTF-16 code units).
        String extB = new String(Character.toChars(0x20000));
        WordBoundary before = WordBoundary.of("hi " + extB + "ab", "cd ");
        assertEquals(extB.length() + 2, before.before);
        assertEquals(2, before.after);

        WordBoundary split = WordBoundary.of("hi " + extB, extB + "!");
        assertEquals(extB.length(), split.before);
        assertEquals(extB.length(), split.after);
    }
}
