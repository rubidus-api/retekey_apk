package com.retekey;

import android.content.SharedPreferences;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

/**
 * Moving the stored lists to the lossless form: once, with the original kept beside them, and
 * never when the move would lose a record (review W4).
 */
public final class StoreMigrationTest {
    private static final String KEY = "clips";

    @Test
    public void theFirstFormIsRewrittenOnceWithTheOriginalKept() {
        SharedPreferences prefs = new LockedPreferences();
        String old = "1\u001Fpinned\u001E0\u001Fplain";
        prefs.edit().putString(KEY, old).commit();
        ClipHistory read = ClipCodec.decode(old);
        String rewritten = ClipCodec.encode(read.clips());

        StoreMigration.rewrite(prefs, KEY, old, rewritten, true);

        Assert.assertEquals(rewritten, prefs.getString(KEY, ""));
        Assert.assertEquals(old, prefs.getString(KEY + "_v1", ""));
        Assert.assertEquals(read.clips(), ClipCodec.decode(prefs.getString(KEY, "")).clips());

        // Read again: already current, so nothing is written and the first copy stays as it was.
        prefs.edit().putString(KEY + "_v1", "kept").commit();
        StoreMigration.rewrite(prefs, KEY, prefs.getString(KEY, ""), rewritten, true);
        Assert.assertEquals("kept", prefs.getString(KEY + "_v1", ""));
    }

    /** If the rewrite would not read back as what was there, the old text is left alone. */
    @Test
    public void anUnfaithfulRewriteIsNotWritten() {
        SharedPreferences prefs = new LockedPreferences();
        String old = "0\u001Fplain";
        prefs.edit().putString(KEY, old).commit();

        StoreMigration.rewrite(prefs, KEY, old, "retekey-store 2 2\n0:0:", false);

        Assert.assertEquals(old, prefs.getString(KEY, ""));
        Assert.assertEquals(old, prefs.getString(KEY + "_v1", ""));
    }

    /**
     * Text in the current form that is not what its own records write is damaged, and kept. The
     * store asks exactly as it does in the app: a round trip of what was read cannot tell, since
     * the readable prefix of a cut store re-encodes to itself.
     */
    @Test
    public void damagedCurrentTextIsKeptAside() {
        SharedPreferences prefs = new LockedPreferences();
        String whole = ClipCodec.encode(ClipCodec.decode("0\u001Fone\u001E0\u001Ftwo").clips());
        String cut = whole.substring(0, whole.length() - 2);
        prefs.edit().putString(KEY, cut).commit();
        ClipHistory readable = ClipCodec.decode(cut);
        String rewritten = ClipCodec.encode(readable.clips());

        StoreMigration.rewrite(prefs, KEY, cut, rewritten,
            readable.clips().equals(ClipCodec.decode(rewritten).clips()));

        Assert.assertEquals(cut, prefs.getString(KEY + "_damaged", ""));
        Assert.assertEquals(cut, prefs.getString(KEY, ""));
        Assert.assertFalse(prefs.contains(KEY + "_v1"));
    }

    /** A whole store in the current form is left alone, and nothing is copied aside. */
    @Test
    public void wholeCurrentTextIsLeftAlone() {
        SharedPreferences prefs = new LockedPreferences();
        ClipHistory history = ClipHistory.empty().record("kept", false);
        String stored = ClipCodec.encode(history.clips());
        prefs.edit().putString(KEY, stored).commit();
        ClipHistory read = ClipCodec.decode(stored);
        String rewritten = ClipCodec.encode(read.clips());

        StoreMigration.rewrite(prefs, KEY, stored, rewritten,
            read.clips().equals(ClipCodec.decode(rewritten).clips()));

        Assert.assertEquals(stored, prefs.getString(KEY, ""));
        Assert.assertFalse(prefs.contains(KEY + "_damaged"));
        Assert.assertFalse(prefs.contains(KEY + "_v1"));
    }

    @Test
    public void anEmptyStoreIsNotMigrated() {
        SharedPreferences prefs = new LockedPreferences();
        StoreMigration.rewrite(prefs, KEY, "", ClipCodec.encode(
            Collections.<ClipHistory.Clip>emptyList()), true);
        Assert.assertFalse(prefs.contains(KEY));
        Assert.assertFalse(prefs.contains(KEY + "_v1"));
    }
}
