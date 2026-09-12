package com.retekey;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tells this keyboard's own echoes from a cursor the user moved, in an editor that materialises
 * composition as committed text — a remote-desktop window (issue #7, the owner's report).
 *
 * <p>Those editors hold a dummy buffer that reports selection changes, and the reports were
 * ignored outright: judging a cursor move by them reset the composer mid-syllable and 일 arrived
 * as 이ㄹ (64f1b60). Measured against the Microsoft client on 2026-09-13, the reports are in fact
 * readable — while typing they step forward with each commit and never move on their own, not even
 * after twelve seconds of nothing; a click on the remote screen sends the buffer back to 0, which
 * no key of ours did.
 *
 * <p>So the rule is the one the platform's own keyboard uses: remember where each write should
 * leave the cursor, and match every report against those expectations rather than against the
 * present. A report that matches one is an echo, however late it arrives — which is the part that
 * misfired before, since two keys can be typed before the first report lands. A report that
 * matches nothing is the user, and the syllable must be settled where it stands.
 *
 * <p>Writes whose outcome cannot be predicted — a raw key, Enter, an editor action, anything the
 * editor answers for — clear the expectations instead, and until a report re-anchors them nothing
 * is judged a move. Being wrong that way costs nothing; being wrong the other way splits syllables.
 */
public final class MaterializedCursorMoves {
    /** Enough for the keys a hand can type before the first report comes back. */
    private static final int MAX_PENDING = 8;

    private final Deque<Integer> expected = new ArrayDeque<>();
    private boolean anchored;

    /** Session boundary: nothing is expected and nothing is judged until a report arrives. */
    public void reset() {
        expected.clear();
        anchored = false;
    }

    /**
     * Records where a dispatched write should leave the cursor.
     *
     * @param cursor the predicted cursor position, or a negative number when the write's outcome
     *     is the editor's to decide (a raw key, Enter), which drops every expectation
     */
    public void expect(int cursor) {
        if (cursor < 0) {
            expected.clear();
            anchored = false;
            return;
        }
        if (expected.size() >= MAX_PENDING) {
            expected.removeFirst();
        }
        expected.addLast(cursor);
        anchored = true;
    }

    /**
     * Whether this report is a cursor the user moved rather than an echo of our own writing.
     * Matching entries, and everything older, are consumed: reports arrive in order.
     */
    public boolean isForeignMove(int newSelStart, int newSelEnd) {
        if (!anchored) {
            // Nothing to compare against yet: adopt the report as the new anchor.
            expected.clear();
            expected.addLast(newSelStart);
            anchored = newSelStart == newSelEnd;
            return false;
        }
        if (newSelStart != newSelEnd) {
            // Composing never selects a range, so a range is the user's doing.
            expected.clear();
            anchored = false;
            return true;
        }
        boolean matched = false;
        while (!expected.isEmpty()) {
            int candidate = expected.removeFirst();
            if (candidate == newSelStart) {
                matched = true;
                break;
            }
        }
        if (matched) {
            return false;
        }
        // No expectation left to explain it: the far side moved. Re-anchor on what it reports.
        expected.clear();
        expected.addLast(newSelStart);
        anchored = true;
        return true;
    }
}
