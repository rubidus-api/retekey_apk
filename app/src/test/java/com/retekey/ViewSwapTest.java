package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

/** RFC-0014 F1: a swap requested from inside a swap runs after it, never inside it. */
public class ViewSwapTest {
    @Test
    public void aSwapRequestedDuringASwapRunsAfterItNotInsideIt() {
        ViewSwap swap = new ViewSwap();
        StringBuilder trace = new StringBuilder();
        int[] depth = {0};
        Runnable[] body = new Runnable[1];
        body[0] = () -> {
            depth[0]++;
            trace.append("in").append(depth[0]);
            if (trace.length() < 4) {
                // The old view's detach asks for another rebuild while this one is running.
                assertEquals(0, swap.run(body[0]));
            }
            trace.append("out");
            depth[0]--;
        };
        assertEquals(2, swap.run(body[0]));
        assertEquals("in1outin1out", trace.toString());
        assertFalse(swap.isRunning());
    }

    @Test
    public void aSwapThatAlwaysAsksAgainStopsAtTheLimit() {
        ViewSwap swap = new ViewSwap();
        int[] runs = {0};
        Runnable[] body = new Runnable[1];
        body[0] = () -> {
            runs[0]++;
            swap.run(body[0]);
        };
        assertEquals(ViewSwap.MOST_PASSES, swap.run(body[0]));
        assertEquals(ViewSwap.MOST_PASSES, runs[0]);
    }

    @Test
    public void aRequestRaisedWhilePreparingIsServedByTheSamePass() {
        // Letting the old view's held keys up can close a panel, which asks for a rebuild; the
        // build that follows already reflects it, so it must not run twice (RFC-0014 F1, F3).
        ViewSwap swap = new ViewSwap();
        int[] builds = {0};
        Runnable[] prepare = new Runnable[1];
        prepare[0] = () -> assertEquals(0, swap.run(() -> { }, () -> builds[0]++));
        assertEquals(1, swap.run(prepare[0], () -> builds[0]++));
        assertEquals(1, builds[0]);
    }

    @Test
    public void aFailedSwapLeavesTheGuardOpen() {
        ViewSwap swap = new ViewSwap();
        try {
            swap.run(() -> {
                throw new IllegalStateException("window gone");
            });
        } catch (IllegalStateException expected) {
            // the caller's catch
        }
        assertFalse(swap.isRunning());
        assertEquals(1, swap.run(() -> { }));
    }
}
