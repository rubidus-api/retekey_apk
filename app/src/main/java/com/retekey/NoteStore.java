package com.retekey;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Where the notes live: the keyboard's own preferences, under one key.
 *
 * <p>Notes are small and few, and the keyboard already owns a preferences file, so a database
 * would be machinery without a job. Every change is written through immediately — a keyboard can
 * be torn down between two keystrokes, and a note that only existed in memory would go with it.
 */
final class NoteStore {
    private static final String PREFS = "retekey_notes";
    private static final String KEY_NOTES = "notes";

    private NoteStore() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static NoteList load(Context context) {
        if (context == null) {
            return NoteList.empty();
        }
        return NoteList.of(NoteCodec.decode(prefs(context).getString(KEY_NOTES, "")))
            .sortedBy(NoteList.Sort.STAMP);
    }

    static void save(Context context, NoteList notes) {
        if (context == null) {
            return;
        }
        prefs(context).edit().putString(KEY_NOTES, NoteCodec.encode(notes.notes())).apply();
    }
}
