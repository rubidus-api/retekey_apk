package com.retekey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * What the settings screens hold, and in what order.
 *
 * <p>Three settings — height, the layout list, the floating panel — are stored separately for a
 * screen held upright and one held sideways; the rest are not. They used to sit in the main page
 * among settings that are not, with a chooser near the top saying which orientation was being
 * edited, so knowing what you were about to change meant remembering a line of small print and
 * scrolling back to a toggle. Seeing the other orientation's values meant the same trip twice
 * (issue #6).
 *
 * <p>They now have pages of their own — <em>Portrait settings</em> and <em>Landscape settings</em>
 * — reached from the main page, below the action bar's. Which orientation you are editing is the
 * page you are on, so there is nothing to remember and nothing to scroll back to; the two are
 * side by side in the list, so comparing them is opening the other one.
 *
 * <p>Android-free, so the shape of the pages is a unit test rather than something only a person
 * with the phone in hand can check.
 */
public final class SettingsOutline {

    /** One section of a settings screen. */
    public enum Section {
        /** Light, dark, or the system's choice. */
        THEME,
        /** Room left for the system's own bottom buttons. */
        SYSTEM_BAND,
        /** Visual, haptic and sound feedback on a key press. */
        FEEDBACK,
        /** The strip of actions above the keys — opens its own page. */
        ACTION_BAR,
        /** Opens the settings for a screen held upright. */
        PORTRAIT_PAGE,
        /** Opens the settings for a screen held sideways. */
        LANDSCAPE_PAGE,
        /** Auto-repeat while a key is held. */
        REPEAT,
        /** Shortcuts on a physical keyboard. */
        HARDWARE,
        /** How much of the screen the keyboard takes. Per orientation. */
        HEIGHT,
        /** Which layouts are enabled, and their order. Per orientation. */
        LAYOUTS,
        /** The floating panel and its opacity. Per orientation. */
        FLOATING;

        /** Whether this section's value is stored separately for each screen orientation. */
        public boolean isPerOrientation() {
            return this == HEIGHT || this == LAYOUTS || this == FLOATING;
        }
    }

    /** The main settings page, in the order it builds. */
    public static final List<Section> MAIN = Collections.unmodifiableList(Arrays.asList(
        Section.THEME,
        Section.SYSTEM_BAND,
        Section.FEEDBACK,
        Section.ACTION_BAR,
        Section.PORTRAIT_PAGE,
        Section.LANDSCAPE_PAGE,
        Section.REPEAT,
        Section.HARDWARE));

    /** One orientation's own page — the same three settings, for whichever screen it is. */
    public static final List<Section> ORIENTATION = Collections.unmodifiableList(Arrays.asList(
        Section.HEIGHT,
        Section.LAYOUTS,
        Section.FLOATING));

    private SettingsOutline() {
    }

    /** Every section that either page builds. */
    public static List<Section> everything() {
        List<Section> all = new ArrayList<>(MAIN);
        all.addAll(ORIENTATION);
        return Collections.unmodifiableList(all);
    }
}
