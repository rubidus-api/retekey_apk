package dev.hellgates.retekeyime;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public final class EditorActionPolicyTest {
    @Test
    public void customActionAlwaysUsesExactIdIncludingZero() {
        for (boolean noEnterAction : Arrays.asList(false, true)) {
            EditorProfile profile = EditorProfile.richText(
                false,
                false,
                false,
                noEnterAction,
                true,
                0,
                -1
            );

            DispatchResult result = EditorActionPolicy.enter(profile);

            Assert.assertEquals(
                Arrays.asList(KeyAction.performEditorAction(0)),
                result.actions()
            );
        }
    }

    @Test
    public void standardActionsUseExactCapturedConnectionIdWhenNotSuppressed() {
        for (int actionId : Arrays.asList(0, 2, 3, 4, 5, 6, 7)) {
            EditorProfile profile = EditorProfile.richText(
                false,
                false,
                true,
                false,
                false,
                0,
                actionId
            );

            Assert.assertEquals(
                Arrays.asList(KeyAction.performEditorAction(actionId)),
                EditorActionPolicy.enter(profile).actions()
            );
        }
    }

    @Test
    public void multilineWithoutActionCommitsNewline() {
        for (boolean noEnterAction : Arrays.asList(false, true)) {
            EditorProfile profile = EditorProfile.richText(
                false,
                false,
                true,
                noEnterAction,
                false,
                0,
                -1
            );

            Assert.assertEquals(
                Arrays.asList(KeyAction.commitText("\n")),
                EditorActionPolicy.enter(profile).actions()
            );
        }
    }

    @Test
    public void noEnterActionSuppressesStandardActionBeforeMultilineFallback() {
        EditorProfile profile = EditorProfile.richText(
            false,
            false,
            true,
            true,
            false,
            0,
            6
        );

        Assert.assertEquals(
            Arrays.asList(KeyAction.commitText("\n")),
            EditorActionPolicy.enter(profile).actions()
        );
    }

    @Test
    public void enterWithoutAnEditorActionSendsARawEnterOnAnyEditor() {
        // Both a terminal (TYPE_NULL) and a plain single-line rich field with no IME action send a
        // real Enter key, which every editor understands. Doing nothing left Enter dead.
        Assert.assertEquals(
            Arrays.asList(KeyAction.rawEnter()),
            EditorActionPolicy.enter(EditorProfile.typeNull(false, false, 0, -1)).actions()
        );
        Assert.assertEquals(
            Arrays.asList(KeyAction.rawEnter()),
            EditorActionPolicy.enter(EditorProfile.richText(
                false, false, false, false, false, 0, -1)).actions()
        );
    }
}
