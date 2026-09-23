package com.retekey;

import java.util.ArrayList;
import java.util.List;

/**
 * How the clip history is written down and read back: two fields a clip — the pin flag and the
 * text — through {@link RecordCodec}. The first form separated records with U+001E and fields
 * with U+001F and dropped any clip that contained one, so a copied fragment could be shown in the
 * list and be gone after a restart (review finding R07). That form is still read once, so nothing
 * saved before is lost.
 */
final class ClipCodec {
    /** U+001E RECORD SEPARATOR, between clips in the first form. */
    private static final String RECORD = "\u001E";
    /** U+001F UNIT SEPARATOR, between a clip's pin flag and its text in the first form. */
    private static final String FIELD = "\u001F";
    private static final int FIELDS = 2;

    private ClipCodec() {
    }

    static String encode(List<ClipHistory.Clip> clips) {
        List<String[]> records = new ArrayList<>(clips.size());
        for (ClipHistory.Clip clip : clips) {
            records.add(new String[] {clip.pinned ? "1" : "0", clip.text});
        }
        return RecordCodec.encode(records, FIELDS);
    }

    static ClipHistory decode(String stored) {
        if (stored == null || stored.isEmpty()) {
            return ClipHistory.empty();
        }
        List<ClipHistory.Clip> clips = new ArrayList<>();
        if (RecordCodec.isNewFormat(stored)) {
            for (String[] record : RecordCodec.decode(stored, FIELDS)) {
                if (!record[1].isEmpty()) {
                    clips.add(new ClipHistory.Clip(record[1], "1".equals(record[0])));
                }
            }
            return ClipHistory.of(clips);
        }
        for (String record : stored.split(RECORD, -1)) {
            int field = record.indexOf(FIELD);
            if (field < 0) {
                continue;
            }
            String text = record.substring(field + 1);
            if (text.isEmpty()) {
                continue;
            }
            clips.add(new ClipHistory.Clip(text, "1".equals(record.substring(0, field))));
        }
        return ClipHistory.of(clips);
    }
}
