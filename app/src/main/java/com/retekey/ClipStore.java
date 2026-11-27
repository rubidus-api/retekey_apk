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
    private static final String KEY_CLIPS = "clips";
    private static final VersionedStore.Codec<ClipHistory> CODEC =
        new VersionedStore.Codec<ClipHistory>() {
            @Override
            public ClipHistory decode(String stored) {
                return ClipCodec.decode(stored);
            }

            @Override
            public String encode(ClipHistory value) {
                return ClipCodec.encode(value.clips());
            }

            @Override
            public boolean sameData(ClipHistory first, ClipHistory second) {
                return first.clips().equals(second.clips());
            }
        };

    private ClipStore() {
    }

    static ClipHistory load(Context context) {
        if (context == null) {
            return ClipHistory.empty();
        }
        return VersionedStore.load(AppPrefs.clips(context), KEY_CLIPS, CODEC);
    }

    static void save(Context context, ClipHistory history) {
        if (context == null) {
            return;
        }
        VersionedStore.save(AppPrefs.clips(context), KEY_CLIPS, history, CODEC);
    }
}
