package dev.hellgates.retekeyime;

/**
 * The editor context-menu ids the ☰ menu sends.
 *
 * <p>{@code android.R.id.undo} and {@code redo} are API 23 constants. Their values are frozen
 * platform resource ids, so naming them here lets the app be built against a lower minimum without
 * a version check on a constant — an editor older than 23 simply ignores an action it does not
 * implement, which is what {@code performContextMenuAction} does with any unknown id.
 */
final class EditMenuIds {
    /** {@code android.R.id.undo}, added in API 23. */
    static final int UNDO = 0x0102001a;
    /** {@code android.R.id.redo}, added in API 23. */
    static final int REDO = 0x0102001b;

    private EditMenuIds() {
    }
}
