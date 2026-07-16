package dev.hellgates.retekeyime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A reading → Hanja-candidate table parsed from {@code hanja.txt}. Keys are Korean readings: a
 * single syllable (가 → 佳,假,價 …) or a whole word (가격 → 價格). A line may list several readings
 * for one candidate set ("가,고:賈"); each reading is indexed separately.
 *
 * <p>Parsing and lookup are Android-free so they are unit-tested directly; {@link HanjaDictionary}
 * feeds this the asset's lines at runtime.
 */
public final class HanjaTable {
    private final Map<String, List<String>> byReading;
    private final int maxKeyLength;

    private HanjaTable(Map<String, List<String>> byReading, int maxKeyLength) {
        this.byReading = byReading;
        this.maxKeyLength = maxKeyLength;
    }

    public static HanjaTable parse(Iterable<String> lines) {
        Map<String, List<String>> map = new HashMap<>();
        int maxKeyLength = 1;
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
            List<String> candidates = new ArrayList<>();
            for (String raw : text.substring(colon + 1).split(",")) {
                String candidate = raw.trim();
                if (!candidate.isEmpty()) {
                    candidates.add(candidate);
                }
            }
            if (candidates.isEmpty()) {
                continue;
            }
            for (String rawKey : text.substring(0, colon).split(",")) {
                String key = rawKey.trim();
                if (key.isEmpty()) {
                    continue;
                }
                List<String> existing = map.get(key);
                if (existing == null) {
                    map.put(key, new ArrayList<>(candidates));
                } else {
                    for (String candidate : candidates) {
                        if (!existing.contains(candidate)) {
                            existing.add(candidate);
                        }
                    }
                }
                if (key.length() > maxKeyLength) {
                    maxKeyLength = key.length();
                }
            }
        }
        Map<String, List<String>> frozen = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            frozen.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return new HanjaTable(frozen, maxKeyLength);
    }

    /** Candidates for an exact reading (syllable or word); empty when none. */
    public List<String> candidates(String reading) {
        List<String> result = reading == null ? null : byReading.get(reading);
        return result == null ? Collections.emptyList() : result;
    }

    public int size() {
        return byReading.size();
    }

    public int maxKeyLength() {
        return maxKeyLength;
    }

    /**
     * The longest suffix of {@code before} (bounded by {@code maxLen} and the table's longest key)
     * that has candidates, so "학교" converts as a word while a trailing lone syllable still
     * converts on its own. Returns {@code null} when no suffix matches.
     */
    public Match longestSuffixMatch(String before, int maxLen) {
        if (before == null || before.isEmpty()) {
            return null;
        }
        int cap = Math.min(maxLen, Math.min(before.length(), maxKeyLength));
        for (int length = cap; length >= 1; length--) {
            String suffix = before.substring(before.length() - length);
            List<String> candidates = byReading.get(suffix);
            if (candidates != null && !candidates.isEmpty()) {
                return new Match(suffix, length, candidates);
            }
        }
        return null;
    }

    /** A matched reading: its text, its length in characters, and the Hanja candidates. */
    public static final class Match {
        public final String reading;
        public final int length;
        public final List<String> candidates;

        Match(String reading, int length, List<String> candidates) {
            this.reading = reading;
            this.length = length;
            this.candidates = candidates;
        }
    }
}
