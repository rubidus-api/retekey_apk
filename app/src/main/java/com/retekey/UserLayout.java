package com.retekey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A layout somebody else wrote: the letters and what they hold, and nothing else.
 *
 * <p>Issue #11 asked for a way to add a keyboard without waiting for a release — a file you share
 * into the app. The danger in that is not the parsing; it is the promise. A format published in a
 * release has to be read for ever, so this one says as little as it can get away with: the keys of
 * three rows and the characters they hold. Everything around them — the bottom row, the modifier
 * column, Shift, backspace, Enter, the layout key — is the keyboard's own and is not up for
 * negotiation. That is what keeps the promise small enough to make.
 *
 * <p>The format is lines, not JSON, so that a person can write one in a message and read it back
 * in one:
 *
 * <pre>
 * retekey-layout 1
 * name: Greek phonetic
 * cap: grk
 * row: α β γ|ϝ δ ε|έ ζ η|ή θ ι|ί κ
 * row: λ μ ν ξ ο|ό π ρ σ|ς τ
 * row: υ|ύ φ χ ψ ω|ώ
 * </pre>
 *
 * <p>A cell is the character the key types; anything after {@code |} is what it holds, separated by
 * more {@code |}. Unknown lines are ignored rather than refused — the next version of this format
 * will add lines, and a file from it must still work here. That rule is the whole compatibility
 * story, so it is tested.
 *
 * <p>Android-free: parsing and validation are unit tests, not something to find out on a phone.
 */
public final class UserLayout {
    /** The first line of every file this understands. */
    public static final String HEADER = "retekey-layout 1";

    /**
     * A whole layout, small enough to read at a glance — shown on the settings screen so that the
     * format can be learned from the thing itself rather than from a description of it (the
     * reporter of #11 asked for exactly this). It is parsed by a test, so the example on screen
     * cannot drift away from what the parser accepts.
     */
    public static final String EXAMPLE = HEADER + "\n"
        + "name: Greek phonetic\n"
        + "cap: grk\n"
        + "row: α β γ|ϝ δ ε|έ ζ η|ή θ ι|ί κ\n"
        + "row: λ μ ν ξ ο|ό π ρ σ|ς τ\n"
        + "row: υ|ύ φ χ ψ ω|ώ";
    /** How many rows of letters a layout has: the keyboard's own shape. */
    public static final int ROWS = 3;
    /** The most keys a row may carry, which is the grid's width. */
    public static final int MAX_KEYS_PER_ROW = 10;
    /** The most a key may hold. */
    public static final int MAX_HOLDS = 5;
    /** How long a name may be before it stops fitting anywhere it is shown. */
    public static final int MAX_NAME = 40;

    /** One key: what it types, and what it holds. */
    public static final class Key {
        public final String types;
        public final List<String> holds;

        Key(String types, List<String> holds) {
            this.types = types;
            this.holds = Collections.unmodifiableList(holds);
        }
    }

    private final String name;
    private final String cap;
    private final List<List<Key>> rows;

    private UserLayout(String name, String cap, List<List<Key>> rows) {
        this.name = name;
        this.cap = cap;
        this.rows = Collections.unmodifiableList(rows);
    }

    public String name() {
        return name;
    }

    /** The three letters the layout key shows for this layout. */
    public String cap() {
        return cap;
    }

    public List<List<Key>> rows() {
        return rows;
    }

    /** Whether this text looks like a layout file at all — the cheapest question there is. */
    public static boolean looksLikeOne(String text) {
        return text != null && text.trim().startsWith(HEADER);
    }

    /**
     * Reads a layout, or returns null when the text is not one this version can use. Null is the
     * whole error report on purpose: the caller has one thing to say to the user either way.
     */
    public static UserLayout parse(String text) {
        if (!looksLikeOne(text)) {
            return null;
        }
        String name = null;
        String cap = null;
        List<List<Key>> rows = new ArrayList<>(ROWS);
        for (String raw : text.split("\r\n|\n|\r")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#") || line.equals(HEADER)) {
                continue;
            }
            if (line.startsWith("name:")) {
                name = trimmedTo(line.substring(5).trim(), MAX_NAME);
            } else if (line.startsWith("cap:")) {
                cap = line.substring(4).trim();
            } else if (line.startsWith("row:")) {
                if (rows.size() < ROWS) {
                    List<Key> keys = keysOf(line.substring(4).trim());
                    if (keys.isEmpty()) {
                        return null;
                    }
                    rows.add(keys);
                }
            }
            // Anything else is a line from a later version of the format: ignored, not refused.
        }
        if (name == null || name.isEmpty() || rows.size() != ROWS) {
            return null;
        }
        return new UserLayout(name, capOf(cap, name), rows);
    }

    private static List<Key> keysOf(String row) {
        List<Key> keys = new ArrayList<>(MAX_KEYS_PER_ROW);
        for (String cell : row.split("\\s+")) {
            if (cell.isEmpty() || keys.size() >= MAX_KEYS_PER_ROW) {
                continue;
            }
            String[] parts = cell.split("\\|", -1);
            String types = parts[0];
            if (types.isEmpty()) {
                continue;
            }
            List<String> holds = new ArrayList<>(MAX_HOLDS);
            for (int i = 1; i < parts.length && holds.size() < MAX_HOLDS; i++) {
                if (!parts[i].isEmpty()) {
                    holds.add(parts[i]);
                }
            }
            keys.add(new Key(types, holds));
        }
        return keys;
    }

    /** Three letters for the layout key: the file's own, or the start of its name. */
    private static String capOf(String cap, String name) {
        String from = cap == null || cap.trim().isEmpty() ? name : cap.trim();
        StringBuilder out = new StringBuilder(3);
        // By code point: a character outside the basic plane takes two units, and stopping
        // between them would leave half of it on the key (R08).
        for (int i = 0; i < from.length() && out.length() < 3;) {
            int codePoint = from.codePointAt(i);
            if (!Character.isWhitespace(codePoint)) {
                out.appendCodePoint(codePoint);
            }
            i += Character.charCount(codePoint);
        }
        while (out.length() < 3) {
            out.append('·');
        }
        return out.toString();
    }

    private static String trimmedTo(String text, int limit) {
        return Scalars.truncate(text, limit);
    }
}
