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
    private static final String KEY_ITEMS = "kept";
    /** How long a kept item lives, in minutes; 0 means until it is removed. */
    static final String KEY_MINUTES = "stash_minutes";
    private static final VersionedStore.Codec<StashHistory> CODEC =
        new VersionedStore.Codec<StashHistory>() {
            @Override
            public StashHistory decode(String stored) {
                return StashCodec.decode(stored);
            }

            @Override
            public String encode(StashHistory value) {
                return StashCodec.encode(value.items());
            }

            @Override
            public boolean sameData(StashHistory first, StashHistory second) {
                return first.items().equals(second.items());
            }
        };

    private StashStore() {
    }

    static StashHistory load(Context context) {
        if (context == null) {
            return StashHistory.empty();
        }
        return VersionedStore.load(AppPrefs.stash(context), KEY_ITEMS, CODEC);
    }

    static void save(Context context, StashHistory history) {
        if (context == null) {
            return;
        }
        VersionedStore.save(AppPrefs.stash(context), KEY_ITEMS, history, CODEC);
    }

    /** The user's time limit in minutes, from the keyboard's own settings file. */
    static int minutes(Context context) {
        if (context == null) {
            return StashHistory.DEFAULT_MINUTES;
        }
        return AppPrefs.view(context).getInt(KEY_MINUTES, StashHistory.DEFAULT_MINUTES);
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
