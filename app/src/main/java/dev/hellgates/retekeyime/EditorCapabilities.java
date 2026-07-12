package dev.hellgates.retekeyime;

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
