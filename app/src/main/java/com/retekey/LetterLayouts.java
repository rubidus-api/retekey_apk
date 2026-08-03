package com.retekey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Which letter layouts the globe key cycles through, and in what order.
 *
 * <p>The user chooses both in settings: a layout can be turned off entirely, and the ones left on
 * are visited in the order they appear here. The globe key walks the list, so two enabled layouts
 * behave exactly like the old KO/EN toggle and five behave like a carousel.
 *
 * <p>Android-free: the list is stored as a comma-separated string of enum names, and this class
 * owns parsing, repair, and the walk. Unknown or duplicated names are dropped rather than throwing,
 * because a preference file can outlive the build that wrote it.
 */
public final class LetterLayouts {
    static final String KEY_ORDER = "letter_layout_order";

    /** Every layout the globe key can reach, in the order settings offers them. */
    public static final List<KeyboardLayoutId> ALL = Arrays.asList(
        KeyboardLayoutId.KO_DUBEOLSIK,
        KeyboardLayoutId.EN_QWERTY,
        KeyboardLayoutId.EN_DVORAK,
        KeyboardLayoutId.KO_CHEONJIIN,
        KeyboardLayoutId.KO_NARATGEUL
    );

    /** What a user gets before touching settings: the two full keyboards. */
    public static final List<KeyboardLayoutId> DEFAULT = Arrays.asList(
        KeyboardLayoutId.KO_DUBEOLSIK,
        KeyboardLayoutId.EN_QWERTY
    );

    private LetterLayouts() {
    }

    /** Human-readable name for settings and for the toast shown when the globe key switches. */
    public static String displayName(KeyboardLayoutId id) {
        if (id == null) {
            return "";
        }
        switch (id) {
            case EN_QWERTY:
                return "QWERTY";
            case EN_DVORAK:
                return "Dvorak";
            case KO_DUBEOLSIK:
                return "두벌식";
            case KO_CHEONJIIN:
                return "천지인";
            case KO_NARATGEUL:
                return "나랏글";
            default:
                return id.name();
        }
    }

    /**
     * Reads a stored order, keeping only known letter layouts and dropping repeats. An empty or
     * unusable list falls back to {@link #DEFAULT}, so the globe key always has somewhere to go.
     */
    public static List<KeyboardLayoutId> parse(String stored) {
        LinkedHashSet<KeyboardLayoutId> order = new LinkedHashSet<>();
        if (stored != null) {
            for (String raw : stored.split(",")) {
                String name = raw.trim();
                if (name.isEmpty()) {
                    continue;
                }
                for (KeyboardLayoutId candidate : ALL) {
                    if (candidate.name().equals(name)) {
                        order.add(candidate);
                        break;
                    }
                }
            }
        }
        return order.isEmpty() ? DEFAULT : new ArrayList<>(order);
    }

    /** The stored form of an order. */
    public static String format(List<KeyboardLayoutId> order) {
        StringBuilder text = new StringBuilder();
        if (order != null) {
            for (KeyboardLayoutId id : order) {
                if (id == null || !ALL.contains(id)) {
                    continue;
                }
                if (text.length() > 0) {
                    text.append(',');
                }
                text.append(id.name());
            }
        }
        return text.toString();
    }

    /**
     * The layout after {@code current} in {@code order}, wrapping at the end. A layout that is not
     * in the order — it was just turned off in settings — hands over to the first one.
     */
    public static KeyboardLayoutId next(List<KeyboardLayoutId> order, KeyboardLayoutId current) {
        List<KeyboardLayoutId> walk = order == null || order.isEmpty() ? DEFAULT : order;
        int index = walk.indexOf(current);
        if (index < 0) {
            return walk.get(0);
        }
        return walk.get((index + 1) % walk.size());
    }

    /** The layout to start from: the first enabled one, or the stored one when still enabled. */
    public static KeyboardLayoutId firstOf(List<KeyboardLayoutId> order) {
        List<KeyboardLayoutId> walk = order == null || order.isEmpty() ? DEFAULT : order;
        return walk.get(0);
    }
}
