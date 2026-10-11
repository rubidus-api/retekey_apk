package com.retekey;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Costly keys give the edge they share with a typing key to that key. */
public final class TouchTargetsTest {
    private static final int W = 480;
    private static final int H = 200;

    private static String at(KeyboardLayout layout, float x, float y) {
        int[] t = TouchTargets.resolve(layout, W, H, x, y);
        return layout.rows().get(t[0]).get(t[1]).stableKeyId();
    }

    private static String at(KeyboardLayout layout, float x, float y, JamoExpectation.Kind expected) {
        int[] t = TouchTargets.resolve(layout, W, H, x, y, expected);
        return layout.rows().get(t[0]).get(t[1]).stableKeyId();
    }

    private static float colEdge(KeyboardLayout l, int column) {
        return l.columnEdge(column, W);
    }

    @Test
    public void aNearMissBeside123TypesTheHangulKey() {
        KeyboardLayout cji = KeyboardLayouts.phone(KeyboardLayoutId.KO_CHEONJIIN, PhoneOverlay.NONE);
        float boundary = colEdge(cji, 2);          // 123 is column 1; ㅣ starts at column 2
        float cell = colEdge(cji, 2) - colEdge(cji, 1);
        float y = cji.rowEdge(0, H) + 10;
        assertEquals("touch.cheonjiin.i", at(cji, boundary - cell * 0.2f, y));
        assertEquals("the middle of 123 is still 123",
            "touch.phone.overlay.digits", at(cji, boundary - cell * 0.6f, y));
    }

    @Test
    public void aNearMissBesideMoveTypesTheHangulKey() {
        KeyboardLayout nrg = KeyboardLayouts.phone(KeyboardLayoutId.KO_NARATGEUL, PhoneOverlay.NONE);
        float boundary = colEdge(nrg, 2);
        float cell = colEdge(nrg, 2) - colEdge(nrg, 1);
        float y = nrg.rowEdge(1, H) + 10;
        assertEquals("touch.naratgeul.rieul", at(nrg, boundary - cell * 0.2f, y));
        assertEquals("touch.phone.overlay.nav", at(nrg, boundary - cell * 0.6f, y));
    }

    @Test
    public void backspaceKeepsItsWholeCellAboveTheSpaceBar() {
        // It used to give its lower third to the space bar, on the reasoning that a stray ⌫ costs
        // more than a stray letter. But the key below ⌫ on the 12-key pages is the space bar, and
        // a space is not a letter: pressing backspace in a hurry typed a space, and the key could
        // not be relied on at all (owner's report, 2026-09-16). A costly key keeps its cell where
        // the neighbour is the space bar or Enter.
        KeyboardLayout cji = KeyboardLayouts.phone(KeyboardLayoutId.KO_CHEONJIIN, PhoneOverlay.NONE);
        float x = W - 10;
        float rowOneTop = cji.rowEdge(1, H);
        float rowHeight = cji.rowEdge(1, H) - cji.rowEdge(0, H);
        assertEquals("touch.edit.backspace", at(cji, x, rowOneTop - rowHeight * 0.05f));
        assertEquals("touch.edit.backspace", at(cji, x, rowOneTop - rowHeight * 0.2f));
        assertEquals("touch.edit.backspace", at(cji, x, rowOneTop - rowHeight * 0.6f));
        // The space bar begins where it is drawn, not a third of a row higher.
        assertEquals("touch.text.space", at(cji, x, rowOneTop + rowHeight * 0.1f));
    }

    @Test
    public void backspaceStillGivesItsEdgeToALetter() {
        // The measurement that put the yield there stands where the neighbour types letters: on
        // the 12-key pad ⌫ sits beside ㅏ, and a roll onto ⌫ erased the syllable.
        KeyboardLayout nrg = KeyboardLayouts.phone(KeyboardLayoutId.KO_NARATGEUL, PhoneOverlay.NONE);
        float y = nrg.rowEdge(0, H) + (nrg.rowEdge(1, H) - nrg.rowEdge(0, H)) * 0.5f;
        int index = -1;
        for (int i = 0; i < nrg.rows().get(0).size(); i++) {
            if ("touch.edit.backspace".equals(nrg.rows().get(0).get(i).stableKeyId())) {
                index = i;
            }
        }
        int start = nrg.startColumn(0, index);
        float left = colEdge(nrg, start);
        float cell = colEdge(nrg, start + nrg.rows().get(0).get(index).columnSpan()) - left;
        assertEquals("touch.naratgeul.a", at(nrg, left + cell * 0.1f, y));
        assertEquals("touch.edit.backspace", at(nrg, left + cell * 0.6f, y));
    }

    @Test
    public void theTopEdgeOfEnterIsTheSpaceBar() {
        // The other half of the same report: aiming at the space bar sent Enter. A stray Enter is
        // the most expensive miss on the board — it sends the message, or runs the command — so it
        // gives the top fifth of its cell back to the key above.
        KeyboardLayout nrg = KeyboardLayouts.phone(KeyboardLayoutId.KO_NARATGEUL, PhoneOverlay.NONE);
        float x = W - 10;
        float rowTwoTop = nrg.rowEdge(2, H);
        float rowHeight = nrg.rowEdge(2, H) - nrg.rowEdge(1, H);
        assertEquals("touch.text.space", at(nrg, x, rowTwoTop + rowHeight * 0.1f));
        assertEquals("touch.edit.enter", at(nrg, x, rowTwoTop + rowHeight * 0.5f));
        assertEquals("touch.edit.enter", at(nrg, x, rowTwoTop + rowHeight * 0.9f));
    }

    @Test
    public void theSpellingDecidesOnTheLineBetweenAConsonantAndAVowel() {
        KeyboardLayout ko = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);
        float y = ko.rowEdge(1, H) + 5;                 // ㅁㄴㅇㄹㅎ | ㅗㅓㅏㅣ
        float edge = colEdge(ko, 5);                    // ㅎ ends, ㅗ begins
        float cell = colEdge(ko, 5) - colEdge(ko, 4);
        float justInsideHieuh = edge - cell * 0.1f;
        float justInsideO = edge + cell * 0.1f;
        assertEquals("after ㅎ only a vowel can follow", "touch.ko2.o",
            at(ko, justInsideHieuh, y, JamoExpectation.Kind.VOWEL));
        assertEquals("at the start of a syllable only a consonant can", "touch.ko2.hieuh",
            at(ko, justInsideO, y, JamoExpectation.Kind.CONSONANT));
        assertEquals("with nothing expected the line is where it is drawn", "touch.ko2.hieuh",
            at(ko, justInsideHieuh, y, JamoExpectation.Kind.NONE));
        float wellInside = edge - cell * 0.5f;
        assertEquals("only the band beside the line is decided this way", "touch.ko2.hieuh",
            at(ko, wellInside, y, JamoExpectation.Kind.VOWEL));
    }

    @Test
    public void shiftGivesALittleOfItsEdgeToTheLetterBesideIt() {
        KeyboardLayout ko = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);
        float y = (ko.rowEdge(2, H) + ko.rowEdge(3, H)) / 2f;   // the middle of Shift's row
        float edge = colEdge(ko, 1);                    // Shift is column 0; ㅋ begins at 1
        float cell = colEdge(ko, 1) - colEdge(ko, 0);
        assertEquals("touch.ko2.kieuk", at(ko, edge - cell * 0.1f, y, JamoExpectation.Kind.NONE));
        assertEquals("touch.modifier.shift", at(ko, edge - cell * 0.5f, y, JamoExpectation.Kind.NONE));
    }

    @Test
    public void ordinaryKeysKeepTheirWholeCells() {
        KeyboardLayout ko = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);
        float y = ko.rowEdge(0, H) + 5;
        // ㅂ and ㅈ share an edge; neither is costly, so the edge is exactly where it is drawn.
        float edge = colEdge(ko, 1);
        assertEquals("touch.ko2.bieup", at(ko, edge - 1, y));
        assertEquals("touch.ko2.jieut", at(ko, edge + 1, y));
    }

    private static String at(KeyboardLayout layout, float x, float y, int gapPx) {
        int[] t = TouchTargets.resolve(layout, W, H, x, y, JamoExpectation.Kind.NONE, gapPx);
        return t == null ? null : layout.rows().get(t[0]).get(t[1]).stableKeyId();
    }

    @Test
    public void theGapBetweenTwoKeysBelongsToNeither() {
        // Owner's rule, 2026-10-12: the space between keys is not a key. 6 px is taken from each
        // side of a cell, so 12 px astride a shared edge answer nothing.
        KeyboardLayout ko = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);
        float edge = colEdge(ko, 3);
        float y = ko.rowEdge(0, H) + 25;
        String left = at(ko, edge - 20, y, 0);
        String right = at(ko, edge + 20, y, 0);
        assertEquals(null, at(ko, edge - 5, y, 6));
        assertEquals(null, at(ko, edge + 5, y, 6));
        assertEquals(left, at(ko, edge - 7, y, 6));
        assertEquals(right, at(ko, edge + 6, y, 6));
        // And between two rows.
        float rowEdge = ko.rowEdge(1, H);
        float x = colEdge(ko, 3) + 24;
        assertEquals(null, at(ko, x, rowEdge - 5, 6));
        assertEquals(null, at(ko, x, rowEdge + 5, 6));
        assertEquals(at(ko, x, rowEdge - 20, 0), at(ko, x, rowEdge - 7, 6));
        assertEquals(at(ko, x, rowEdge + 20, 0), at(ko, x, rowEdge + 6, 6));
    }

    @Test
    public void withNoGapEveryPixelIsStillAKey() {
        KeyboardLayout ko = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y += 7) {
                assertEquals(at(ko, x, y), at(ko, x, y, 0));
            }
        }
    }

    @Test
    public void theRimOfTheKeyboardIsNotAGapBetweenKeys() {
        // The margin outside the outermost faces has a key on one side only. A thumb coming in
        // from the bezel lands there, and it still gets the key it was reaching for.
        KeyboardLayout ko = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);
        assertEquals(at(ko, 20, 25, 0), at(ko, 1, 1, 6));
        assertEquals(at(ko, W - 20, 25, 0), at(ko, W - 1, 1, 6));
        assertEquals(at(ko, 20, H - 25, 0), at(ko, 1, H - 1, 6));
        assertEquals(at(ko, W - 20, H - 25, 0), at(ko, W - 1, H - 1, 6));
    }

    @Test
    public void aKeyTwoCellsWideHasNoGapInItsMiddle() {
        KeyboardLayout ko = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);
        int last = ko.rows().size() - 1;
        int index = -1;
        for (int i = 0; i < ko.rows().get(last).size(); i++) {
            if ("touch.text.space".equals(ko.rows().get(last).get(i).stableKeyId())) {
                index = i;
            }
        }
        int start = ko.startColumn(last, index);
        assertEquals(true, ko.rows().get(last).get(index).columnSpan() > 1);
        assertEquals("touch.text.space", at(ko, colEdge(ko, start + 1), H - 25, 6));
        assertEquals("touch.text.space", at(ko, colEdge(ko, start + 1) - 3, H - 25, 6));
    }

    @Test
    public void aGapTooWideForItsKeyStillLeavesHalfTheKeyToTouch() {
        // Cells here are 48 by 50. A 40 px gap would leave nothing; it stops at a quarter of the
        // cell, exactly as the drawing does, so what is drawn is what answers.
        KeyboardLayout ko = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);
        float left = colEdge(ko, 3);
        assertEquals(null, at(ko, left + 11, 25, 40));
        assertEquals(at(ko, left + 24, 25, 0), at(ko, left + 12, 25, 40));
        assertEquals(at(ko, left + 24, 25, 0), at(ko, left + 24, 25, 40));
    }

    @Test
    public void aCostlyKeyStillGivesItsEdgeOnceTheGapIsCrossed() {
        KeyboardLayout cji = KeyboardLayouts.phone(KeyboardLayoutId.KO_CHEONJIIN, PhoneOverlay.NONE);
        float boundary = colEdge(cji, 2);
        float cell = colEdge(cji, 2) - colEdge(cji, 1);
        float y = cji.rowEdge(0, H) + 25;
        assertEquals(null, at(cji, boundary - 3, y, 6));
        assertEquals("touch.cheonjiin.i", at(cji, boundary - cell * 0.2f, y, 6));
        assertEquals("touch.phone.overlay.digits", at(cji, boundary - cell * 0.6f, y, 6));
    }
}
