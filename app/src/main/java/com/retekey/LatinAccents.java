package com.retekey;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Where each Latin-script language's letters live on their base keys — as <em>flicks</em>, one per
 * direction, so no key piles its essentials into a slide-along list (the owner's rule, 2026-08-24:
 * the digit and symbol holds must never crowd the letters a language cannot do without).
 *
 * <p>The directions follow the mark: the acute leans right, the grave leans left, what sits on top
 * — circumflex, umlaut, a dot, a breve — goes up, and what hangs below or comes second — cedilla,
 * ogonek, the diaeresis where up is taken, a stroke — goes down. Each entry is four strings, left
 * up right down, empty where the direction has nothing.
 */
final class LatinAccents {
    private LatinAccents() {
    }

    static final Map<String, String[]> SPANISH = table(
        "a", " |á| | ",
        "e", " |é| | ",
        "i", " |í| | ",
        "o", " |ó| | ",
        "u", " |ú| |ü"
    );

    static final Map<String, String[]> PORTUGUESE = table(
        "a", "à|â|á|ã|ª",
        "e", " |ê|é| ",
        "i", " |í| | ",
        "o", " |ô|ó|õ|º",
        "u", " |ú| |ü",
        "c", " | | |ç",
        ".", "«| |»|€"
    );

    static final Map<String, String[]> ITALIAN = table(
        "a", "à| | | ",
        "e", "è| |é| ",
        "i", "ì| | | ",
        "o", "ò| | | ",
        "u", "ù| | | ",
        ".", "«| |»|€"
    );

    static final Map<String, String[]> POLISH = table(
        "a", " | | |ą",
        "c", " | |ć| ",
        "e", " | | |ę",
        "l", " | |ł| ",
        "n", " | |ń| ",
        "o", " | |ó| ",
        "s", " | |ś| ",
        "z", " |ż|ź| ",
        ".", "„| |”|€"
    );

    static final Map<String, String[]> FRENCH = table(
        "a", "à|â|æ| ",
        "e", "è|ê|é|ë",
        "i", " |î| |ï",
        "o", " |ô|œ| ",
        "u", "ù|û| |ü",
        "y", " | | |ÿ",
        "c", " | | |ç"
    );

    static final Map<String, String[]> GERMAN = table(
        "a", " |ä| | ",
        "o", " |ö| | ",
        "u", " |ü| | ",
        "s", " | | |ß",
        ".", "„| |“|€"
    );

    static final Map<String, String[]> TURKISH = table(
        "u", " |ü| | ",
        "o", " |ö| | ",
        "g", " |ğ| | ",
        "i", " | | |ı",
        "s", " | | |ş",
        "c", " | | |ç",
        ".", "“| |”|₺"
    );

    /** Telex is the real way in; the flicks are for anyone who does not know it. */
    static final Map<String, String[]> VIETNAMESE = table(
        "a", " |â| |ă",
        "e", " |ê| | ",
        "o", " |ô|ơ| ",
        "u", " | |ư| ",
        "d", " | | |đ",
        ".", "“| |”|₫"
    );

    /**
     * Thai: Kedmanee's Shift layer, key for key, as the upward flick; the folded letters ride
     * their kin sideways and down. ฟ carries its whole family: ๅ left, ฤ up, ฦ right, ฝ down.
     */
    static final Map<String, String[]> THAI = table(
        "บ", " |ฐ| | ",
        "ุ", " |ู| | ",
        "ค", " | | |ฅ",
        "ข", " | | |ฃ",
        "ไ", " |\"| |ใ",
        "ำ", " |ฎ| | ",
        "พ", " |ฑ| | ",
        "ะ", " |ธ| | ",
        "ั", " |ํ| | ",
        "ี", " |๊| | ",
        "ร", " |ณ| | ",
        "น", " |ฯ| | ",
        "ย", " |ญ| | ",
        "ล", " |ฬ| | ",
        "ฟ", "ๅ|ฤ|ฦ|ฝ",
        "ห", " |ฆ| | ",
        "ก", " |ฏ| |ง",
        "ด", " |โ| | ",
        "เ", " |ฌ| | ",
        "้", " |็| | ",
        "่", " |๋| | ",
        "า", " |ษ| | ",
        "ส", " |ศ| | ",
        "ว", " |ซ| | ",
        "แ", " |ฉ| | ",
        "อ", " |ฮ| | ",
        "ิ", " |ฺ| | ",
        "ื", " |์| | ",
        "ท", " |?| | ",
        "ม", " |ฒ| | "
    );

    /**
     * Persian: the dotted twins go up (the dots sit on top), and the hamza family rides its
     * carriers — آ أ إ ء around alef, ؤ over vav, ئ over ye, ة over he.
     */
    static final Map<String, String[]> PERSIAN = table(
        "ت", " |ث| | ",
        "د", " |ذ| | ",
        "ط", " |ظ| | ",
        "ز", " |ژ| | ",
        "ا", "ء|آ|أ|إ",
        "و", " |ؤ| | ",
        "ی", " |ئ| | ",
        "ه", " |ة| | "
    );

    /** The tonos leans right; the diaeresis goes down; both together go up. */
    static final Map<String, String[]> GREEK = table(
        "α", " | |ά| ",
        "ε", " | |έ| ",
        "η", " | |ή| ",
        "ι", " |ΐ|ί|ϊ",
        "ο", " | |ό| ",
        "υ", " |ΰ|ύ|ϋ",
        "ω", " | |ώ| ",
        ".", "«| |»|€"
    );

    /** Pairs of base letter and a left|up|right|down string; a space (or nothing) is an empty way. */
    private static Map<String, String[]> table(String... pairs) {
        Map<String, String[]> map = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            String[] ways = pairs[i + 1].split("\\|", -1);
            if (ways.length != 4 && ways.length != 5) {
                throw new IllegalArgumentException("a flick entry is left|up|right|down[|hold]");
            }
            for (int w = 0; w < ways.length; w++) {
                ways[w] = ways[w].trim();
            }
            map.put(pairs[i], ways);
        }
        return Collections.unmodifiableMap(map);
    }
}
