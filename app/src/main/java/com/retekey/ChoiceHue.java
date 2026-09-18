package com.retekey;

/**
 * The colour of choosing, which must not be the colour of pressing.
 *
 * <p>A held key is painted in the accent; so was the strip of alternates a hold raises, and so was
 * the echo box over it. Three different things in one colour is one thing too many: with a finger
 * down and a strip up, nothing on screen said which of them was the choice being made (owner's
 * request, 2026-09-18). Choosing is now a hue of its own — the accent turned two-thirds of the way
 * round the circle, which keeps the theme's own saturation and lightness (so it stays legible in
 * light and dark alike, and follows a Material You palette when the device sets one) while landing
 * somewhere the eye reads as a different colour rather than a different shade.
 *
 * <p>Packed-ARGB arithmetic with no Android types, so the conversion is unit-tested.
 */
final class ChoiceHue {
    /** How far round the hue circle the choosing colour sits from the accent, in degrees. */
    static final float TURN_DEGREES = 210.0f;

    private ChoiceHue() {
    }

    /** The accent, turned to the choosing hue. Alpha is carried through. */
    static int of(int accent) {
        return turn(accent, TURN_DEGREES);
    }

    /** A soft version for the cells that are offered but not aimed at: the hue over the face. */
    static int softOf(int accent, int face) {
        return KeyPressTint.mix(face, of(accent), 0.30f);
    }

    static int turn(int colour, float degrees) {
        int alpha = (colour >>> 24) & 0xFF;
        float r = ((colour >> 16) & 0xFF) / 255.0f;
        float g = ((colour >> 8) & 0xFF) / 255.0f;
        float b = (colour & 0xFF) / 255.0f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue;
        if (delta == 0.0f) {
            // A grey has no hue to turn; give the choosing colour the turn's own hue so that a
            // greyscale theme still says "choosing" with something other than a shade.
            hue = degrees;
        } else if (max == r) {
            hue = 60.0f * (((g - b) / delta) % 6.0f);
        } else if (max == g) {
            hue = 60.0f * (((b - r) / delta) + 2.0f);
        } else {
            hue = 60.0f * (((r - g) / delta) + 4.0f);
        }
        hue = (hue + degrees) % 360.0f;
        if (hue < 0.0f) {
            hue += 360.0f;
        }
        float saturation = max == 0.0f ? 0.0f : delta / max;
        return (alpha << 24) | rgbOf(hue, delta == 0.0f ? 0.45f : saturation, max);
    }

    /** HSV back to packed RGB, the usual sextant walk. */
    private static int rgbOf(float hue, float saturation, float value) {
        float chroma = value * saturation;
        float second = chroma * (1.0f - Math.abs(((hue / 60.0f) % 2.0f) - 1.0f));
        float match = value - chroma;
        float r;
        float g;
        float b;
        if (hue < 60.0f) {
            r = chroma; g = second; b = 0.0f;
        } else if (hue < 120.0f) {
            r = second; g = chroma; b = 0.0f;
        } else if (hue < 180.0f) {
            r = 0.0f; g = chroma; b = second;
        } else if (hue < 240.0f) {
            r = 0.0f; g = second; b = chroma;
        } else if (hue < 300.0f) {
            r = second; g = 0.0f; b = chroma;
        } else {
            r = chroma; g = 0.0f; b = second;
        }
        return (channel(r + match) << 16) | (channel(g + match) << 8) | channel(b + match);
    }

    private static int channel(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0f)));
    }
}
