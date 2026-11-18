package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/**
 * The remote-desktop page: a physical layout's caps on a QWERTY's keys. Each key sends the US
 * key at its position as a key event, so the far side's own IME is what turns it into ㅂ or ф.
 */
public final class RemoteDesktopLayoutTest {
    private static final String[] LETTER_ROWS = {"qwertyuiop", "asdfghjkl", "zxcvbnm"};

    private static SoftwareKeySpec letterAt(KeyboardLayout page, int row, int index) {
        List<SoftwareKeySpec> keys = page.rows().get(row);
        // Row 2 starts with Shift.
        return keys.get(row == 2 ? index + 1 : index);
    }

    @Test
    public void everyLetterKeySendsTheUsKeyAtItsPositionAsARawKey() {
        for (KeyboardLayoutId id : RemoteDesktopLayouts.candidates()) {
            for (boolean shifted : new boolean[] {false, true}) {
                KeyboardLayout page = KeyboardLayouts.remoteDesktop(id, shifted);
                Set<RawKey> seen = new HashSet<>();
                for (int row = 0; row < LETTER_ROWS.length; row++) {
                    String letters = LETTER_ROWS[row];
                    for (int i = 0; i < letters.length(); i++) {
                        SoftwareKeySpec key = letterAt(page, row, i);
                        SemanticInput input = key.semanticInput();
                        assertNotNull(id + " " + letters.charAt(i), input);
                        assertEquals(SemanticInput.Kind.RAW_KEY, input.kind());
                        RawKey expected = RawKey.valueOf(
                            String.valueOf(Character.toUpperCase(letters.charAt(i))));
                        assertEquals(id + " " + letters.charAt(i), expected, input.rawKey());
                        // Shift travels with the press (the view folds it in), not in the key.
                        assertTrue(input.modifiers().isEmpty());
                        seen.add(input.rawKey());
                    }
                }
                assertEquals(26, seen.size());
            }
        }
    }

    @Test
    public void thePageHasTheShapeOfTheOtherLetterPages() {
        KeyboardLayout page = KeyboardLayouts.remoteDesktop(KeyboardLayoutId.KO_DUBEOLSIK, false);
        assertEquals(4, page.rows().size());
        assertEquals(10, page.rows().get(0).size());
        assertEquals(10, page.rows().get(1).size());
        assertEquals(10, page.rows().get(2).size());
        assertEquals(ControlKey.SHIFT, page.rows().get(2).get(0).control());
        assertEquals(10, page.columns());
    }

    @Test
    public void backspaceEnterSpaceAndThePeriodAreKeyEventsToo() {
        // The far side's IME is composing: everything that ends or edits a syllable there must
        // reach it as a key, not as text this side commits around it.
        KeyboardLayout page = KeyboardLayouts.remoteDesktop(KeyboardLayoutId.KO_DUBEOLSIK, false);
        assertEquals(RawKey.BACKSPACE, page.rows().get(1).get(9).semanticInput().rawKey());
        assertEquals(RawKey.PERIOD, page.rows().get(2).get(8).semanticInput().rawKey());
        assertEquals(RawKey.ENTER, page.rows().get(2).get(9).semanticInput().rawKey());
        SoftwareKeySpec space = page.findById("touch.rd.space");
        assertNotNull(space);
        assertEquals(RawKey.SPACE, space.semanticInput().rawKey());
        assertEquals(KeyboardLayouts.SPACE_COLUMN_SPAN, space.columnSpan());
    }

    @Test
    public void theRemoteToggleSitsWhereTheGlobeIsSoTheModeCanBeLeftFromThePage() {
        KeyboardLayout page = KeyboardLayouts.remoteDesktop(KeyboardLayoutId.KO_DUBEOLSIK, false);
        List<SoftwareKeySpec> bottom = page.rows().get(3);
        assertEquals(ControlKey.REMOTE_DESKTOP, bottom.get(bottom.size() - 1).control());
    }

    @Test
    public void dubeolsikCapsMatchTheSoftDubeolsikPage() {
        for (boolean shifted : new boolean[] {false, true}) {
            KeyboardLayout remote = KeyboardLayouts.remoteDesktop(
                KeyboardLayoutId.KO_DUBEOLSIK, shifted);
            KeyboardLayout soft = KeyboardLayouts.of(KeyboardLayoutId.KO_DUBEOLSIK, shifted);
            for (int row = 0; row < LETTER_ROWS.length; row++) {
                for (int i = 0; i < LETTER_ROWS[row].length(); i++) {
                    assertEquals(shifted + " " + LETTER_ROWS[row].charAt(i),
                        letterAt(soft, row, i).label(), letterAt(remote, row, i).label());
                }
            }
        }
    }

    @Test
    public void scriptLayoutsShowTheirOwnLetters() {
        KeyboardLayout ru = KeyboardLayouts.remoteDesktop(KeyboardLayoutId.RU_JCUKEN, false);
        assertEquals("ф", letterAt(ru, 1, 0).label());
        assertEquals("й", letterAt(ru, 0, 0).label());
        assertEquals("ю", ru.rows().get(2).get(8).label());
        KeyboardLayout ruShift = KeyboardLayouts.remoteDesktop(KeyboardLayoutId.RU_JCUKEN, true);
        assertEquals("Ф", letterAt(ruShift, 1, 0).label());
    }

    @Test
    public void qwertyIsItsOwnCaps() {
        KeyboardLayout en = KeyboardLayouts.remoteDesktop(KeyboardLayoutId.EN_QWERTY, false);
        assertEquals("q", letterAt(en, 0, 0).label());
        assertEquals(".", en.rows().get(2).get(8).label());
        KeyboardLayout shifted = KeyboardLayouts.remoteDesktop(KeyboardLayoutId.EN_QWERTY, true);
        assertEquals("Q", letterAt(shifted, 0, 0).label());
        assertEquals(">", shifted.rows().get(2).get(8).label());
    }

    @Test
    public void qwertzAndAzertyMoveTheirLettersOnTheCaps() {
        // The physical tables carry only what a US cap does not say; the remote page has to say
        // where the far side's German or French layout puts its letters.
        KeyboardLayout de = KeyboardLayouts.remoteDesktop(KeyboardLayoutId.DE_QWERTZ, false);
        assertEquals("z", letterAt(de, 0, 5).label());
        assertEquals("y", letterAt(de, 2, 0).label());
        assertEquals(RawKey.Y, letterAt(de, 0, 5).semanticInput().rawKey());

        KeyboardLayout fr = KeyboardLayouts.remoteDesktop(KeyboardLayoutId.FR_AZERTY, false);
        assertEquals("a", letterAt(fr, 0, 0).label());
        assertEquals("z", letterAt(fr, 0, 1).label());
        assertEquals("q", letterAt(fr, 1, 0).label());
        assertEquals("w", letterAt(fr, 2, 0).label());
        assertEquals(",", letterAt(fr, 2, 6).label());
        KeyboardLayout frShift = KeyboardLayouts.remoteDesktop(KeyboardLayoutId.FR_AZERTY, true);
        assertEquals("A", letterAt(frShift, 0, 0).label());
        assertEquals("?", letterAt(frShift, 2, 6).label());
        assertEquals(":", fr.rows().get(2).get(8).label());
        assertEquals("/", frShift.rows().get(2).get(8).label());
    }

    @Test
    public void theCandidatesAreThePhysicalLayoutsThatTypeOneCharacterPerKey() {
        List<KeyboardLayoutId> candidates = RemoteDesktopLayouts.candidates();
        assertEquals(RemoteDesktopLayouts.DEFAULT, candidates.get(0));
        assertEquals(KeyboardLayoutId.KO_DUBEOLSIK, RemoteDesktopLayouts.DEFAULT);
        assertTrue(candidates.contains(KeyboardLayoutId.EN_QWERTY));
        assertTrue(candidates.contains(KeyboardLayoutId.EN_DVORAK));
        assertTrue(candidates.contains(KeyboardLayoutId.RU_JCUKEN));
        assertTrue(candidates.contains(KeyboardLayoutId.FA_ISIRI));
        assertTrue(candidates.contains(KeyboardLayoutId.DE_QWERTZ));
        // No physical form, or a composer rather than a key per character, or not a language.
        assertFalse(candidates.contains(KeyboardLayoutId.KO_CHEONJIIN));
        assertFalse(candidates.contains(KeyboardLayoutId.VI_TELEX));
        assertFalse(candidates.contains(KeyboardLayoutId.JA_ROMAJI));
        assertFalse(candidates.contains(KeyboardLayoutId.ETC_IPA));
        assertFalse(candidates.contains(KeyboardLayoutId.MENU));
        assertEquals(candidates.size(), new HashSet<>(candidates).size());
    }

    @Test
    public void aStoredChoiceThatIsNotACandidateFallsBackToTheDefault() {
        assertEquals(KeyboardLayoutId.RU_JCUKEN, RemoteDesktopLayouts.resolve("RU_JCUKEN"));
        assertEquals(RemoteDesktopLayouts.DEFAULT, RemoteDesktopLayouts.resolve(null));
        assertEquals(RemoteDesktopLayouts.DEFAULT, RemoteDesktopLayouts.resolve("VI_TELEX"));
        assertEquals(RemoteDesktopLayouts.DEFAULT, RemoteDesktopLayouts.resolve("nonsense"));
    }

    @Test
    public void thePeriodRawKeyReachesTheEditorAsThePeriodKey() {
        assertEquals(android.view.KeyEvent.KEYCODE_PERIOD,
            InputConnectionEditorBridge.keyCodeFor(RawKey.PERIOD));
        assertEquals(52, InputConnectionEditorBridge.scanCodeFor(RawKey.PERIOD));
    }
}
