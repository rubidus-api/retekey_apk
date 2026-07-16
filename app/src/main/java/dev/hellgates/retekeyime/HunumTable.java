package dev.hellgates.retekeyime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A Hanja → 훈음 (gloss: meaning + reading, e.g. 家 → "집 가") table parsed from
 * {@code hanja_hunum.txt}. Rare characters may have no gloss; the candidate strip then shows the
 * character alone. Parsing is Android-free and unit-tested; {@link HanjaDictionary} loads the asset.
 */
public final class HunumTable {
    private final Map<String, String> glosses;

    private HunumTable(Map<String, String> glosses) {
        this.glosses = glosses;
    }

    public static HunumTable parse(Iterable<String> lines) {
        Map<String, String> map = new HashMap<>();
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String text = line.trim();
            if (text.isEmpty() || text.charAt(0) == '#') {
                continue;
            }
            int colon = text.indexOf(':');
            if (colon <= 0 || colon == text.length() - 1) {
                continue;
            }
            String hanja = text.substring(0, colon).trim();
            String gloss = text.substring(colon + 1).trim();
            if (!hanja.isEmpty() && !gloss.isEmpty()) {
                map.putIfAbsent(hanja, gloss);
            }
        }
        return new HunumTable(Collections.unmodifiableMap(map));
    }

    /** The gloss for a Hanja, or {@code null} when none is recorded. */
    public String gloss(String hanja) {
        return hanja == null ? null : glosses.get(hanja);
    }

    public int size() {
        return glosses.size();
    }
}
