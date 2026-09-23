package com.retekey;

import java.util.ArrayList;
import java.util.List;

/**
 * How the notes are written down and read back.
 *
 * <p>Four fields a note — id, stamp, title, body — written through {@link RecordCodec}, where a
 * field says how long it is. The first form put a tab between stamp and title, a newline before
 * the body and U+001E between notes, on the grounds that no keyboard types U+001E; a pasted one
 * turned a note into two notes and cut its body (review finding R06). That form is still read,
 * once, so nothing written before is lost: each old note is given an id from its place in the
 * stored order, which is the same id every time the same stored text is read.
 *
 * <p>Android-free, so a note written today can be read back by a test rather than by a device.
 */
public final class NoteCodec {
    /** U+001E RECORD SEPARATOR: what the first form put between notes. */
    private static final String RECORD = "\u001E";
    private static final int FIELDS = 4;

    private NoteCodec() {
    }

    public static String encode(List<Note> notes) {
        List<String[]> records = new ArrayList<>(notes.size());
        for (Note note : notes) {
            records.add(new String[] {note.id(), note.stamp(), note.title(), note.body()});
        }
        return RecordCodec.encode(records, FIELDS);
    }

    public static List<Note> decode(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        if (RecordCodec.isNewFormat(text)) {
            List<Note> notes = new ArrayList<>();
            for (String[] record : RecordCodec.decode(text, FIELDS)) {
                if (!record[0].isEmpty() && !record[1].isEmpty()) {
                    notes.add(new Note(record[0], record[1], record[2], record[3]));
                }
            }
            return notes;
        }
        return decodeFirstForm(text);
    }

    /** Whether {@code text} was written by the form this codec writes now. */
    static boolean isCurrentForm(String text) {
        return RecordCodec.isNewFormat(text);
    }

    private static List<Note> decodeFirstForm(String text) {
        List<Note> notes = new ArrayList<>();
        String[] records = text.split(RECORD, -1);
        for (int i = 0; i < records.length; i++) {
            Note note = decodeOne("legacy-" + i, records[i]);
            if (note != null) {
                notes.add(note);
            }
        }
        return notes;
    }

    private static Note decodeOne(String id, String record) {
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
        return new Note(id, stamp, title, body);
    }
}
