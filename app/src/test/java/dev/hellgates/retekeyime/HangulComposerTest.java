package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * The 2-beolsik composer. The sanitized vectors are the input-key-sequence to output-string pairs
 * translated from the Jamotong automata test corpus (owner-authorized, technical only), plus new
 * ReteKey cases for reversible backspace and no-loss that the upstream tests never covered.
 */
public final class HangulComposerTest {
    // Jamotong 2-beolsik keymap: QWERTY char -> semantic jamo.
    private static final Map<Character, SemanticJamo> LOWER = new HashMap<>();
    private static final Map<Character, SemanticJamo> UPPER = new HashMap<>();

    static {
        cho('r', 0); cho('R', 1); cho('s', 2); cho('e', 3); cho('E', 4);
        cho('f', 5); cho('a', 6); cho('q', 7); cho('Q', 8); cho('t', 9);
        cho('T', 10); cho('d', 11); cho('w', 12); cho('W', 13); cho('c', 14);
        cho('z', 15); cho('x', 16); cho('v', 17); cho('g', 18);
        jung('k', 0); jung('o', 1); jung('i', 2); jung('O', 3); jung('j', 4);
        jung('p', 5); jung('u', 6); jung('P', 7); jung('h', 8); jung('y', 12);
        jung('n', 13); jung('b', 17); jung('m', 18); jung('l', 20);
    }

    private static void cho(char key, int index) {
        put(key, SemanticJamo.contextualConsonant(index));
    }

    private static void jung(char key, int index) {
        put(key, SemanticJamo.vowel(index));
    }

    private static void put(char key, SemanticJamo jamo) {
        (Character.isUpperCase(key) ? UPPER : LOWER).put(key, jamo);
    }

    private static SemanticJamo jamoFor(char key) {
        if (key >= 'A' && key <= 'Z') {
            SemanticJamo shifted = UPPER.get(key);
            return shifted != null ? shifted : LOWER.get(Character.toLowerCase(key));
        }
        return LOWER.get(key);
    }

    /** Runs a key sequence the way the service would, flushing at non-jamo keys and at the end. */
    private static String type(String keys) {
        HangulComposer composer = new HangulComposer();
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < keys.length(); i++) {
            char key = keys.charAt(i);
            SemanticJamo jamo = jamoFor(key);
            if (jamo != null) {
                out.append(composer.input(jamo).commit());
            } else {
                out.append(composer.flush());
                out.append(key);
            }
        }
        out.append(composer.flush());
        return out.toString();
    }

    @Test
    public void basicSyllables() {
        assertEquals("가", type("rk"));
        assertEquals("까", type("Rk"));
        assertEquals("각", type("rkr"));
        assertEquals("감", type("rka"));
    }

    @Test
    public void ghostBatchimMovesToTheNextSyllable() {
        assertEquals("가가", type("rkrk"));
        assertEquals("갓을", type("rktdmf"));
        assertEquals("간정", type("rkswjd"));
    }

    @Test
    public void words() {
        assertEquals("안녕", type("dkssud"));
        assertEquals("한글", type("gksrmf"));
    }

    @Test
    public void everyCompoundVowel() {
        assertEquals("와", type("dhk"));
        assertEquals("왜", type("dho"));
        assertEquals("외", type("dhl"));
        assertEquals("워", type("dnj"));
        assertEquals("웨", type("dnp"));
        assertEquals("위", type("dnl"));
        assertEquals("의", type("dml"));
    }

    @Test
    public void compoundFinals() {
        assertEquals("닭", type("ekfr"));
        assertEquals("삶", type("tkfa"));
        assertEquals("값", type("rkqt"));
        assertEquals("앉", type("dksw"));
        assertEquals("많", type("aksg"));
        assertEquals("앏", type("dkfq"));
    }

    @Test
    public void partialStateFlushesToCompatibilityJamo() {
        assertEquals("ㄱ ", type("r "));
        assertEquals("ㅏ ", type("k "));
        assertEquals("가1", type("rk1"));
    }

    @Test
    public void tenseConsonantsFromShift() {
        assertEquals("깠", type("RkT"));
        assertEquals("빴", type("QkT"));
    }

    // --- ReteKey-specific behavior the upstream tests never covered ---

    @Test
    public void backspaceDecomposesACompoundFinal() {
        HangulComposer composer = feed("ekfr"); // 닭 (ㄷㅏㄺ)
        assertEquals("달", composer.backspace().preedit()); // ㄺ -> ㄹ, not 다
        assertEquals("다", composer.backspace().preedit());
    }

    @Test
    public void backspaceDecomposesACompoundVowel() {
        HangulComposer composer = feed("dhk"); // 와 (ㅇㅘ)
        assertEquals("오", composer.backspace().preedit()); // ㅘ -> ㅗ, not ㅇ
        assertEquals("ㅇ", composer.backspace().preedit());
    }

    @Test
    public void backspacePeelsASimpleFinalThenTheVowel() {
        HangulComposer composer = feed("rkr"); // 각
        assertEquals("가", composer.backspace().preedit());
        assertEquals("ㄱ", composer.backspace().preedit());
        assertEquals("", composer.backspace().preedit());
        assertFalse(composer.isComposing());
    }

    @Test
    public void backspaceWithNothingComposingReturnsNull() {
        HangulComposer composer = new HangulComposer();
        assertNull(composer.backspace());
    }

    @Test
    public void noKeystrokeIsEverLostAcrossSyllables() {
        // Every committed-plus-composing character round-trips: no silent drops.
        assertEquals("맑은", type("akfrdms"));
    }

    @Test
    public void flushCommitsAndResets() {
        HangulComposer composer = feed("rk");
        assertTrue(composer.isComposing());
        assertEquals("가", composer.flush());
        assertFalse(composer.isComposing());
        assertEquals("", composer.flush());
    }

    private static HangulComposer feed(String keys) {
        HangulComposer composer = new HangulComposer();
        for (int i = 0; i < keys.length(); i++) {
            composer.input(jamoFor(keys.charAt(i)));
        }
        return composer;
    }
}
