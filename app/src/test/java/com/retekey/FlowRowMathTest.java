package com.retekey;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class FlowRowMathTest {
    @Test
    public void everythingOnOneLineWhenItFits() {
        int[] widths = {30, 40, 20};
        assertArrayEquals(new int[] {0, 0, 0}, FlowRowMath.lines(widths, 100));
        assertEquals(1, FlowRowMath.lineCount(widths, 100));
    }

    @Test
    public void itWrapsWhenTheNextOneWouldNotFit() {
        int[] widths = {60, 60, 30};
        assertArrayEquals(new int[] {0, 1, 1}, FlowRowMath.lines(widths, 100));
        assertEquals(2, FlowRowMath.lineCount(widths, 100));
    }

    @Test
    public void anItemWiderThanTheLineGetsItsOwn() {
        int[] widths = {40, 200, 40};
        assertArrayEquals(new int[] {0, 1, 2}, FlowRowMath.lines(widths, 100));
    }

    @Test
    public void anExactFitDoesNotWrap() {
        assertArrayEquals(new int[] {0, 0}, FlowRowMath.lines(new int[] {50, 50}, 100));
    }

    @Test
    public void nothingStillOccupiesALine() {
        assertEquals(1, FlowRowMath.lineCount(new int[0], 100));
        assertArrayEquals(new int[0], FlowRowMath.lines(new int[0], 100));
    }
}
