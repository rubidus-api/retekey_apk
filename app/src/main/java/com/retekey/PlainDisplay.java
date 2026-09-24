package com.retekey;

/**
 * A keyboard for a screen that does colour and motion badly — E-Ink above all (issue #15). Two
 * settings, each on its own:
 *
 * <ul>
 *   <li><b>Monochrome</b>: keys drawn the way an e-reader's own keyboard draws them, an outline in
 *       the ink colour round a face in the paper colour, and every state a shade of grey. Light
 *       and dark both exist, so the theme setting still decides which way round.</li>
 *   <li><b>No motion</b>: nothing is drawn for a moment and taken away again — no press shade, no
 *       flash, no echo of the typed letter. On E-Ink each of those is two refreshes and a ghost.
 *       Android's own "remove animations" switch turns this on as well.</li>
 * </ul>
 *
 * <p>Android-free: {@link KeyboardPalette} and the keyboard view apply what this decides.
 */
final class PlainDisplay {
    /** Preference keys, in the same {@code retekey_view} preferences as the theme. */
    static final String KEY_MONOCHROME = "monochrome";
    static final String KEY_STILL = "still_keys";

    /** Indexes into {@link #monochrome(boolean)}, in {@link KeyboardPalette}'s own order. */
    static final int BACKGROUND = 0;
    static final int KEY_FACE = 1;
    static final int KEY_DISABLED = 2;
    static final int HELD = 3;
    static final int ARMED = 4;
    static final int KEY_TEXT = 5;
    static final int KEY_TEXT_MUTED = 6;
    static final int EDGE = 7;
    static final int HINT = 8;
    static final int PRESS = 9;
    static final int CHOICE = 10;

    private PlainDisplay() {
    }

    /**
     * The monochrome colours. Paper and ink are pure white and pure black; the greys in between
     * mark the states that must still be told apart — armed for one key against held, and the
     * alternates a hold offers against a held key.
     */
    static int[] monochrome(boolean night) {
        int paper = night ? grey(0) : grey(255);
        int ink = night ? grey(255) : grey(0);
        return new int[] {
            paper,                          // background: the page itself
            paper,                          // key face
            night ? grey(26) : grey(238),   // disabled
            ink,                            // held
            night ? grey(119) : grey(153),  // armed for one key
            ink,                            // label
            night ? grey(170) : grey(102),  // muted label
            ink,                            // key outline
            night ? grey(170) : grey(85),   // long-press hint
            ink,                            // press shade, if it is drawn at all
            night ? grey(187) : grey(85),   // choosing
        };
    }

    /** Whether keys stay still: the user asked, or Android has animations switched off. */
    static boolean still(boolean asked, float animatorDurationScale) {
        return asked || animatorDurationScale == 0.0f;
    }

    private static int grey(int level) {
        return 0xFF000000 | (level << 16) | (level << 8) | level;
    }
}
