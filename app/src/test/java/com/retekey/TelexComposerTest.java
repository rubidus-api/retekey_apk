package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Telex, checked on the words every Vietnamese keyboard is judged by. */
public final class TelexComposerTest {
    private static String type(String keys) {
        TelexComposer composer = new TelexComposer();
        String preedit = "";
        for (char c : keys.toCharArray()) {
            preedit = composer.input(String.valueOf(c)).preedit;
        }
        return preedit;
    }

    @Test
    public void marksFromDoubledVowelsWAndDoubledD() {
        assertEquals("â", type("aa"));
        assertEquals("ê", type("ee"));
        assertEquals("ô", type("oo"));
        assertEquals("ă", type("aw"));
        assertEquals("ơ", type("ow"));
        assertEquals("ư", type("uw"));
        assertEquals("ư", type("w"));
        assertEquals("đ", type("dd"));
        assertEquals("ươ", type("uow"));
    }

    @Test
    public void tonesLandOnTheRightVowel() {
        assertEquals("tiếng", type("tieesng"));
        assertEquals("Việt", type("Vieejt"));
        assertEquals("đất", type("ddaats"));
        assertEquals("người", type("nguwowif"));
        assertEquals("uống", type("uoongs"));
        assertEquals("thủy", type("thuyr"));
        assertEquals("hòa", type("hoaf"));
        assertEquals("hoàn", type("hoanf"));
        assertEquals("quả", type("quar"));
        assertEquals("giờ", type("giowf"));
        assertEquals("gì", type("gif"));
        assertEquals("ngoài", type("ngoaif"));
        assertEquals("mừa", type("muwaf"));
        assertEquals("huỳnh", type("huynhf"));
    }

    @Test
    public void aToneTypedEarlyMovesAsTheWordGrows() {
        assertEquals("hoán", type("hoasn"));
    }

    @Test
    public void pressingAMarkOrToneAgainGivesItBackAndTypesTheLetter() {
        assertEquals("aa", type("aaa"));
        assertEquals("tos", type("toss"));
        assertEquals("aw", type("aww"));
        assertEquals("dd", type("ddd"));
    }

    @Test
    public void zTakesTheToneOffAndIsOtherwiseALetter() {
        assertEquals("to", type("tosz"));
        assertEquals("xz", type("xz"));
    }

    @Test
    public void toneKeysBeforeAnyVowelAreLetters() {
        assertEquals("sf", type("sf"));
        assertEquals("str", type("str"));
    }

    @Test
    public void caseIsKeptAndMarksGoOnCapitals() {
        assertEquals("Â", type("Aa"));
        assertEquals("ĐẤT", type("DDAATS"));
    }

    @Test
    public void backspaceDropsTheLastKeystroke() {
        TelexComposer composer = new TelexComposer();
        for (char c : "ddaats".toCharArray()) {
            composer.input(String.valueOf(c));
        }
        assertEquals("đât", composer.backspace().preedit);
        assertEquals("đâ", composer.backspace().preedit);
        assertEquals("đa", composer.backspace().preedit);
        assertEquals("đ", composer.backspace().preedit);
        assertEquals("d", composer.backspace().preedit);
        assertEquals("", composer.backspace().preedit);
        assertNull(composer.backspace());
    }

    @Test
    public void flushHandsOverTheWordAndEmptiesTheComposer() {
        TelexComposer composer = new TelexComposer();
        composer.input("v"); composer.input("i"); composer.input("e"); composer.input("e");
        assertTrue(composer.isComposing());
        assertEquals("viê", composer.flush());
        assertFalse(composer.isComposing());
        assertEquals("", composer.flush());
    }

    @Test
    public void onlySingleLatinLettersAreItsBusiness() {
        TelexComposer composer = new TelexComposer();
        assertTrue(composer.accepts("a"));
        assertTrue(composer.accepts("W"));
        assertFalse(composer.accepts(" "));
        assertFalse(composer.accepts("."));
        assertFalse(composer.accepts("ab"));
        assertFalse(composer.accepts("ñ"));
    }
}
