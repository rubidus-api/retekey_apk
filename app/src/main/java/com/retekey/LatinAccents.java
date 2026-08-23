package com.retekey;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * What each Latin-script language holds under its base letters — the accented letters a phone
 * keyboard reaches by holding, since the letter rows are QWERTY's and have no cell to spare.
 *
 * <p>Order matters: it is the order the hold strip shows them in, after the key's own group hold.
 * The commonest letter for that language comes first so the shortest slide reaches it. Tables are
 * by lowercase base letter; the shifted page capitalises them.
 *
 * <p>Sources: the language's own orthography, and what Gboard/iOS put first (RFC-0011 §2.14).
 */
final class LatinAccents {
    private LatinAccents() {
    }

    static final Map<String, String[]> SPANISH = table(
        "a", "á",
        "e", "é",
        "i", "í",
        "o", "ó",
        "u", "ú ü"
    );

    static final Map<String, String[]> PORTUGUESE = table(
        "a", "á â ã à ª",
        "e", "é ê",
        "i", "í",
        "o", "ó ô õ º",
        "u", "ú ü",
        "c", "ç"
    );

    static final Map<String, String[]> ITALIAN = table(
        "a", "à",
        "e", "è é",
        "i", "ì",
        "o", "ò",
        "u", "ù"
    );

    static final Map<String, String[]> POLISH = table(
        "a", "ą",
        "c", "ć",
        "e", "ę",
        "l", "ł",
        "n", "ń",
        "o", "ó",
        "s", "ś",
        "z", "ż ź"
    );

    /** Pairs of base letter and space-separated accents, in strip order. */
    private static Map<String, String[]> table(String... pairs) {
        Map<String, String[]> map = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1].split(" "));
        }
        return Collections.unmodifiableMap(map);
    }
}
