package dev.hellgates.retekeyime;

public final class EditorActionPolicy {
    private EditorActionPolicy() {
    }

    public static DispatchResult enter(EditorProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("profile must not be null");
        }
        if (!profile.capabilities().isSupported()) {
            return DispatchResult.delegate();
        }
        if (profile.hasCustomAction()) {
            return DispatchResult.handled(
                KeyAction.performEditorAction(profile.customActionId())
            );
        }
        if (!profile.hasNoEnterAction() && profile.hasStandardAction()) {
            return DispatchResult.handled(
                KeyAction.performEditorAction(profile.standardActionId())
            );
        }
        if (profile.isMultiline()) {
            return DispatchResult.handled(KeyAction.commitText("\n"));
        }
        if (profile.capabilities().deletionMode()
            == EditorCapabilities.DeletionMode.RAW_KEY) {
            return DispatchResult.handled(KeyAction.rawEnter());
        }
        return DispatchResult.handled();
    }
}
