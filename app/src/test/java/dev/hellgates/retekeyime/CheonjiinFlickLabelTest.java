package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import dev.hellgates.retekeyime.CheonjiinInterpreter.Flick;
import dev.hellgates.retekeyime.CheonjiinInterpreter.Key;
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
    public void theConsonantKeysPromiseTheirOtherLetters() {
        assertEquals("ㅋ", CheonjiinInterpreter.flickLabel(Key.GIYEOK, Flick.RIGHT));
        assertEquals("ㄲ", CheonjiinInterpreter.flickLabel(Key.GIYEOK, Flick.LEFT));
        assertEquals("ㅌ", CheonjiinInterpreter.flickLabel(Key.DIGEUT, Flick.RIGHT));
        assertEquals("ㅉ", CheonjiinInterpreter.flickLabel(Key.JIEUT, Flick.LEFT));
        assertEquals("ㄹ", CheonjiinInterpreter.flickLabel(Key.NIEUN, Flick.RIGHT));
        assertEquals("ㅁ", CheonjiinInterpreter.flickLabel(Key.IEUNG, Flick.LEFT));
    }

    @Test
    public void aConsonantHasNothingAboveOrBelowSoItPromisesItsOwnLetter() {
        assertEquals("ㄱ", CheonjiinInterpreter.flickLabel(Key.GIYEOK, Flick.UP));
        assertEquals("ㄱ", CheonjiinInterpreter.flickLabel(Key.GIYEOK, Flick.DOWN));
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
                assertEquals(key + " " + direction,
                    CheonjiinInterpreter.flickLabel(key, direction), typed.toString());
            }
        }
    }
}
