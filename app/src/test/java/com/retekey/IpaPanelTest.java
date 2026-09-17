package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/**
 * The two doors off the IPA page: find a symbol by the name you have for it, or walk the families.
 * Both end in a list of choices, which is why they are one panel (issue #11).
 */
public final class IpaPanelTest {
    @Test
    public void aQueryOffersWhatItNames() {
        IpaPanel panel = IpaPanel.find().append("p").append("a").append("l").append("m");
        assertEquals("find: palm", panel.label());
        assertEquals("ɑ", panel.choices().get(0)[0]);
        assertFalse("and says what it is", panel.choices().get(0)[1].isEmpty());
    }

    @Test
    public void anEmptyQueryOffersNothingButSaysWhatToDo() {
        IpaPanel panel = IpaPanel.find();
        assertTrue(panel.choices().isEmpty());
        assertTrue(panel.label().startsWith("find:"));
    }

    @Test
    public void backspaceWalksTheQueryBack() {
        IpaPanel panel = IpaPanel.find().append("t").append("h").backspace();
        assertEquals("find: t", panel.label());
        assertEquals("an empty query says what to do again", "find: type a name",
            panel.backspace().label());
        // And a backspace with nothing typed changes nothing; the caller closes the panel.
        assertEquals("", panel.backspace().backspace().query());
    }

    @Test
    public void theFamiliesAreOfferedFirstAndOpenOnPicking() {
        IpaPanel panel = IpaPanel.families();
        assertEquals("IPA", panel.label());
        List<String[]> families = panel.choices();
        assertEquals(IpaPanel.FAMILIES.size(), families.size());
        assertEquals("vowels", families.get(0)[0]);

        int index = panel.familyAt("fricatives");
        assertTrue(index >= 0);
        IpaPanel open = panel.openFamily(index);
        assertEquals("fricatives", open.label());
        assertTrue(symbols(open).contains("θ"));
        assertTrue(symbols(open).contains("ʒ"));
        // Inside a family, a pick is a symbol rather than another family.
        assertEquals(-1, open.familyAt("θ"));
    }

    @Test
    public void everyFamilyHasSymbolsAndTheyAreKnownOnes() {
        for (IpaPanel.Family family : IpaPanel.FAMILIES) {
            assertTrue(family.name + " has symbols", family.symbols.length > 0);
        }
        // The everyday page's own symbols are all reachable through a family too, so the two ways
        // of getting one never disagree about what exists.
        assertTrue(symbols(IpaPanel.families().openFamily(0)).contains("ə"));
        assertTrue(symbols(IpaPanel.families().openFamily(
            IpaPanel.families().familyAt("nasals"))).contains("ŋ"));
    }

    @Test
    public void aSymbolCarriesItsNameWhereTheIndexKnowsOne() {
        assertTrue(IpaPanel.glossOf("ə").contains("schwa"));
        assertTrue(IpaPanel.glossOf("θ").contains("dental"));
        assertEquals("", IpaPanel.glossOf("이 기호는 없다"));
    }

    private static List<String> symbols(IpaPanel panel) {
        List<String> found = new java.util.ArrayList<>();
        for (String[] choice : panel.choices()) {
            found.add(choice[0]);
        }
        return found;
    }
}
