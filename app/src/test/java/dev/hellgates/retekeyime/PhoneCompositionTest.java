package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;

import dev.hellgates.retekeyime.CheonjiinInterpreter.Key;
import java.util.List;
import org.junit.Test;

/**
 * The two 12-key automata read end to end: key presses through their interpreter, through the
 * shared Hangul composer, into the text an editor would be left holding. The interpreters answer in
 * jamo, and only this pairing shows whether those jamo actually spell the syllables intended.
 */
public final class PhoneCompositionTest {
    @Test
    public void cheonjiinSpellsTheSimpleSyllables() {
        assertEquals("가", cheonjiin(Key.GIYEOK, Key.I, Key.DOT));
        assertEquals("각", cheonjiin(Key.GIYEOK, Key.I, Key.DOT, Key.GIYEOK));
        assertEquals("나", cheonjiin(Key.NIEUN, Key.I, Key.DOT));
        assertEquals("고", cheonjiin(Key.GIYEOK, Key.DOT, Key.EU));
        assertEquals("구", cheonjiin(Key.GIYEOK, Key.EU, Key.DOT));
        assertEquals("그", cheonjiin(Key.GIYEOK, Key.EU));
        assertEquals("기", cheonjiin(Key.GIYEOK, Key.I));
    }

    @Test
    public void cheonjiinSpellsTheCompoundVowels() {
        assertEquals("개", cheonjiin(Key.GIYEOK, Key.I, Key.DOT, Key.I));
        assertEquals("과", cheonjiin(Key.GIYEOK, Key.DOT, Key.EU, Key.I, Key.DOT));
        assertEquals("궈", cheonjiin(Key.GIYEOK, Key.EU, Key.DOT, Key.DOT, Key.I));
        assertEquals("긔", cheonjiin(Key.GIYEOK, Key.EU, Key.I));
    }

    @Test
    public void cheonjiinCyclesConsonantsInPlace() {
        assertEquals("카", cheonjiin(Key.GIYEOK, Key.GIYEOK, Key.I, Key.DOT));
        assertEquals("까", cheonjiin(Key.GIYEOK, Key.GIYEOK, Key.GIYEOK, Key.I, Key.DOT));
        assertEquals("갘", cheonjiin(Key.GIYEOK, Key.I, Key.DOT, Key.GIYEOK, Key.GIYEOK));
    }

    @Test
    public void cheonjiinEndsASyllableOnDemand() {
        // Without the commit key the second ㄱ becomes 가's final consonant.
        assertEquals("각", cheonjiin(Key.GIYEOK, Key.I, Key.DOT, Key.GIYEOK));
        // With it, the syllable is finished and the next one starts on its own.
        assertEquals("가ㄱ", cheonjiinThenCommit(Key.GIYEOK, Key.I, Key.DOT));
    }

    @Test
    public void naratgeulSpellsWhatItsStrokesBuild() {
        assertEquals("가", naratgeul(N.GIYEOK, N.A));
        assertEquals("카", naratgeul(N.GIYEOK, N.STROKE, N.A));
        assertEquals("까", naratgeul(N.GIYEOK, N.TWIN, N.A));
        assertEquals("다", naratgeul(N.NIEUN, N.STROKE, N.A));
        assertEquals("타", naratgeul(N.NIEUN, N.STROKE, N.STROKE, N.A));
        assertEquals("따", naratgeul(N.NIEUN, N.STROKE, N.TWIN, N.A));
        assertEquals("자", naratgeul(N.SIOT, N.STROKE, N.A));
        assertEquals("차", naratgeul(N.SIOT, N.STROKE, N.STROKE, N.A));
        assertEquals("하", naratgeul(N.IEUNG, N.STROKE, N.A));
        assertEquals("바", naratgeul(N.MIEUM, N.STROKE, N.A));
        assertEquals("파", naratgeul(N.MIEUM, N.STROKE, N.STROKE, N.A));
    }

    @Test
    public void naratgeulSpellsItsVowels() {
        assertEquals("고", naratgeul(N.GIYEOK, N.O));
        assertEquals("구", naratgeul(N.GIYEOK, N.EU, N.O));
        assertEquals("기", naratgeul(N.GIYEOK, N.I));
        assertEquals("거", naratgeul(N.GIYEOK, N.I, N.A));
        assertEquals("과", naratgeul(N.GIYEOK, N.O, N.A));
        assertEquals("개", naratgeul(N.GIYEOK, N.A, N.I));
        assertEquals("거", naratgeul(N.GIYEOK, N.A, N.A));
    }

    @Test
    public void naratgeulReachesEveryVowelThereIs() {
        // ㅗ twice is ㅜ, so ㅛ and ㅠ come from the stroke key, which iotates a vowel the way it
        // adds a stroke to a consonant. Between them nothing is out of reach.
        assertEquals("가", naratgeul(N.GIYEOK, N.A));
        assertEquals("거", naratgeul(N.GIYEOK, N.A, N.A));
        assertEquals("갸", naratgeul(N.GIYEOK, N.A, N.STROKE));
        assertEquals("거", naratgeul(N.GIYEOK, N.I, N.A));
        assertEquals("겨", naratgeul(N.GIYEOK, N.I, N.A, N.STROKE));
        assertEquals("고", naratgeul(N.GIYEOK, N.O));
        assertEquals("교", naratgeul(N.GIYEOK, N.O, N.STROKE));
        assertEquals("구", naratgeul(N.GIYEOK, N.O, N.O));
        assertEquals("구", naratgeul(N.GIYEOK, N.EU, N.O));
        assertEquals("규", naratgeul(N.GIYEOK, N.O, N.O, N.STROKE));
        assertEquals("그", naratgeul(N.GIYEOK, N.EU));
        assertEquals("기", naratgeul(N.GIYEOK, N.I));
        assertEquals("개", naratgeul(N.GIYEOK, N.A, N.I));
        assertEquals("걔", naratgeul(N.GIYEOK, N.A, N.STROKE, N.I));
        assertEquals("게", naratgeul(N.GIYEOK, N.I, N.A, N.I));
        assertEquals("계", naratgeul(N.GIYEOK, N.I, N.A, N.STROKE, N.I));
        assertEquals("과", naratgeul(N.GIYEOK, N.O, N.A));
        assertEquals("괘", naratgeul(N.GIYEOK, N.O, N.A, N.I));
        assertEquals("괴", naratgeul(N.GIYEOK, N.O, N.I));
        assertEquals("궈", naratgeul(N.GIYEOK, N.O, N.O, N.A));
        assertEquals("궤", naratgeul(N.GIYEOK, N.O, N.O, N.A, N.I));
        assertEquals("귀", naratgeul(N.GIYEOK, N.O, N.O, N.I));
        assertEquals("긔", naratgeul(N.GIYEOK, N.EU, N.I));
    }

    @Test
    public void naratgeulStrokesEveryConsonantChain() {
        assertEquals("마", naratgeul(N.MIEUM, N.A));
        assertEquals("바", naratgeul(N.MIEUM, N.STROKE, N.A));
        assertEquals("파", naratgeul(N.MIEUM, N.STROKE, N.STROKE, N.A));
        assertEquals("빠", naratgeul(N.MIEUM, N.STROKE, N.TWIN, N.A));
        // And on a final consonant, where the composer has to take it apart first.
        assertEquals("갑", naratgeul(N.GIYEOK, N.A, N.MIEUM, N.STROKE));
    }

    @Test
    public void naratgeulClosesASyllableWithAFinalConsonant() {
        assertEquals("각", naratgeul(N.GIYEOK, N.A, N.GIYEOK));
        assertEquals("갈", naratgeul(N.GIYEOK, N.A, N.RIEUL));
    }

    /**
     * A compound final whose tail is reached by 획추가 or by a multi-tap: the consonant starts a
     * fresh syllable when it is typed, and only the transform that follows reveals that it belongs
     * to the syllable before it.
     */
    @Test
    public void naratgeulSpellsCompoundFinalsReachedByAStroke() {
        assertEquals("많", naratgeul(N.MIEUM, N.A, N.NIEUN, N.IEUNG, N.STROKE));
        assertEquals("않", naratgeul(N.IEUNG, N.A, N.NIEUN, N.IEUNG, N.STROKE));
        assertEquals("앉", naratgeul(N.IEUNG, N.A, N.NIEUN, N.SIOT, N.STROKE));
        assertEquals("옳", naratgeul(N.IEUNG, N.O, N.RIEUL, N.IEUNG, N.STROKE));
        assertEquals("핥", naratgeul(N.IEUNG, N.STROKE, N.A, N.RIEUL, N.NIEUN, N.STROKE, N.STROKE));
    }

    /** The compound finals whose parts are both typed outright still work, unchanged. */
    @Test
    public void naratgeulSpellsTheCompoundFinalsTypedOutright() {
        assertEquals("값", naratgeul(N.GIYEOK, N.A, N.MIEUM, N.STROKE, N.SIOT));
        assertEquals("삶", naratgeul(N.SIOT, N.A, N.RIEUL, N.MIEUM));
        assertEquals("닭", naratgeul(N.NIEUN, N.STROKE, N.A, N.RIEUL, N.GIYEOK));
        assertEquals("읽", naratgeul(N.IEUNG, N.I, N.RIEUL, N.GIYEOK));
    }

    /** And a stroke that spells no compound final still just starts the next syllable. */
    @Test
    public void naratgeulLeavesAnImpossibleFinalAlone() {
        // ㄴ + ㅂ is not a compound final, so ㅂ stays the initial it started as.
        assertEquals("만ㅂ", naratgeul(N.MIEUM, N.A, N.NIEUN, N.MIEUM, N.STROKE));
        // And the batchim still moves when a vowel follows one that did combine.
        assertEquals("만하", naratgeul(N.MIEUM, N.A, N.NIEUN, N.IEUNG, N.STROKE, N.A));
    }

    /** 천지인 reaches ㅎ by tapping ㅅ again, and needs the same re-opening. */
    @Test
    public void cheonjiinSpellsCompoundFinalsReachedByACycle() {
        // ㅁ is ㅇ tapped twice, ㅏ is ㅣ ㆍ, and the final ㅎ is ㅅ tapped twice.
        assertEquals("많", cheonjiin(
            Key.IEUNG, Key.IEUNG, Key.I, Key.DOT, Key.NIEUN, Key.SIOT, Key.SIOT));
        // ㅈ has a key of its own, so this one never needed re-opening; it is the control.
        assertEquals("앉", cheonjiin(Key.IEUNG, Key.I, Key.DOT, Key.NIEUN, Key.JIEUT));
    }

    private interface N {
        NaratgeulInterpreter.Key GIYEOK = NaratgeulInterpreter.Key.GIYEOK;
        NaratgeulInterpreter.Key NIEUN = NaratgeulInterpreter.Key.NIEUN;
        NaratgeulInterpreter.Key RIEUL = NaratgeulInterpreter.Key.RIEUL;
        NaratgeulInterpreter.Key MIEUM = NaratgeulInterpreter.Key.MIEUM;
        NaratgeulInterpreter.Key SIOT = NaratgeulInterpreter.Key.SIOT;
        NaratgeulInterpreter.Key IEUNG = NaratgeulInterpreter.Key.IEUNG;
        NaratgeulInterpreter.Key A = NaratgeulInterpreter.Key.A;
        NaratgeulInterpreter.Key O = NaratgeulInterpreter.Key.O;
        NaratgeulInterpreter.Key I = NaratgeulInterpreter.Key.I;
        NaratgeulInterpreter.Key EU = NaratgeulInterpreter.Key.EU;
        NaratgeulInterpreter.Key STROKE = NaratgeulInterpreter.Key.STROKE;
        NaratgeulInterpreter.Key TWIN = NaratgeulInterpreter.Key.TWIN;
    }

    private static String cheonjiin(Key... keys) {
        CheonjiinInterpreter interpreter = new CheonjiinInterpreter();
        FakeEditor editor = new FakeEditor();
        for (Key key : keys) {
            editor.apply(interpreter.press(key));
        }
        return editor.text();
    }

    private static String cheonjiinThenCommit(Key... keys) {
        CheonjiinInterpreter interpreter = new CheonjiinInterpreter();
        FakeEditor editor = new FakeEditor();
        for (Key key : keys) {
            editor.apply(interpreter.press(key));
        }
        editor.apply(java.util.Collections.singletonList(SemanticInput.flush()));
        interpreter.reset();
        editor.apply(interpreter.press(Key.GIYEOK));
        return editor.text();
    }

    private static String naratgeul(NaratgeulInterpreter.Key... keys) {
        NaratgeulInterpreter interpreter = new NaratgeulInterpreter();
        FakeEditor editor = new FakeEditor();
        for (NaratgeulInterpreter.Key key : keys) {
            editor.apply(interpreter.press(key));
        }
        return editor.text();
    }

    /** Committed text plus the composing region, the way an editor holds them. */
    private static final class FakeEditor {
        private final HangulInputProcessor processor =
            new HangulInputProcessor(EditorProfile::unsupported);
        private final StringBuilder committed = new StringBuilder();
        private String composing = "";

        void apply(List<SemanticInput> inputs) {
            for (SemanticInput input : inputs) {
                for (KeyAction action : processor.process(input).actions()) {
                    switch (action.kind()) {
                        case COMMIT_TEXT:
                            composing = "";
                            committed.append(action.text());
                            break;
                        case SET_COMPOSING_TEXT:
                            composing = action.text();
                            break;
                        case DELETE_BACKWARD:
                            if (!composing.isEmpty()) {
                                composing = composing.substring(0, composing.length() - 1);
                            } else if (committed.length() > 0) {
                                committed.deleteCharAt(committed.length() - 1);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        String text() {
            return committed.toString() + composing;
        }
    }
}
