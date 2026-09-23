package com.retekey;

/**
 * A key that can be held without a finger on it. A tap arms it for exactly one key; holding it
 * toggles a persistent lock on or off; a tap while locked clears the lock, because the way out of
 * a lock should be the easiest thing to find.
 *
 * <p>Shift has worked this way for a long time. Ctrl, Meta and Alt are the same idea — the state
 * is view-local and never reaches the dispatcher, and what differs between them is only which
 * modifier the armed state contributes to a chord.
 */
public final class LatchState {
    public enum State {
        OFF,
        ONE_SHOT,
        LOCKED
    }

    private State state = State.OFF;

    public State state() {
        return state;
    }

    /** A tap: off arms one-shot, one-shot cancels, a lock clears. */
    public void tap() {
        state = state == State.OFF ? State.ONE_SHOT : State.OFF;
    }

    /** A hold: toggles the persistent lock. */
    public void toggleLock() {
        state = state == State.LOCKED ? State.OFF : State.LOCKED;
    }

    /** Consumes an armed one-shot after a key press. Returns true when the state changed. */
    public boolean consumeOneShot() {
        if (state != State.ONE_SHOT) {
            return false;
        }
        state = State.OFF;
        return true;
    }

    public void clear() {
        state = State.OFF;
    }

    /**
     * Puts the latch back the way it was before a press that was then canceled — a finger that
     * slid off the keyboard, a view rebuilt under it. A tap cannot undo a tap: it arms what was
     * off and clears what was locked, so the state before the press is what is restored
     * (review finding R11).
     */
    public void restore(State previous) {
        state = previous == null ? State.OFF : previous;
    }

    public boolean isActive() {
        return state != State.OFF;
    }

    public boolean isLocked() {
        return state == State.LOCKED;
    }
}
