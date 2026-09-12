package com.retekey;

/**
 * What the composer may keep believing about the screen after a plan has been executed, where
 * composition is materialised as committed text — a terminal, a remote-desktop window.
 *
 * <p>Those editors get only the difference: a syllable grows by taking back what was written and
 * committing the longer form. That works while the keyboard's record of what it wrote is true. A
 * plan the editor refused, or one that reached nothing, makes it false — and the belief survives,
 * because nothing repaints a whole preedit here the way {@code setComposingText} does in an
 * ordinary field. The next syllable then takes back characters it never wrote, which is the
 * shape of 바다가 arriving as 받닥다.
 *
 * <p>So after anything but a clean dispatch, the record is dropped: the syllable keeps being
 * typed, but the next update commits it whole and deletes nothing. Deleting text this keyboard
 * is not sure it wrote is the one outcome worth ruling out.
 */
public final class MaterializedCompositionPolicy {
    private MaterializedCompositionPolicy() {
    }

    /**
     * @param materialises the editor takes composition as commits (no composing region)
     * @param outcome      what the executor reported, or null when nothing ran at all
     */
    public static boolean shouldForgetWhatWasWritten(
        boolean materialises, ExecutionResult.Outcome outcome) {
        if (!materialises) {
            return false;
        }
        if (outcome == null) {
            return true;
        }
        switch (outcome) {
            case DISPATCHED:
                return false;
            case NO_EDITOR_ACTIONS:
                // Nothing was asked of the editor, so nothing changed and the record still holds.
                // Forgetting here would be worse than keeping it: the next syllable would commit
                // itself a second time beside the one already on screen.
                return false;
            default:
                // NOT_DISPATCHED, UNCERTAIN, CONFIRMED_NO_EFFECT: the screen did not move the way
                // the plan said it would, so what this keyboard believes it wrote is not evidence.
                return true;
        }
    }
}
