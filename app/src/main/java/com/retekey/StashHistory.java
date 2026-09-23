package com.retekey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Text shared into ReteKey and kept for the user to type somewhere else — without the system
 * clipboard being told anything at all.
 *
 * <p>Android lets the foreground app and the current keyboard read the clipboard, which on some
 * makers' ROMs means a keyboard that is not on screen is still reading everything copied and
 * writing it into a history the user cannot turn off (issue #10). The way out is not to use the
 * clipboard: an app's own Share menu hands text straight to ReteKey, ReteKey keeps it in its own
 * storage, and the keyboard types it when asked. Nothing is copied, so nothing can be read.
 *
 * <p>What is kept is decided here rather than at the call site:
 *
 * <ul>
 *   <li><b>It ages out.</b> A share is for using now, not for keeping: entries older than the
 *       user's time limit are gone the next time the list is read. Zero means "until removed".</li>
 *   <li><b>There are not many of them.</b> {@link #LIMIT} at most, newest first.</li>
 *   <li><b>A repeat moves rather than duplicates</b>, the way the clip list does.</li>
 *   <li><b>Nothing enormous is kept</b> — {@link #MAX_LENGTH} characters, the clip list's limit.</li>
 * </ul>
 *
 * <p>Android-free and immutable, so the rules are unit-tested rather than reasoned about.
 */
final class StashHistory {
    /** How many kept items are held, newest first. */
    static final int LIMIT = 20;
    /** The longest kept item, in characters. */
    static final int MAX_LENGTH = 4000;
    /** The time limit when the user has not chosen one, in minutes. */
    static final int DEFAULT_MINUTES = 60;

    /** One kept piece of text and the moment it arrived. */
    static final class Kept {
        final String text;
        final long keptAt;

        Kept(String text, long keptAt) {
            this.text = text;
            this.keptAt = keptAt;
        }

        // Two kept items are the same when their text and moment are, which is what the store's
        // migration compares before it writes anything (review W4).
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Kept)) {
                return false;
            }
            Kept that = (Kept) other;
            return keptAt == that.keptAt && text.equals(that.text);
        }

        @Override
        public int hashCode() {
            return text.hashCode() * 31 + (int) (keptAt ^ (keptAt >>> 32));
        }
    }

    private static final StashHistory EMPTY = new StashHistory(Collections.<Kept>emptyList());

    private final List<Kept> items;

    private StashHistory(List<Kept> items) {
        this.items = Collections.unmodifiableList(items);
    }

    static StashHistory empty() {
        return EMPTY;
    }

    static StashHistory of(List<Kept> items) {
        List<Kept> copy = new ArrayList<>(items.size());
        for (Kept item : items) {
            if (item != null && item.text != null && !item.text.isEmpty()) {
                copy.add(item);
            }
        }
        return copy.isEmpty() ? EMPTY : new StashHistory(cap(copy));
    }

    List<Kept> items() {
        return items;
    }

    boolean isEmpty() {
        return items.isEmpty();
    }

    /** The same list with {@code text} at the front, arrived at {@code now}. */
    StashHistory record(CharSequence text, long now) {
        if (text == null) {
            return this;
        }
        String value = text.toString();
        if (value.trim().isEmpty()) {
            return this;
        }
        value = Scalars.truncate(value, MAX_LENGTH);
        List<Kept> next = new ArrayList<>(items.size() + 1);
        for (Kept item : items) {
            if (!item.text.equals(value)) {
                next.add(item);
            }
        }
        next.add(0, new Kept(value, now));
        return new StashHistory(cap(next));
    }

    /**
     * The same list without what has expired. {@code minutes} of zero keeps everything until it is
     * removed by hand; anything kept before {@code now - minutes} is dropped.
     */
    StashHistory pruned(long now, int minutes) {
        if (minutes <= 0 || items.isEmpty()) {
            return this;
        }
        long cutoff = now - (long) minutes * 60_000L;
        List<Kept> next = new ArrayList<>(items.size());
        for (Kept item : items) {
            if (item.keptAt >= cutoff) {
                next.add(item);
            }
        }
        return next.size() == items.size() ? this : of(next);
    }

    StashHistory remove(String text) {
        List<Kept> next = new ArrayList<>(items.size());
        for (Kept item : items) {
            if (!item.text.equals(text)) {
                next.add(item);
            }
        }
        return next.size() == items.size() ? this : of(next);
    }

    StashHistory clear() {
        return EMPTY;
    }

    private static List<Kept> cap(List<Kept> items) {
        return items.size() <= LIMIT ? items : new ArrayList<>(items.subList(0, LIMIT));
    }
}
