package com.retekey;

/**
 * When a finger already on a key has moved far enough to belong to a different one.
 *
 * <p>A finger is never still. It rolls as it presses, and a tap that lands a pixel from a key's
 * edge would flip to the neighbour and back if the keyboard simply took whatever key was under the
 * finger at each move. So a touch keeps the key it started on until it is a whole touch slop
 * <em>clear</em> of that key's cell — hysteresis, not a boundary. Below that it is the same tap;
 * beyond it the finger has genuinely gone somewhere else and should take the key it went to.
 *
 * <p>Android-free, so the rule can be tested without a device.
 */
public final class TouchTargeting {
    private TouchTargeting() {
    }

    /** Whether {@code (x, y)} has left the cell {@code [left, right) × [top, bottom)} by {@code slop}. */
    public static boolean escaped(
            float x, float y, int left, int top, int right, int bottom, int slop) {
        return x < left - slop || x > right + slop || y < top - slop || y > bottom + slop;
    }
}
