package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** The set of layouts the globe key walks, and the walk itself. */
public final class LetterLayoutsTest {
    @Test
    public void anEmptyOrPointlessStoredOrderFallsBackToTheDefault() {
        assertEquals(LetterLayouts.DEFAULT, LetterLayouts.parse(null));
        assertEquals(LetterLayouts.DEFAULT, LetterLayouts.parse(""));
        assertEquals(LetterLayouts.DEFAULT, LetterLayouts.parse("SPECIAL_KEYS,NOT_A_LAYOUT"));
    }

    @Test
    public void storedOrderKeepsItsOrderAndDropsRepeats() {
        List<KeyboardLayoutId> parsed =
            LetterLayouts.parse("EN_DVORAK,KO_CHEONJIIN,EN_DVORAK,MENU");

        assertEquals(
            Arrays.asList(KeyboardLayoutId.EN_DVORAK, KeyboardLayoutId.KO_CHEONJIIN), parsed);
    }

    @Test
    public void formatAndParseRoundTrip() {
        List<KeyboardLayoutId> order = Arrays.asList(
            KeyboardLayoutId.KO_NARATGEUL,
            KeyboardLayoutId.EN_QWERTY,
            KeyboardLayoutId.KO_DUBEOLSIK);

        assertEquals(order, LetterLayouts.parse(LetterLayouts.format(order)));
    }

    @Test
    public void formatIgnoresLayoutsThatAreNotLetterPages() {
        assertEquals("EN_QWERTY", LetterLayouts.format(
            Arrays.asList(KeyboardLayoutId.EN_QWERTY, KeyboardLayoutId.MENU, null)));
    }

    @Test
    public void theGlobeWalksTheOrderAndWraps() {
        List<KeyboardLayoutId> order = Arrays.asList(
            KeyboardLayoutId.KO_DUBEOLSIK,
            KeyboardLayoutId.EN_QWERTY,
            KeyboardLayoutId.KO_CHEONJIIN);

        assertEquals(KeyboardLayoutId.EN_QWERTY,
            LetterLayouts.next(order, KeyboardLayoutId.KO_DUBEOLSIK));
        assertEquals(KeyboardLayoutId.KO_CHEONJIIN,
            LetterLayouts.next(order, KeyboardLayoutId.EN_QWERTY));
        assertEquals(KeyboardLayoutId.KO_DUBEOLSIK,
            LetterLayouts.next(order, KeyboardLayoutId.KO_CHEONJIIN));
    }

    @Test
    public void aLayoutThatWasTurnedOffHandsOverToTheFirstOne() {
        List<KeyboardLayoutId> order = Arrays.asList(KeyboardLayoutId.EN_DVORAK);

        assertEquals(KeyboardLayoutId.EN_DVORAK,
            LetterLayouts.next(order, KeyboardLayoutId.KO_DUBEOLSIK));
        assertEquals(KeyboardLayoutId.EN_DVORAK,
            LetterLayouts.next(order, KeyboardLayoutId.EN_DVORAK));
    }

    @Test
    public void everyOfferedLayoutHasAName() {
        for (KeyboardLayoutId id : LetterLayouts.ALL) {
            assertTrue(id.name(), LetterLayouts.displayName(id).length() > 0);
        }
    }
}
