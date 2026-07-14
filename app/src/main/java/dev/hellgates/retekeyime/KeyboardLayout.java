package dev.hellgates.retekeyime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A uniform orthogonal key grid.
 *
 * <p>Every row spans exactly {@link #columns()} columns and every key occupies a whole number of
 * columns, so keys are never staggered and only a declared span makes a key wider. Column and row
 * edges are computed from the view size, which keeps geometry and hit testing identical.
 */
public final class KeyboardLayout {
    private final KeyboardLayoutId id;
    private final boolean shifted;
    private final int columns;
    private final List<List<SoftwareKeySpec>> rows;

    private KeyboardLayout(
        KeyboardLayoutId id,
        boolean shifted,
        int columns,
        List<List<SoftwareKeySpec>> rows
    ) {
        if (id == null) {
            throw new IllegalArgumentException("layout id must not be null");
        }
        if (columns < 1) {
            throw new IllegalArgumentException("layout needs at least one column");
        }
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("layout needs at least one row");
        }
        for (List<SoftwareKeySpec> row : rows) {
            if (row.isEmpty()) {
                throw new IllegalArgumentException("layout rows must not be empty");
            }
            int spanned = 0;
            for (SoftwareKeySpec key : row) {
                spanned += key.columnSpan();
            }
            if (spanned != columns) {
                throw new IllegalArgumentException(
                    "every row must span exactly " + columns + " columns but one spans " + spanned
                );
            }
        }
        this.id = id;
        this.shifted = shifted;
        this.columns = columns;
        this.rows = rows;
    }

    public static KeyboardLayout of(
        KeyboardLayoutId id,
        boolean shifted,
        int columns,
        List<List<SoftwareKeySpec>> rows
    ) {
        List<List<SoftwareKeySpec>> copied = new ArrayList<>(rows.size());
        for (List<SoftwareKeySpec> row : rows) {
            copied.add(Collections.unmodifiableList(new ArrayList<>(row)));
        }
        return new KeyboardLayout(id, shifted, columns, Collections.unmodifiableList(copied));
    }

    public static List<SoftwareKeySpec> row(SoftwareKeySpec... keys) {
        return Collections.unmodifiableList(Arrays.asList(keys.clone()));
    }

    public KeyboardLayoutId id() {
        return id;
    }

    public boolean shifted() {
        return shifted;
    }

    public int columns() {
        return columns;
    }

    public List<List<SoftwareKeySpec>> rows() {
        return rows;
    }

    public SoftwareKeySpec findById(String stableKeyId) {
        if (stableKeyId == null) {
            return null;
        }
        for (List<SoftwareKeySpec> row : rows) {
            for (SoftwareKeySpec key : row) {
                if (stableKeyId.equals(key.stableKeyId())) {
                    return key;
                }
            }
        }
        return null;
    }

    /** First column occupied by the key at {@code keyIndex} of {@code rowIndex}. */
    public int startColumn(int rowIndex, int keyIndex) {
        List<SoftwareKeySpec> keys = rows.get(rowIndex);
        int column = 0;
        for (int i = 0; i < keyIndex; i++) {
            column += keys.get(i).columnSpan();
        }
        return column;
    }

    /** Pixel edge of a column boundary, shared by drawing and hit testing. */
    public int columnEdge(int column, int width) {
        return column * width / columns;
    }

    /** Pixel edge of a row boundary, shared by drawing and hit testing. */
    public int rowEdge(int rowIndex, int height) {
        return rowIndex * height / rows.size();
    }

    public SoftwareKeySpec keyAt(float x, float y, int width, int height) {
        if (width <= 0 || height <= 0 || x < 0.0f || y < 0.0f || x >= width || y >= height) {
            return null;
        }
        int rowIndex = Math.min(rows.size() - 1, (int) (y * rows.size() / height));
        int column = Math.min(columns - 1, (int) (x * columns / width));
        List<SoftwareKeySpec> keys = rows.get(rowIndex);
        int cursor = 0;
        for (SoftwareKeySpec key : keys) {
            cursor += key.columnSpan();
            if (column < cursor) {
                return key;
            }
        }
        return keys.get(keys.size() - 1);
    }
}
