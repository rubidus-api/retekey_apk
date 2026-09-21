package com.retekey;

/**
 * Replaces text just before the cursor — the kana ゛゜小 key turning か into が, a Hanja pick
 * turning 한자 into 漢字 — as one batch: delete, then commit. The delete is a prerequisite: if the
 * editor refused it or threw, the commit is not sent, since committing anyway doubles the text
 * (か becomes かが). The batch is closed whatever happened, and the outcome says whether the
 * editor may have changed, so a caller never reports an undone replacement as done
 * (review finding R02).
 */
final class TextReplacement {
    enum Outcome {
        /** Deleted (if asked to) and committed, in a batch that closed. */
        REPLACED,
        /** The editor refused before anything could have changed. */
        NOTHING_WRITTEN,
        /** Something may have been written, or not: the editor threw or refused midway. */
        UNCERTAIN
    }

    private TextReplacement() {
    }

    /**
     * Deletes {@code deleteBeforeUtf16} units before the cursor (none for a selection, which the
     * commit replaces), then commits {@code text}.
     */
    static Outcome replace(EditorBridge bridge, int deleteBeforeUtf16, String text) {
        Outcome outcome = Outcome.UNCERTAIN;
        try {
            outcome = writeInsideTheBatch(bridge, deleteBeforeUtf16, text);
        } finally {
            EditorCallResult end = call(bridge::endBatchEdit);
            if (outcome == Outcome.REPLACED && !end.isSucceeded()) {
                // The writes went out, but a batch that did not close is not a clean one.
                outcome = Outcome.UNCERTAIN;
            }
        }
        return outcome;
    }

    private static Outcome writeInsideTheBatch(
        EditorBridge bridge,
        int deleteBeforeUtf16,
        String text
    ) {
        EditorCallResult begin = call(bridge::beginBatchEdit);
        if (!begin.isSucceeded()) {
            // A refused batch means the connection is gone; a throwing one says nothing.
            return begin.isRejected() ? Outcome.NOTHING_WRITTEN : Outcome.UNCERTAIN;
        }
        boolean deleted = false;
        if (deleteBeforeUtf16 > 0) {
            EditorCallResult delete =
                call(() -> bridge.deleteSurroundingText(deleteBeforeUtf16, 0));
            if (!delete.isSucceeded()) {
                return delete.isRejected() ? Outcome.NOTHING_WRITTEN : Outcome.UNCERTAIN;
            }
            deleted = true;
        }
        EditorCallResult commit = call(() -> bridge.commitText(text, 1));
        if (!commit.isSucceeded()) {
            return deleted || !commit.isRejected() ? Outcome.UNCERTAIN : Outcome.NOTHING_WRITTEN;
        }
        return Outcome.REPLACED;
    }

    /**
     * Whether {@code source} is still exactly the text before the cursor — what a Hanja pick
     * checks before replacing it, so a pick made after the cursor moved does not delete the
     * characters in front of it instead. An editor that cannot say does not confirm it.
     */
    static boolean stillBeforeCursor(EditorBridge bridge, String source) {
        if (source == null || source.isEmpty()) {
            return false;
        }
        EditorTextResult before;
        try {
            before = bridge.getTextBeforeCursor(source.length(), 0);
        } catch (RuntimeException unavailable) {
            return false;
        }
        return before != null && before.hasValue() && source.equals(before.value());
    }

    private static EditorCallResult call(Fn.Supplier<EditorCallResult> call) {
        try {
            EditorCallResult result = call.get();
            return result == null ? EditorCallResult.runtimeFailure() : result;
        } catch (RuntimeException failed) {
            return EditorCallResult.runtimeFailure();
        }
    }
}
