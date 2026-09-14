package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/** Ctrl on the Korean page chords by the key's place, as a physical Korean keyboard does. */
public final class ChordLettersTest {
    @Test
    public void theClipboardChordsSitWhereTheyDoOnAKoreanKeyboard() {
        assertEquals(RawKey.C, ChordLetters.forKeyId("touch.ko2.chieut"));
        assertEquals(RawKey.V, ChordLetters.forKeyId("touch.ko2.pieup"));
        assertEquals(RawKey.X, ChordLetters.forKeyId("touch.ko2.tieut"));
        assertEquals(RawKey.A, ChordLetters.forKeyId("touch.ko2.mieum"));
        assertEquals(RawKey.Z, ChordLetters.forKeyId("touch.ko2.kieuk"));
        assertNull(ChordLetters.forKeyId("touch.en.a"));
    }

    @Test
    public void everyLetterKeyOnTheKoreanPageHasItsOwnPlace() {
        Set<RawKey> seen = new HashSet<>();
        int letters = 0;
        for (boolean shifted : new boolean[] {false, true}) {
            KeyboardLayout layout = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, shifted);
            for (List<SoftwareKeySpec> row : layout.rows()) {
                for (SoftwareKeySpec key : row) {
                    if (key.semanticInput() != null
                            && key.semanticInput().kind() == SemanticInput.Kind.JAMO) {
                        RawKey place = ChordLetters.forKeyId(key.stableKeyId());
                        assertEquals(key.stableKeyId() + " has a place", true, place != null);
                        if (!shifted) {
                            letters++;
                            seen.add(place);
                        }
                    }
                }
            }
        }
        assertEquals("26 keys, 26 different letters", 26, letters);
        assertEquals(26, seen.size());
    }
}
