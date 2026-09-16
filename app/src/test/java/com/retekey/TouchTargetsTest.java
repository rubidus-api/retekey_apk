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
    public void theLowerEdgeOfBackspaceIsTheSpaceBar() {
        KeyboardLayout cji = KeyboardLayouts.phone(KeyboardLayoutId.KO_CHEONJIIN, PhoneOverlay.NONE);
        float x = W - 10;
        float rowOneTop = cji.rowEdge(1, H);
        float rowHeight = cji.rowEdge(1, H) - cji.rowEdge(0, H);
        assertEquals("touch.text.space", at(cji, x, rowOneTop - rowHeight * 0.2f));
        assertEquals("touch.edit.backspace", at(cji, x, rowOneTop - rowHeight * 0.6f));
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
}
