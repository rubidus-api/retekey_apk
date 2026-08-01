package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

/** Bisecting sorted key/value lines where they lie, instead of parsing them into a map. */
public final class SortedIndexTest {
    private static SortedIndex index(String text) {
        return SortedIndex.over(ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static final String SAMPLE =
        "# a notice that travels with the data\n"
        + "#!maxkey 3\n"
        + "가\t佳,假\n"
        + "가나\t伽那\n"
        + "학\t學,鶴\n"
        + "학교\t學校\n";

    @Test
    public void findsEveryKeyItHolds() {
        SortedIndex index = index(SAMPLE);

        assertEquals("佳,假", index.find("가"));
        assertEquals("伽那", index.find("가나"));
        assertEquals("學,鶴", index.find("학"));
        assertEquals("學校", index.find("학교"));
    }

    @Test
    public void aKeyItDoesNotHoldIsNotAPrefixMatch() {
        SortedIndex index = index(SAMPLE);

        assertNull(index.find("나"));
        assertNull(index.find("가나다"));
        assertNull(index.find("하"));
        // A prefix of a key it has must not answer for it.
        assertNull(index.find("학교다"));
    }

    @Test
    public void theHeaderIsSkippedAndItsDirectiveRead() {
        assertEquals(3, index(SAMPLE).maxKeyLength());
        // A file with no directive still works; it just does not promise a key length.
        assertEquals(1, index("#only a notice\n가\t佳\n").maxKeyLength());
        assertEquals("佳", index("#only a notice\n가\t佳\n").find("가"));
    }

    @Test
    public void nothingAtAllIsAnEmptyIndex() {
        assertTrue(SortedIndex.empty().isEmpty());
        assertNull(SortedIndex.empty().find("가"));
        assertTrue(index("# nothing but a header\n").isEmpty());
    }

    @Test
    public void nullAndEmptyKeysFindNothing() {
        SortedIndex index = index(SAMPLE);

        assertNull(index.find(null));
        assertNull(index.find(""));
    }

    @Test
    public void aFileWithoutATrailingNewlineStillSearches() {
        assertEquals("學校", index("#h\n가\t佳\n학교\t學校").find("학교"));
    }

    @Test
    public void everyLineIsReachableAtEveryPosition() {
        // A bisect that mishandles line boundaries tends to lose the first or the last entry.
        StringBuilder text = new StringBuilder("#h\n#!maxkey 4\n");
        for (int i = 100; i < 400; i++) {
            text.append("k").append(i).append('\t').append("v").append(i).append('\n');
        }
        SortedIndex index = index(text.toString());
        for (int i = 100; i < 400; i++) {
            assertEquals("v" + i, index.find("k" + i));
        }
        assertNull(index.find("k099"));
        assertNull(index.find("k400"));
    }
}
