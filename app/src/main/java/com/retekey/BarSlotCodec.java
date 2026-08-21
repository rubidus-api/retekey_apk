package com.retekey;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * How the action bar's slots are written down and read back.
 *
 * <p>Records are separated by U+001E and their fields by U+001F — the two characters no keyboard
 * produces — because a slot can now carry text the user wrote, and text is exactly where every
 * separator anyone can type will eventually appear. A record carrying one of them is dropped rather
 * than corrupting the record after it.
 *
 * <p>It also reads the older format: a comma-separated list of {@link BarAction} words, which is
 * what every bar stored before custom slots existed. That is why an upgrade does not silently clear
 * someone's bar; the next write is in the new format.
 */
final class BarSlotCodec {
    /** U+001E RECORD SEPARATOR, between slots. */
    private static final String RECORD = "\u001E";
    /** U+001F UNIT SEPARATOR, between a slot's fields. */
    private static final String FIELD = "\u001F";
    private static final String BUILT_IN = "a";
    private static final String TEXT = "t";
    private static final String CHORD = "c";

    private BarSlotCodec() {
    }

    static String encode(List<BarSlot> slots) {
        StringBuilder out = new StringBuilder();
        for (BarSlot slot : slots) {
            String record = encodeOne(slot);
            if (record == null) {
                continue;
            }
            if (out.length() > 0) {
                out.append(RECORD);
            }
            out.append(record);
        }
        return out.toString();
    }

    private static String encodeOne(BarSlot slot) {
        switch (slot.kind()) {
            case BUILT_IN:
                return BUILT_IN + FIELD + slot.action().stored();
            case TEXT:
                if (unsafe(slot.text()) || unsafe(slot.customLabel())) {
                    return null;
                }
                return TEXT + FIELD + slot.text() + FIELD + nullToEmpty(slot.customLabel());
            default:
                if (slot.key() == null || unsafe(slot.customLabel())) {
                    return null;
                }
                return CHORD + FIELD + modifiers(slot.modifiers()) + FIELD + slot.key().name()
                    + FIELD + nullToEmpty(slot.customLabel());
        }
    }

    static List<BarSlot> decode(String stored) {
        List<BarSlot> slots = new ArrayList<>();
        if (stored == null || stored.isEmpty()) {
            return slots;
        }
        if (!stored.contains(RECORD) && !stored.contains(FIELD)) {
            // The old format: nothing but built-in actions, separated by commas.
            for (String word : stored.split(",")) {
                BarAction action = BarAction.parse(word.trim());
                if (action != null) {
                    slots.add(BarSlot.of(action));
                }
            }
            return slots;
        }
        for (String record : stored.split(RECORD, -1)) {
            BarSlot slot = decodeOne(record);
            if (slot != null) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private static BarSlot decodeOne(String record) {
        String[] fields = record.split(FIELD, -1);
        if (fields.length < 2) {
            return null;
        }
        if (BUILT_IN.equals(fields[0])) {
            BarAction action = BarAction.parse(fields[1]);
            return action == null ? null : BarSlot.of(action);
        }
        if (TEXT.equals(fields[0])) {
            if (fields[1].isEmpty()) {
                return null;
            }
            return BarSlot.text(fields[1], fields.length > 2 ? emptyToNull(fields[2]) : null);
        }
        if (CHORD.equals(fields[0]) && fields.length >= 3) {
            RawKey key = key(fields[2]);
            if (key == null) {
                return null;
            }
            return BarSlot.chord(key, modifiers(fields[1]),
                fields.length > 3 ? emptyToNull(fields[3]) : null);
        }
        return null;
    }

    private static RawKey key(String name) {
        for (RawKey key : RawKey.values()) {
            if (key.name().equals(name)) {
                return key;
            }
        }
        return null;
    }

    private static String modifiers(Set<KeyModifier> modifiers) {
        StringBuilder out = new StringBuilder();
        for (KeyModifier modifier : new TreeSet<>(modifiers)) {
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(modifier.name());
        }
        return out.toString();
    }

    private static Set<KeyModifier> modifiers(String stored) {
        Set<KeyModifier> modifiers = new LinkedHashSet<>();
        if (stored == null || stored.isEmpty()) {
            return modifiers;
        }
        for (String name : stored.split(",")) {
            for (KeyModifier modifier : KeyModifier.values()) {
                if (modifier.name().equals(name.trim())) {
                    modifiers.add(modifier);
                }
            }
        }
        return modifiers;
    }

    private static boolean unsafe(String value) {
        return value != null && (value.contains(RECORD) || value.contains(FIELD));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
