package com.retekey;

public final class EditorCapabilities {
    public enum DeletionMode {
        RICH_TEXT,
        RAW_KEY,
        UNSUPPORTED
    }

    private static final EditorCapabilities UNSUPPORTED =
        new EditorCapabilities(false, false, false, false, DeletionMode.UNSUPPORTED);
    private static final EditorCapabilities RAW_KEY =
        new EditorCapabilities(true, false, false, false, DeletionMode.RAW_KEY);

    private final boolean supported;
    private final boolean sensitive;
    private final boolean allowLegacyCodeUnitFallback;
    private final boolean allowRawDeleteFallback;
    private final DeletionMode deletionMode;
    /**
     * Deletion goes out as backspace key events rather than deleteSurroundingText. Remote-desktop
     * clients show the IME a local dummy buffer: a surrounding-text delete "succeeds" against that
     * buffer and reaches the remote machine only while recently typed text still sits in it —
     * which is exactly a backspace that works right after typing and dies otherwise. A key event
     * is the one deletion those clients always forward, and the key event <em>is</em> the deletion
     * on their side, so nothing is deleted twice.
     */
    private boolean deleteByKeyEvents;
    /**
     * Nothing behind this editor holds text the IME can edit — a terminal. Its InputConnection is
     * a dummy buffer that forwards <em>commits</em> and key events to a program on the other side
     * and swallows everything else, so a surrounding-text delete reaches the local dummy and stops
     * there. Every deletion, our own take-backs included, has to be a backspace key event.
     */
    private boolean noSurroundingText;

    private EditorCapabilities(
        boolean supported,
        boolean sensitive,
        boolean allowLegacyCodeUnitFallback,
        boolean allowRawDeleteFallback,
        DeletionMode deletionMode
    ) {
        this.supported = supported;
        this.sensitive = sensitive;
        this.allowLegacyCodeUnitFallback = allowLegacyCodeUnitFallback;
        this.allowRawDeleteFallback = allowRawDeleteFallback;
        this.deletionMode = deletionMode;
    }

    public static EditorCapabilities richText(
        boolean sensitive,
        boolean allowLegacyCodeUnitFallback
    ) {
        return richText(sensitive, allowLegacyCodeUnitFallback, false);
    }

    public static EditorCapabilities richText(
        boolean sensitive,
        boolean allowLegacyCodeUnitFallback,
        boolean allowRawDeleteFallback
    ) {
        return new EditorCapabilities(
            true,
            sensitive,
            allowLegacyCodeUnitFallback,
            allowRawDeleteFallback,
            DeletionMode.RICH_TEXT
        );
    }

    public static EditorCapabilities rawKey() {
        return RAW_KEY;
    }

    /** A copy that deletes by backspace key events — the remote-desktop shape. */
    public EditorCapabilities withDeleteByKeyEvents() {
        EditorCapabilities copy = new EditorCapabilities(
            supported, sensitive, allowLegacyCodeUnitFallback, allowRawDeleteFallback, deletionMode);
        copy.deleteByKeyEvents = true;
        return copy;
    }

    public boolean deleteByKeyEvents() {
        return deleteByKeyEvents;
    }

    /**
     * A terminal: no composing region, no editable buffer, deletion only by key event. TYPE_NULL
     * says this outright, and terminals that report a text field say it by being terminals.
     */
    public EditorCapabilities asTerminal() {
        EditorCapabilities copy = new EditorCapabilities(
            true, sensitive, allowLegacyCodeUnitFallback, allowRawDeleteFallback,
            DeletionMode.RAW_KEY);
        copy.deleteByKeyEvents = true;
        copy.noSurroundingText = true;
        return copy;
    }

    /** Whether a surrounding-text call reaches anything (see {@link #noSurroundingText}). */
    public boolean hasSurroundingText() {
        return !noSurroundingText;
    }

    public static EditorCapabilities unsupported() {
        return UNSUPPORTED;
    }

    public boolean isSupported() {
        return supported;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public boolean allowLegacyCodeUnitFallback() {
        return allowLegacyCodeUnitFallback;
    }

    public boolean allowRawDeleteFallback() {
        return allowRawDeleteFallback;
    }

    public DeletionMode deletionMode() {
        return deletionMode;
    }
}
