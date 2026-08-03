package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * The shipped indexes, checked against the tables the app used to parse. The point of the change
 * was to stop holding the data in memory, not to change a single answer.
 */
public final class MappedHanjaTableTest {
    private static Path asset(String name) {
        for (String candidate : new String[]{"src/main/assets/", "app/src/main/assets/",
            "src/test/resources/hanja/", "app/src/test/resources/hanja/"}) {
            Path path = Paths.get(candidate + name);
            if (Files.exists(path)) {
                return path;
            }
        }
        throw new AssertionError(name + " not found from " + Paths.get("").toAbsolutePath());
    }

    private static SortedIndex index(String name) {
        try {
            return SortedIndex.over(ByteBuffer.wrap(Files.readAllBytes(asset(name))));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static List<String> lines(String name) {
        try {
            return Files.readAllLines(asset(name), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static final MappedHanjaTable MAPPED =
        new MappedHanjaTable(index("hanja_fwd.idx"), index("hanja_rev.idx"),
            index("hanja_hunum.idx"));

    @Test
    public void theIndexesAnswerWhatTheParsedTableAnswers() {
        HanjaTable parsed = HanjaTable.parse(lines("hanja.txt"));
        int checked = 0;
        for (String line : lines("hanja.txt")) {
            if (line.startsWith("#") || line.trim().isEmpty()) {
                continue;
            }
            String keys = line.substring(0, Math.max(0, line.indexOf(':')));
            for (String reading : keys.split(",")) {
                if (reading.isEmpty()) {
                    continue;
                }
                assertEquals(reading, parsed.candidates(reading), MAPPED.candidates(reading));
                checked++;
            }
        }
        assertTrue("checked the whole file", checked > 2000);
    }

    @Test
    public void theReverseDirectionAgreesTheSameWay() {
        HanjaTable parsed = HanjaTable.parse(lines("hanja.txt"));
        int checked = 0;
        for (String line : lines("hanja.txt")) {
            if (line.startsWith("#") || line.indexOf(':') < 0) {
                continue;
            }
            for (String hanja : line.substring(line.indexOf(':') + 1).split(",")) {
                if (hanja.isEmpty()) {
                    continue;
                }
                assertEquals(hanja, parsed.readings(hanja), MAPPED.readings(hanja));
                checked++;
                if (checked > 4000) {
                    return;
                }
            }
        }
    }

    @Test
    public void theGlossesAgree() {
        HunumTable parsed = HunumTable.parse(lines("hanja_hunum.txt"));
        for (String line : lines("hanja_hunum.txt")) {
            if (line.startsWith("#") || line.indexOf(':') < 0) {
                continue;
            }
            String hanja = line.substring(0, line.indexOf(':'));
            assertEquals(hanja, parsed.gloss(hanja), MAPPED.gloss(hanja));
        }
    }

    @Test
    public void theLongestSuffixWinsInBothDirections() {
        HanjaTable.Match word = MAPPED.longestSuffixMatch("나는 학교", 8);
        assertEquals("학교", word.reading);
        assertEquals(2, word.length);
        assertTrue(word.candidates.contains("學校"));

        HanjaTable.Match syllable = MAPPED.longestSuffixMatch("가", 8);
        assertEquals("가", syllable.reading);

        HanjaTable.Match reverse = MAPPED.longestSuffixReverseMatch("學校", 8);
        assertEquals("學校", reverse.reading);
        assertTrue(reverse.candidates.contains("학교"));
    }

    @Test
    public void anUnknownReadingMatchesNothing() {
        assertNull(MAPPED.longestSuffixMatch("", 8));
        assertNull(MAPPED.longestSuffixMatch(null, 8));
        assertEquals(Arrays.asList(), MAPPED.candidates("없는독음입니다"));
        assertNull(MAPPED.gloss("없"));
    }

    @Test
    public void aTableWithNoDataLeavesTheKeyboardWorking() {
        MappedHanjaTable empty = MappedHanjaTable.empty();

        assertTrue(empty.candidates("가").isEmpty());
        assertTrue(empty.readings("佳").isEmpty());
        assertNull(empty.gloss("佳"));
        assertNull(empty.longestSuffixMatch("가", 8));
        assertNull(empty.longestSuffixReverseMatch("佳", 8));
    }
}
