package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Which key a character is typed on, for the apps that only take key events. */
public final class AsciiKeyStrokesTest {
    @Test
    public void lettersAreTheirOwnKeyAndCapitalsAreShifted() {
        assertEquals(new AsciiKeyStrokes.Stroke(RawKey.A, false), AsciiKeyStrokes.of('a'));
        assertEquals(new AsciiKeyStrokes.Stroke(RawKey.Z, false), AsciiKeyStrokes.of('z'));
        assertEquals(new AsciiKeyStrokes.Stroke(RawKey.A, true), AsciiKeyStrokes.of('A'));
        assertEquals(new AsciiKeyStrokes.Stroke(RawKey.Q, true), AsciiKeyStrokes.of('Q'));
    }

    @Test
    public void digitsAndTheCharactersAboveThem() {
        assertEquals(new AsciiKeyStrokes.Stroke(RawKey.DIGIT_0, false), AsciiKeyStrokes.of('0'));
        assertEquals(new AsciiKeyStrokes.Stroke(RawKey.DIGIT_7, false), AsciiKeyStrokes.of('7'));
        assertEquals(new AsciiKeyStrokes.Stroke(RawKey.DIGIT_1, true), AsciiKeyStrokes.of('!'));
        assertEquals(new AsciiKeyStrokes.Stroke(RawKey.DIGIT_2, true), AsciiKeyStrokes.of('@'));
        assertEquals(new AsciiKeyStrokes.Stroke(RawKey.DIGIT_9, true), AsciiKeyStrokes.of('('));
        assertEquals("Shift+0 is )", new AsciiKeyStrokes.Stroke(RawKey.DIGIT_0, true),
            AsciiKeyStrokes.of(')'));
    }

    @Test
    public void whitespaceHasKeysToo() {
        assertEquals(new AsciiKeyStrokes.Stroke(RawKey.SPACE, false), AsciiKeyStrokes.of(' '));
        assertEquals(new AsciiKeyStrokes.Stroke(RawKey.ENTER, false), AsciiKeyStrokes.of('\n'));
        assertEquals(new AsciiKeyStrokes.Stroke(RawKey.TAB, false), AsciiKeyStrokes.of('\t'));
    }

    @Test
    public void punctuationSitsOnItsKeyWithTheShiftedCharacterAboveIt() {
        assertEquals(new AsciiKeyStrokes.Stroke(RawKey.MINUS, false), AsciiKeyStrokes.of('-'));
        assertEquals(new AsciiKeyStrokes.Stroke(RawKey.MINUS, true), AsciiKeyStrokes.of('_'));
        assertEquals(new AsciiKeyStrokes.Stroke(RawKey.SLASH, false), AsciiKeyStrokes.of('/'));
        assertEquals(new AsciiKeyStrokes.Stroke(RawKey.SLASH, true), AsciiKeyStrokes.of('?'));
        assertEquals(new AsciiKeyStrokes.Stroke(RawKey.SEMICOLON, true), AsciiKeyStrokes.of(':'));
        assertEquals(new AsciiKeyStrokes.Stroke(RawKey.GRAVE, true), AsciiKeyStrokes.of('~'));
    }

    @Test
    public void hangulHasNoKeyToBePressedOn() {
        assertNull(AsciiKeyStrokes.of('가'));
        assertNull(AsciiKeyStrokes.of('ㅎ'));
        assertNull(AsciiKeyStrokes.of('漢'));
        assertNull(AsciiKeyStrokes.of('€'));
    }

    @Test
    public void onlyASingleCharacterIsAStroke() {
        assertEquals(new AsciiKeyStrokes.Stroke(RawKey.A, false), AsciiKeyStrokes.ofText("a"));
        assertNull(AsciiKeyStrokes.ofText("ab"));
        assertNull(AsciiKeyStrokes.ofText(""));
        assertNull(AsciiKeyStrokes.ofText(null));
    }

    @Test
    public void everyPrintableAsciiCharacterHasAStroke() {
        for (char c = 0x20; c < 0x7f; c++) {
            assertTrue("no key for '" + c + "'", AsciiKeyStrokes.of(c) != null);
        }
    }
}
