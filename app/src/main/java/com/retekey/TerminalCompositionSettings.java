package com.retekey;

/**
 * Where the syllable being built is shown while typing into a terminal. A terminal has no
 * composing region, so the keyboard has only two ways to show one: draw it into the terminal as
 * ordinary text and take it back with every jamo, or keep it on the keyboard's own strip and send
 * the syllable only once it closes. Drawing it is what a terminal user sees in place; keeping it
 * on the strip is what removes the take-back entirely — with it the flicker, the stray erase
 * characters, and the two races behind issue #7.
 *
 * <p>Android-free so it can be unit-tested; the settings screen does the preference I/O.
 */
public final class TerminalCompositionSettings {
    static final String KEY_ON_STRIP = "terminal_compose_on_strip";

    /** The strip: no take-backs, and the syllable appears in the terminal when it closes. */
    public static final boolean DEFAULT_ON_STRIP = true;

    private TerminalCompositionSettings() {
    }

    /**
     * Whether this editor should keep the preedit off screen. Only a terminal ever does: every
     * other editor either has a composing region or, like a remote-desktop client, reports a
     * cursor the keyboard can reconcile against.
     */
    public static boolean appliesTo(EditorProfile profile, boolean onStrip) {
        return onStrip
            && profile != null
            && !profile.capabilities().hasSurroundingText()
            && !profile.capabilities().composesOffScreen();
    }
}
