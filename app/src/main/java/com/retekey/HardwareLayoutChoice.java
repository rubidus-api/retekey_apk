package com.retekey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Which physical-keyboard layout is used while a given on-screen layout is showing.
 *
 * <p>The two are not the same choice. A phone's screen can hold a layout shaped for thumbs —
 * 천지인, a flick grid — that no physical keyboard has; and a physical keyboard's caps say QWERTY
 * whatever the screen is doing. Until now the physical mapping simply followed the screen layout's
 * identity, which left English with no say at all: Dvorak on the screen still typed QWERTY on the
 * keyboard, because English needs no remapping to be typed. Someone who types Dvorak on glass may
 * want Dvorak under their fingers — or Colemak, or the caps as printed. That is a preference, and
 * it is stored per screen layout and per screen orientation, on that screen's own layout page: a
 * phone docked in landscape can answer to a different physical keyboard from the same phone held
 * upright, and the pairing sits beside the list of layouts it pairs with.
 *
 * <p>Candidates are the layouts of the <em>same language</em> that a physical keyboard can
 * actually type. English has three; Korean has one — 2벌식, which is what 천지인 and 나랏글 already
 * use, since neither has a physical form. When a 3벌식 layout is added it joins that list and
 * Korean becomes a choice with nothing else to change here.
 *
 * <p>Where a language has one candidate there is nothing to choose, and this class says so: the
 * service keeps the mapper it would have used anyway. Android-free, so both the candidate lists
 * and the fallbacks are unit tested.
 */
public final class HardwareLayoutChoice {

    /** English, the one language whose physical layout is a matter of taste rather than script. */
    private static final List<KeyboardLayoutId> ENGLISH = Collections.unmodifiableList(
        Arrays.asList(
            KeyboardLayoutId.EN_QWERTY, KeyboardLayoutId.EN_DVORAK, KeyboardLayoutId.EN_COLEMAK));

    /** Korean: only 2벌식 has a physical form today. 3벌식 would be added here. */
    private static final List<KeyboardLayoutId> KOREAN = Collections.unmodifiableList(
        Arrays.asList(KeyboardLayoutId.KO_DUBEOLSIK));

    /** Turkish: the QWERTY-shaped Q, and the national standard F. */
    private static final List<KeyboardLayoutId> TURKISH = Collections.unmodifiableList(
        Arrays.asList(KeyboardLayoutId.TR_QWERTY, KeyboardLayoutId.TR_F));

    /** Bulgarian: the phonetic layout phones use, and the official BDS 5237. */
    private static final List<KeyboardLayoutId> BULGARIAN = Collections.unmodifiableList(
        Arrays.asList(KeyboardLayoutId.BG_PHONETIC, KeyboardLayoutId.BG_BDS));

    private HardwareLayoutChoice() {
    }

    /** The preference key holding the physical layout chosen for one on-screen layout. */
    public static String prefKey(KeyboardLayoutId screenLayout) {
        return "hardware_layout." + screenLayout.name();
    }

    /**
     * The physical layouts that can be used while {@code screenLayout} is showing, in the order
     * they are offered. Empty when the screen layout is not a letter layout at all.
     */
    public static List<KeyboardLayoutId> candidates(KeyboardLayoutId screenLayout) {
        if (screenLayout == null) {
            return Collections.emptyList();
        }
        String language = LetterLayouts.languageTag(screenLayout);
        if ("en".equals(language)) {
            return ENGLISH;
        }
        if ("ko".equals(language)) {
            return KOREAN;
        }
        if ("tr".equals(language)) {
            return TURKISH;
        }
        if ("bg".equals(language)) {
            return BULGARIAN;
        }
        return Collections.singletonList(screenLayout);
    }

    /**
     * Whether this screen layout offers a choice at all. One candidate is not a choice, and a
     * setting with one value is a line of text pretending to be a control.
     */
    public static boolean isChoosable(KeyboardLayoutId screenLayout) {
        return candidates(screenLayout).size() > 1;
    }

    /** Every screen layout that offers a choice, in the order the layout list has them. */
    public static List<KeyboardLayoutId> choosableLayouts() {
        List<KeyboardLayoutId> choosable = new ArrayList<>();
        for (KeyboardLayoutId id : LetterLayouts.ALL) {
            if (isChoosable(id)) {
                choosable.add(id);
            }
        }
        return Collections.unmodifiableList(choosable);
    }

    /**
     * What is used when the user has chosen nothing. This is what the keyboard did before there
     * was anything to choose, so upgrading changes no one's typing: English falls back to the
     * caps as printed, and every other language to its own layout.
     */
    public static KeyboardLayoutId defaultFor(KeyboardLayoutId screenLayout) {
        List<KeyboardLayoutId> candidates = candidates(screenLayout);
        if (candidates.isEmpty()) {
            return screenLayout;
        }
        // The default is what the keyboard did before there was a choice: each script layout
        // answered to its own table, and English — which needs no remapping at all — answered to
        // the caps as printed.
        return candidates.contains(screenLayout) && !"en".equals(
            LetterLayouts.languageTag(screenLayout))
            ? screenLayout
            : candidates.get(0);
    }

    /**
     * The physical layout to use, from what was stored for this screen layout. A stored name that
     * no longer parses, or names a layout that is not a candidate — a layout removed, a file
     * copied between versions — falls back to the default rather than to nothing.
     */
    public static KeyboardLayoutId resolve(String stored, KeyboardLayoutId screenLayout) {
        if (stored == null || stored.isEmpty()) {
            return defaultFor(screenLayout);
        }
        for (KeyboardLayoutId candidate : candidates(screenLayout)) {
            if (candidate.name().equals(stored)) {
                return candidate;
            }
        }
        return defaultFor(screenLayout);
    }
}
