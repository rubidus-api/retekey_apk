package com.retekey;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Telling our own echoes from a cursor the user moved, in a materialised editor (issue #7). */
public final class MaterializedCursorMovesTest {
    @Test
    public void nothingIsJudgedBeforeTheFirstExpectation() {
        MaterializedCursorMoves moves = new MaterializedCursorMoves();
        assertFalse("no writes yet: the report is the anchor", moves.isForeignMove(7, 7));
    }

    @Test
    public void anEchoOfOurOwnWriteIsNotAMove() {
        MaterializedCursorMoves moves = new MaterializedCursorMoves();
        moves.expect(1);
        assertFalse(moves.isForeignMove(1, 1));
    }

    @Test
    public void aLateEchoIsStillAnEcho() {
        // Two keys typed before the first report comes back — the shape that made the old rule
        // reset the composer mid-syllable (일 arriving as 이ㄹ).
        MaterializedCursorMoves moves = new MaterializedCursorMoves();
        moves.expect(1);
        moves.expect(2);
        assertFalse("the report for the first write", moves.isForeignMove(1, 1));
        assertFalse("and then the second", moves.isForeignMove(2, 2));
    }

    @Test
    public void aJumpNobodyAskedForIsAMove() {
        // Measured against the Microsoft remote-desktop client: a click on the remote screen sends
        // its buffer back to 0 while our writing had it at 2.
        MaterializedCursorMoves moves = new MaterializedCursorMoves();
        moves.expect(1);
        moves.expect(2);
        assertTrue(moves.isForeignMove(0, 0));
    }

    @Test
    public void aSelectedRangeIsAlwaysTheUser() {
        MaterializedCursorMoves moves = new MaterializedCursorMoves();
        moves.expect(3);
        assertTrue(moves.isForeignMove(1, 3));
    }

    @Test
    public void anUnpredictableWriteSuspendsJudgementUntilAReportReAnchors() {
        // Enter, a raw key: the editor decides where the cursor lands, and the client's buffer is
        // flushed. Guessing there would split syllables for nothing.
        MaterializedCursorMoves moves = new MaterializedCursorMoves();
        moves.expect(2);
        moves.expect(-1);
        assertFalse("the flush's own report", moves.isForeignMove(0, 0));
        moves.expect(1);
        assertFalse(moves.isForeignMove(1, 1));
        assertTrue("and judgement is back", moves.isForeignMove(9, 9));
    }

    @Test
    public void theQueueCannotGrowWithoutBound() {
        MaterializedCursorMoves moves = new MaterializedCursorMoves();
        for (int i = 1; i <= 40; i++) {
            moves.expect(i);
        }
        assertFalse("the newest expectation still holds", moves.isForeignMove(40, 40));
    }

    /**
     * The owner's report (2026-09-23): typing 자모통 into a remote desktop sometimes came out
     * 자ㅁㅗ통 — the jamo of one syllable committed separately. Rewriting a syllable is two writes
     * on the wire, a delete and a commit, and the client's own buffer reports where its cursor
     * went after each of them. Only the end of the plan was expected, so the report from the
     * delete matched nothing, was read as the user moving the cursor, and settled the syllable
     * where it stood — after which the next jamo could not take it back.
     */
    @Test
    public void theStepsOfARewriteAreEchoesToo() {
        MaterializedCursorMoves moves = new MaterializedCursorMoves();
        moves.expect(2);
        assertFalse(moves.isForeignMove(2, 2));

        // ㅗ replaces ㅁ: delete one (the buffer goes back to 1), commit 모 (forward to 2).
        moves.expect(1);
        moves.expect(2);
        assertFalse("the delete's own report", moves.isForeignMove(1, 1));
        assertFalse("and the commit's", moves.isForeignMove(2, 2));
    }

    /** The same report twice — the client repeating itself — is not the user doing anything. */
    @Test
    public void aRepeatOfTheLastReportIsNotAMove() {
        MaterializedCursorMoves moves = new MaterializedCursorMoves();
        moves.expect(1);
        moves.expect(2);
        assertFalse(moves.isForeignMove(1, 1));
        assertFalse(moves.isForeignMove(2, 2));
        assertFalse("the client reported where it already was", moves.isForeignMove(2, 2));
        assertTrue("but a jump from there is still the user", moves.isForeignMove(0, 0));
    }

    /** A report of where we were before the write in flight is an echo on its way, not a move. */
    @Test
    public void aReportFromBeforeTheWriteInFlightIsNotAMove() {
        MaterializedCursorMoves moves = new MaterializedCursorMoves();
        moves.expect(3);
        assertFalse(moves.isForeignMove(3, 3));
        moves.expect(4);
        assertFalse("the buffer had not caught up yet", moves.isForeignMove(3, 3));
        assertFalse(moves.isForeignMove(4, 4));
    }
}
