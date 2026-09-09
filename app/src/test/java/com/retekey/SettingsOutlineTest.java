package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.retekey.SettingsOutline.Section;
import java.util.EnumSet;
import java.util.List;
import org.junit.Test;

/**
 * The shape of the settings screens. Issue #6: the settings kept per screen orientation sat in
 * the main page among settings that are not, so which orientation you were editing was a toggle
 * to remember rather than a place to be.
 */
public final class SettingsOutlineTest {

    @Test
    public void theMainPageHoldsNothingThatDependsOnOrientation() {
        for (Section section : SettingsOutline.MAIN) {
            assertFalse(section + " belongs on an orientation page", section.isPerOrientation());
        }
    }

    @Test
    public void theOrientationPageHoldsExactlyTheSettingsStoredThatWay() {
        // Height, the layout list and the floating panel are what goes through OrientedPrefs.
        for (Section section : SettingsOutline.ORIENTATION) {
            assertTrue(section + " is not stored per orientation", section.isPerOrientation());
        }
        for (Section section : Section.values()) {
            assertEquals(
                section + " must be on the orientation page exactly when it is stored that way",
                section.isPerOrientation(),
                SettingsOutline.ORIENTATION.contains(section));
        }
    }

    @Test
    public void theTwoOrientationPagesAreOpenedTogetherBelowTheActionBar() {
        // Side by side, so comparing the two is opening the other one, and below the action bar's
        // page because that is where the owner put them.
        List<Section> main = SettingsOutline.MAIN;
        int bar = main.indexOf(Section.ACTION_BAR);
        assertEquals(Section.PORTRAIT_PAGE, main.get(bar + 1));
        assertEquals(Section.LANDSCAPE_PAGE, main.get(bar + 2));
    }

    @Test
    public void theLayoutListIsTheLastThingOnAnOrientationPage() {
        // It is far taller than everything else on the page put together.
        List<Section> page = SettingsOutline.ORIENTATION;
        assertEquals(Section.LAYOUTS, page.get(page.size() - 1));
    }

    @Test
    public void everySectionIsBuiltOnExactlyOnePage() {
        List<Section> all = SettingsOutline.everything();
        assertEquals(EnumSet.allOf(Section.class).size(), all.size());
        assertEquals(EnumSet.allOf(Section.class), EnumSet.copyOf(all));
    }
}
