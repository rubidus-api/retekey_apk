package com.retekey;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Where the clip history lives: the app's own private preferences, and nowhere else.
 *
 * <p>Its own file rather than the keyboard's, so that clearing it is one line and so that nothing
 * about clips is mixed into settings that get read on every keystroke.
 */
final class ClipStore {
    private static final String PREFS = "retekey_clips";
    private static final String KEY_CLIPS = "clips";

    private ClipStore() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static ClipHistory load(Context context) {
        if (context == null) {
            return ClipHistory.empty();
        }
        SharedPreferences prefs = prefs(context);
        String stored = prefs.getString(KEY_CLIPS, "");
        ClipHistory history = ClipCodec.decode(stored);
        String rewritten = ClipCodec.encode(history.clips());
        StoreMigration.rewrite(prefs, KEY_CLIPS, stored, rewritten,
            history.clips().equals(ClipCodec.decode(rewritten).clips()));
        return history;
    }

    static void save(Context context, ClipHistory history) {
        if (context == null) {
            return;
        }
        prefs(context).edit().putString(KEY_CLIPS, ClipCodec.encode(history.clips())).apply();
    }
}
