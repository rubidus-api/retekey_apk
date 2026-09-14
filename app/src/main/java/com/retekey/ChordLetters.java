package com.retekey;

import java.util.HashMap;
import java.util.Map;

/**
 * The Latin letter a key stands for when a soft Ctrl, Alt or Meta is armed. A chord is named by
 * the key's place, not by what it types: on a physical Korean keyboard Ctrl+C is the key that
 * types ㅊ, and people press that key. The on-screen two-set layout types jamo, so without this a
 * Ctrl armed on the Korean page typed ㅊ instead of copying.
 */
public final class ChordLetters {
    private static final String KOREAN_PREFIX = "touch.ko2.";
    private static final Map<String, RawKey> KOREAN = new HashMap<>();

    static {
        String[][] places = {
            {"bieup", "Q"}, {"jieut", "W"}, {"digeut", "E"}, {"giyeok", "R"}, {"siot", "T"},
            {"yo", "Y"}, {"yeo", "U"}, {"ya", "I"}, {"ae", "O"}, {"e", "P"},
            {"mieum", "A"}, {"nieun", "S"}, {"ieung", "D"}, {"rieul", "F"}, {"hieuh", "G"},
            {"o", "H"}, {"eo", "J"}, {"a", "K"}, {"i", "L"},
            {"kieuk", "Z"}, {"tieut", "X"}, {"chieut", "C"}, {"pieup", "V"},
            {"yu", "B"}, {"u", "N"}, {"eu", "M"},
        };
        for (String[] place : places) {
            KOREAN.put(KOREAN_PREFIX + place[0], RawKey.valueOf(place[1]));
        }
    }

    private ChordLetters() {
    }

    /** The letter key for a Korean two-set key id, or null for any other key. */
    public static RawKey forKeyId(String stableKeyId) {
        return stableKeyId == null ? null : KOREAN.get(stableKeyId);
    }
}
