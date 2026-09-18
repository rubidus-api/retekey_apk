package com.retekey;

/**
 * The box over the keyboard that shows what was just typed: whether it is drawn at all, and how
 * much of the keyboard shows through it.
 *
 * <p>It exists because a finger covers the key it is pressing, so the result has to appear
 * somewhere else. But it is a box over the keys, and some people would rather see the keys — and
 * on a small screen it can sit exactly where the next key is. So it is a setting, and its opacity
 * is a setting too: at less than full, the row underneath reads through it (owner's request,
 * 2026-09-18).
 *
 * <p>Plain arithmetic with no Android types, so the clamping is unit-tested.
 */
final class EchoBoxSettings {
    static final String KEY_ENABLED = "echo_box_enabled";
    static final String KEY_OPACITY = "echo_box_opacity";

    /** On by default: it is what makes a keystroke readable under the finger that made it. */
    static final boolean DEFAULT_ENABLED = true;
    /** Solid enough to read at a glance, and not so solid that the keys vanish behind it. */
    static final int DEFAULT_OPACITY = 85;
    /** Below this the box is a ghost and the character cannot be read; above it is opaque. */
    static final int MIN_OPACITY = 20;
    static final int MAX_OPACITY = 100;

    private EchoBoxSettings() {
    }

    static int clampOpacity(int percent) {
        return Math.max(MIN_OPACITY, Math.min(MAX_OPACITY, percent));
    }

    /** The alpha channel (0–255) for a stored percentage. */
    static int alphaOf(int percent) {
        return Math.round(clampOpacity(percent) * 255.0f / 100.0f);
    }

    /** {@code colour} with the box's alpha, keeping its red, green and blue. */
    static int withOpacity(int colour, int percent) {
        return (alphaOf(percent) << 24) | (colour & 0x00FFFFFF);
    }

    /**
     * The alpha for the character inside the box, which is not the box's own.
     *
     * <p>The point of a see-through box is to read the keys underneath it; the point of the box is
     * to read the character. Fading the character with the background makes it unreadable at the
     * settings that make the keys readable (measured on the emulator at 35 %), so the ink keeps at
     * least {@link #MIN_INK_OPACITY} of itself however faint the box is.
     */
    static final int MIN_INK_OPACITY = 80;

    static int inkOpacity(int percent) {
        return Math.max(MIN_INK_OPACITY, clampOpacity(percent));
    }

    static int inkWithOpacity(int colour, int percent) {
        return withOpacity(colour, inkOpacity(percent));
    }
}
