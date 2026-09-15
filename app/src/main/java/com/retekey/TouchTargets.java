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

    private TouchTargets() {
    }

    /** {row, key index}, or null outside the keyboard. */
    static int[] resolve(KeyboardLayout layout, int width, int height, float x, float y) {
        if (layout == null || width <= 0 || height <= 0 || x < 0 || y < 0 || x >= width || y >= height) {
            return null;
        }
        int rows = layout.rows().size();
        int row = Math.min(rows - 1, (int) (y * rows / height));
        int key = keyIndex(layout, row, x, width);
        SoftwareKeySpec hit = layout.rows().get(row).get(key);
        if (!isCostly(hit)) {
            return new int[] {row, key};
        }
        List<SoftwareKeySpec> keys = layout.rows().get(row);
        float left = layout.columnEdge(layout.startColumn(row, key), width);
        float right = layout.columnEdge(layout.startColumn(row, key) + hit.columnSpan(), width);
        float cellWidth = right - left;
        if (key > 0 && isInput(keys.get(key - 1)) && x - left < cellWidth * YIELD) {
            return new int[] {row, key - 1};
        }
        if (key + 1 < keys.size() && isInput(keys.get(key + 1)) && right - x <= cellWidth * YIELD) {
            return new int[] {row, key + 1};
        }
        float top = layout.rowEdge(row, height);
        float bottom = layout.rowEdge(row + 1, height);
        float cellHeight = bottom - top;
        if (row > 0 && y - top < cellHeight * YIELD) {
            int above = keyIndex(layout, row - 1, x, width);
            if (isInput(layout.rows().get(row - 1).get(above))) {
                return new int[] {row - 1, above};
            }
        }
        if (row + 1 < rows && bottom - y <= cellHeight * YIELD) {
            int below = keyIndex(layout, row + 1, x, width);
            if (isInput(layout.rows().get(row + 1).get(below))) {
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

    /** A key whose accidental press costs more than a wrong letter. */
    static boolean isCostly(SoftwareKeySpec key) {
        if ("touch.edit.backspace".equals(key.stableKeyId())) {
            return true;
        }
        if (!key.isControl()) {
            return false;
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
}
