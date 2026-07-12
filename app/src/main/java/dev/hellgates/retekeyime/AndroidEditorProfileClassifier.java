package dev.hellgates.retekeyime;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;

public final class AndroidEditorProfileClassifier {
    private static final int API_TIRAMISU = 33;

    private AndroidEditorProfileClassifier() {
    }

    public static EditorProfile classify(EditorInfo editorInfo, int platformApi) {
        if (editorInfo == null) {
            return EditorProfile.unsupported();
        }
        return classifyFields(
            editorInfo.inputType,
            editorInfo.imeOptions,
            editorInfo.actionLabel != null,
            editorInfo.actionId,
            platformApi
        );
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
