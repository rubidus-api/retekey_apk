package com.retekey;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Where text shared into ReteKey lives: the app's own private preferences, and nowhere else — the
 * whole point of the feature is that it never reaches the system clipboard (issue #10).
 *
 * <p>Its own file, like the clip history's, so that clearing it is one line.
 */
final class StashStore {
    private static final String PREFS = "retekey_stash";
    private static final String KEY_ITEMS = "kept";
    /** How long a kept item lives, in minutes; 0 means until it is removed. */
    static final String KEY_MINUTES = "stash_minutes";

    private StashStore() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static StashHistory load(Context context) {
        if (context == null) {
            return StashHistory.empty();
        }
        return StashCodec.decode(prefs(context).getString(KEY_ITEMS, ""));
    }

    static void save(Context context, StashHistory history) {
        if (context == null) {
            return;
        }
        prefs(context).edit().putString(KEY_ITEMS, StashCodec.encode(history.items())).apply();
    }

    /** The user's time limit in minutes, from the keyboard's own settings file. */
    static int minutes(Context context) {
        if (context == null) {
            return StashHistory.DEFAULT_MINUTES;
        }
        return context.getSharedPreferences("retekey_view", Context.MODE_PRIVATE)
            .getInt(KEY_MINUTES, StashHistory.DEFAULT_MINUTES);
    }

    /** Loads what is still in date, writing back the pruning so it is gone for good. */
    static StashHistory loadPruned(Context context) {
        StashHistory loaded = load(context);
        StashHistory kept = loaded.pruned(System.currentTimeMillis(), minutes(context));
        if (kept != loaded) {
            save(context, kept);
        }
        return kept;
    }
}
