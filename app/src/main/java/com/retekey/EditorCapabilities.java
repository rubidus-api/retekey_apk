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
