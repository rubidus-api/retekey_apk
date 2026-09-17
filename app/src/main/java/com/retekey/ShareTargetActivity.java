package com.retekey;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/**
 * The door text comes in by: any app's **Share** (or the text-selection menu) hands ReteKey a
 * piece of text, and ReteKey keeps it to type later (issue #10).
 *
 * <p>Nothing is copied. The system clipboard is not read and not written — that is the request and
 * the whole point: on some ROMs a keyboard sitting in the background reads every copy and keeps it
 * in a history the user cannot turn off, and the answer is to have text that was never copied at
 * all. It also needs no permission of any kind; a share is the sending app's own decision.
 *
 * <p>There is no screen: the text is kept, a word is said, and the activity is gone. Whatever the
 * user was doing is still in front of them.
 */
public final class ShareTargetActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        CharSequence text = sharedText(getIntent());
        if (text == null || text.toString().trim().isEmpty()) {
            Toast.makeText(this, R.string.stash_nothing, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (UserLayout.looksLikeOne(text.toString())) {
            // A layout somebody wrote, shared in the same way text is (issue #11). It replaces the
            // one installed before, if any: there is one slot, and saying so is better than a list.
            UserLayout installed = UserLayouts.install(this, text.toString());
            Toast.makeText(this,
                installed == null
                    ? getString(R.string.layout_not_read)
                    : getString(R.string.layout_installed, installed.name()),
                Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        StashHistory kept = StashStore.loadPruned(this)
            .record(text, System.currentTimeMillis());
        StashStore.save(this, kept);
        Toast.makeText(this, R.string.stash_kept, Toast.LENGTH_SHORT).show();
        finish();
    }

    /** The text of a share or a text-selection action, or null if the intent carries none. */
    private static CharSequence sharedText(Intent intent) {
        if (intent == null) {
            return null;
        }
        CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        if (text != null) {
            return text;
        }
        // The text-selection menu's own extra, which arrives on Android 6 and later.
        return intent.getCharSequenceExtra("android.intent.extra.PROCESS_TEXT");
    }
}
