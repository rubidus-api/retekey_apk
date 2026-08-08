package com.retekey;

import java.util.ArrayList;
import java.util.List;

/**
 * Undo and redo for one text field, as snapshots of what it said and where the cursor was.
 *
 * <p>Android's own undo lives on {@code TextView} from API 23 and this keyboard reaches API 14, so
 * the notepad keeps its own — which also means undo means the same thing on every device it runs
 * on rather than depending on the platform underneath.
 *
 * <p>Snapshots rather than diffs: a note is short, the cap is small, and the whole state being one
 * string is what makes "undo, then keep typing" behave — the redo tail is dropped on the next edit,
 * the way every editor does it.
 */
public final class NoteHistory {
    /** How many steps back the history goes. Beyond this the oldest state is forgotten. */
    public static final int LIMIT = 60;

    /** One remembered state of the field. */
    public static final class Snapshot {
        private final String text;
        private final int cursor;

        public Snapshot(String text, int cursor) {
            this.text = text == null ? "" : text;
            this.cursor = Math.max(0, Math.min(cursor, this.text.length()));
        }

        public String text() {
            return text;
        }

        public int cursor() {
            return cursor;
        }
    }

    private final List<Snapshot> states = new ArrayList<>();
    private int position = -1;

    /**
     * Records a state. The first call establishes the starting point; a state identical to the
     * current one is not recorded, so holding a key or refreshing the screen costs nothing.
     * Recording after an undo drops whatever was ahead — that future is no longer reachable.
     */
    public void record(String text, int cursor) {
        String value = text == null ? "" : text;
        if (position >= 0 && states.get(position).text().equals(value)) {
            return;
        }
        while (states.size() > position + 1) {
            states.remove(states.size() - 1);
        }
        states.add(new Snapshot(value, cursor));
        if (states.size() > LIMIT) {
            states.remove(0);
        }
        position = states.size() - 1;
    }

    public boolean canUndo() {
        return position > 0;
    }

    public boolean canRedo() {
        return position >= 0 && position < states.size() - 1;
    }

    /** The state before the current one, or null when there is nothing to go back to. */
    public Snapshot undo() {
        if (!canUndo()) {
            return null;
        }
        position--;
        return states.get(position);
    }

    /** The state undone last, or null when nothing was undone. */
    public Snapshot redo() {
        if (!canRedo()) {
            return null;
        }
        position++;
        return states.get(position);
    }

    /** Forgets everything: a different note is a different history. */
    public void reset() {
        states.clear();
        position = -1;
    }

    /** How many states are remembered, counting the current one. */
    public int size() {
        return states.size();
    }
}
