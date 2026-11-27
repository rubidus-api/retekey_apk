package com.retekey;

/**
 * Which ink a label needs to stay readable on the colour behind it.
 *
 * <p>A latched key is painted in a strong colour so it can be told apart at a glance, and a strong
 * colour is dark in one theme and vivid in the other. Deciding the label's colour by hand for each
 * one is how a key ends up with dark text on a dark fill; deciding it from the fill's own
 * luminance cannot drift, and is the same rule in both themes.
 *
 * <p>Android-free on purpose: it takes channels, not a packed colour, so it is testable without a
 * device.
 */
public final class KeyLabelContrast {
    /**
     * Above this relative luminance a fill is light enough that dark ink beats light ink on it.
     * 0.179 is where the two win equally against sRGB black and white; it is not a taste setting,
     * and moving it by eye is how one of the four latch colours ends up unreadable.
     */
    private static final double DARK_INK_ABOVE = 0.179;

    private KeyLabelContrast() {
    }

    /** Whether a label on this fill should be drawn in the dark ink rather than the light one. */
    public static boolean prefersDarkInk(int red, int green, int blue) {
        return relativeLuminance(red, green, blue) > DARK_INK_ABOVE;
    }

    /**
     * Whether the inverse ink (used on contrasting accent fills) must be dark: true when the
     * ordinary text is light (so the inverse is dark), false when the ordinary text is dark.
     */
    public static boolean inversePrefersDarkInk(int textRed, int textGreen, int textBlue) {
        return prefersDarkInk(textRed, textGreen, textBlue);
    }

    /**
     * Whether a label on a fill should switch from ordinary text ink to inverse ink to stay
     * readable. Both share the same dark/light polarity precisely when the fill needs the
     * opposite ink from ordinary text.
     */
    public static boolean useInverseInk(
        int fillRed, int fillGreen, int fillBlue,
        int textRed, int textGreen, int textBlue
    ) {
        return prefersDarkInk(fillRed, fillGreen, fillBlue)
            == prefersDarkInk(textRed, textGreen, textBlue);
    }

    /**
     * Perceived brightness, 0 (black) to 1 (white), weighted the way the eye weights the channels:
     * green carries most of it, blue almost none.
     */
    public static double relativeLuminance(int red, int green, int blue) {
        return (0.2126 * linear(red) + 0.7152 * linear(green) + 0.0722 * linear(blue));
    }

    /** Undoes the sRGB transfer curve, so the weighting above is applied to light, not to bytes. */
    private static double linear(int channel) {
        if (channel < 0 || channel > 255) {
            throw new IllegalArgumentException("channel out of range: " + channel);
        }
        double value = channel / 255.0;
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
