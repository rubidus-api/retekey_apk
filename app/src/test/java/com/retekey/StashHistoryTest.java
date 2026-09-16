package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/**
 * Text shared into ReteKey rather than copied (issue #10): what is kept, for how long, and that a
 * round trip through storage does not change it.
 */
public final class StashHistoryTest {
    private static final long NOW = 1_700_000_000_000L;
    private static final long MINUTE = 60_000L;

    @Test
    public void theNewestIsFirstAndARepeatMovesRatherThanDuplicates() {
        StashHistory kept = StashHistory.empty()
            .record("one", NOW)
            .record("two", NOW + MINUTE)
            .record("one", NOW + 2 * MINUTE);
        List<StashHistory.Kept> items = kept.items();
        assertEquals(2, items.size());
        assertEquals("one", items.get(0).text);
        assertEquals("two", items.get(1).text);
        assertEquals(NOW + 2 * MINUTE, items.get(0).keptAt);
    }

    @Test
    public void itAgesOutOnTheUsersClock() {
        StashHistory kept = StashHistory.empty()
            .record("old", NOW - 90 * MINUTE)
            .record("fresh", NOW - 5 * MINUTE);
        assertEquals(1, kept.pruned(NOW, 60).items().size());
        assertEquals("fresh", kept.pruned(NOW, 60).items().get(0).text);
        // Zero minutes is the user saying "until I remove it".
        assertEquals(2, kept.pruned(NOW, 0).items().size());
    }

    @Test
    public void itKeepsOnlySoMany() {
        StashHistory kept = StashHistory.empty();
        for (int i = 0; i < StashHistory.LIMIT + 5; i++) {
            kept = kept.record("item " + i, NOW + i);
        }
        assertEquals(StashHistory.LIMIT, kept.items().size());
        assertEquals("item " + (StashHistory.LIMIT + 4), kept.items().get(0).text);
    }

    @Test
    public void nothingBlankAndNothingEnormous() {
        assertTrue(StashHistory.empty().record("   ", NOW).isEmpty());
        assertTrue(StashHistory.empty().record(null, NOW).isEmpty());
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < StashHistory.MAX_LENGTH + 100; i++) {
            huge.append('x');
        }
        assertEquals(StashHistory.MAX_LENGTH,
            StashHistory.empty().record(huge, NOW).items().get(0).text.length());
    }

    @Test
    public void storageRoundTripsWhatWasKept() {
        StashHistory kept = StashHistory.empty()
            .record("a link https://example.com/x?y=1", NOW)
            .record("a line with\na newline in it", NOW + MINUTE);
        StashHistory back = StashCodec.decode(StashCodec.encode(kept.items()));
        assertEquals(kept.items().size(), back.items().size());
        assertEquals(kept.items().get(0).text, back.items().get(0).text);
        assertEquals(kept.items().get(0).keptAt, back.items().get(0).keptAt);
        assertEquals(kept.items().get(1).text, back.items().get(1).text);
    }

    @Test
    public void removingOneLeavesTheRest() {
        StashHistory kept = StashHistory.empty().record("one", NOW).record("two", NOW + 1);
        assertEquals(1, kept.remove("two").items().size());
        assertEquals("one", kept.remove("two").items().get(0).text);
        assertTrue(kept.clear().isEmpty());
    }
}
