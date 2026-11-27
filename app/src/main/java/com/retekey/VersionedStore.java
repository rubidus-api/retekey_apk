package com.retekey;

import android.content.SharedPreferences;

/**
 * Shared load/save helper over {@link StoreMigration} so persisted lists share one guarded
 * migration and encoding path.
 */
final class VersionedStore {
    interface Codec<T> {
        T decode(String stored);

        String encode(T value);

        boolean sameData(T first, T second);
    }

    private VersionedStore() {
    }

    static <T> T load(SharedPreferences prefs, String key, Codec<T> codec) {
        String stored = prefs.getString(key, "");
        T value = codec.decode(stored);
        String rewritten = codec.encode(value);
        StoreMigration.rewrite(
            prefs,
            key,
            stored,
            rewritten,
            codec.sameData(value, codec.decode(rewritten))
        );
        return value;
    }

    static <T> void save(SharedPreferences prefs, String key, T value, Codec<T> codec) {
        prefs.edit().putString(key, codec.encode(value)).apply();
    }
}
