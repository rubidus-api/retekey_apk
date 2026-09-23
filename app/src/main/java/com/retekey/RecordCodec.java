package com.retekey;

import java.util.ArrayList;
import java.util.List;

/**
 * The stored form of the keyboard's own lists — notes, clip history, shared-text stash.
 *
 * <p>The first form separated records with U+001E and fields with U+001F, on the grounds that no
 * keyboard types those. Text does not only come from a keyboard: pasted or entered by code point,
 * one U+001E turned a note into two notes and made a clip disappear on save (review R06, R07).
 * Here every field says how long it is, so nothing inside a field can end it, and the header says
 * how many fields a record has, so a store written for one list is not read as another.
 *
 * <p>Form: {@code retekey-store 2 <fields>\n} then, for every field, its length in UTF-16 units,
 * a colon, and the text itself. Android-free, so what a device wrote a test can read.
 */
final class RecordCodec {
    private static final String HEAD = "retekey-store 2 ";

    private RecordCodec() {
    }

    /** Whether {@code stored} was written by this codec rather than the first form. */
    static boolean isNewFormat(String stored) {
        return stored != null && stored.startsWith(HEAD);
    }

    static String encode(List<String[]> records, int fields) {
        StringBuilder out = new StringBuilder();
        out.append(HEAD).append(fields).append('\n');
        for (String[] record : records) {
            for (int i = 0; i < fields; i++) {
                String field = i < record.length && record[i] != null ? record[i] : "";
                out.append(field.length()).append(':').append(field);
            }
        }
        return out.toString();
    }

    /**
     * The records {@code stored} holds. A store written for a different record shape, or one whose
     * text is not this form, reads as no records; a store cut short keeps the records that are
     * whole, and the caller keeps the original rather than writing this back over it.
     */
    static List<String[]> decode(String stored, int fields) {
        List<String[]> records = new ArrayList<>();
        if (!isNewFormat(stored)) {
            return records;
        }
        int newline = stored.indexOf('\n');
        if (newline < 0) {
            return records;
        }
        try {
            if (Integer.parseInt(stored.substring(HEAD.length(), newline)) != fields) {
                return records;
            }
        } catch (NumberFormatException notThisCodec) {
            return records;
        }
        int at = newline + 1;
        while (at < stored.length()) {
            String[] record = new String[fields];
            for (int i = 0; i < fields; i++) {
                int colon = stored.indexOf(':', at);
                if (colon < 0) {
                    return records;
                }
                int length;
                try {
                    length = Integer.parseInt(stored.substring(at, colon));
                } catch (NumberFormatException damaged) {
                    return records;
                }
                if (length < 0 || colon + 1 + length > stored.length()) {
                    return records;
                }
                record[i] = stored.substring(colon + 1, colon + 1 + length);
                at = colon + 1 + length;
            }
            records.add(record);
        }
        return records;
    }
}
