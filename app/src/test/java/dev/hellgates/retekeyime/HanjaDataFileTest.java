package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Test;

/**
 * Parses the real bundled {@code hanja.txt} (not a fixture) so a data regression — a bad edit, a
 * re-generation that drops entries — fails the build. Complements {@link HanjaTableTest}, which
 * exercises the parser on synthetic lines.
 */
public final class HanjaDataFileTest {
    private static HanjaTable loadShippedTable() {
        for (String candidate : new String[]{
            "src/main/assets/hanja.txt", "app/src/main/assets/hanja.txt"}) {
            Path path = Paths.get(candidate);
            if (Files.exists(path)) {
                try {
                    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                    return HanjaTable.parse(lines);
                } catch (IOException failure) {
                    throw new UncheckedIOException(failure);
                }
            }
        }
        throw new AssertionError("hanja.txt not found from working dir " + Paths.get("").toAbsolutePath());
    }

    private static HunumTable loadShippedHunum() {
        for (String candidate : new String[]{
            "src/main/assets/hanja_hunum.txt", "app/src/main/assets/hanja_hunum.txt"}) {
            Path path = Paths.get(candidate);
            if (Files.exists(path)) {
                try {
                    return HunumTable.parse(Files.readAllLines(path, StandardCharsets.UTF_8));
                } catch (IOException failure) {
                    throw new UncheckedIOException(failure);
                }
            }
        }
        throw new AssertionError("hanja_hunum.txt not found from " + Paths.get("").toAbsolutePath());
    }

    @Test
    public void shippedDataConvertsCommonReadingsAndWords() {
        HanjaTable table = loadShippedTable();
        assertTrue("table should hold many readings", table.size() > 1000);

        assertTrue("가 → 家", table.candidates("가").contains("家"));
        assertTrue("학 → 學", table.candidates("학").contains("學"));
        assertTrue("한 → 韓", table.candidates("한").contains("韓"));

        // Word entries convert whole.
        assertTrue("학교 → 學校", table.candidates("학교").contains("學校"));
        assertTrue("가족 → 家族", table.candidates("가족").contains("家族"));
    }

    @Test
    public void longestSuffixMatchWorksOnRealData() {
        HanjaTable table = loadShippedTable();
        HanjaTable.Match word = table.longestSuffixMatch("우리학교", 8);
        assertNotNull(word);
        assertTrue("학교 word wins over 교 alone",
            word.reading.equals("학교") && word.candidates.contains("學校"));
    }

    @Test
    public void shippedDataReverseConvertsHanjaToReadings() {
        HanjaTable table = loadShippedTable();
        assertTrue("學 → 학", table.readings("學").contains("학"));
        assertTrue("家 → 가", table.readings("家").contains("가"));
        assertTrue("學校 → 학교", table.readings("學校").contains("학교"));
    }

    @Test
    public void shippedHunumHasCommonGlosses() {
        HunumTable hunum = loadShippedHunum();
        assertTrue("many glosses", hunum.size() > 1000);
        assertEquals("집 가", hunum.gloss("家"));
        assertEquals("배울 학", hunum.gloss("學"));
    }
}
