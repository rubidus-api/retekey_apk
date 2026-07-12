package dev.hellgates.retekeyime;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public final class AndroidEditorProfileClassifierTest {
    @Test
    public void classifiesEveryAndroidPasswordVariationAsSensitive() {
        for (int inputType : Arrays.asList(
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD
        )) {
            EditorProfile profile = AndroidEditorProfileClassifier.classifyFields(
                inputType,
                EditorInfo.IME_ACTION_NONE,
                false,
                0,
                32
            );

            Assert.assertTrue(profile.capabilities().isSensitive());
        }
    }

    @Test
    public void typeNullUsesRawCompatibilityAndRichFallbackBoundaryIsApi33() {
        EditorProfile typeNull = AndroidEditorProfileClassifier.classifyFields(
            InputType.TYPE_NULL,
            EditorInfo.IME_ACTION_NONE,
            false,
            0,
            32
        );
        EditorProfile api32 = AndroidEditorProfileClassifier.classifyFields(
            InputType.TYPE_CLASS_TEXT,
            EditorInfo.IME_ACTION_NONE,
            false,
            0,
            32
        );
        EditorProfile api33 = AndroidEditorProfileClassifier.classifyFields(
            InputType.TYPE_CLASS_TEXT,
            EditorInfo.IME_ACTION_NONE,
            false,
            0,
            33
        );

        Assert.assertEquals(
            EditorCapabilities.DeletionMode.RAW_KEY,
            typeNull.capabilities().deletionMode()
        );
        Assert.assertTrue(api32.capabilities().allowLegacyCodeUnitFallback());
        Assert.assertFalse(api33.capabilities().allowLegacyCodeUnitFallback());
    }

    @Test
    public void preservesMultilineNoEnterCustomAndStandardActionPolicyInputs() {
        EditorProfile custom = AndroidEditorProfileClassifier.classifyFields(
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_ENTER_ACTION,
            true,
            42,
            33
        );
        EditorProfile none = AndroidEditorProfileClassifier.classifyFields(
            InputType.TYPE_CLASS_TEXT,
            EditorInfo.IME_ACTION_NONE,
            false,
            0,
            33
        );

        Assert.assertTrue(custom.isMultiline());
        Assert.assertTrue(custom.hasNoEnterAction());
        Assert.assertTrue(custom.hasCustomAction());
        Assert.assertEquals(42, custom.customActionId());
        Assert.assertEquals(
            Arrays.asList(KeyAction.performEditorAction(42)),
            EditorActionPolicy.enter(custom).actions()
        );
        Assert.assertFalse(none.hasStandardAction());
    }
}
