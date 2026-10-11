package com.retekey;

/**
 * The gap between keys, as a percentage of one cell's width — kept per screen orientation, because
 * the same gap is a larger share of a short landscape row than of a tall portrait one.
 *
 * <p>The measure is one cell, the width of a key one column wide, because keys come two and more
 * columns wide and a percentage of "the key" would then mean a different gap beside each of them.
 * The percentage is the whole space between two faces; half of it is taken from each side of a
 * key, on all four sides alike.
 *
 * <p>Until the setting is touched the gap is the 4 dp a side the keyboard has always drawn, so
 * nobody's keyboard looks different for this existing. The gap is not part of any key: a touch
 * that lands between two faces presses nothing ({@link TouchTargets}). Android-free.
 */
final class KeyGapSettings {
    static final String KEY_CELL_PERCENT = "key_gap_cell_percent";
    /** What an orientation nobody has set reads as: follow the gap the keyboard always had. */
    static final int UNSET = -1;
    /** No gap: faces meet edge to edge. */
    static final int MIN_PERCENT = 0;
    /** Two fifths of a cell between faces. Beyond that the gap is most of the keyboard. */
    static final int MAX_PERCENT = 40;

    /**
     * The key 0.2.2 wrote: a percentage of the 4 dp gap. Still read where the new one is unset,
     * so a gap set under the old unit stays exactly as it was set.
     */
    static final String LEGACY_KEY_PERCENT = "key_gap_percent";
    static final int LEGACY_DEFAULT_PERCENT = 100;
    private static final int LEGACY_MAX_PERCENT = 300;

    /** The gap on each side of a face that the keyboard has always drawn. */
    private static final float USUAL_DP = 4.0f;

    private KeyGapSettings() {
    }

    static int clamp(int percent) {
        return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, percent));
    }

    /** The gap on each side of a face while the setting is untouched, in pixels. */
    static int usualPx(int legacyPercent, float density) {
        int percent = Math.max(0, Math.min(LEGACY_MAX_PERCENT, legacyPercent));
        return Math.round(USUAL_DP * density * percent / 100.0f);
    }

    /** The gap on each side of a face: half of that percentage of one cell's width. */
    static int sidePx(int percent, int cellWidthPx) {
        return Math.round(cellWidthPx * clamp(percent) / 200.0f);
    }

    /** The percentage of a cell that a gap of {@code sidePx} a side comes to. */
    static int percentFor(int sidePx, int cellWidthPx) {
        if (cellWidthPx <= 0) {
            return MIN_PERCENT;
        }
        return clamp(Math.round(sidePx * 200.0f / cellWidthPx));
    }

    /**
     * The gap one key can afford. It takes at most a quarter of the key's smaller side from each
     * edge, so the face keeps half the key: never turned inside out on a short row, and always
     * something left to touch. Drawing and hit testing both go through here, so what is drawn is
     * what answers.
     */
    static int fit(int gapPx, int keyWidthPx, int keyHeightPx) {
        return Math.max(0, Math.min(gapPx, Math.min(keyWidthPx, keyHeightPx) / 4));
    }

    /** Whether a changed preference is this setting, for either orientation, in either unit. */
    static boolean isKey(String changed) {
        return changed != null && (changed.startsWith(KEY_CELL_PERCENT + ".")
            || changed.startsWith(LEGACY_KEY_PERCENT + "."));
    }
}
