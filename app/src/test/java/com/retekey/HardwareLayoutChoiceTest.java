package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** Which physical layout is used while a given on-screen layout is showing. */
public final class HardwareLayoutChoiceTest {

    @Test
    public void englishOffersTheThreeItHasAndDefaultsToTheCapsAsPrinted() {
        List<KeyboardLayoutId> expected = Arrays.asList(
            KeyboardLayoutId.EN_QWERTY, KeyboardLayoutId.EN_DVORAK, KeyboardLayoutId.EN_COLEMAK);
        for (KeyboardLayoutId screen : expected) {
            assertEquals(screen + " offers all three", expected,
                HardwareLayoutChoice.candidates(screen));
            assertTrue(screen + " is a choice", HardwareLayoutChoice.isChoosable(screen));
            // Before this setting existed a physical keyboard typed what its caps said, whatever
            // the screen layout was. Upgrading must not change anyone's typing.
            assertEquals(screen + " defaults to the caps as printed",
                KeyboardLayoutId.EN_QWERTY, HardwareLayoutChoice.defaultFor(screen));
        }
    }

    @Test
    public void koreanHasOnePhysicalLayoutAndSoOffersNoChoice() {
        // 천지인 and 나랏글 have no physical form; both already type 2벌식 on a real keyboard.
        for (KeyboardLayoutId screen : Arrays.asList(KeyboardLayoutId.KO_DUBEOLSIK,
                KeyboardLayoutId.KO_CHEONJIIN, KeyboardLayoutId.KO_NARATGEUL)) {
            assertEquals(Arrays.asList(KeyboardLayoutId.KO_DUBEOLSIK),
                HardwareLayoutChoice.candidates(screen));
            assertFalse(screen + " has nothing to choose",
                HardwareLayoutChoice.isChoosable(screen));
            assertEquals(KeyboardLayoutId.KO_DUBEOLSIK, HardwareLayoutChoice.defaultFor(screen));
        }
    }

    @Test
    public void aLanguageWithOneScriptLayoutKeepsIt() {
        // Russian's physical layout is Russian; there is no second way to type Cyrillic here.
        assertEquals(Arrays.asList(KeyboardLayoutId.RU_JCUKEN),
            HardwareLayoutChoice.candidates(KeyboardLayoutId.RU_JCUKEN));
        assertEquals(KeyboardLayoutId.RU_JCUKEN,
            HardwareLayoutChoice.defaultFor(KeyboardLayoutId.RU_JCUKEN));
        assertFalse(HardwareLayoutChoice.isChoosable(KeyboardLayoutId.RU_JCUKEN));
    }

    @Test
    public void onlyTheLayoutsWithSomethingToChooseAreOffered() {
        // The settings page lists these and nothing else; today that is English's three.
        assertEquals(
            Arrays.asList(KeyboardLayoutId.EN_QWERTY, KeyboardLayoutId.EN_DVORAK,
                KeyboardLayoutId.EN_COLEMAK),
            HardwareLayoutChoice.choosableLayouts());
    }

    @Test
    public void aStoredChoiceIsUsedAndNonsenseFallsBack() {
        assertEquals(KeyboardLayoutId.EN_COLEMAK,
            HardwareLayoutChoice.resolve("EN_COLEMAK", KeyboardLayoutId.EN_DVORAK));
        // Nothing stored yet.
        assertEquals(KeyboardLayoutId.EN_QWERTY,
            HardwareLayoutChoice.resolve(null, KeyboardLayoutId.EN_DVORAK));
        assertEquals(KeyboardLayoutId.EN_QWERTY,
            HardwareLayoutChoice.resolve("", KeyboardLayoutId.EN_DVORAK));
        // A name from another version, or one that is not a candidate for this language.
        assertEquals(KeyboardLayoutId.EN_QWERTY,
            HardwareLayoutChoice.resolve("EN_WORKMAN", KeyboardLayoutId.EN_DVORAK));
        assertEquals(KeyboardLayoutId.EN_QWERTY,
            HardwareLayoutChoice.resolve("RU_JCUKEN", KeyboardLayoutId.EN_DVORAK));
    }

    @Test
    public void eachScreenLayoutStoresItsOwnChoice() {
        assertEquals("hardware_layout.EN_DVORAK",
            HardwareLayoutChoice.prefKey(KeyboardLayoutId.EN_DVORAK));
        assertFalse(HardwareLayoutChoice.prefKey(KeyboardLayoutId.EN_DVORAK)
            .equals(HardwareLayoutChoice.prefKey(KeyboardLayoutId.EN_COLEMAK)));
    }

    @Test
    public void capitalsMeanTheScreenAndLowerCaseMeansTheKeys() {
        // The two are offered side by side now, so the abbreviation says which is which on its
        // own — the layout key already paints the screen layout's three letters in capitals.
        assertEquals("QWE-QWERTY", LetterLayouts.screenName(KeyboardLayoutId.EN_QWERTY));
        assertEquals("qwe-QWERTY", LetterLayouts.hardwareName(KeyboardLayoutId.EN_QWERTY));
        assertEquals("DVO-Dvorak", LetterLayouts.screenName(KeyboardLayoutId.EN_DVORAK));
        assertEquals("cmk-Colemak", LetterLayouts.hardwareName(KeyboardLayoutId.EN_COLEMAK));
        // The rest of the name is untouched, non-Latin scripts included.
        assertTrue(LetterLayouts.screenName(KeyboardLayoutId.KO_DUBEOLSIK).startsWith("2BS-"));
        assertTrue(LetterLayouts.hardwareName(KeyboardLayoutId.KO_DUBEOLSIK).startsWith("2bs-"));
    }
}
