package com.retekey;

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

    @Test
    public void remoteDesktopPackagesDeleteByKeyEvents() {
        // MS Remote Desktop and friends show the IME a dummy buffer; their deletion must go out
        // as backspace key events (the intermittent-backspace report, 2026-08-27). The flag rides
        // only on rich profiles — a TYPE_NULL remote editor is already fully raw.
        org.junit.Assert.assertTrue(
            AndroidEditorProfileClassifier.isRemoteDesktop("com.microsoft.rdc.androidx"));
        org.junit.Assert.assertTrue(
            AndroidEditorProfileClassifier.isRemoteDesktop("com.microsoft.rdc.android"));
        org.junit.Assert.assertFalse(AndroidEditorProfileClassifier.isRemoteDesktop("com.example"));
        org.junit.Assert.assertFalse(AndroidEditorProfileClassifier.isRemoteDesktop(null));
        EditorProfile plain = AndroidEditorProfileClassifier.classifyFields(
            InputType.TYPE_CLASS_TEXT, 0, false, 0, 34);
        org.junit.Assert.assertFalse(plain.capabilities().deleteByKeyEvents());
        org.junit.Assert.assertTrue(
            plain.withDeleteByKeyEvents().capabilities().deleteByKeyEvents());
    }

    @Test
    public void aTerminalIsRecognisedInBothOfTheShapesItReports() {
        // Termux reports TYPE_NULL when enforce-char-based-input is on and an ordinary
        // visible-password text field when it is off. Both are the same thing behind the
        // connection: a program on a pipe, with no composing region and no buffer (issue #7).
        android.view.inputmethod.EditorInfo charBased = new android.view.inputmethod.EditorInfo();
        charBased.inputType = android.text.InputType.TYPE_NULL;
        charBased.packageName = "com.termux";
        android.view.inputmethod.EditorInfo textBased = new android.view.inputmethod.EditorInfo();
        textBased.inputType = android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        textBased.packageName = "com.termux";

        for (android.view.inputmethod.EditorInfo info : new android.view.inputmethod.EditorInfo[] {
                charBased, textBased}) {
            EditorCapabilities capabilities =
                AndroidEditorProfileClassifier.classify(info, 33).capabilities();
            Assert.assertFalse(info.inputType + " has no buffer", capabilities.hasSurroundingText());
            Assert.assertTrue(info.inputType + " composes by commits",
                capabilities.deleteByKeyEvents());
        }
    }

    @Test
    public void anOrdinaryVisiblePasswordFieldIsNotATerminal() {
        // A login form with "show password" ticked reports exactly what Termux reports with
        // char-based input off. Only the name tells them apart, so only the name may.
        android.view.inputmethod.EditorInfo info = new android.view.inputmethod.EditorInfo();
        info.inputType = android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
        info.packageName = "com.example.shop";

        EditorCapabilities capabilities =
            AndroidEditorProfileClassifier.classify(info, 33).capabilities();
        Assert.assertTrue(capabilities.hasSurroundingText());
        Assert.assertTrue("still never read, never remembered", capabilities.isSensitive());
    }
}
