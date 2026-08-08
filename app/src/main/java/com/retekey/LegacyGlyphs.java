package com.retekey;

import java.util.HashMap;
import java.util.Map;

/**
 * Words for the keys whose glyph an old Android has no font for.
 *
 * <p>A screenshot of the legacy build on Android 4.4 showed the menu key as an empty cell: the
 * device's fonts have no ☰, so it drew nothing at all. A key that shows nothing is worse than a key
 * that shows a short word, so below the version where these glyphs became dependable the labels
 * fall back to text. The layouts keep the glyph; only what is painted changes.
 *
 * <p>Android-free, so the table is unit-tested directly.
 */
public final class LegacyGlyphs {
    /** Material and its font arrived together; from here the glyphs below are dependable. */
    static final int GLYPHS_FROM = 21;

    private static final Map<String, String> WORDS = new HashMap<>();

    static {
        WORDS.put("☰", "Menu");
        // The menu page and the layout key say their names in words on every version now, so
        // only the handful of glyphs still in use need an entry here.
        WORDS.put("★1", "C1");
        WORDS.put("★2", "C2");
        WORDS.put("▷", ">|");
        WORDS.put("⇲", "Size");
        WORDS.put("✕", "X");
        WORDS.put("‹", "<");
        WORDS.put("›", ">");
        WORDS.put("⌫", "Bksp");
        WORDS.put("⏎", "Enter");
        WORDS.put("⇧", "Shift");
    }

    private LegacyGlyphs() {
    }

    /** The label to paint: the glyph itself when the device can draw it, a word when it cannot. */
    public static String label(String glyph, int sdkInt) {
        if (glyph == null || sdkInt >= GLYPHS_FROM) {
            return glyph;
        }
        String word = WORDS.get(glyph);
        return word == null ? glyph : word;
    }
}
