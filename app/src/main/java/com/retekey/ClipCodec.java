package com.retekey;

import java.util.ArrayList;
import java.util.List;

/**
 * How the clip history is written down and read back.
 *
 * <p>The same idea as {@link NoteCodec}, and for the same reason: a clip is exactly the place where
 * every separator anyone can type will eventually appear, so the records are separated by U+001E
 * and the pin flag by U+001F — two characters no keyboard produces. A clip carrying one anyway is
 * dropped on the way in rather than corrupting the record after it.
 */
final class ClipCodec {
    /** U+001E RECORD SEPARATOR, between clips. */
    private static final String RECORD = "\u001E";
    /** U+001F UNIT SEPARATOR, between a clip's pin flag and its text. */
    private static final String FIELD = "\u001F";

    private ClipCodec() {
    }

    static String encode(List<ClipHistory.Clip> clips) {
        StringBuilder out = new StringBuilder();
        for (ClipHistory.Clip clip : clips) {
            if (clip.text.contains(RECORD) || clip.text.contains(FIELD)) {
                continue;
            }
            if (out.length() > 0) {
                out.append(RECORD);
            }
            out.append(clip.pinned ? '1' : '0').append(FIELD).append(clip.text);
        }
        return out.toString();
    }

    static ClipHistory decode(String stored) {
        List<ClipHistory.Clip> clips = new ArrayList<>();
        if (stored == null || stored.isEmpty()) {
            return ClipHistory.empty();
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
