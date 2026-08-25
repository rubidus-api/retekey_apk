package com.retekey;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Romaji to hiragana, on the words every Japanese IME is judged by. */
public final class RomajiKanaComposerTest {
    private static String type(String keys) {
        RomajiKanaComposer composer = new RomajiKanaComposer();
        String preedit = "";
        for (char c : keys.toCharArray()) {
            preedit = composer.input(String.valueOf(c)).preedit;
        }
        return preedit;
    }

    private static String flush(String keys) {
        RomajiKanaComposer composer = new RomajiKanaComposer();
        for (char c : keys.toCharArray()) {
            composer.input(String.valueOf(c));
        }
        return composer.flush();
    }

    @Test
    public void plainRowsAndHepburnSpellings() {
        assertEquals("ありがとう", type("arigatou"));
        assertEquals("すし", type("sushi"));
        assertEquals("すし", type("susi"));
        assertEquals("つき", type("tsuki"));
        assertEquals("ふじ", type("fuji"));
    }

    @Test
    public void doubledConsonantsAreTheSmallTsu() {
        assertEquals("がっこう", type("gakkou"));
        assertEquals("ちょっと", type("chotto"));
        assertEquals("ざっし", type("zasshi"));
    }

    @Test
    public void nBecomesItsKanaBeforeConsonantsAndAsNN() {
        assertEquals("しんぶん", flush("shinbun"));
        assertEquals("にほんご", type("nihongo"));
        assertEquals("こんにちは", type("konnnichiha"));
        assertEquals("じゃんけん", flush("jankenn"));
        assertEquals("な", type("na"));
    }

    @Test
    public void youonBothWays() {
        assertEquals("とうきょう", type("toukyou"));
        assertEquals("しゃしん", flush("shashin"));
        assertEquals("しゃ", type("sya"));
        assertEquals("じゅ", type("ju"));
        assertEquals("ちゃ", type("tya"));
    }

    @Test
    public void smallKanaAndTheLongVowelBar() {
        assertEquals("っ", type("xtu"));
        assertEquals("ゃ", type("lya"));
        assertEquals("らーめん", flush("ra-men"));
    }

    @Test
    public void thePendingTailShowsAsRomajiUntilItResolves() {
        assertEquals("k", type("k"));
        assertEquals("ky", type("ky"));
        assertEquals("きゃ", type("kya"));
        assertEquals("n", type("n"));
        assertEquals("ん", flush("n"));
        assertEquals("みかん", flush("mikan"));
    }

    @Test
    public void backspaceDropsAKeystroke() {
        RomajiKanaComposer composer = new RomajiKanaComposer();
        for (char c : "kya".toCharArray()) {
            composer.input(String.valueOf(c));
        }
        assertEquals("ky", composer.backspace().preedit);
        assertEquals("k", composer.backspace().preedit);
        assertEquals("", composer.backspace().preedit);
    }

    @Test
    public void lettersItCannotUseStayAsTyped() {
        assertEquals("qあ", type("qa"));
    }
}
