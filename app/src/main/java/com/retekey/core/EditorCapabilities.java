package com.retekey.core;

public final class EditorCapabilities {
    public enum DeletionMode {
        RICH_TEXT,
        RAW_KEY,
        UNSUPPORTED
    }

    public enum EditorKind {
        RICH_TEXT,
        REMOTE_DESKTOP,
        TERMINAL,
        UNSUPPORTED
    }

    private static final EditorCapabilities UNSUPPORTED = new EditorCapabilities(
        false, false, false, false, DeletionMode.UNSUPPORTED,
        EditorKind.UNSUPPORTED, false, false, false);
    private static final EditorCapabilities RAW_KEY = new EditorCapabilities(
        true, false, false, false, DeletionMode.RAW_KEY,
        EditorKind.TERMINAL, false, false, false);

    private final boolean supported;
    private final boolean sensitive;
    private final boolean allowLegacyCodeUnitFallback;
    private final boolean allowRawDeleteFallback;
    private final DeletionMode deletionMode;
    private final EditorKind editorKind;
    /**
     * Deletion goes out as backspace key events rather than deleteSurroundingText. Remote-desktop
     * clients show the IME a local dummy buffer: a surrounding-text delete "succeeds" against that
     * buffer and reaches the remote machine only while recently typed text still sits in it —
     * which is exactly a backspace that works right after typing and dies otherwise. A key event
     * is the one deletion those clients always forward, and the key event <em>is</em> the deletion
     * on their side, so nothing is deleted twice.
     */
    private final boolean deleteByKeyEvents;
    /**
     * Nothing behind this editor holds text the IME can edit — a terminal. Its InputConnection is
     * a dummy buffer that forwards <em>commits</em> and key events to a program on the other side
     * and swallows everything else, so a surrounding-text delete reaches the local dummy and stops
     * there. Every deletion, our own take-backs included, has to be a backspace key event.
     */
    private final boolean noSurroundingText;
    private final boolean composesOffScreen;

    private EditorCapabilities(
        boolean supported,
        boolean sensitive,
        boolean allowLegacyCodeUnitFallback,
        boolean allowRawDeleteFallback,
        DeletionMode deletionMode,
        EditorKind editorKind,
        boolean deleteByKeyEvents,
        boolean noSurroundingText,
        boolean composesOffScreen
    ) {
        this.supported = supported;
        this.sensitive = sensitive;
        this.allowLegacyCodeUnitFallback = allowLegacyCodeUnitFallback;
        this.allowRawDeleteFallback = allowRawDeleteFallback;
        this.deletionMode = deletionMode;
        this.editorKind = editorKind;
        this.deleteByKeyEvents = deleteByKeyEvents;
        this.noSurroundingText = noSurroundingText;
        this.composesOffScreen = composesOffScreen;
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
            DeletionMode.RICH_TEXT,
            EditorKind.RICH_TEXT,
            false,
            false,
            false
        );
    }

    public static EditorCapabilities rawKey() {
        return RAW_KEY;
    }

    /** A copy that deletes by backspace key events — the remote-desktop shape. */
    public EditorCapabilities withDeleteByKeyEvents() {
        return new EditorCapabilities(
            supported,
            sensitive,
            allowLegacyCodeUnitFallback,
            allowRawDeleteFallback,
            deletionMode,
            noSurroundingText ? EditorKind.TERMINAL : EditorKind.REMOTE_DESKTOP,
            true,
            noSurroundingText,
            composesOffScreen
        );
    }

    public boolean deleteByKeyEvents() {
        return deleteByKeyEvents;
    }

    /**
     * A terminal: no composing region, no editable buffer, deletion only by key event. TYPE_NULL
     * says this outright, and terminals that report a text field say it by being terminals.
     */
    public EditorCapabilities asTerminal() {
        return new EditorCapabilities(
            true,
            sensitive,
            allowLegacyCodeUnitFallback,
            allowRawDeleteFallback,
            DeletionMode.RAW_KEY,
            EditorKind.TERMINAL,
            true,
            true,
            composesOffScreen
        );
    }

    /**
     * A copy that keeps the composing text to itself: the preedit is shown on the keyboard's own
     * strip and only closed syllables are committed. For a terminal, where there is no composing
     * region to draw into and drawing it as commits means taking those characters back again —
     * with every race, flicker and stray cursor that entails (issue #7).
     */
    public EditorCapabilities composingOffScreen() {
        return new EditorCapabilities(
            supported,
            sensitive,
            allowLegacyCodeUnitFallback,
            allowRawDeleteFallback,
            deletionMode,
            editorKind,
            deleteByKeyEvents,
            noSurroundingText,
            true
        );
    }

    /** Whether the preedit is kept out of the editor entirely (see {@link #composingOffScreen}). */
    public boolean composesOffScreen() {
        return composesOffScreen;
    }

    public EditorKind kind() {
        return editorKind;
    }

    public boolean isRemoteDesktop() {
        return editorKind == EditorKind.REMOTE_DESKTOP;
    }

    /**
     * A terminal rather than a remote desktop: both send keys instead of editing text, but a
     * terminal has no buffer of its own at all. The difference matters where the far side reads a
     * chord differently — Ctrl+V is a paste on a Windows desktop and quoted-insert in a shell.
     */
    public boolean isTerminal() {
        return deleteByKeyEvents && noSurroundingText;
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
