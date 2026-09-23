package com.retekey;

import android.content.SharedPreferences;

/**
 * Moves one stored list from the first form to the one {@link RecordCodec} writes, once, keeping
 * the original beside it (review W4). Three rules:
 *
 * <ul>
 *   <li>the original text is kept under {@code <key>_v1} and written only once, so a later save
 *       cannot overwrite the copy the migration was made from;</li>
 *   <li>the new text is written only when reading it back gives what was read from the old one,
 *       so a migration that would lose a record does not happen at all;</li>
 *   <li>text in the new form that does not read back as itself is damaged: the original is kept
 *       under {@code <key>_damaged}, once, and what could be read is used.</li>
 * </ul>
 *
 * <p>There is no way back: an older build reading the new form finds no records and would write
 * its own empty list over it. The copy under {@code <key>_v1} is what a downgrade is recovered
 * from, by hand.
 */
final class StoreMigration {
    private StoreMigration() {
    }

    /**
     * @param stored what the key holds now
     * @param rewritten the same records in the current form
     * @param faithful whether reading {@code rewritten} back gave the records {@code stored} did
     */
    static void rewrite(
        SharedPreferences prefs,
        String key,
        String stored,
        String rewritten,
        boolean faithful
    ) {
        if (prefs == null || stored == null || stored.isEmpty()) {
            return;
        }
        boolean current = RecordCodec.isNewFormat(stored);
        if (current && faithful) {
            return;
        }
        String backup = key + (current ? "_damaged" : "_v1");
        try {
            SharedPreferences.Editor editor = prefs.edit();
            if (!prefs.contains(backup)) {
                editor.putString(backup, stored);
            }
            if (faithful) {
                editor.putString(key, rewritten);
            }
            editor.commit();
        } catch (RuntimeException unavailable) {
            // Storage that will not take the migration keeps the old text; the next load tries
            // again. Nothing is lost by not migrating.
            android.util.Log.w("ReteKey", "store migration failed: " + key + ": "
                + unavailable.getClass().getName());
        }
    }
}
