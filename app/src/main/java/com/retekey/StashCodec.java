package com.retekey;

import java.util.ArrayList;
import java.util.List;

/**
 * How the shared-in text is written down and read back — the clip codec's shape, with the moment
 * it arrived in place of the pin flag, because the stash ages out and the clip list does not. The
 * first form dropped an item containing U+001E or U+001F, so sharing could report success and keep
 * nothing (review finding R07); it is still read once.
 */
final class StashCodec {
    /** U+001E RECORD SEPARATOR, between items in the first form. */
    private static final String RECORD = "\u001E";
    /** U+001F UNIT SEPARATOR, between an item's timestamp and its text in the first form. */
    private static final String FIELD = "\u001F";
    private static final int FIELDS = 2;

    private StashCodec() {
    }

    static String encode(List<StashHistory.Kept> items) {
        List<String[]> records = new ArrayList<>(items.size());
        for (StashHistory.Kept item : items) {
            records.add(new String[] {Long.toString(item.keptAt), item.text});
        }
        return RecordCodec.encode(records, FIELDS);
    }

    static StashHistory decode(String stored) {
        if (stored == null || stored.isEmpty()) {
            return StashHistory.empty();
        }
        List<StashHistory.Kept> items = new ArrayList<>();
        if (RecordCodec.isNewFormat(stored)) {
            for (String[] record : RecordCodec.decode(stored, FIELDS)) {
                if (record[1].isEmpty()) {
                    continue;
                }
                try {
                    items.add(new StashHistory.Kept(record[1], Long.parseLong(record[0])));
                } catch (NumberFormatException damaged) {
                    // A record whose timestamp did not survive is dropped, not guessed at.
                }
            }
            return StashHistory.of(items);
        }
        for (String record : stored.split(RECORD, -1)) {
            int field = record.indexOf(FIELD);
            if (field <= 0) {
                continue;
            }
            String text = record.substring(field + 1);
            if (text.isEmpty()) {
                continue;
            }
            try {
                items.add(new StashHistory.Kept(text, Long.parseLong(record.substring(0, field))));
            } catch (NumberFormatException ignored) {
                // A record whose timestamp did not survive is dropped, not guessed at.
            }
        }
        return StashHistory.of(items);
    }
}
