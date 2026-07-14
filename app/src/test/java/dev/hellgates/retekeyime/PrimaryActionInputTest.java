package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import org.junit.Test;

/**
 * The Enter key emits one profile-free semantic input. Only the editor profile decides whether
 * that becomes a newline, a standard action, a custom action, or a delegated raw key.
 */
public final class PrimaryActionInputTest {
    @Test
    public void enterKeyEmitsThePrimaryActionRatherThanANewline() {
        SemanticInput enter = KeyboardLayouts
            .of(KeyboardLayoutId.KO_DUBEOLSIK, false)
            .findById("touch.edit.enter")
            .semanticInput();

        assertEquals(SemanticInput.Kind.PRIMARY_ACTION, enter.kind());
        assertEquals("", enter.text());
    }

    @Test
    public void multilineEditorsCommitANewline() {
        EditorProfile multiline = EditorProfile.richText(
            false,
            false,
            true,
            true,
            false,
            0,
            -1
        );
        StatelessInputProcessor processor = new ScaffoldInputProcessor(() -> multiline);

        DispatchResult result = processor.process(SemanticInput.primaryAction());

        assertEquals(Arrays.asList(KeyAction.commitText("\n")), result.actions());
    }

    @Test
    public void editorsWithAStandardActionPerformThatAction() {
        EditorProfile searchField = EditorProfile.richText(
            false,
            false,
            false,
            false,
            false,
            0,
            3
        );
        StatelessInputProcessor processor = new ScaffoldInputProcessor(() -> searchField);

        DispatchResult result = processor.process(SemanticInput.primaryAction());

        assertEquals(Arrays.asList(KeyAction.performEditorAction(3)), result.actions());
    }

    @Test
    public void unsupportedEditorsDelegateInsteadOfGuessing() {
        StatelessInputProcessor processor = new ScaffoldInputProcessor();

        DispatchResult result = processor.process(SemanticInput.primaryAction());

        assertFalse(result.isHandled());
        assertEquals(Arrays.asList(), result.actions());
    }
}
