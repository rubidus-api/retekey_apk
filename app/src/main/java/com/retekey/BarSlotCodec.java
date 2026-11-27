package com.retekey;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import com.retekey.core.KeyModifier;
import com.retekey.core.RawKey;
import com.retekey.core.RecordCodec;

/**
 * How the action bar's slots are written down and read back.
 *
 * <p>Current saves use {@link RecordCodec} (4 fields per record), so slot text and custom labels
 * containing U+001E, U+001F, or newlines survive without loss. It also decodes the two older
 * formats: comma-separated {@link BarAction} words, and the v1 U+001E/U+001F record format.
 */
final class BarSlotCodec {
    /** U+001E RECORD SEPARATOR, between slots in v1 format. */
    private static final String RECORD = "\u001E";
    /** U+001F UNIT SEPARATOR, between a slot's fields in v1 format. */
    private static final String FIELD = "\u001F";
    private static final String BUILT_IN = "a";
    private static final String TEXT = "t";
    private static final String CHORD = "c";
    private static final int FIELDS = 4;
    private static final String V2_HEADER = "retekey-store 2 4\n";

    private BarSlotCodec() {
    }

    static boolean hasExpectedHeader(String stored) {
        return stored != null && stored.startsWith(V2_HEADER);
    }

    static boolean isEmptyV2(String stored) {
        return V2_HEADER.equals(stored);
    }

    static String encode(List<BarSlot> slots) {
        List<String[]> records = new ArrayList<>();
        if (slots != null) {
            for (BarSlot slot : slots) {
                String[] record = recordOf(slot);
                if (record != null) {
                    records.add(record);
                }
            }
        }
        return RecordCodec.encode(records, FIELDS);
    }

    private static String[] recordOf(BarSlot slot) {
        if (slot == null) {
            return null;
        }
        switch (slot.kind()) {
            case BUILT_IN:
                return slot.action() == null
                    ? null
                    : new String[] {BUILT_IN, slot.action().stored(), "", ""};
            case TEXT:
                if (slot.text() == null || slot.text().isEmpty()) {
                    return null;
                }
                return new String[] {TEXT, slot.text(), nullToEmpty(slot.customLabel()), ""};
            default:
                if (slot.key() == null) {
                    return null;
                }
                return new String[] {
                    CHORD, modifiers(slot.modifiers()), slot.key().name(),
                    nullToEmpty(slot.customLabel())
                };
        }
    }

    static List<BarSlot> decode(String stored) {
        List<BarSlot> slots = new ArrayList<>();
        if (stored == null || stored.isEmpty()) {
            return slots;
        }
        if (RecordCodec.isNewFormat(stored)) {
            for (String[] fields : RecordCodec.decode(stored, FIELDS)) {
                BarSlot slot = decodeFields(fields);
                if (slot != null) {
                    slots.add(slot);
                }
            }
            return slots;
        }
        if (!stored.contains(RECORD) && !stored.contains(FIELD)) {
            // The oldest format: nothing but built-in actions, separated by commas.
            for (String word : stored.split(",")) {
                BarAction action = BarAction.parse(word.trim());
                if (action != null) {
                    slots.add(BarSlot.of(action));
                }
            }
            return slots;
        }
        for (String record : stored.split(RECORD, -1)) {
            BarSlot slot = decodeFields(record.split(FIELD, -1));
            if (slot != null) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private static BarSlot decodeFields(String[] fields) {
        if (fields == null || fields.length < 2) {
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

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
