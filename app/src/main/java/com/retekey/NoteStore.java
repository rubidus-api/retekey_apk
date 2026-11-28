package com.retekey;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Where the notes live: the keyboard's own preferences, under one key.
 *
 * <p>Notes are small and few, and the keyboard already owns a preferences file, so a database
 * would be machinery without a job. A change is written once typing pauses for 300 ms, and at
 * once when the panel closes, the keyboard hides or the service is destroyed — a keyboard can be
 * torn down between two keystrokes, and a note that only existed in memory would go with it.
 * Writing the whole store on every keystroke cost its full size per key (RFC-0014 F14); a process
 * killed without warning can now lose at most the last 300 ms of typing.
 */
final class NoteStore {
    private static final String KEY_NOTES = "notes";
    private static final String KEY_SORT = "notes_sort";
    private static final String KEY_ASCENDING = "notes_ascending";
    private static final VersionedStore.Codec<java.util.List<Note>> CODEC =
        new VersionedStore.Codec<java.util.List<Note>>() {
            @Override
            public java.util.List<Note> decode(String stored) {
                // Ids are given to notes from the first form by their place in the stored order,
                // so this happens before any sorting (review R05, R06).
                return NoteCodec.decode(stored);
            }

            @Override
            public String encode(java.util.List<Note> value) {
                return NoteCodec.encode(value);
            }

            @Override
            public boolean sameData(java.util.List<Note> first, java.util.List<Note> second) {
                return first.equals(second);
            }
        };

    private NoteStore() {
    }

    static NoteList load(Context context) {
        if (context == null) {
            return NoteList.empty();
        }
        SharedPreferences prefs = AppPrefs.notes(context);
        java.util.List<Note> notes = VersionedStore.load(prefs, KEY_NOTES, CODEC);
        boolean hasSort = prefs.contains(KEY_SORT);
        NoteList.Sort sort = parseSort(prefs.getString(KEY_SORT, null));
        boolean ascending = hasSort
            ? prefs.getBoolean(KEY_ASCENDING, sort != NoteList.Sort.STAMP)
            : false;
        return NoteList.of(notes, sort, ascending);
    }

    static void save(Context context, NoteList notes) {
        if (context == null) {
            return;
        }
        AppPrefs.notes(context).edit()
            .putString(KEY_NOTES, NoteCodec.encode(notes.notes()))
            .putString(KEY_SORT, notes.sort().name())
            .putBoolean(KEY_ASCENDING, notes.ascending())
            .apply();
    }

    private static NoteList.Sort parseSort(String stored) {
        if (stored == null || stored.isEmpty()) {
            return NoteList.Sort.STAMP;
        }
        try {
            return NoteList.Sort.valueOf(stored);
        } catch (IllegalArgumentException ignored) {
            return NoteList.Sort.STAMP;
        }
    }
}
