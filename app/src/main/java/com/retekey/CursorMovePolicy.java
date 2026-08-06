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
}
