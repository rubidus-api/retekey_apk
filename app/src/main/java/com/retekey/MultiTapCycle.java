package com.retekey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A key that holds more than one character and cycles through them as it is tapped, the way a
 * phone keypad always has: the first tap types the first character, and a tap that arrives while
 * the run is still open takes back what it typed and puts the next one in its place.
 *
 * <p>The characters are the key's label read one code point at a time — a key labelled {@code .,}
 * types a period, then a comma, then a period again — so the label cannot disagree with what the
 * key does. Dragging picks one outright instead of cycling to it.
 *
 * <p>Android-free: what ends a run (a timeout, another key, a new editor) is the view's business,
 * and all this decides is which character a press means.
 */
public final class MultiTapCycle {
    /** Which character a press means, and whether it replaces the one before it. */
    public static final class Step {
        public final String character;
        public final int index;
        public final boolean replacesPrevious;

        Step(String character, int index, boolean replacesPrevious) {
            this.character = character;
            this.index = index;
            this.replacesPrevious = replacesPrevious;
        }
    }

    private MultiTapCycle() {
    }

    /**
     * The characters a label stands for, one per code point, so a label outside the basic plane is
     * still read as one character rather than two halves of a surrogate pair.
     */
    public static List<String> charactersOf(String label) {
        if (label == null || label.isEmpty()) {
            throw new IllegalArgumentException("a cycling key needs at least one character");
        }
        List<String> characters = new ArrayList<>();
        int offset = 0;
        while (offset < label.length()) {
            int codePoint = label.codePointAt(offset);
            int width = Character.charCount(codePoint);
            characters.add(label.substring(offset, offset + width));
            offset += width;
        }
        return Collections.unmodifiableList(characters);
    }

    /**
     * The press after {@code previousIndex}. A run that is not open starts at the first character
     * and types it outright; one that is open moves on and replaces what it typed, wrapping so a
     * key with two characters flips between them.
     */
    public static Step press(List<String> characters, int previousIndex, boolean runIsOpen) {
        require(characters);
        if (!runIsOpen) {
            return new Step(characters.get(0), 0, false);
        }
        if (previousIndex < 0 || previousIndex >= characters.size()) {
            throw new IllegalArgumentException("no character at " + previousIndex);
        }
        int next = (previousIndex + 1) % characters.size();
        return new Step(characters.get(next), next, true);
    }

    /**
     * The character a drag picks: the leftmost for a drag left, the rightmost for a drag right —
     * the same order they are written on the key. It never replaces anything, because a drag ends
     * whatever run was open rather than continuing it.
     */
    public static Step pick(List<String> characters, boolean rightwards) {
        require(characters);
        int index = rightwards ? characters.size() - 1 : 0;
        return new Step(characters.get(index), index, false);
    }

    private static void require(List<String> characters) {
        if (characters == null || characters.isEmpty()) {
            throw new IllegalArgumentException("a cycling key needs at least one character");
        }
    }
}
