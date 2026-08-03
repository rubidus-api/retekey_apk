package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.retekey.CheonjiinInterpreter.Flick;
import com.retekey.CheonjiinInterpreter.Key;
import org.junit.Test;

/** What the guide under a held finger promises, which must be what the drag delivers. */
public final class CheonjiinFlickLabelTest {
    @Test
    public void theVowelKeysPromiseTheVowelTheDirectionPointsAt() {
        assertEquals("ㅓ", CheonjiinInterpreter.flickLabel(Key.DOT, Flick.LEFT));
        assertEquals("ㅏ", CheonjiinInterpreter.flickLabel(Key.DOT, Flick.RIGHT));
        assertEquals("ㅗ", CheonjiinInterpreter.flickLabel(Key.DOT, Flick.UP));
        assertEquals("ㅜ", CheonjiinInterpreter.flickLabel(Key.DOT, Flick.DOWN));
        assertEquals("ㅔ", CheonjiinInterpreter.flickLabel(Key.I, Flick.LEFT));
        assertEquals("ㅐ", CheonjiinInterpreter.flickLabel(Key.I, Flick.RIGHT));
        assertEquals("ㅒ", CheonjiinInterpreter.flickLabel(Key.I, Flick.UP));
        assertEquals("ㅖ", CheonjiinInterpreter.flickLabel(Key.I, Flick.DOWN));
        assertEquals("ㅝ", CheonjiinInterpreter.flickLabel(Key.EU, Flick.LEFT));
        assertEquals("ㅘ", CheonjiinInterpreter.flickLabel(Key.EU, Flick.RIGHT));
        assertEquals("ㅚ", CheonjiinInterpreter.flickLabel(Key.EU, Flick.UP));
        assertEquals("ㅟ", CheonjiinInterpreter.flickLabel(Key.EU, Flick.DOWN));
    }

    @Test
    public void theConsonantKeysPromiseTheirGroupInOrder() {
        // Left the plain one, right the aspirate, down the tense one.
        assertEquals("ㄱ", CheonjiinInterpreter.flickLabel(Key.GIYEOK, Flick.LEFT));
        assertEquals("ㅋ", CheonjiinInterpreter.flickLabel(Key.GIYEOK, Flick.RIGHT));
        assertEquals("ㄲ", CheonjiinInterpreter.flickLabel(Key.GIYEOK, Flick.DOWN));
        assertEquals("ㄷ", CheonjiinInterpreter.flickLabel(Key.DIGEUT, Flick.LEFT));
        assertEquals("ㅌ", CheonjiinInterpreter.flickLabel(Key.DIGEUT, Flick.RIGHT));
        assertEquals("ㄸ", CheonjiinInterpreter.flickLabel(Key.DIGEUT, Flick.DOWN));
        assertEquals("ㅅ", CheonjiinInterpreter.flickLabel(Key.SIOT, Flick.LEFT));
        assertEquals("ㅎ", CheonjiinInterpreter.flickLabel(Key.SIOT, Flick.RIGHT));
        assertEquals("ㅆ", CheonjiinInterpreter.flickLabel(Key.SIOT, Flick.DOWN));
        assertEquals("ㅈ", CheonjiinInterpreter.flickLabel(Key.JIEUT, Flick.LEFT));
        assertEquals("ㅊ", CheonjiinInterpreter.flickLabel(Key.JIEUT, Flick.RIGHT));
        assertEquals("ㅉ", CheonjiinInterpreter.flickLabel(Key.JIEUT, Flick.DOWN));
    }

    @Test
    public void aGroupWithNoTenseLetterHasNothingBelowIt() {
        assertEquals("ㄴ", CheonjiinInterpreter.flickLabel(Key.NIEUN, Flick.LEFT));
        assertEquals("ㄹ", CheonjiinInterpreter.flickLabel(Key.NIEUN, Flick.RIGHT));
        assertNull(CheonjiinInterpreter.flickLabel(Key.NIEUN, Flick.DOWN));
        assertEquals("ㅇ", CheonjiinInterpreter.flickLabel(Key.IEUNG, Flick.LEFT));
        assertEquals("ㅁ", CheonjiinInterpreter.flickLabel(Key.IEUNG, Flick.RIGHT));
        assertNull(CheonjiinInterpreter.flickLabel(Key.IEUNG, Flick.DOWN));
    }

    /** Nothing is above a consonant at all: the digit is held for, never dragged to. */
    @Test
    public void nothingIsAboveAConsonant() {
        for (Key key : new Key[] {Key.GIYEOK, Key.NIEUN, Key.DIGEUT, Key.BIEUP, Key.SIOT,
            Key.JIEUT, Key.IEUNG}) {
            assertNull(CheonjiinInterpreter.flickLabel(key, Flick.UP));
        }
        // A vowel key still has all four of its own.
        assertEquals("ㅗ", CheonjiinInterpreter.flickLabel(Key.DOT, Flick.UP));
    }

    @Test
    public void nothingIsPromisedForNothing() {
        assertNull(CheonjiinInterpreter.flickLabel(null, Flick.UP));
        assertNull(CheonjiinInterpreter.flickLabel(Key.GIYEOK, null));
    }

    /** The promise and the delivery are the same thing, checked key by key and way by way. */
    @Test
    public void everyPromiseIsKept() {
        Key[] keys = {Key.GIYEOK, Key.NIEUN, Key.DIGEUT, Key.BIEUP, Key.SIOT, Key.JIEUT,
            Key.IEUNG, Key.I, Key.DOT, Key.EU};
        for (Key key : keys) {
            for (Flick direction : Flick.values()) {
                CheonjiinInterpreter interpreter = new CheonjiinInterpreter();
                StringBuilder typed = new StringBuilder();
                for (SemanticInput input : interpreter.flick(key, direction)) {
                    if (input.kind() == SemanticInput.Kind.JAMO) {
                        SemanticJamo jamo = input.jamo();
                        typed.append(jamo.role() == SemanticJamo.Role.VOWEL
                            ? HangulTables.jungJamo(jamo.index())
                            : HangulTables.choJamo(jamo.index()));
                    }
                }
                // An empty promise must be an empty delivery: the ways that show no cell —
                // down off a consonant, up off a group with no tense letter — type no jamo.
                String promised = CheonjiinInterpreter.flickLabel(key, direction);
                assertEquals(key + " " + direction,
                    promised == null ? "" : promised, typed.toString());
            }
        }
    }
}
