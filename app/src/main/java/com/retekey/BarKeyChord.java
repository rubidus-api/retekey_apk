package com.retekey;

import java.util.EnumSet;
import java.util.Set;

/**
 * How an action-bar text slot is sent when modifiers are down. A slot that types one letter or
 * digit is a key like any other: with Ctrl, Alt or Meta held — from the bar's own toggles, the
 * keyboard's, or a physical keyboard — it goes out as that chord (Ctrl+C copies), and with Shift
 * alone a letter is typed in capitals. Longer text, and anything without modifiers, is typed as
 * written. Android-free, so the rule is a unit test.
 */
final class BarKeyChord {
    private BarKeyChord() {
    }

    /** The key to chord for {@code text} under {@code mods}, or null to type the text instead. */
    static RawKey chordKey(String text, Set<KeyModifier> mods) {
        if (text == null || text.length() != 1 || mods == null || !hasChordModifier(mods)) {
            return null;
        }
        return keyFor(text.charAt(0));
    }

    /** The text to type when no chord is sent: Shift capitalises a single letter. */
    static String typed(String text, Set<KeyModifier> mods) {
        if (text != null && text.length() == 1 && mods != null
                && mods.contains(KeyModifier.SHIFT) && !hasChordModifier(mods)) {
            return text.toUpperCase(java.util.Locale.ROOT);
        }
        return text;
    }

    /** The union of every source of modifiers a bar press can be held with. */
    static Set<KeyModifier> union(Set<KeyModifier> first, Set<KeyModifier> second) {
        Set<KeyModifier> all = EnumSet.noneOf(KeyModifier.class);
        if (first != null) {
            all.addAll(first);
        }
        if (second != null) {
            all.addAll(second);
        }
        return all;
    }

    private static boolean hasChordModifier(Set<KeyModifier> mods) {
        return mods.contains(KeyModifier.CTRL) || mods.contains(KeyModifier.ALT)
            || mods.contains(KeyModifier.META);
    }

    private static RawKey keyFor(char c) {
        char upper = Character.toUpperCase(c);
        if (upper >= 'A' && upper <= 'Z') {
            return RawKey.valueOf(String.valueOf(upper));
        }
        if (c >= '0' && c <= '9') {
            return RawKey.valueOf("DIGIT_" + c);
        }
        if (c == ' ') {
            return RawKey.SPACE;
        }
        return null;
    }
}
