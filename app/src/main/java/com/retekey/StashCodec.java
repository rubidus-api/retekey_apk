package com.retekey;

import java.util.ArrayList;
import java.util.List;

/**
 * How the shared-in text is written down and read back — the clip codec's shape, with the moment
 * it arrived in place of the pin flag, because the stash ages out and the clip list does not.
 */
final class StashCodec {
    /** U+001E RECORD SEPARATOR, between items. */
    private static final String RECORD = "";
    /** U+001F UNIT SEPARATOR, between an item's timestamp and its text. */
    private static final String FIELD = "";

    private StashCodec() {
    }

    static String encode(List<StashHistory.Kept> items) {
        StringBuilder out = new StringBuilder();
        for (StashHistory.Kept item : items) {
            if (item.text.contains(RECORD) || item.text.contains(FIELD)) {
                continue;
            }
            if (out.length() > 0) {
                out.append(RECORD);
            }
            out.append(item.keptAt).append(FIELD).append(item.text);
        }
        return out.toString();
    }

    static StashHistory decode(String stored) {
        List<StashHistory.Kept> items = new ArrayList<>();
        if (stored == null || stored.isEmpty()) {
            return StashHistory.empty();
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
