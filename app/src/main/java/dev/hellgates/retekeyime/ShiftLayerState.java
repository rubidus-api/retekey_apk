package dev.hellgates.retekeyime;

/**
 * Shift follows the RFC-0002 rule: a tap on inactive Shift arms it for one key, a tap while armed
 * makes it sticky, and a tap while sticky clears it. It is view-local state and never reaches the
 * dispatcher.
 */
public final class ShiftLayerState {
    public enum State {
        OFF,
        ONE_SHOT,
        STICKY
    }

    private State state = State.OFF;

    public State state() {
        return state;
    }

    public void advance() {
        switch (state) {
            case OFF:
                state = State.ONE_SHOT;
                break;
            case ONE_SHOT:
                state = State.STICKY;
                break;
            default:
                state = State.OFF;
                break;
        }
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

    public boolean isSticky() {
        return state == State.STICKY;
    }
}
