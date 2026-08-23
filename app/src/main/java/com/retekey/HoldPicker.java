package com.retekey;

/**
 * Where a held key's candidates go, and which one the finger is on.
 *
 * <p>A key with one alternate types it the moment the hold fires — nothing to aim at. A key with
 * several raises a strip of them and waits; the finger, still down, slides sideways to the one it
 * wants and lifts. Two rules decide the strip, both in column units so they can be tested without
 * a screen:
 *
 * <ul>
 * <li><b>Out from under the finger.</b> The strip sits on the row <em>above</em> the key, where the
 *     finger does not cover it — and on the top row, which has nothing above it, on the same row but
 *     starting beside the key rather than on it. A strip below the key would be under the hand, so
 *     there is never one, which is also why the bottom rows need no special case.</li>
 * <li><b>The side with room.</b> The strip runs to the right when the right has room for it, else to
 *     the left when the left has; when neither has, it runs towards the side with more room and is
 *     shifted inside the keyboard. A key at the right edge therefore never asks for a drag off the
 *     edge, and a key at the left edge never asks for one off the other.</li>
 * </ul>
 *
 * <p>Selection is by the column under the finger: not moved, the first candidate; moved onto the
 * strip, the one there; moved past either end, the nearest end. Distance selects — not direction —
 * so the strip's own side is the only thing the user needs to see.
 */
final class HoldPicker {
    final int columns;
    final int count;
    /** The row the strip is drawn on. */
    final int stripRow;
    /** The column candidate 0 occupies. */
    final int stripStart;
    /** +1 when candidates run to the right of {@link #stripStart}, -1 when to the left. */
    final int direction;

    private HoldPicker(int columns, int count, int stripRow, int stripStart, int direction) {
        this.columns = columns;
        this.count = count;
        this.stripRow = stripRow;
        this.stripStart = stripStart;
        this.direction = direction;
    }

    /**
     * Places a strip of {@code count} candidates for a key on {@code row} that starts at
     * {@code startColumn} and is {@code span} columns wide, in a grid {@code columns} wide.
     */
    static HoldPicker place(int columns, int row, int startColumn, int span, int count) {
        if (count < 1) {
            throw new IllegalArgumentException("a picker needs at least one candidate");
        }
        int stripRow = row > 0 ? row - 1 : 0;
        // Above the key, candidate 0 can sit over the key itself. On the top row it starts beside
        // it, so the finger is not on top of what it is choosing from.
        int anchorRight = row > 0 ? startColumn : startColumn + span;
        int anchorLeft = row > 0 ? startColumn + span - 1 : startColumn - 1;
        int roomRight = columns - anchorRight;       // columns available from anchorRight onwards
        int roomLeft = anchorLeft + 1;               // columns available up to anchorLeft
        if (roomRight >= count) {
            return new HoldPicker(columns, count, stripRow, anchorRight, +1);
        }
        if (roomLeft >= count) {
            return new HoldPicker(columns, count, stripRow, anchorLeft, -1);
        }
        // Neither side has room: run towards the roomier side and slide the strip inside.
        int clamped = Math.min(count, columns);
        if (roomRight >= roomLeft) {
            return new HoldPicker(columns, clamped, stripRow, Math.max(0, columns - clamped), +1);
        }
        return new HoldPicker(columns, clamped, stripRow, Math.min(columns - 1, clamped - 1), -1);
    }

    /** The column candidate {@code index} is drawn in. */
    int columnOf(int index) {
        return stripStart + index * direction;
    }

    /**
     * The candidate under a finger that is over {@code column}, or 0 when it has not moved off the
     * key. Past either end of the strip is the nearer end.
     */
    int indexAt(int column, boolean moved) {
        if (!moved) {
            return 0;
        }
        int index = (column - stripStart) * direction;
        return Math.max(0, Math.min(count - 1, index));
    }
}
