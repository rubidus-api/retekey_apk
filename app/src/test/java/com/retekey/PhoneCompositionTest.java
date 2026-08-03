package com.retekey;

import static org.junit.Assert.assertEquals;

import com.retekey.CheonjiinInterpreter.Flick;
import com.retekey.CheonjiinInterpreter.Key;
import java.util.Collections;
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

    /**
     * Every compound final, spelled on 천지인. Which ones were reachable used to depend on whether
     * the tail happened to be the first letter of its key: ㅈ and ㅅ and ㅂ have their own tap, but
     * ㅎ is ㅅ twice, ㅁ is ㅇ twice, ㅌ is ㄷ twice and ㅍ is ㅂ twice — and each of those types a
     * letter that closes the syllable before the second tap can say what it really was.
     */
    @Test
    public void cheonjiinSpellsEveryCompoundFinal() {
        assertEquals("넋", cheonjiin(Key.NIEUN, Key.DOT, Key.I, Key.GIYEOK, Key.SIOT));
        assertEquals("앉", cheonjiin(Key.IEUNG, Key.I, Key.DOT, Key.NIEUN, Key.JIEUT));
        assertEquals("많", cheonjiin(
            Key.IEUNG, Key.IEUNG, Key.I, Key.DOT, Key.NIEUN, Key.SIOT, Key.SIOT));
        assertEquals("닭", cheonjiin(
            Key.DIGEUT, Key.I, Key.DOT, Key.NIEUN, Key.NIEUN, Key.GIYEOK));
        assertEquals("삶", cheonjiin(
            Key.SIOT, Key.I, Key.DOT, Key.NIEUN, Key.NIEUN, Key.IEUNG, Key.IEUNG));
        assertEquals("밟", cheonjiin(
            Key.BIEUP, Key.I, Key.DOT, Key.NIEUN, Key.NIEUN, Key.BIEUP));
        assertEquals("곬", cheonjiin(
            Key.GIYEOK, Key.DOT, Key.EU, Key.NIEUN, Key.NIEUN, Key.SIOT));
        assertEquals("핥", cheonjiin(
            Key.SIOT, Key.SIOT, Key.I, Key.DOT, Key.NIEUN, Key.NIEUN, Key.DIGEUT, Key.DIGEUT));
        assertEquals("읊", cheonjiin(
            Key.IEUNG, Key.EU, Key.NIEUN, Key.NIEUN, Key.BIEUP, Key.BIEUP));
        assertEquals("옳", cheonjiin(
            Key.IEUNG, Key.DOT, Key.EU, Key.NIEUN, Key.NIEUN, Key.SIOT, Key.SIOT));
        assertEquals("값", cheonjiin(Key.GIYEOK, Key.I, Key.DOT, Key.BIEUP, Key.SIOT));
    }

    /** And the syllable after one of them, so the re-opened final is left in a usable state. */
    @Test
    public void cheonjiinCarriesOnAfterACompoundFinal() {
        assertEquals("많은", cheonjiin(
            Key.IEUNG, Key.IEUNG, Key.I, Key.DOT, Key.NIEUN, Key.SIOT, Key.SIOT,
            Key.IEUNG, Key.EU, Key.NIEUN));
        // A vowel after ㄶ moves the ㅎ on, exactly as a batchim should.
        assertEquals("안하", cheonjiin(
            Key.IEUNG, Key.I, Key.DOT, Key.NIEUN, Key.SIOT, Key.SIOT, Key.I, Key.DOT));
    }

    /**
     * The multi-tap boundary. Two letters from one key in a row are one cycle, not two letters, so
     * 삶 followed by ㅇ needs the ▷ key that ends the syllable — the reason that key is on the page.
     */
    @Test
    public void cheonjiinNeedsTheCommitKeyBetweenTwoLettersOfOneKey() {
        assertEquals("살은", cheonjiin(
            Key.SIOT, Key.I, Key.DOT, Key.NIEUN, Key.NIEUN, Key.IEUNG, Key.IEUNG,
            Key.IEUNG, Key.EU, Key.NIEUN));
        assertEquals("삶은", cheonjiinCommitting(
            new Key[] {Key.SIOT, Key.I, Key.DOT, Key.NIEUN, Key.NIEUN, Key.IEUNG, Key.IEUNG},
            new Key[] {Key.IEUNG, Key.EU, Key.NIEUN}));
    }

    /**
     * 천지인 플러스 borrows a gesture: a drag off a key types at once what tapping it two or three
     * times would reach. Right is the aspirate, left the tense one.
     */
    @Test
    public void cheonjiinDragsAConsonantThroughItsGroup() {
        // Left the plain letter, right the aspirate, down the tense one.
        assertEquals("가", cheonjiinDragging(drag(Key.GIYEOK, Flick.LEFT), tap(Key.I), tap(Key.DOT)));
        assertEquals("카", cheonjiinDragging(drag(Key.GIYEOK, Flick.RIGHT), tap(Key.I), tap(Key.DOT)));
        assertEquals("까", cheonjiinDragging(drag(Key.GIYEOK, Flick.DOWN), tap(Key.I), tap(Key.DOT)));
        assertEquals("타", cheonjiinDragging(drag(Key.DIGEUT, Flick.RIGHT), tap(Key.I), tap(Key.DOT)));
        assertEquals("짜", cheonjiinDragging(drag(Key.JIEUT, Flick.DOWN), tap(Key.I), tap(Key.DOT)));
        assertEquals("라", cheonjiinDragging(drag(Key.NIEUN, Flick.RIGHT), tap(Key.I), tap(Key.DOT)));
        assertEquals("마", cheonjiinDragging(drag(Key.IEUNG, Flick.RIGHT), tap(Key.I), tap(Key.DOT)));
    }

    /** A group with no tense letter types nothing downwards, rather than something else. */
    @Test
    public void cheonjiinDragsDownToNothingWhenThereIsNoTenseLetter() {
        assertEquals("아", cheonjiinDragging(
            drag(Key.NIEUN, Flick.DOWN), tap(Key.IEUNG), tap(Key.I), tap(Key.DOT)));
    }

    /** And upwards off a consonant is nothing at all — the digit is held for, not dragged to. */
    @Test
    public void cheonjiinDragsUpOffAConsonantToNothing() {
        assertEquals("", cheonjiinDragging(drag(Key.GIYEOK, Flick.UP)));
        assertEquals("아", cheonjiinDragging(
            drag(Key.GIYEOK, Flick.UP), tap(Key.IEUNG), tap(Key.I), tap(Key.DOT)));
    }

    /** And off a vowel key it types the vowel the direction points at. */
    @Test
    public void cheonjiinDragsAVowelInTheDirectionItLeans() {
        assertEquals("거", cheonjiinDragging(tap(Key.GIYEOK), drag(Key.DOT, Flick.LEFT)));
        assertEquals("가", cheonjiinDragging(tap(Key.GIYEOK), drag(Key.DOT, Flick.RIGHT)));
        assertEquals("고", cheonjiinDragging(tap(Key.GIYEOK), drag(Key.DOT, Flick.UP)));
        assertEquals("구", cheonjiinDragging(tap(Key.GIYEOK), drag(Key.DOT, Flick.DOWN)));
        assertEquals("게", cheonjiinDragging(tap(Key.GIYEOK), drag(Key.I, Flick.LEFT)));
        assertEquals("개", cheonjiinDragging(tap(Key.GIYEOK), drag(Key.I, Flick.RIGHT)));
        assertEquals("걔", cheonjiinDragging(tap(Key.GIYEOK), drag(Key.I, Flick.UP)));
        assertEquals("계", cheonjiinDragging(tap(Key.GIYEOK), drag(Key.I, Flick.DOWN)));
        assertEquals("궈", cheonjiinDragging(tap(Key.GIYEOK), drag(Key.EU, Flick.LEFT)));
        assertEquals("과", cheonjiinDragging(tap(Key.GIYEOK), drag(Key.EU, Flick.RIGHT)));
        assertEquals("괴", cheonjiinDragging(tap(Key.GIYEOK), drag(Key.EU, Flick.UP)));
        assertEquals("귀", cheonjiinDragging(tap(Key.GIYEOK), drag(Key.EU, Flick.DOWN)));
    }

    /**
     * A dragged vowel leaves the element run exactly where the taps behind it would have, so it
     * goes on combining — with a tap, with another drag, and in either order.
     */
    @Test
    public void aDraggedVowelStillCombines() {
        assertEquals("괴", cheonjiinDragging(
            tap(Key.GIYEOK), drag(Key.DOT, Flick.UP), tap(Key.I)));
        assertEquals("과", cheonjiinDragging(
            tap(Key.GIYEOK), drag(Key.DOT, Flick.UP), drag(Key.DOT, Flick.RIGHT)));
        assertEquals("개", cheonjiinDragging(
            tap(Key.GIYEOK), drag(Key.DOT, Flick.RIGHT), tap(Key.I)));
        assertEquals("과", cheonjiinDragging(
            tap(Key.GIYEOK), tap(Key.DOT), tap(Key.EU), drag(Key.DOT, Flick.RIGHT)));
        assertEquals("괘", cheonjiinDragging(
            tap(Key.GIYEOK), drag(Key.EU, Flick.RIGHT), tap(Key.I)));
        // And a final consonant still closes the syllable after one.
        assertEquals("관", cheonjiinDragging(
            tap(Key.GIYEOK), drag(Key.EU, Flick.RIGHT), tap(Key.NIEUN)));
        assertEquals("많", cheonjiinDragging(
            drag(Key.IEUNG, Flick.RIGHT), tap(Key.I), tap(Key.DOT),
            tap(Key.NIEUN), tap(Key.SIOT), tap(Key.SIOT)));
    }

    /**
     * The pause. Two letters of one key in a row are a cycle while the taps keep coming, and two
     * separate letters once the run has ended — which is what a phone's multi-tap timeout does,
     * and what lets 삶 be followed by ㅇ without the 다음 key.
     */
    @Test
    public void aPauseEndsTheMultiTapRunWithoutEndingTheSyllable() {
        CheonjiinInterpreter interpreter = new CheonjiinInterpreter();
        FakeEditor editor = new FakeEditor();
        for (Key key : new Key[] {
            Key.SIOT, Key.I, Key.DOT, Key.NIEUN, Key.NIEUN, Key.IEUNG, Key.IEUNG}) {
            editor.apply(interpreter.press(key));
        }
        interpreter.endMultiTap();                       // the finger rests
        for (Key key : new Key[] {Key.IEUNG, Key.EU, Key.NIEUN}) {
            editor.apply(interpreter.press(key));
        }
        assertEquals("삶은", editor.text());
    }

    /** But a pause must not break a vowel: its elements are different keys, and belong to the syllable. */
    @Test
    public void aPauseLeavesAVowelBeingSpelledAlone() {
        CheonjiinInterpreter interpreter = new CheonjiinInterpreter();
        FakeEditor editor = new FakeEditor();
        editor.apply(interpreter.press(Key.SIOT));
        editor.apply(interpreter.press(Key.I));
        interpreter.endMultiTap();                       // a slow ㅣ ㆍ is still ㅏ
        editor.apply(interpreter.press(Key.DOT));
        assertEquals("사", editor.text());
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

    /** Types one run, presses ▷ (the key that ends the syllable), then types the next. */
    private static String cheonjiinCommitting(Key[] first, Key[] second) {
        CheonjiinInterpreter interpreter = new CheonjiinInterpreter();
        FakeEditor editor = new FakeEditor();
        for (Key key : first) {
            editor.apply(interpreter.press(key));
        }
        interpreter.reset();
        editor.apply(Collections.singletonList(SemanticInput.flush()));
        for (Key key : second) {
            editor.apply(interpreter.press(key));
        }
        return editor.text();
    }

    /** A tap or a drag, so one list can spell a word with both. */
    private static final class Gesture {
        final Key key;
        final Flick flick;

        Gesture(Key key, Flick flick) {
            this.key = key;
            this.flick = flick;
        }
    }

    private static Gesture tap(Key key) {
        return new Gesture(key, null);
    }

    private static Gesture drag(Key key, Flick flick) {
        return new Gesture(key, flick);
    }

    private static String cheonjiinDragging(Gesture... gestures) {
        CheonjiinInterpreter interpreter = new CheonjiinInterpreter();
        FakeEditor editor = new FakeEditor();
        for (Gesture gesture : gestures) {
            editor.apply(gesture.flick == null
                ? interpreter.press(gesture.key)
                : interpreter.flick(gesture.key, gesture.flick));
        }
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
