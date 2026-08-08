package com.retekey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The notes, in the order the list screen shows them, with the selection the checkboxes carry.
 *
 * <p>The list behaves the way a file manager's details view behaves, because that is what people
 * already know: each column header sorts by its own field, and pressing the same header again
 * turns the sort around. A hand-made order is possible too — the arrows move a note up or down —
 * and it survives until a column is sorted, which is the moment the user has asked for a different
 * order instead.
 *
 * <p>Immutable: every operation answers a new list, so the view can hold the old one while it
 * animates and nothing can be changed underneath a redraw. Android-free and unit-tested.
 */
public final class NoteList {
    /** Which column the list is sorted by, if any. */
    public enum Sort {
        /** The order the user arranged by hand. */
        MANUAL,
        STAMP,
        TITLE
    }

    private final List<Note> notes;
    private final Set<String> selected;
    private final Sort sort;
    private final boolean ascending;

    private NoteList(List<Note> notes, Set<String> selected, Sort sort, boolean ascending) {
        this.notes = Collections.unmodifiableList(notes);
        this.selected = Collections.unmodifiableSet(selected);
        this.sort = sort;
        this.ascending = ascending;
    }

    public static NoteList empty() {
        return new NoteList(new ArrayList<Note>(), new LinkedHashSet<String>(), Sort.STAMP, false);
    }

    public static NoteList of(List<Note> notes) {
        return new NoteList(new ArrayList<>(notes), new LinkedHashSet<String>(), Sort.MANUAL, true);
    }

    public List<Note> notes() {
        return notes;
    }

    public int size() {
        return notes.size();
    }

    public boolean isEmpty() {
        return notes.isEmpty();
    }

    public Sort sort() {
        return sort;
    }

    public boolean ascending() {
        return ascending;
    }

    /** The stamps of the notes whose checkbox is ticked. */
    public Set<String> selected() {
        return selected;
    }

    public boolean isSelected(String stamp) {
        return selected.contains(stamp);
    }

    /** Whether every note is ticked — what the header checkbox shows. */
    public boolean allSelected() {
        return !notes.isEmpty() && selected.size() == notes.size();
    }

    /** A new note at the top of the list, ready to be written into. */
    public NoteList added(Note note) {
        List<Note> next = new ArrayList<>(notes.size() + 1);
        next.add(note);
        next.addAll(notes);
        // Re-sorted in the direction the list is already in, so a new note lands where that
        // order puts it — at the top of a newest-first list, which is where it is looked for.
        return new NoteList(next, new LinkedHashSet<>(selected), sort, ascending)
            .sorted(sort, ascending);
    }

    /** Replaces the note with this one's stamp, leaving the order alone. */
    public NoteList replaced(Note note) {
        List<Note> next = new ArrayList<>(notes);
        for (int i = 0; i < next.size(); i++) {
            if (next.get(i).stamp().equals(note.stamp())) {
                next.set(i, note);
                break;
            }
        }
        return new NoteList(next, new LinkedHashSet<>(selected), sort, ascending);
    }

    public Note byStamp(String stamp) {
        for (Note note : notes) {
            if (note.stamp().equals(stamp)) {
                return note;
            }
        }
        return null;
    }

    /**
     * Sorted by a column. Pressing the column that is already sorted turns it around, which is the
     * behaviour of every details view; {@code keepDirection} asks for the current direction
     * instead, for the re-sort that follows adding a note.
     */
    public NoteList sortedBy(Sort column) {
        boolean nextAscending = sort == column ? !ascending : defaultAscending(column);
        return sorted(column, nextAscending);
    }

    private static boolean defaultAscending(Sort column) {
        // Dates open newest-first, which is the note you just wrote; titles open A to Z.
        return column != Sort.STAMP;
    }

    private NoteList sorted(Sort column, boolean nextAscending) {
        if (column == Sort.MANUAL) {
            return new NoteList(new ArrayList<>(notes), new LinkedHashSet<>(selected),
                Sort.MANUAL, nextAscending);
        }
        List<Note> next = new ArrayList<>(notes);
        Comparator<Note> comparator = column == Sort.STAMP
            ? new Comparator<Note>() {
                @Override
                public int compare(Note a, Note b) {
                    return a.stamp().compareTo(b.stamp());
                }
            }
            : new Comparator<Note>() {
                @Override
                public int compare(Note a, Note b) {
                    int byTitle = a.sortTitle().compareToIgnoreCase(b.sortTitle());
                    return byTitle != 0 ? byTitle : a.stamp().compareTo(b.stamp());
                }
            };
        Collections.sort(next, comparator);
        if (!nextAscending) {
            Collections.reverse(next);
        }
        return new NoteList(next, new LinkedHashSet<>(selected), column, nextAscending);
    }

    /** One note moved a place up or down; a hand-made order is a manual sort from then on. */
    public NoteList moved(String stamp, int delta) {
        List<Note> next = new ArrayList<>(notes);
        int from = indexOf(stamp);
        int to = from + delta;
        if (from < 0 || to < 0 || to >= next.size()) {
            return this;
        }
        Note moving = next.remove(from);
        next.add(to, moving);
        return new NoteList(next, new LinkedHashSet<>(selected), Sort.MANUAL, ascending);
    }

    private int indexOf(String stamp) {
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).stamp().equals(stamp)) {
                return i;
            }
        }
        return -1;
    }

    public NoteList toggledSelection(String stamp) {
        Set<String> next = new LinkedHashSet<>(selected);
        if (!next.remove(stamp)) {
            next.add(stamp);
        }
        return new NoteList(new ArrayList<>(notes), next, sort, ascending);
    }

    /** The header checkbox: all on when any is off, all off when every one is on. */
    public NoteList toggledSelectAll() {
        Set<String> next = new LinkedHashSet<>();
        if (!allSelected()) {
            for (Note note : notes) {
                next.add(note.stamp());
            }
        }
        return new NoteList(new ArrayList<>(notes), next, sort, ascending);
    }

    /** The notes that are not ticked; what "delete selected" leaves behind. */
    public NoteList withoutSelected() {
        List<Note> next = new ArrayList<>();
        for (Note note : notes) {
            if (!selected.contains(note.stamp())) {
                next.add(note);
            }
        }
        return new NoteList(next, new LinkedHashSet<String>(), sort, ascending);
    }

    /** Everything gone. */
    public NoteList cleared() {
        return new NoteList(new ArrayList<Note>(), new LinkedHashSet<String>(), sort, ascending);
    }
}
