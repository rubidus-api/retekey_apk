package com.retekey;

import java.util.List;

/**
 * Which key a touch belongs to. Every pixel still belongs to some key; what changes is who owns
 * the strip along an edge where a costly key meets one that types.
 *
 * <p>A near miss is not equally cheap everywhere. Landing on the neighbouring letter is one wrong
 * letter. Landing on 123 or Move turns the whole 12-key pad into digits or a cursor cluster, and
 * every key after it types the wrong thing; landing on ⌫ erases what was just typed; landing on
 * the layer or layout keys leaves the page. On the 12-key pages those keys sit directly beside the
 * first Hangul column and above the space bar, and jittered taps measured on the emulator landed
 * there often enough to wreck a sentence from that point on. So a costly key gives up the part of
 * its cell nearest an ordinary key — {@link #YIELD} of its width or height — and a finger has to
 * mean it to reach it.
 *
 * <p>Android-free, so the geometry is unit-tested.
 */
final class TouchTargets {
    /** How much of a costly key's cell, measured from a shared edge, goes to the key beside it. */
    static final float YIELD = 0.35f;
    /**
     * The same for a modifier (Shift, Ctrl, Alt, Meta). Less, because these are pressed often and
     * on purpose; enough, because every handling error in the fast-roll measurements was a finger
     * that landed on one of them instead of the letter beside it.
     */
    static final float MODIFIER_YIELD = 0.2f;

    /**
     * How much of its cell a key gives up along a shared edge. Enter gives the smaller share: it
     * is aimed at on purpose, and it is at the edge of the board where there is nowhere to overrun
     * to — what it has to stop is the finger that fell short of it.
     */
    private static float yieldFraction(SoftwareKeySpec key) {
        return isModifier(key) || isEnter(key) ? MODIFIER_YIELD : YIELD;
    }

    private TouchTargets() {
    }

    /** How close to the line between a consonant and a vowel the spelling is allowed to decide. */
    static final float JAMO_BAND = 0.22f;

    /** {row, key index}, or null outside the keyboard. */
    static int[] resolve(KeyboardLayout layout, int width, int height, float x, float y) {
        return resolve(layout, width, height, x, y, JamoExpectation.Kind.NONE);
    }

    /**
     * The same, with what the syllable being spelled expects next. A touch that landed within
     * {@link #JAMO_BAND} of the line between a consonant key and a vowel key goes to the one the
     * spelling asks for: after ㅎ only a vowel can follow, so ㅎ|ㅗ resolves to ㅗ, and at the
     * start of a syllable only a consonant can, so it resolves the other way.
     */
    static int[] resolve(KeyboardLayout layout, int width, int height, float x, float y,
                         JamoExpectation.Kind expected) {
        if (layout == null || width <= 0 || height <= 0 || x < 0 || y < 0 || x >= width || y >= height) {
            return null;
        }
        int rows = layout.rows().size();
        int row = Math.min(rows - 1, (int) (y * rows / height));
        int key = keyIndex(layout, row, x, width);
        SoftwareKeySpec hit = layout.rows().get(row).get(key);
        if (!isCostly(hit)) {
            int spelled = spellingNeighbour(layout, row, key, x, width, expected);
            return new int[] {row, spelled < 0 ? key : spelled};
        }
        List<SoftwareKeySpec> keys = layout.rows().get(row);
        float left = layout.columnEdge(layout.startColumn(row, key), width);
        float right = layout.columnEdge(layout.startColumn(row, key) + hit.columnSpan(), width);
        float cellWidth = right - left;
        float yield = cellWidth * yieldFraction(hit);
        if (key > 0 && yieldsTo(hit, keys.get(key - 1)) && x - left < yield) {
            return new int[] {row, key - 1};
        }
        if (key + 1 < keys.size() && yieldsTo(hit, keys.get(key + 1)) && right - x <= yield) {
            return new int[] {row, key + 1};
        }
        float top = layout.rowEdge(row, height);
        float bottom = layout.rowEdge(row + 1, height);
        float cellHeight = bottom - top;
        float yieldY = cellHeight * yieldFraction(hit);
        if (row > 0 && y - top < yieldY) {
            int above = keyIndex(layout, row - 1, x, width);
            if (yieldsTo(hit, layout.rows().get(row - 1).get(above))) {
                return new int[] {row - 1, above};
            }
        }
        if (row + 1 < rows && bottom - y <= yieldY) {
            int below = keyIndex(layout, row + 1, x, width);
            if (yieldsTo(hit, layout.rows().get(row + 1).get(below))) {
                return new int[] {row + 1, below};
            }
        }
        return new int[] {row, key};
    }

    private static int keyIndex(KeyboardLayout layout, int row, float x, int width) {
        int column = Math.min(layout.columns() - 1, (int) (x * layout.columns() / width));
        List<SoftwareKeySpec> keys = layout.rows().get(row);
        int cursor = 0;
        for (int index = 0; index < keys.size(); index++) {
            cursor += keys.get(index).columnSpan();
            if (column < cursor) {
                return index;
            }
        }
        return keys.size() - 1;
    }

    /**
     * The key beside this one that the spelling wants, or -1. Only jamo keys take part, only
     * within the band along their shared edge, and only when the neighbour is the expected kind
     * and this key is not.
     */
    private static int spellingNeighbour(KeyboardLayout layout, int row, int key, float x,
                                         int width, JamoExpectation.Kind expected) {
        if (expected == JamoExpectation.Kind.NONE || jamoKind(layout.rows().get(row).get(key)) == null
                || jamoKind(layout.rows().get(row).get(key)) == expected) {
            return -1;
        }
        List<SoftwareKeySpec> keys = layout.rows().get(row);
        float left = layout.columnEdge(layout.startColumn(row, key), width);
        float right = layout.columnEdge(layout.startColumn(row, key) + keys.get(key).columnSpan(), width);
        float band = (right - left) * JAMO_BAND;
        if (key > 0 && x - left < band && jamoKind(keys.get(key - 1)) == expected) {
            return key - 1;
        }
        if (key + 1 < keys.size() && right - x <= band && jamoKind(keys.get(key + 1)) == expected) {
            return key + 1;
        }
        return -1;
    }

    /** Whether this key types a consonant or a vowel, or null when it types neither. */
    static JamoExpectation.Kind jamoKind(SoftwareKeySpec key) {
        SemanticInput input = key.semanticInput();
        if (!key.enabled() || key.isControl() || input == null
                || input.kind() != SemanticInput.Kind.JAMO || input.jamo() == null) {
            return null;
        }
        switch (input.jamo().role()) {
            case VOWEL:
            case DIRECT_MEDIAL:
                return JamoExpectation.Kind.VOWEL;
            case CONTEXTUAL_CONSONANT:
            case DIRECT_INITIAL:
            case DIRECT_FINAL:
                return JamoExpectation.Kind.CONSONANT;
            default:
                return null;
        }
    }

    /** Shift and the three latches: pressed on purpose often, and mis-hit while rolling. */
    static boolean isModifier(SoftwareKeySpec key) {
        if (!key.isControl()) {
            return false;
        }
        switch (key.control()) {
            case SHIFT:
            case CTRL:
            case ALT:
            case META:
            case RSHIFT:
                return true;
            default:
                return false;
        }
    }

    /** A key whose accidental press costs more than a wrong letter. */
    static boolean isCostly(SoftwareKeySpec key) {
        if ("touch.edit.backspace".equals(key.stableKeyId()) || isEnter(key)) {
            return true;
        }
        if (!key.isControl()) {
            return false;
        }
        if (isModifier(key)) {
            return true;
        }
        switch (key.control()) {
            case PHONE_DIGITS:
            case PHONE_NAV:
            case HANJA:
            case SPECIAL_CHARS_LAYER:
            case SPECIAL_KEYS_LAYER:
            case MENU_LAYER:
            case LAYOUT_TOGGLE:
                return true;
            default:
                return false;
        }
    }

    /** A key that types: the kind a costly neighbour gives its edge to. */
    static boolean isInput(SoftwareKeySpec key) {
        return key.enabled() && !key.isControl() && !isCostly(key);
    }

    /**
     * The space bar and Enter. They type, but a stray one is not "one wrong letter": a space lands
     * in the middle of a word, and Enter sends the message or runs the command. They are also the
     * keys that sit directly under ⌫ on the 12-key pages, and giving them ⌫'s lower third made
     * backspace unreachable in a hurry — pressing it typed a space instead (owner's report,
     * 2026-09-16). So a costly key keeps its whole cell where these two are the neighbour.
     */
    static boolean isStructural(SoftwareKeySpec key) {
        String id = key.stableKeyId();
        return "touch.text.space".equals(id) || "touch.edit.enter".equals(id);
    }

    /** The Enter key, which is where a near miss costs most: it sends, or it runs. */
    static boolean isEnter(SoftwareKeySpec key) {
        return "touch.edit.enter".equals(key.stableKeyId());
    }

    /**
     * Whether {@code costly} gives part of its cell to {@code neighbour}.
     *
     * <p>Letters always take the edge. The space bar and Enter normally do not — that is what made
     * ⌫ unreachable — but the space bar does take Enter's, because a stray Enter sends the message
     * or runs the command, and the pair sits one above the other in the same column where the
     * misses were reported (owner, 2026-09-16: "the key below gets pressed — space instead of
     * backspace, Enter instead of space").
     */
    private static boolean yieldsTo(SoftwareKeySpec costly, SoftwareKeySpec neighbour) {
        if (!isInput(neighbour)) {
            return false;
        }
        return !isStructural(neighbour) || isEnter(costly) && !isEnter(neighbour);
    }
}
