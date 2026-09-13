package com.retekey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * What the settings screens hold, and in what order.
 *
 * <p>Three settings are stored separately for a screen held upright and one held sideways: the
 * keyboard's height, the floating panel, and the layout list. They began scattered through one
 * long page with a toggle near the top saying which orientation was being edited, which meant
 * remembering a line of small print and scrolling back to a switch (issue #6). They then moved to
 * a page per orientation — better, but that page was, in practice, mostly the layout list: one
 * row per layout, 32 layouts, with the two small settings above it.
 *
 * <p>So the split is now twice over. Each orientation keeps a settings page of its own — height
 * and the floating panel — and the layout list is lifted out of it onto a page of its own again,
 * per orientation. Four pages, opened from the general page and named for what they are:
 * <em>Portrait settings</em>, <em>Landscape settings</em>, <em>Layout (portrait)</em>,
 * <em>Layout (landscape)</em>. Which orientation you are editing is the page you are on, and a
 * settings page is never a scroll past a list.
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
        /** Auto-repeat while a key is held. */
        REPEAT,
        /** Shortcuts on a physical keyboard. */
        HARDWARE,
        /** Where a terminal's half-built syllable is shown. */
        TERMINAL,
        /** Which physical layout each on-screen layout uses, on this screen. */
        HARDWARE_LAYOUTS,
        /** How much of the screen the keyboard takes. On an orientation's settings page. */
        HEIGHT,
        /** The floating panel and its opacity. On an orientation's settings page. */
        FLOATING,
        /** Which layouts are enabled, and their order. The whole of a layout page. */
        LAYOUTS;

        /** Whether this section's value is stored separately for each screen orientation. */
        public boolean isPerOrientation() {
            return this == HEIGHT || this == FLOATING || this == LAYOUTS
                || this == HARDWARE_LAYOUTS;
        }
    }

    /**
     * The general settings page, in the order it builds. It holds settings, not doors: the way
     * into the action bar's page and into each screen's own pages is the app's main screen, so
     * none of them is a submenu of another (owner's request).
     */
    public static final List<Section> MAIN = Collections.unmodifiableList(Arrays.asList(
        Section.THEME,
        Section.SYSTEM_BAND,
        Section.FEEDBACK,
        Section.REPEAT,
        Section.HARDWARE,
        Section.TERMINAL));

    /** One orientation's settings page: the two small things kept per screen. */
    public static final List<Section> ORIENTATION_PAGE = Collections.unmodifiableList(
        Arrays.asList(Section.HEIGHT, Section.FLOATING));

    /**
     * One orientation's layout page: which layouts this screen walks through, and — for the ones
     * that have more than one physical form — which layout a plugged-in keyboard follows while
     * each is up. That pairing is kept per screen too, so a phone docked in landscape can answer
     * to a different physical layout from the same phone held upright.
     */
    public static final List<Section> LAYOUT_PAGE = Collections.unmodifiableList(
        Arrays.asList(Section.LAYOUTS, Section.HARDWARE_LAYOUTS));

    private SettingsOutline() {
    }

    /** Every section that any of the pages builds. */
    public static List<Section> everything() {
        List<Section> all = new ArrayList<>(MAIN);
        all.addAll(ORIENTATION_PAGE);
        all.addAll(LAYOUT_PAGE);
        return Collections.unmodifiableList(all);
    }
}
