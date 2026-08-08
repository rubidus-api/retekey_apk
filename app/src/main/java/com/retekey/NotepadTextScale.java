package com.retekey;

/**
 * How large the notepad's text is, as a multiplier the user sets by pinching.
 *
 * <p>Kept as a whole percentage so it stores as an int and comes back exactly as it was left; the
 * bounds are what stays legible at one end and what still fits a word on a phone at the other.
 */
public final class NotepadTextScale {
    public static final int MIN_PERCENT = 60;
    public static final int MAX_PERCENT = 300;
    public static final int DEFAULT_PERCENT = 100;

    private NotepadTextScale() {
    }

    public static int clamp(int percent) {
        return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, percent));
    }

    /**
     * The percentage a pinch lands on: the one it started from, times the gesture's own factor.
     * Rounded rather than truncated, so a pinch that ends where it began does not drift downward.
     */
    public static int scaled(int startPercent, float factor) {
        if (factor <= 0f || Float.isNaN(factor) || Float.isInfinite(factor)) {
            return clamp(startPercent);
        }
        return clamp(Math.round(clamp(startPercent) * factor));
    }

    /** The multiplier to paint with. */
    public static float multiplier(int percent) {
        return clamp(percent) / 100.0f;
    }

    /** A size in sp/dp at this scale, never rounded away to nothing. */
    public static float sizeOf(float base, int percent) {
        return Math.max(1.0f, base * multiplier(percent));
    }
}
