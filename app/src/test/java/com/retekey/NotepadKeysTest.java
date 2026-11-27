package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.retekey.NotepadKeys.Command;
import java.util.EnumSet;
import org.junit.Test;
import com.retekey.core.KeyModifier;
import com.retekey.core.RawKey;

/** The notepad's own reading of editing keys — which used to fall through to the app behind. */
public final class NotepadKeysTest {
    private static final EnumSet<KeyModifier> NONE = EnumSet.noneOf(KeyModifier.class);
    private static final EnumSet<KeyModifier> CTRL = EnumSet.of(KeyModifier.CTRL);
    private static final EnumSet<KeyModifier> SHIFT = EnumSet.of(KeyModifier.SHIFT);

    @Test
    public void theClipboardChordsAreTheNotepadsOwn() {
        assertEquals(Command.SELECT_ALL, NotepadKeys.of(RawKey.A, CTRL));
        assertEquals(Command.COPY, NotepadKeys.of(RawKey.C, CTRL));
        assertEquals(Command.CUT, NotepadKeys.of(RawKey.X, CTRL));
        assertEquals(Command.PASTE, NotepadKeys.of(RawKey.V, CTRL));
        assertEquals(Command.UNDO, NotepadKeys.of(RawKey.Z, CTRL));
        assertEquals(Command.REDO, NotepadKeys.of(RawKey.Y, CTRL));
        assertEquals(Command.REDO,
            NotepadKeys.of(RawKey.Z, EnumSet.of(KeyModifier.CTRL, KeyModifier.SHIFT)));
    }

    @Test
    public void theOldClipboardKeysToo() {
        assertEquals(Command.COPY, NotepadKeys.of(RawKey.INSERT, CTRL));
        assertEquals(Command.PASTE, NotepadKeys.of(RawKey.INSERT, SHIFT));
        assertEquals(Command.CUT, NotepadKeys.of(RawKey.FORWARD_DELETE, SHIFT));
    }

    @Test
    public void theActionBarsMovementKeysMoveInTheNote() {
        assertEquals(Command.LEFT, NotepadKeys.of(RawKey.LEFT, NONE));
        assertEquals(Command.UP, NotepadKeys.of(RawKey.UP, NONE));
        assertEquals(Command.LINE_END, NotepadKeys.of(RawKey.END, NONE));
        assertEquals(Command.PAGE_DOWN, NotepadKeys.of(RawKey.PAGE_DOWN, NONE));
        assertEquals(Command.WORD_RIGHT, NotepadKeys.of(RawKey.RIGHT, CTRL));
        assertEquals(Command.TEXT_START, NotepadKeys.of(RawKey.HOME, CTRL));
        assertEquals("Shift+arrow is still a movement — the view extends the selection",
            Command.RIGHT, NotepadKeys.of(RawKey.RIGHT, SHIFT));
        assertTrue(NotepadKeys.extendsSelection(SHIFT));
        assertTrue(NotepadKeys.isMovement(Command.WORD_LEFT));
        assertFalse(NotepadKeys.isMovement(Command.PASTE));
    }

    @Test
    public void aChordTheNotepadDoesNotHaveDoesNothing() {
        assertEquals(Command.NONE, NotepadKeys.of(RawKey.B, CTRL));
        assertEquals(Command.NONE, NotepadKeys.of(RawKey.C, EnumSet.of(KeyModifier.ALT)));
        assertEquals(Command.NONE, NotepadKeys.of(RawKey.F5, NONE));
        assertEquals(Command.TAB, NotepadKeys.of(RawKey.TAB, NONE));
        assertEquals(Command.FORWARD_DELETE, NotepadKeys.of(RawKey.FORWARD_DELETE, NONE));
    }

    @Test
    public void wordJumpsStopAtWordEdges() {
        String text = "hello, 한글 world";
        assertEquals(7, NotepadKeys.wordLeft(text, 10));
        assertEquals(0, NotepadKeys.wordLeft(text, 5));
        assertEquals(9, NotepadKeys.wordRight(text, 5));
        assertEquals(text.length(), NotepadKeys.wordRight(text, 10));
    }

    @Test
    public void cursorAndWordHelpersKeepSupplementaryPairsWhole() {
        String mixed = "a😀한\uD840\uDC00글!";
        // Index 1 is '😀' (UTF-16 1..3).
        assertEquals(3, NotepadKeys.nextCodePoint(mixed, 1));
        assertEquals(1, NotepadKeys.prevCodePoint(mixed, 3));
        assertEquals("😀", NotepadKeys.codePointBefore(mixed, 3));
        // Inside the CJK Ext-B pair at 4..6 (`\uD840\uDC00` is a letter): wordLeft/Right and wordBounds snap safely.
        assertEquals(3, NotepadKeys.wordLeft(mixed, 5));
        assertEquals(7, NotepadKeys.wordRight(mixed, 4));
        int[] bounds = NotepadKeys.wordBounds(mixed, 5);
        assertEquals(3, bounds[0]);
        assertEquals(7, bounds[1]);
        assertEquals("한\uD840\uDC00글", mixed.substring(bounds[0], bounds[1]));
    }
}
