package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** The flick-only kana pad: taps, flicks, and the modifier's cycles. */
public final class KanaFlickTest {
    @Test
    public void aTapIsTheAColumnAndAFlickIsItsDirection() {
        assertEquals("あ", KanaFlick.tap(KanaFlick.Key.A));
        assertEquals("い", KanaFlick.flick(KanaFlick.Key.A, CheonjiinInterpreter.Flick.LEFT));
        assertEquals("く", KanaFlick.flick(KanaFlick.Key.KA, CheonjiinInterpreter.Flick.UP));
        assertEquals("せ", KanaFlick.flick(KanaFlick.Key.SA, CheonjiinInterpreter.Flick.RIGHT));
        assertEquals("と", KanaFlick.flick(KanaFlick.Key.TA, CheonjiinInterpreter.Flick.DOWN));
    }

    @Test
    public void yaCarriesTheBracketsAndWaCarriesWoNAndTheBar() {
        assertEquals("「", KanaFlick.flick(KanaFlick.Key.YA, CheonjiinInterpreter.Flick.LEFT));
        assertEquals("ゆ", KanaFlick.flick(KanaFlick.Key.YA, CheonjiinInterpreter.Flick.UP));
        assertEquals("を", KanaFlick.flick(KanaFlick.Key.WA, CheonjiinInterpreter.Flick.LEFT));
        assertEquals("ん", KanaFlick.flick(KanaFlick.Key.WA, CheonjiinInterpreter.Flick.UP));
        assertEquals("ー", KanaFlick.flick(KanaFlick.Key.WA, CheonjiinInterpreter.Flick.RIGHT));
        assertNull(KanaFlick.flick(KanaFlick.Key.WA, CheonjiinInterpreter.Flick.DOWN));
    }

    @Test
    public void theModifierCyclesDakutenHandakutenAndSmall() {
        assertEquals("が", KanaFlick.modified('か'));
        assertEquals("か", KanaFlick.modified('が'));
        assertEquals("ば", KanaFlick.modified('は'));
        assertEquals("ぱ", KanaFlick.modified('ば'));
        assertEquals("は", KanaFlick.modified('ぱ'));
        assertEquals("づ", KanaFlick.modified('つ'));
        assertEquals("っ", KanaFlick.modified('づ'));
        assertEquals("つ", KanaFlick.modified('っ'));
        assertEquals("ぁ", KanaFlick.modified('あ'));
        assertEquals("ゃ", KanaFlick.modified('や'));
        assertNull(KanaFlick.modified('ん'));
        assertNull(KanaFlick.modified('A'));
    }
}
