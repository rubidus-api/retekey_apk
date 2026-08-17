package com.retekey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The clips the keyboard remembers, newest first.
 *
 * <p>What is kept is decided here rather than at the call site, because the rules are the whole
 * point of the feature being safe to have:
 *
 * <ul>
 *   <li><b>Nothing is recorded from a sensitive editor.</b> A password field's contents must not
 *       survive the keystroke, and a clipboard list is exactly the kind of place they would.</li>
 *   <li><b>Pinned clips stay; unpinned ones age out</b> at {@link #UNPINNED_LIMIT}, so the list is
 *       a recent history rather than a growing record of everything ever copied.</li>
 *   <li><b>A repeat moves rather than duplicates.</b> Copying the same thing twice should not push
 *       something else off the end.</li>
 *   <li><b>Nothing enormous is kept.</b> A clip longer than {@link #MAX_LENGTH} characters is
 *       truncated: the list is for pasting a line back, not for storing a document.</li>
 * </ul>
 *
 * <p>Android-free, and immutable: every operation returns a new history, so the caller decides when
 * something is written down.
 */
final class ClipHistory {
    /** How many unpinned clips are kept. Pinned ones are not counted. */
    static final int UNPINNED_LIMIT = 20;
    /** The longest clip kept, in characters. */
    static final int MAX_LENGTH = 4000;

    /** One remembered clip. */
    static final class Clip {
        final String text;
        final boolean pinned;

        Clip(String text, boolean pinned) {
            this.text = text;
            this.pinned = pinned;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Clip)) {
                return false;
            }
            Clip clip = (Clip) other;
            return pinned == clip.pinned && text.equals(clip.text);
        }

        @Override
        public int hashCode() {
            return text.hashCode() * 31 + (pinned ? 1 : 0);
        }

        @Override
        public String toString() {
            return (pinned ? "pinned:" : "clip:") + text;
        }
    }

    private final List<Clip> clips;

    private ClipHistory(List<Clip> clips) {
        this.clips = Collections.unmodifiableList(clips);
    }

    static ClipHistory empty() {
        return new ClipHistory(new ArrayList<Clip>());
    }

    static ClipHistory of(List<Clip> clips) {
        return new ClipHistory(new ArrayList<>(clips));
    }

    /** The clips, pinned ones first, then the rest newest first. */
    List<Clip> clips() {
        List<Clip> ordered = new ArrayList<>(clips.size());
        for (Clip clip : clips) {
            if (clip.pinned) {
                ordered.add(clip);
            }
        }
        for (Clip clip : clips) {
            if (!clip.pinned) {
                ordered.add(clip);
            }
        }
        return ordered;
    }

    boolean isEmpty() {
        return clips.isEmpty();
    }

    /**
     * Records a clip.
     *
     * @param sensitive whether the editor it came from was a password or other sensitive field, in
     *     which case nothing is recorded at all
     */
    ClipHistory record(CharSequence text, boolean sensitive) {
        if (sensitive || text == null) {
            return this;
        }
        String value = text.toString();
        if (value.trim().isEmpty()) {
            return this;
        }
        if (value.length() > MAX_LENGTH) {
            value = value.substring(0, MAX_LENGTH);
        }
        List<Clip> next = new ArrayList<>(clips.size() + 1);
        boolean wasPinned = false;
        for (Clip clip : clips) {
            if (clip.text.equals(value)) {
                // A repeat is the same clip arriving again: it moves to the front and keeps its pin
                // rather than becoming a second copy of itself.
                wasPinned = clip.pinned;
                continue;
            }
            next.add(clip);
        }
        next.add(0, new Clip(value, wasPinned));
        return new ClipHistory(cap(next));
    }

    /** Pins or unpins a clip. A pinned clip is never aged out. */
    ClipHistory setPinned(String text, boolean pinned) {
        List<Clip> next = new ArrayList<>(clips.size());
        for (Clip clip : clips) {
            next.add(clip.text.equals(text) ? new Clip(clip.text, pinned) : clip);
        }
        return new ClipHistory(cap(next));
    }

    /** Forgets one clip. */
    ClipHistory remove(String text) {
        List<Clip> next = new ArrayList<>(clips.size());
        for (Clip clip : clips) {
            if (!clip.text.equals(text)) {
                next.add(clip);
            }
        }
        return new ClipHistory(next);
    }

    /** Forgets everything that is not pinned. The way out for someone who copied the wrong thing. */
    ClipHistory clearUnpinned() {
        List<Clip> next = new ArrayList<>();
        for (Clip clip : clips) {
            if (clip.pinned) {
                next.add(clip);
            }
        }
        return new ClipHistory(next);
    }

    private static List<Clip> cap(List<Clip> clips) {
        List<Clip> kept = new ArrayList<>(clips.size());
        int unpinned = 0;
        for (Clip clip : clips) {
            if (clip.pinned) {
                kept.add(clip);
                continue;
            }
            if (unpinned < UNPINNED_LIMIT) {
                kept.add(clip);
                unpinned++;
            }
        }
        return kept;
    }
}
