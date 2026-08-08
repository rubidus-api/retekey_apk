package com.retekey;

import java.util.ArrayList;
import java.util.List;

/**
 * How the notes are written down and read back.
 *
 * <p>One string holds them all: each note is its stamp, a tab, its title, a newline, and then its
 * body, with the notes separated by the record separator U+001E — a character no keyboard can type
 * and no note can therefore contain. A line-based format was tempting and wrong: a note's body is
 * exactly the place where every separator anyone can type will eventually appear.
 *
 * <p>Android-free, so a note written today can be read back by a test rather than by a device.
 */
public final class NoteCodec {
    /** U+001E RECORD SEPARATOR: the one character that cannot arrive from a keyboard. */
    private static final String RECORD = "\u001E";

    private NoteCodec() {
    }

    public static String encode(List<Note> notes) {
        StringBuilder out = new StringBuilder();
        for (Note note : notes) {
            if (out.length() > 0) {
                out.append(RECORD);
            }
            out.append(note.stamp()).append('\t').append(note.title()).append('\n')
                .append(note.body());
        }
        return out.toString();
    }

    public static List<Note> decode(String text) {
        List<Note> notes = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return notes;
        }
        for (String record : text.split(RECORD, -1)) {
            Note note = decodeOne(record);
            if (note != null) {
                notes.add(note);
            }
        }
        return notes;
    }

    private static Note decodeOne(String record) {
        if (record.isEmpty()) {
            return null;
        }
        int newline = record.indexOf('\n');
        String head = newline < 0 ? record : record.substring(0, newline);
        String body = newline < 0 ? "" : record.substring(newline + 1);
        int tab = head.indexOf('\t');
        String stamp = tab < 0 ? head : head.substring(0, tab);
        String title = tab < 0 ? "" : head.substring(tab + 1);
        if (stamp.isEmpty()) {
            return null;
        }
        return new Note(stamp, title, body);
    }
}
