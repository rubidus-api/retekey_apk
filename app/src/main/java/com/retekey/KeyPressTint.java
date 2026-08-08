package com.retekey;

/**
 * The colour a key's face takes while a finger is on it: the same key, brighter.
 *
 * <p>Brightening alone is not enough on its own. In a light theme the face is already close to
 * white, and lifting it further changes almost nothing; in a dark theme lifting it is exactly what
 * reads as "pressed". So the pressed face is the fill moved toward white <em>and</em> a little way
 * toward the accent, which shows in both — brighter where there is room to be brighter, and tinted
 * where there is not.
 *
 * <p>Plain integer arithmetic over packed ARGB, with no Android types, so it is unit-tested on the
 * JVM. Alpha is carried through from the fill.
 */
final class KeyPressTint {
    /** How far the pressed face moves toward white. */
    static final float TOWARD_WHITE = 0.28f;
    /**
     * How far it moves toward the accent. Well past a tint: a press has to be unmistakable at a
     * glance on a key the finger is sitting on top of, and half the accent is what reads that way
     * on both a pale face and a dark one.
     */
    static final float TOWARD_ACCENT = 0.42f;

    private KeyPressTint() {
    }

    /** The face to paint while the key is held down. */
    static int pressed(int fill, int accent) {
        int lifted = mix(fill, 0xFFFFFFFF, TOWARD_WHITE);
        return mix(lifted, accent, TOWARD_ACCENT);
    }

    /** {@code from} moved {@code amount} of the way toward {@code to}, keeping from's alpha. */
    static int mix(int from, int to, float amount) {
        float t = Math.max(0.0f, Math.min(1.0f, amount));
        int a = (from >>> 24) & 0xFF;
        int r = channel((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = channel((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = channel(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int channel(int from, int to, float t) {
        return Math.round(from + (to - from) * t);
    }
}
