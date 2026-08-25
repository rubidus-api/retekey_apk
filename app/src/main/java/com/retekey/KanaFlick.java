package com.retekey;

/**
 * The Japanese 12-key flick pad, flick-only (Gboard's フリックのみ): a tap is the あ-column kana,
 * a flick is the column the direction names — left い, up う, right え, down お — with no cycling,
 * so every press is one character and the pad needs no run state at all. What the layouts and the
 * keyboard view need to know about it lives here, Android-free.
 *
 * <p>The や key's sideways flicks are the corner brackets, the わ key carries を ん ー, and the
 * modifier key is not here at all: it edits the character before the cursor through
 * {@link #modified}, turning か into が, は through ば into ぱ, つ through づ into っ, and a plain
 * vowel into its small form — one cycle per tap, closing back on the plain letter.
 */
final class KanaFlick {
    /** The ten kana keys, named by their tap character's romanisation. */
    enum Key { A, KA, SA, TA, NA, HA, MA, YA, RA, WA }

    private static final String[][] CELLS = {
        // tap, left, up, right, down
        {"あ", "い", "う", "え", "お"},
        {"か", "き", "く", "け", "こ"},
        {"さ", "し", "す", "せ", "そ"},
        {"た", "ち", "つ", "て", "と"},
        {"な", "に", "ぬ", "ね", "の"},
        {"は", "ひ", "ふ", "へ", "ほ"},
        {"ま", "み", "む", "め", "も"},
        {"や", "「", "ゆ", "」", "よ"},
        {"ら", "り", "る", "れ", "ろ"},
        {"わ", "を", "ん", "ー", null},
    };

    /** The dakuten/handakuten/small cycles, each string one closed loop. */
    private static final String[] MODIFIER_CYCLES = {
        "かが", "きぎ", "くぐ", "けげ", "こご",
        "さざ", "しじ", "すず", "せぜ", "そぞ",
        "ただ", "ちぢ", "つづっ", "てで", "とど",
        "はばぱ", "ひびぴ", "ふぶぷ", "へべぺ", "ほぼぽ",
        "あぁ", "いぃ", "うぅ", "えぇ", "おぉ",
        "やゃ", "ゆゅ", "よょ", "わゎ",
    };

    private KanaFlick() {
    }

    /** The kana key a spec is, or null when it is not one. */
    static Key of(SoftwareKeySpec spec) {
        String id = spec.stableKeyId();
        if (!id.startsWith("touch.kana.")) {
            return null;
        }
        try {
            return Key.valueOf(id.substring("touch.kana.".length())
                .toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException notAKanaKey) {
            return null;
        }
    }

    /** What a tap types. */
    static String tap(Key key) {
        return CELLS[key.ordinal()][0];
    }

    /** What a flick types, or null where the direction has nothing (わ down). */
    static String flick(Key key, CheonjiinInterpreter.Flick direction) {
        String[] cell = CELLS[key.ordinal()];
        switch (direction) {
            case LEFT: return cell[1];
            case UP: return cell[2];
            case RIGHT: return cell[3];
            case DOWN: return cell[4];
            default: return null;
        }
    }

    /**
     * What the modifier key turns {@code kana} into — the next stop on its cycle — or null when
     * the character has none, in which case the key does nothing, like a 천지인 drag at an empty
     * cell.
     */
    static String modified(char kana) {
        for (String cycle : MODIFIER_CYCLES) {
            int at = cycle.indexOf(kana);
            if (at >= 0) {
                return String.valueOf(cycle.charAt((at + 1) % cycle.length()));
            }
        }
        return null;
    }
}
