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
    /** The alias a layout arrives through; the plain entry keeps text and installs nothing. */
    private static final String LAYOUT_DOOR = "com.retekey.InstallLayoutActivity";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Intent intent = getIntent();
        boolean layoutDoor = throughLayoutDoor(intent);
        CharSequence shared = sharedText(intent);
        String text = shared == null ? null : shared.toString();
        if (layoutDoor && (text == null || text.trim().isEmpty())) {
            // Opened as a file: read what it holds, which needs no permission — the opener granted
            // this one URI when it chose ReteKey.
            text = fileText(intent);
        }
        switch (ShareRoute.of(layoutDoor, text)) {
            case INSTALL_LAYOUT: {
                // A layout somebody wrote (issue #11). It replaces the one installed before, if
                // any: there is one slot, and saying so is better than a list.
                UserLayout installed = text == null ? null : UserLayouts.install(this, text);
                Toast.makeText(this,
                    installed == null
                        ? getString(R.string.layout_not_read)
                        : getString(R.string.layout_installed, installed.name()),
                    Toast.LENGTH_LONG).show();
                break;
            }
            case KEEP_TEXT: {
                StashHistory kept = StashStore.loadPruned(this)
                    .record(text, System.currentTimeMillis());
                StashStore.save(this, kept);
                Toast.makeText(this, R.string.stash_kept, Toast.LENGTH_SHORT).show();
                break;
            }
            default:
                Toast.makeText(this, R.string.stash_nothing, Toast.LENGTH_SHORT).show();
                break;
        }
        finish();
    }

    /** Whether this arrived at the layout entry, or as an opened file. */
    private static boolean throughLayoutDoor(Intent intent) {
        if (intent == null) {
            return false;
        }
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            return true;
        }
        return intent.getComponent() != null
            && LAYOUT_DOOR.equals(intent.getComponent().getClassName());
    }

    /** The text of an opened file, or null when it cannot be read. */
    private String fileText(Intent intent) {
        android.net.Uri uri = intent == null ? null : intent.getData();
        if (uri == null) {
            return null;
        }
        java.io.InputStream in = null;
        try {
            in = getContentResolver().openInputStream(uri);
            if (in == null) {
                return null;
            }
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            // A layout is a few hundred bytes; anything past a sensible ceiling is not one, and
            // reading it whole into memory would be somebody else's file doing it.
            while ((read = in.read(buffer)) > 0 && out.size() < MOST_A_LAYOUT_CAN_BE) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), "UTF-8");
        } catch (java.io.IOException | RuntimeException e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (java.io.IOException ignored) {
                    // Nothing useful to do about a file that will not close.
                }
            }
        }
    }

    /** The most of an opened file that is read: a layout is small, and this is not a file viewer. */
    private static final int MOST_A_LAYOUT_CAN_BE = 64 * 1024;

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
