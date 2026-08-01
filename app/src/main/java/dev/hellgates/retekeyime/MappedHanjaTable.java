package dev.hellgates.retekeyime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Hanja conversion table, searched on disk rather than held in memory.
 *
 * <p>It answers exactly what {@link HanjaTable} answers — candidates for a reading, readings for a
 * character, the longest suffix of either that the table knows, and the 훈음 gloss — but over three
 * {@link SortedIndex} files instead of three hash maps, so the data costs the Java heap nothing.
 */
public final class MappedHanjaTable {
    private final SortedIndex forward;
    private final SortedIndex reverse;
    private final SortedIndex glosses;

    public MappedHanjaTable(SortedIndex forward, SortedIndex reverse, SortedIndex glosses) {
        this.forward = forward == null ? SortedIndex.empty() : forward;
        this.reverse = reverse == null ? SortedIndex.empty() : reverse;
        this.glosses = glosses == null ? SortedIndex.empty() : glosses;
    }

    /** An empty table, so a failure to open the data leaves the keyboard working without Hanja. */
    public static MappedHanjaTable empty() {
        return new MappedHanjaTable(SortedIndex.empty(), SortedIndex.empty(), SortedIndex.empty());
    }

    /** Candidates for an exact reading (syllable or word); empty when none. */
    public List<String> candidates(String reading) {
        return split(forward.find(reading));
    }

    /** Readings for a Hanja character or word (한자 → 한글); empty when unknown. */
    public List<String> readings(String hanja) {
        return split(reverse.find(hanja));
    }

    /** The 훈음 gloss for a character (家 → 집 가), or {@code null} when the table has none. */
    public String gloss(String hanja) {
        return hanja == null ? null : glosses.find(hanja);
    }

    /**
     * The longest suffix of {@code before} that has candidates, so 학교 converts as a word while a
     * trailing lone syllable still converts on its own. Returns {@code null} when nothing matches.
     */
    public HanjaTable.Match longestSuffixMatch(String before, int maxLen) {
        return longestSuffix(forward, before, maxLen);
    }

    /** The same, for 한자 → 한글: 學校 wins over 校 alone. */
    public HanjaTable.Match longestSuffixReverseMatch(String before, int maxLen) {
        return longestSuffix(reverse, before, maxLen);
    }

    private static HanjaTable.Match longestSuffix(SortedIndex index, String before, int maxLen) {
        if (before == null || before.isEmpty() || index.isEmpty()) {
            return null;
        }
        int cap = Math.min(maxLen, Math.min(before.length(), index.maxKeyLength()));
        for (int length = cap; length >= 1; length--) {
            String suffix = before.substring(before.length() - length);
            List<String> values = split(index.find(suffix));
            if (!values.isEmpty()) {
                return new HanjaTable.Match(suffix, length, values);
            }
        }
        return null;
    }

    private static List<String> split(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> parts = new ArrayList<>();
        for (String part : value.split(",")) {
            if (!part.isEmpty()) {
                parts.add(part);
            }
        }
        return Collections.unmodifiableList(parts);
    }
}
