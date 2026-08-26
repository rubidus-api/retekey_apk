package com.retekey;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;

public final class AndroidEditorProfileClassifier {
    private static final int API_TIRAMISU = 33;

    /**
     * Apps whose editor is a window onto another machine. They show the IME a local dummy buffer,
     * so a surrounding-text delete only reaches the remote side while recently typed text still
     * sits in that buffer — the "backspace works right after typing and sometimes not" report.
     * For these, deletion goes out as backspace key events, which they always forward; everything
     * else (committing, composing) keeps the ordinary rich path, so Hangul still composes.
     */
    private static final java.util.Set<String> REMOTE_DESKTOP_PACKAGES =
        new java.util.HashSet<>(java.util.Arrays.asList(
            "com.microsoft.rdc.android",
            "com.microsoft.rdc.androidx",
            "com.google.chromeremotedesktop",
            "com.google.chromoting"
        ));

    private AndroidEditorProfileClassifier() {
    }

    public static EditorProfile classify(EditorInfo editorInfo, int platformApi) {
        if (editorInfo == null) {
            return EditorProfile.unsupported();
        }
        EditorProfile profile = classifyFields(
            editorInfo.inputType,
            editorInfo.imeOptions,
            editorInfo.actionLabel != null,
            editorInfo.actionId,
            platformApi
        );
        if (isRemoteDesktop(editorInfo.packageName)
                && profile.capabilities().deletionMode()
                    == EditorCapabilities.DeletionMode.RICH_TEXT) {
            return profile.withDeleteByKeyEvents();
        }
        return profile;
    }

    /** Whether this package's editor is a remote-desktop window (see the set above). */
    static boolean isRemoteDesktop(String packageName) {
        return packageName != null && REMOTE_DESKTOP_PACKAGES.contains(packageName);
    }

    static EditorProfile classifyFields(
        int inputType,
        int imeOptions,
        boolean customActionPresent,
        int customActionId,
        int platformApi
    ) {
        if (platformApi < 1) {
            throw new IllegalArgumentException("platformApi must be positive");
        }
        boolean noEnterAction = (imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;
        int maskedAction = imeOptions & EditorInfo.IME_MASK_ACTION;
        int standardActionId = maskedAction == EditorInfo.IME_ACTION_NONE
            ? -1
            : maskedAction;
        int inputClass = inputType & InputType.TYPE_MASK_CLASS;

        if (inputType == InputType.TYPE_NULL) {
            return EditorProfile.typeNull(
                noEnterAction,
                customActionPresent,
                customActionId,
                standardActionId
            );
        }

        boolean multiline = inputClass == InputType.TYPE_CLASS_TEXT
            && (inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0;
        boolean sensitive = isSensitive(inputType, inputClass);
        return EditorProfile.richText(
            sensitive,
            platformApi < API_TIRAMISU,
            platformApi < API_TIRAMISU,
            multiline,
            noEnterAction,
            customActionPresent,
            customActionId,
            standardActionId
        );
    }

    private static boolean isSensitive(int inputType, int inputClass) {
        int variation = inputType & InputType.TYPE_MASK_VARIATION;
        if (inputClass == InputType.TYPE_CLASS_TEXT) {
            return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD;
        }
        return inputClass == InputType.TYPE_CLASS_NUMBER
            && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD;
    }
}
