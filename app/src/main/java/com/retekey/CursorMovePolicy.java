package com.retekey;

/**
 * Decides when a selection report means the user moved the cursor away from the syllable being
 * composed — a tap, a mouse click, a drag — so the composition must be finished where it is and
 * the composer reset. Without this the composer keeps its half-typed syllable, and the next key
 * paints that whole stale syllable at the new cursor through {@code setComposingText}.
 *
 * <p>The only signal used is the composing region the editor itself reports
 * ({@code candidatesStart..candidatesEnd}) against the new selection. Predictions are deliberately
 * not consulted: judging "was this movement ours?" by predicted positions mis-fired on real
 * editors and thrashed the composer once before (manual §15.27 tells that story). The editor's
 * own composing span cannot mis-fire that way: while the keyboard is composing, its own edits
 * always leave the cursor at the end of that span, and reports without a span (a commit's
 * intermediate state, editors that never report one) are left alone.
 *
 * <p>Platform-neutral and stateless, so the rule is unit-tested directly.
 */
public final class CursorMovePolicy {
    private CursorMovePolicy() {
    }

    /**
     * Whether to finish the composition in place and reset the composer. True only when the
     * keyboard is composing, the editor reports a composing region, and the new selection lies
     * (even partly) outside it — the one shape our own edits never produce.
     */
    public static boolean shouldAbandonComposition(
        boolean composing,
        int newSelStart,
        int newSelEnd,
        int candidatesStart,
        int candidatesEnd
    ) {
        if (!composing) {
            return false;
        }
        if (newSelStart < 0 || newSelEnd < 0) {
            // An editor that cannot say where its cursor is cannot say the user moved it.
            return false;
        }
        if (candidatesStart < 0 || candidatesEnd < candidatesStart) {
            // No composing region reported: either a commit's intermediate state or an editor
            // that never reports one. Resetting here would break composition per keystroke.
            return false;
        }
        return newSelStart < candidatesStart || newSelStart > candidatesEnd
            || newSelEnd < candidatesStart || newSelEnd > candidatesEnd;
    }

    /**
     * The same verdict for editors that never report a composing region (Compose text fields —
     * Google Keep among them — pass -1 even mid-composition). With no region to test against, the
     * evidence is the text itself: while the keyboard is composing, its own edits always leave
     * the cursor immediately after the preedit, so the characters before the cursor must read as
     * the preedit. When they do not — or when the user has selected a range, which composing
     * never does — the cursor has moved and the stale composition must be settled where it is,
     * or the next keystroke would repaint it at the old spot and drag the cursor back there.
     *
     * @param textBeforeCursor what the editor says sits before the cursor (up to the preedit's
     *     length); null when it cannot say, which leaves the composition alone.
     */
    public static boolean shouldAbandonWithoutRegion(
        boolean composing,
        int newSelStart,
        int newSelEnd,
        String preedit,
        CharSequence textBeforeCursor
    ) {
        if (!composing || preedit == null || preedit.isEmpty()) {
            return false;
        }
        if (newSelStart < 0 || newSelEnd < 0) {
            return false;
        }
        if (newSelStart != newSelEnd) {
            return true;
        }
        if (textBeforeCursor == null) {
            return false;
        }
        return !preedit.contentEquals(textBeforeCursor);
    }
}
