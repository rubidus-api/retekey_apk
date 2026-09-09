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
    public void aLayoutPageIsTheListAndNothingElse() {
        // The list is one row per layout and there are 32 of them; anything sharing its page is
        // something the reader must scroll past a list to reach.
        assertEquals(Arrays.asList(Section.LAYOUTS), SettingsOutline.LAYOUT_PAGE);
        assertFalse("a settings page is not a scroll past a list",
            SettingsOutline.ORIENTATION_PAGE.contains(Section.LAYOUTS));
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
    public void theFourPagesAreOpenedTogetherBelowTheActionBar() {
        // Settings before layouts, portrait before landscape: comparing either pair is opening
        // the one next to it. Below the action bar's page, where the owner placed them.
        List<Section> main = SettingsOutline.MAIN;
        int bar = main.indexOf(Section.ACTION_BAR);
        assertEquals(Section.PORTRAIT_SETTINGS, main.get(bar + 1));
        assertEquals(Section.LANDSCAPE_SETTINGS, main.get(bar + 2));
        assertEquals(Section.PORTRAIT_LAYOUTS, main.get(bar + 3));
        assertEquals(Section.LANDSCAPE_LAYOUTS, main.get(bar + 4));
    }

    @Test
    public void everySectionIsBuiltOnExactlyOnePage() {
        List<Section> all = SettingsOutline.everything();
        assertEquals(EnumSet.allOf(Section.class).size(), all.size());
        assertEquals(EnumSet.allOf(Section.class), EnumSet.copyOf(all));
    }
}
