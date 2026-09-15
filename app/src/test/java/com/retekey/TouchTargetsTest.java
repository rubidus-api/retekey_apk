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
    public void ordinaryKeysKeepTheirWholeCells() {
        KeyboardLayout ko = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, false);
        float y = ko.rowEdge(0, H) + 5;
        // ㅂ and ㅈ share an edge; neither is costly, so the edge is exactly where it is drawn.
        float edge = colEdge(ko, 1);
        assertEquals("touch.ko2.bieup", at(ko, edge - 1, y));
        assertEquals("touch.ko2.jieut", at(ko, edge + 1, y));
    }
}
