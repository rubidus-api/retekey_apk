package com.retekey;

import java.util.Objects;

public final class EditorProfile {
    private final EditorCapabilities capabilities;
    private final boolean multiline;
    private final boolean noEnterAction;
    private final boolean customActionPresent;
    private final int customActionId;
    private final int standardActionId;

    private EditorProfile(
        EditorCapabilities capabilities,
        boolean multiline,
        boolean noEnterAction,
        boolean customActionPresent,
        int customActionId,
        int standardActionId
    ) {
        if (standardActionId < -1) {
            throw new IllegalArgumentException("standardActionId must be -1 or non-negative");
        }
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.multiline = multiline;
        this.noEnterAction = noEnterAction;
        this.customActionPresent = customActionPresent;
        this.customActionId = customActionId;
        this.standardActionId = standardActionId;
    }

    public static EditorProfile richText(
        boolean sensitive,
        boolean allowLegacyCodeUnitFallback,
        boolean multiline,
        boolean noEnterAction,
        boolean customActionPresent,
        int customActionId,
        int standardActionId
    ) {
        return richText(
            sensitive,
            allowLegacyCodeUnitFallback,
            false,
            multiline,
            noEnterAction,
            customActionPresent,
            customActionId,
            standardActionId
        );
    }

    public static EditorProfile richText(
        boolean sensitive,
        boolean allowLegacyCodeUnitFallback,
        boolean allowRawDeleteFallback,
        boolean multiline,
        boolean noEnterAction,
        boolean customActionPresent,
        int customActionId,
        int standardActionId
    ) {
        return new EditorProfile(
            EditorCapabilities.richText(
                sensitive,
                allowLegacyCodeUnitFallback,
                allowRawDeleteFallback
            ),
            multiline,
            noEnterAction,
            customActionPresent,
            customActionId,
            standardActionId
        );
    }

    public static EditorProfile typeNull(
        boolean noEnterAction,
        boolean customActionPresent,
        int customActionId,
        int standardActionId
    ) {
        return new EditorProfile(
            // A TYPE_NULL editor has no composing region and no buffer — that is what TYPE_NULL
            // says. Composition is materialised as commits, and every delete is a key event.
            EditorCapabilities.rawKey().asTerminal(),
            false,
            noEnterAction,
            customActionPresent,
            customActionId,
            standardActionId
        );
    }

    public static EditorProfile unsupported() {
        return new EditorProfile(
            EditorCapabilities.unsupported(),
            false,
            true,
            false,
            0,
            -1
        );
    }

    public EditorCapabilities capabilities() {
        return capabilities;
    }

    /** A copy whose deletion goes out as backspace key events (remote-desktop clients). */
    public EditorProfile withDeleteByKeyEvents() {
        return new EditorProfile(
            capabilities.withDeleteByKeyEvents(),
            multiline,
            noEnterAction,
            customActionPresent,
            customActionId,
            standardActionId
        );
    }

    /** The same editor, with the preedit kept on the keyboard's own strip instead of in it. */
    public EditorProfile composingOffScreen() {
        return new EditorProfile(
            capabilities.composingOffScreen(),
            multiline,
            noEnterAction,
            customActionPresent,
            customActionId,
            standardActionId
        );
    }

    /** The same editor, known to be a terminal: no composing region, no buffer, keys for deletes. */
    public EditorProfile asTerminal() {
        return new EditorProfile(
            capabilities.asTerminal(),
            multiline,
            noEnterAction,
            customActionPresent,
            customActionId,
            standardActionId
        );
    }

    public boolean isMultiline() {
        return multiline;
    }

    public boolean hasNoEnterAction() {
        return noEnterAction;
    }

    public boolean hasCustomAction() {
        return customActionPresent;
    }

    public int customActionId() {
        return customActionId;
    }

    public boolean hasStandardAction() {
        return standardActionId >= 0;
    }

    public int standardActionId() {
        if (!hasStandardAction()) {
            throw new IllegalStateException("profile has no standard action");
        }
        return standardActionId;
    }
}
