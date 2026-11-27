package com.retekey;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import com.retekey.core.DispatchResult;
import com.retekey.core.EditorProfile;
import com.retekey.core.HangulInputProcessor;
import com.retekey.core.KeyAction;
import com.retekey.core.RomajiKanaComposer;
import com.retekey.core.SemanticInput;

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

    @Test
    public void extendedRomajiSpellingsAndSokuonAndNyAreHandled() {
        assertEquals("じゃじゅじょ", type("jyajyujyo"));
        assertEquals("ちゃちゅちょ", type("cyacyucyo"));
        assertEquals("しぇちぇじぇ", type("shecheje"));
        assertEquals("っちゃ", type("tcha"));
        assertEquals("ゔぁゔぃゔゔぇゔぉ", type("vavivuvevo"));
        assertEquals("うぃうぇ", type("wiwe"));
        assertEquals("てぃでぃ", type("thidhi"));
        assertEquals("ゎ", type("lwa"));
        // Only valid consonants trigger sokuon; q/x/l do not.
        assertEquals("qqxxll", type("qqxxll"));
        // n before y+non-(a,u,o) resolves to ん rather than literal 'n'.
        assertEquals("んyk", type("nyk"));
        assertEquals("しんy", flush("shiny"));
    }

    @Test
    public void longRomajiStreamsFinalizedPrefixThroughProcessorCommit() {
        HangulInputProcessor processor = new HangulInputProcessor(
            () -> EditorProfile.richText(false, false, false, false, false, 0, -1));
        processor.setLatinComposer(new RomajiKanaComposer());
        StringBuilder committed = new StringBuilder();
        String composing = "";
        for (int i = 0; i < 40; i++) { // 80 keys ("ka" * 40), exceeding MAX_RAW_BUFFER
            for (String ch : new String[] {"k", "a"}) {
                DispatchResult res = processor.process(SemanticInput.text(ch));
                for (KeyAction action : res.actions()) {
                    if (action.kind() == KeyAction.Kind.COMMIT_TEXT) {
                        committed.append(action.text());
                    } else if (action.kind() == KeyAction.Kind.SET_COMPOSING_TEXT) {
                        composing = action.text();
                    }
                }
            }
        }
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            expected.append("か");
        }
        assertEquals(expected.toString(), committed.toString() + composing);
    }
}
