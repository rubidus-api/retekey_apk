package com.retekey;

/**
 * The input view is swapped from one place, and never from inside a swap (RFC-0014 F1).
 *
 * <p>Swapping detaches the old view, and the old view's detach used to dispatch input — a latched
 * chord let up — that closed a panel, which swapped again while {@code setInputView} was still
 * removing the old children. A swap asked for meanwhile now runs once the current one is done.
 * A view that asks every time cannot loop: the passes stop at {@link #MOST_PASSES}.
 */
final class ViewSwap {
    static final int MOST_PASSES = 3;

    private static final Runnable NOTHING = () -> { };

    private boolean running;
    private boolean again;

    /** Runs the swap, or, from inside one, asks for another pass after it; returns passes run. */
    int run(Runnable swap) {
        return run(NOTHING, swap);
    }

    /**
     * The same, with {@code prepare} run first in every pass: a swap it asks for is the one about
     * to run, so it does not add a pass.
     */
    int run(Runnable prepare, Runnable swap) {
        if (running) {
            again = true;
            return 0;
        }
        running = true;
        int passes = 0;
        try {
            do {
                prepare.run();
                again = false;
                swap.run();
                passes++;
            } while (again && passes < MOST_PASSES);
        } finally {
            running = false;
            again = false;
        }
        return passes;
    }

    boolean isRunning() {
        return running;
    }
}
