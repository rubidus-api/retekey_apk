package dev.hellgates.retekeyime;

/**
 * Shift state. A tap arms it for exactly one key; holding it toggles a persistent lock on or off.
 * It is view-local and never reaches the dispatcher.
 */
public final class ShiftLayerState {
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

    /** Consumes an armed one-shot after a key press. Returns true when the layer changed. */
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

    public boolean isActive() {
        return state != State.OFF;
    }

    public boolean isLocked() {
        return state == State.LOCKED;
    }
}
