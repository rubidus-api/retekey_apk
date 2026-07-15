package dev.hellgates.retekeyime;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ModifierChordPolicyTest {
    @Test
    public void plainLetterIsHandledByTheIme() {
        assertFalse(ModifierChordPolicy.passThroughToApp(false, false, false, false));
    }

    @Test
    public void shiftLetterStaysWithTheIme() {
        // Shift is not one of the pass-through modifiers: Shift+letter is ordinary text.
        assertFalse(ModifierChordPolicy.passThroughToApp(false, false, false, false));
    }

    @Test
    public void ctrlChordGoesToTheApp() {
        // Ctrl+A / Ctrl+C / Ctrl+V / Ctrl+X must reach the app's shortcut handling.
        assertTrue(ModifierChordPolicy.passThroughToApp(false, true, false, false));
    }

    @Test
    public void altAndMetaChordsGoToTheApp() {
        assertTrue(ModifierChordPolicy.passThroughToApp(false, false, true, false));
        assertTrue(ModifierChordPolicy.passThroughToApp(false, false, false, true));
    }

    @Test
    public void modifierKeysThemselvesGoToTheApp() {
        // So the app can track that Ctrl/Alt/Meta is held for its own chord detection.
        assertTrue(ModifierChordPolicy.passThroughToApp(true, false, false, false));
    }
}
