package com.retekey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * What the bar above the keys carries, and in what order.
 *
 * <p>A slot is a built-in action, a piece of text the user wrote, or a key combination they
 * assembled — see {@link BarSlot}. This class is the list: what it holds by default, how it is read
 * back from preferences, and the one rule for moving a slot, shared by the arrows and by dragging so
 * a slot cannot land somewhere different depending on how it was picked up.
 *
 * <p>The bar is off until the user turns it on. A keyboard that grows a new strip on update, and so
 * becomes taller, is a keyboard that changed size for a reason its owner did not choose.
 */
final class ActionBarSlots {
    static final String KEY_SLOTS = "action_bar_slots";
    static final String KEY_ENABLED = "action_bar_enabled";
    static final boolean DEFAULT_ENABLED = false;

    /** What the bar holds before anyone rearranges it: select, the clipboard three, the arrows. */
    static final List<BarAction> DEFAULT_ACTIONS = Arrays.asList(
        BarAction.SELECT_WORD,
        BarAction.SELECT_ALL,
        BarAction.CUT,
        BarAction.COPY,
        BarAction.PASTE,
        BarAction.LEFT,
        BarAction.RIGHT);

    private ActionBarSlots() {
    }

    /** The default bar as slots. */
    static List<BarSlot> defaults() {
        List<BarSlot> slots = new ArrayList<>(DEFAULT_ACTIONS.size());
        for (BarAction action : DEFAULT_ACTIONS) {
            slots.add(BarSlot.of(action));
        }
        return slots;
    }

    /**
     * Reads a stored bar. Unset or unreadable legacy strings fall back to the default; an empty bar
     * explicitly stored in v2 {@link RecordCodec} form stays empty instead of coming back with the
     * default slots.
     */
    static List<BarSlot> parse(String stored) {
        if (BarSlotCodec.isEmptyV2(stored)) {
            return new ArrayList<>();
        }
        List<BarSlot> slots = BarSlotCodec.decode(stored);
        if (!slots.isEmpty()) {
            return slots;
        }
        if (BarSlotCodec.hasExpectedHeader(stored)) {
            return slots;
        }
        return defaults();
    }

    /** The stored form of a bar. */
    static String format(List<BarSlot> slots) {
        return BarSlotCodec.encode(slots);
    }

    /**
     * The list with the item at {@code from} put down at {@code to}, or the same list where either
     * index is not one of its own.
     */
    static List<BarSlot> moved(List<BarSlot> slots, int from, int to) {
        if (from == to || from < 0 || to < 0 || from >= slots.size() || to >= slots.size()) {
            return slots;
        }
        List<BarSlot> next = new ArrayList<>(slots);
        next.add(to, next.remove(from));
        return next;
    }
}
