package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import org.junit.Test;

public final class HunumTableTest {
    @Test
    public void parsesGlossesAndSkipsCommentsAndBlanks() {
        HunumTable t = HunumTable.parse(Arrays.asList(
            "# comment", "", "家:집 가", "學:배울 학", "bad", "빈:", ":값"));
        assertEquals("집 가", t.gloss("家"));
        assertEquals("배울 학", t.gloss("學"));
        assertEquals(2, t.size());
    }

    @Test
    public void unknownOrNullReturnsNull() {
        HunumTable t = HunumTable.parse(Arrays.asList("家:집 가"));
        assertNull(t.gloss("鶴"));
        assertNull(t.gloss(null));
    }
}
