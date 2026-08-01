package dev.hellgates.retekeyime;

/**
 * Which way a finger went, for keys that answer to a drag as well as a tap.
 *
 * <p>A drag is judged the moment it passes the threshold, not when the finger lifts, because the
 * whole point of dragging instead of tapping twice is that it costs no waiting. The larger of the
 * two distances wins, so a drag that is mostly sideways is sideways even when it wanders.
 *
 * <p>Android-free, so the rule is a unit test rather than a thing to try with a thumb.
 */
public final class FlickDirection {
    private FlickDirection() {
    }

    /**
     * The direction {@code (dx, dy)} amounts to, or null when the finger has not gone far enough
     * to mean anything. Screen coordinates: y grows downwards.
     */
    public static CheonjiinInterpreter.Flick of(float dx, float dy, int threshold) {
        float horizontal = Math.abs(dx);
        float vertical = Math.abs(dy);
        if (horizontal < threshold && vertical < threshold) {
            return null;
        }
        if (horizontal >= vertical) {
            return dx < 0 ? CheonjiinInterpreter.Flick.LEFT : CheonjiinInterpreter.Flick.RIGHT;
        }
        return dy < 0 ? CheonjiinInterpreter.Flick.UP : CheonjiinInterpreter.Flick.DOWN;
    }
}
