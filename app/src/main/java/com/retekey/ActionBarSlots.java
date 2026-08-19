package com.retekey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Which actions the bar above the keys carries, and in what order.
 *
 * <p>The same shape as {@link LetterLayouts}: a comma-separated list of stored words, parsed and
 * repaired rather than trusted, because a preference file outlives the build that wrote it. An
 * unknown word is dropped — a bar from a newer build opened by an older one loses the action it
 * cannot perform and keeps the rest.
 *
 * <p>The bar is off until the user turns it on. A keyboard that grows a new strip on update, and so
 * becomes taller, is a keyboard that changed size for a reason its owner did not choose.
 */
final class ActionBarSlots {
    static final String KEY_SLOTS = "action_bar_slots";
    static final String KEY_ENABLED = "action_bar_enabled";
    static final boolean DEFAULT_ENABLED = false;

    /** What the bar holds before anyone rearranges it: select, the clipboard three, the arrows. */
    static final List<BarAction> DEFAULT = Arrays.asList(
        BarAction.SELECT_WORD,
        BarAction.SELECT_ALL,
        BarAction.CUT,
        BarAction.COPY,
        BarAction.PASTE,
        BarAction.LEFT,
        BarAction.RIGHT);

    private ActionBarSlots() {
    }

    /** Reads a stored bar, keeping only known actions and dropping repeats. */
    static List<BarAction> parse(String stored) {
        LinkedHashSet<BarAction> slots = new LinkedHashSet<>();
        if (stored != null) {
            for (String raw : stored.split(",")) {
                BarAction action = BarAction.parse(raw.trim());
                if (action != null) {
                    slots.add(action);
                }
            }
        }
        // An empty bar is a bar of nothing, which is what turning it off is for: the setting says
        // whether there is a bar, and this says what is on it.
        return slots.isEmpty() ? DEFAULT : new ArrayList<>(slots);
    }

    /**
     * The list with the item at {@code from} put down at {@code to}, or the same list where either
     * index is not one of its own. Shared by the arrows and by dragging, so a slot cannot end up
     * somewhere different depending on how it was moved.
     */
    static List<BarAction> moved(List<BarAction> slots, int from, int to) {
        if (from == to || from < 0 || to < 0 || from >= slots.size() || to >= slots.size()) {
            return slots;
        }
        List<BarAction> next = new ArrayList<>(slots);
        next.add(to, next.remove(from));
        return next;
    }

    /** The stored form of a bar. */
    static String format(List<BarAction> slots) {
        StringBuilder out = new StringBuilder();
        for (BarAction action : slots) {
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(action.stored());
        }
        return out.toString();
    }
}
