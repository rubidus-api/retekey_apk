package com.retekey;

import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Settings held in memory only, for the time before the user first unlocks the device (review
 * finding R04). The service is direct-boot aware so that a keyboard exists at the lock screen,
 * but credential-encrypted storage — where every setting, layout and history lives — cannot be
 * opened until unlock. Reads here return the defaults, so the keyboard comes up plain; writes last
 * for the session and never reach a disk, so nothing personal is copied to device-protected
 * storage to make the locked keyboard look like the unlocked one.
 */
final class LockedPreferences implements SharedPreferences {
    private final Map<String, Object> values = new HashMap<>();
    private final List<OnSharedPreferenceChangeListener> listeners = new ArrayList<>();

    @Override
    public Map<String, ?> getAll() {
        return new HashMap<>(values);
    }

    @Override
    public String getString(String key, String defValue) {
        Object value = values.get(key);
        return value instanceof String ? (String) value : defValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getStringSet(String key, Set<String> defValues) {
        Object value = values.get(key);
        return value instanceof Set ? new HashSet<>((Set<String>) value) : defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        Object value = values.get(key);
        return value instanceof Integer ? (Integer) value : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        Object value = values.get(key);
        return value instanceof Long ? (Long) value : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        Object value = values.get(key);
        return value instanceof Float ? (Float) value : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Object value = values.get(key);
        return value instanceof Boolean ? (Boolean) value : defValue;
    }

    @Override
    public boolean contains(String key) {
        return values.containsKey(key);
    }

    @Override
    public Editor edit() {
        return new MemoryEditor();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) {
        listeners.remove(l);
    }

    private final class MemoryEditor implements Editor {
        private final Map<String, Object> pending = new HashMap<>();
        private final Set<String> removed = new HashSet<>();
        private boolean clear;

        private Editor put(String key, Object value) {
            pending.put(key, value);
            removed.remove(key);
            return this;
        }

        @Override
        public Editor putString(String key, String value) {
            return value == null ? remove(key) : put(key, value);
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
            return values == null ? remove(key) : put(key, new HashSet<>(values));
        }

        @Override
        public Editor putInt(String key, int value) {
            return put(key, value);
        }

        @Override
        public Editor putLong(String key, long value) {
            return put(key, value);
        }

        @Override
        public Editor putFloat(String key, float value) {
            return put(key, value);
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            return put(key, value);
        }

        @Override
        public Editor remove(String key) {
            pending.remove(key);
            removed.add(key);
            return this;
        }

        @Override
        public Editor clear() {
            clear = true;
            return this;
        }

        @Override
        public boolean commit() {
            List<String> changed = new ArrayList<>();
            if (clear) {
                changed.addAll(values.keySet());
                values.clear();
            }
            for (String key : removed) {
                if (values.remove(key) != null) {
                    changed.add(key);
                }
            }
            for (Map.Entry<String, Object> entry : pending.entrySet()) {
                values.put(entry.getKey(), entry.getValue());
                changed.add(entry.getKey());
            }
            for (String key : changed) {
                for (OnSharedPreferenceChangeListener listener : new ArrayList<>(listeners)) {
                    listener.onSharedPreferenceChanged(LockedPreferences.this, key);
                }
            }
            return true;
        }

        @Override
        public void apply() {
            commit();
        }
    }
}
