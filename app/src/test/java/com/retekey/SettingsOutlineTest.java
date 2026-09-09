package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.retekey.SettingsOutline.Section;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.junit.Test;

/**
 * The shape of the settings screens. Issue #6: which orientation a setting applied to was a
 * toggle to remember rather than something visible where the setting stood.
 */
public final class SettingsOutlineTest {

    @Test
    public void theGeneralPageHoldsNothingThatDependsOnOrientation() {
        for (Section section : SettingsOutline.MAIN) {
            assertFalse(section + " belongs on one of the per-screen pages",
                section.isPerOrientation());
        }
    }

    @Test
    public void anOrientationsSettingsPageHoldsTheTwoSmallThingsKeptPerScreen() {
        assertEquals(Arrays.asList(Section.HEIGHT, Section.FLOATING),
            SettingsOutline.ORIENTATION_PAGE);
        for (Section section : SettingsOutline.ORIENTATION_PAGE) {
            assertTrue(section + " is stored per orientation", section.isPerOrientation());
        }
    }

    @Test
    public void aLayoutPageCarriesNoSettingsOfItsOwn() {
        // The list is one row per layout and there are 32 of them; height and the floating panel
        // must not sit above that, which is why they have a page beside it rather than on it.
        assertFalse("a settings page is not a scroll past a list",
            SettingsOutline.ORIENTATION_PAGE.contains(Section.LAYOUTS));
        assertFalse(SettingsOutline.LAYOUT_PAGE.contains(Section.HEIGHT));
        assertFalse(SettingsOutline.LAYOUT_PAGE.contains(Section.FLOATING));
    }

    @Test
    public void everyPerOrientationSettingIsOnOneOfThePerScreenPages() {
        for (Section section : Section.values()) {
            if (section.isPerOrientation()) {
                assertTrue(section + " has a per-screen page",
                    SettingsOutline.ORIENTATION_PAGE.contains(section)
                        || SettingsOutline.LAYOUT_PAGE.contains(section));
            }
        }
    }

    @Test
    public void theGeneralPageIsSettingsRatherThanDoors() {
        // The way into the action bar's page and into each screen's own pages is the app's main
        // screen: none of them is a submenu of another (owner's request).
        assertEquals(
            Arrays.asList(Section.THEME, Section.SYSTEM_BAND, Section.FEEDBACK, Section.REPEAT,
                Section.HARDWARE),
            SettingsOutline.MAIN);
    }

    @Test
    public void theHardwarePairingSitsWithTheLayoutsItPairs() {
        // Which physical layout answers to which on-screen layout is kept per screen too, so it
        // belongs on that screen's layout page rather than anywhere general.
        assertTrue(SettingsOutline.LAYOUT_PAGE.contains(Section.HARDWARE_LAYOUTS));
        assertTrue(Section.HARDWARE_LAYOUTS.isPerOrientation());
        assertEquals("the list comes first, the pairing reads as a note on it",
            Section.LAYOUTS, SettingsOutline.LAYOUT_PAGE.get(0));
    }

    @Test
    public void everySectionIsBuiltOnExactlyOnePage() {
        List<Section> all = SettingsOutline.everything();
        assertEquals(EnumSet.allOf(Section.class).size(), all.size());
        assertEquals(EnumSet.allOf(Section.class), EnumSet.copyOf(all));
    }
}
