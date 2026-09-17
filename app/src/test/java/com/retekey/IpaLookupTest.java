package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/**
 * Finding a symbol by name (issue #11). The reporter typed "palm" into another keyboard's saved
 * phrases to get ɑ; here that is the search itself, and these are the queries a user actually has.
 */
public final class IpaLookupTest {
    private static String first(String query) {
        List<String> found = IpaLookup.search(query);
        return found.isEmpty() ? "" : found.get(0);
    }

    @Test
    public void aLexicalSetFindsItsVowel() {
        assertEquals("ɑ", first("palm"));
        assertEquals("ɔ", first("thought"));
        assertEquals("ɪ", first("kit"));
        assertEquals("ʊ", first("foot"));
        assertEquals("ʌ", first("strut"));
        assertEquals("æ", first("trap"));
    }

    @Test
    public void anXsampaCodeFindsItsSymbolFirst() {
        // The ASCII notation a phonetician already types: @ is schwa, T is theta, N is eng.
        assertEquals("ə", first("@"));
        assertEquals("θ", first("T"));
        assertEquals("ŋ", first("N"));
        assertEquals("ʃ", first("S"));
        assertEquals("ʒ", first("Z"));
        assertEquals("ð", first("D"));
    }

    @Test
    public void aNameFindsIt() {
        assertEquals("ə", first("schwa"));
        assertEquals("ʔ", first("glottal"));
        assertTrue(IpaLookup.search("nasal").contains("ŋ"));
        assertTrue(IpaLookup.search("fricative").contains("θ"));
        assertTrue(IpaLookup.search("retroflex").contains("ʂ"));
    }

    @Test
    public void aPartialWordStillFinds() {
        assertTrue(IpaLookup.search("fric").contains("ʃ"));
        assertTrue(IpaLookup.search("aspir").contains("ʰ"));
    }

    @Test
    public void nothingIsFoundForNothing() {
        assertTrue(IpaLookup.search("").isEmpty());
        assertTrue(IpaLookup.search(null).isEmpty());
        assertTrue(IpaLookup.search("zzzzz").isEmpty());
    }

    @Test
    public void aRowfulAtMostAndNoRepeats() {
        List<String> many = IpaLookup.search("voiced");
        assertTrue(many.size() <= IpaLookup.LIMIT);
        for (int i = 0; i < many.size(); i++) {
            for (int j = i + 1; j < many.size(); j++) {
                assertFalse(many.get(i).equals(many.get(j)));
            }
        }
    }

    @Test
    public void theIndexIsWellFormed() {
        assertTrue(IpaLookup.entries().size() > 100);
        for (IpaLookup.Entry entry : IpaLookup.entries()) {
            assertFalse(entry.symbol.isEmpty());
            assertFalse("every symbol says what it is: " + entry.symbol, entry.words.isEmpty());
        }
    }
}
