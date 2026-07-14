package dev.hellgates.retekeyime;

/**
 * Geometry and hit testing for the popup that a held key opens.
 *
 * <p>The popup is one row of equal cells laid out over the key's own row, clamped to the keyboard
 * so a key near an edge still shows every candidate. It is pure: the View draws and the pointer
 * selects from the same numbers.
 */
public final class LongPressPopup {
    private final SoftwareKeySpec key;
    private final int candidateCount;
    private final int left;
    private final int top;
    private final int cellWidth;
    private final int height;

    private LongPressPopup(
        SoftwareKeySpec key,
        int candidateCount,
        int left,
        int top,
        int cellWidth,
        int height
    ) {
        this.key = key;
        this.candidateCount = candidateCount;
        this.left = left;
        this.top = top;
        this.cellWidth = cellWidth;
        this.height = height;
    }

    /**
     * @param rowIndex the row the held key sits in
     * @param keyIndex the key's index within that row
     * @return null when the key has no alternates or the view has no size
     */
    public static LongPressPopup open(
        KeyboardLayout layout,
        int rowIndex,
        int keyIndex,
        int width,
        int height
    ) {
        SoftwareKeySpec key = layout.rows().get(rowIndex).get(keyIndex);
        if (!key.hasLongPress() || width <= 0 || height <= 0) {
            return null;
        }
        int rows = layout.rows().size();
        int rowHeight = Math.max(1, height / rows);
        int candidateCount = key.longPressTexts().size();
        // Candidates never get narrower than a key: a twelve-way popup is wider than the keyboard
        // otherwise, and a half-width target is not hittable with a thumb.
        int cellWidth = Math.max(1, width / KeyboardLayouts.COLUMNS);
        int popupWidth = Math.min(width, cellWidth * candidateCount);
        int startColumn = layout.startColumn(rowIndex, keyIndex);
        int keyLeft = layout.columnEdge(startColumn, width);
        int keyRight = layout.columnEdge(startColumn + key.columnSpan(), width);
        int centered = (keyLeft + keyRight) / 2 - popupWidth / 2;
        int clamped = Math.max(0, Math.min(centered, width - popupWidth));
        int popupCellWidth = Math.max(1, popupWidth / candidateCount);
        // Sit above the held key, or below it when the key is already in the top row.
        int keyTop = layout.rowEdge(rowIndex, height);
        int top = rowIndex > 0 ? keyTop - rowHeight : keyTop + rowHeight;
        return new LongPressPopup(key, candidateCount, clamped, top, popupCellWidth, rowHeight);
    }

    public SoftwareKeySpec key() {
        return key;
    }

    public int candidateCount() {
        return candidateCount;
    }

    public String candidate(int index) {
        return key.longPressTexts().get(index);
    }

    public int left() {
        return left;
    }

    public int top() {
        return top;
    }

    public int bottom() {
        return top + height;
    }

    public int right() {
        return left + cellWidth * candidateCount;
    }

    public int cellLeft(int index) {
        return left + cellWidth * index;
    }

    public int cellWidth() {
        return cellWidth;
    }

    /**
     * The candidate under the pointer, or -1 when the pointer is outside the popup. Vertical
     * position is forgiving: anywhere in the popup's column band selects, because a thumb slides up
     * past the popup while choosing.
     */
    public int indexAt(float x, float y) {
        if (x < left || x >= right()) {
            return -1;
        }
        if (y >= bottom()) {
            return -1;
        }
        int index = (int) ((x - left) / cellWidth);
        return Math.min(candidateCount - 1, Math.max(0, index));
    }
}
