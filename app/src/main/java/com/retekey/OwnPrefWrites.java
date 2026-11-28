package com.retekey;

import java.util.HashSet;
import java.util.Set;

/**
 * Keys the service is writing itself, so its preference listener can tell them from a change made
 * in settings (RFC-0014 F11).
 *
 * <p>A theme cycled from the menu page was rebuilding the whole input view, which put the user
 * back on the letters page every press; switching the floating keyboard built the view twice. The
 * service already does what those writes need; only settings changes need the listener's rebuild.
 * {@code apply()} tells listeners on the main thread before it returns, so the write is seen as
 * the service's own while {@link #write} runs.
 */
final class OwnPrefWrites {
    private final Set<String> writing = new HashSet<>();

    /** Runs {@code write} with {@code base} (and its oriented forms) marked as the service's own. */
    void write(String base, Runnable write) {
        writing.add(base);
        try {
            write.run();
        } finally {
            writing.remove(base);
        }
    }

    boolean isOwn(String key) {
        if (key == null) {
            return false;
        }
        for (String base : writing) {
            if (AppPrefs.isOrientedKey(key, base)) {
                return true;
            }
        }
        return false;
    }
}
