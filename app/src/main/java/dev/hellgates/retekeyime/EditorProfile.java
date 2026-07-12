package dev.hellgates.retekeyime;

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
            EditorCapabilities.rawKey(),
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
