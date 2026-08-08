package com.retekey;

/**
 * Where a row of buttons breaks when it runs out of width.
 *
 * <p>The notepad's links are one row when they fit and several when they do not, which is a
 * decision about arithmetic rather than about views — so it is made here, where it can be tested
 * without a device.
 */
public final class FlowRowMath {
    private FlowRowMath() {
    }

    /**
     * The line each item lands on, given the width of every item and the width to fill. An item
     * wider than the whole line gets a line to itself rather than being dropped.
     */
    public static int[] lines(int[] widths, int available) {
        if (widths == null) {
            throw new IllegalArgumentException("widths must not be null");
        }
        int[] lines = new int[widths.length];
        int line = 0;
        int used = 0;
        for (int i = 0; i < widths.length; i++) {
            int width = Math.max(0, widths[i]);
            if (used > 0 && used + width > available) {
                line++;
                used = 0;
            }
            lines[i] = line;
            used += width;
        }
        return lines;
    }

    /** How many lines {@link #lines} needs. Zero items still occupy one empty line. */
    public static int lineCount(int[] widths, int available) {
        int[] lines = lines(widths, available);
        return lines.length == 0 ? 1 : lines[lines.length - 1] + 1;
    }
}
