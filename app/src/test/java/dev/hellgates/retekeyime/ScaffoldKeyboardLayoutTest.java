package dev.hellgates.retekeyime;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public final class ScaffoldKeyboardLayoutTest {
    @Test
    public void enabledKeysHaveUniqueStableIdsAndSemanticInputs() {
        Set<String> ids = new HashSet<>();

        for (List<SoftwareKeySpec> row : ScaffoldKeyboardLayout.rows()) {
            for (SoftwareKeySpec key : row) {
                Assert.assertTrue("duplicate stable key id", ids.add(key.stableKeyId()));
                if (key.enabled()) {
                    Assert.assertNotNull(key.semanticInput());
                }
            }
        }
    }

    @Test
    public void giyeokUsesStructuredJamoAndBackspaceUsesEditCommand() {
        SoftwareKeySpec giyeok = ScaffoldKeyboardLayout.findById("touch.ko2.giyeok");
        SoftwareKeySpec backspace = ScaffoldKeyboardLayout.findById("touch.edit.backspace");

        Assert.assertNotNull(giyeok);
        Assert.assertEquals(
            SemanticInput.jamo(SemanticJamo.contextualConsonant(0)),
            giyeok.semanticInput()
        );
        Assert.assertNotEquals(SemanticInput.text("ㄱ"), giyeok.semanticInput());
        Assert.assertNotNull(backspace);
        Assert.assertEquals(SemanticInput.deleteBackward(), backspace.semanticInput());
    }

    @Test
    public void unimplementedModifierKeysAreExplicitlyDisabled() {
        SoftwareKeySpec ctrl = ScaffoldKeyboardLayout.findById("touch.modifier.ctrl");

        Assert.assertNotNull(ctrl);
        Assert.assertFalse(ctrl.enabled());
        Assert.assertNull(ctrl.semanticInput());
    }

    @Test
    public void enabledJamoKeysHaveVisibleNoLossFallbackUntilComposerLands() {
        InputDispatcher dispatcher = new InputDispatcher();

        for (List<SoftwareKeySpec> row : ScaffoldKeyboardLayout.rows()) {
            for (SoftwareKeySpec key : row) {
                if (key.enabled() && key.semanticInput().kind() == SemanticInput.Kind.JAMO) {
                    DispatchResult result = dispatcher.dispatch(key.pressEvent());
                    Assert.assertTrue(key.stableKeyId(), result.isHandled());
                    Assert.assertEquals(key.stableKeyId(), 1, result.actions().size());
                    Assert.assertEquals(
                        key.stableKeyId(),
                        KeyAction.Kind.COMMIT_TEXT,
                        result.actions().get(0).kind()
                    );
                    Assert.assertEquals(
                        key.stableKeyId(),
                        key.label(),
                        result.actions().get(0).text()
                    );
                }
            }
        }
    }
}
