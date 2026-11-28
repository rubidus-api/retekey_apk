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
    private android.app.AlertDialog activeDialog;

    @Override
    protected void onDestroy() {
        if (activeDialog != null) {
            if (activeDialog.isShowing()) {
                activeDialog.dismiss();
            }
            activeDialog = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Intent intent = getIntent();
        boolean layoutDoor = throughLayoutDoor(intent);
        CharSequence shared = sharedText(intent);
        String text = shared == null ? null : shared.toString();
        switch (ShareRoute.of(layoutDoor, text)) {
            case INSTALL_LAYOUT:
                // A layout somebody wrote (issue #11). Nothing is installed until the user says
                // so: an app that can send a share must not be able to replace the keyboard's
                // layout by sending one (review finding R13).
                offerLayout(intent, text);
                return;
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

    /**
     * Reads the layout — off this thread when it is a file, since somebody else's provider decides
     * how long that takes — and then asks.
     */
    private void offerLayout(final Intent intent, final String shared) {
        if (shared != null && !shared.trim().isEmpty()) {
            ask(UserLayout.check(shared), shared);
            return;
        }
        final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                final LayoutStreamReader.Result result = readLayout(intent);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || (android.os.Build.VERSION.SDK_INT
                                >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1
                                && isDestroyed())) {
                            return;
                        }
                        if (result.status == LayoutStreamReader.Status.TOO_BIG) {
                            showProblem(R.string.layout_problem_too_big);
                            return;
                        }
                        if (result.status != LayoutStreamReader.Status.OK) {
                            showProblem(R.string.layout_not_read);
                            return;
                        }
                        ask(UserLayout.check(result.text), result.text);
                    }
                });
            }
        }, "layout-read").start();
    }

    private android.content.Context dialogContext() {
        switch (ScreenTheme.mode(this)) {
            case LIGHT:
                return new android.view.ContextThemeWrapper(this, R.style.ReteScreenThemeLight);
            case DARK:
                return new android.view.ContextThemeWrapper(this, R.style.ReteScreenThemeDark);
            default:
                return new android.view.ContextThemeWrapper(this, R.style.ReteScreenTheme);
        }
    }

    private void showActiveDialog(android.app.AlertDialog dialog) {
        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.dismiss();
        }
        activeDialog = dialog;
        dialog.show();
    }

    private void showProblem(int messageRes) {
        showActiveDialog(new android.app.AlertDialog.Builder(dialogContext())
            .setTitle(R.string.layout_problem_title)
            .setMessage(messageRes)
            .setOnCancelListener(new android.content.DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(android.content.DialogInterface dialog) {
                    finish();
                }
            })
            .setPositiveButton(android.R.string.ok,
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        finish();
                    }
                })
            .create());
    }

    /** The one question this door asks: install this, or leave what is installed alone. */
    private void ask(UserLayout.Checked checked, final String text) {
        UserLayout layout = checked.layout();
        if (layout == null) {
            showProblem(reasonFor(checked.problem()));
            return;
        }
        UserLayouts.load(this);
        UserLayout installed = UserLayouts.current();
        String details = getString(R.string.layout_install_details,
            layout.name(), layout.cap(), firstKeys(layout));
        String replaces = installed == null
            ? getString(R.string.layout_install_first)
            : getString(R.string.layout_install_replaces, installed.name());
        showActiveDialog(new android.app.AlertDialog.Builder(dialogContext())
            .setTitle(R.string.layout_install_title)
            .setMessage(details + "\n\n" + replaces)
            .setNegativeButton(android.R.string.cancel,
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        finish();
                    }
                })
            .setOnCancelListener(new android.content.DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(android.content.DialogInterface dialog) {
                    finish();
                }
            })
            .setPositiveButton(R.string.layout_install_button,
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        UserLayout put = UserLayouts.install(ShareTargetActivity.this, text);
                        Toast.makeText(ShareTargetActivity.this,
                            put == null
                                ? getString(R.string.layout_not_read)
                                : getString(R.string.layout_installed, put.name()),
                            Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
            .create());
    }

    /** The first row's keys, as a line: what this layout would put under the fingers. */
    private static String firstKeys(UserLayout layout) {
        StringBuilder out = new StringBuilder();
        for (UserLayout.Key key : layout.rows().get(0)) {
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(key.types);
        }
        return out.toString();
    }

    private int reasonFor(UserLayout.Problem problem) {
        switch (problem) {
            case WRONG_VERSION:
                return R.string.layout_problem_wrong_version;
            case TOO_BIG:
            case LINE_TOO_LONG:
                return R.string.layout_problem_too_big;
            case KEY_TOO_LONG:
                return R.string.layout_problem_key_too_long;
            case EMPTY_ROW:
                return R.string.layout_problem_empty_row;
            case NO_NAME:
                return R.string.layout_problem_no_name;
            case NOT_THREE_ROWS:
                return R.string.layout_problem_rows;
            case INVALID_TEXT:
            default:
                return R.string.layout_problem_not_a_layout;
        }
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

    private LayoutStreamReader.Result readLayout(Intent intent) {
        android.net.Uri uri = layoutUri(intent);
        if (uri == null) {
            return LayoutStreamReader.Result.unreadable();
        }
        android.content.pm.ApplicationInfo appInfo = getApplicationInfo();
        String dataDir = appInfo == null ? null : appInfo.dataDir;
        if (LayoutStreamReader.isOwnPrivateFile(uri.getScheme(), uri.getPath(), dataDir)) {
            return LayoutStreamReader.Result.unreadable();
        }
        java.io.InputStream in = null;
        try {
            in = getContentResolver().openInputStream(uri);
            return LayoutStreamReader.read(in, MOST_A_LAYOUT_CAN_BE);
        } catch (java.io.IOException | RuntimeException e) {
            return LayoutStreamReader.Result.unreadable();
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

    private static android.net.Uri layoutUri(Intent intent) {
        if (intent == null) {
            return null;
        }
        android.net.Uri data = intent.getData();
        if (data != null) {
            return data;
        }
        try {
            return Compat.parcelableExtra(intent, Intent.EXTRA_STREAM, android.net.Uri.class);
        } catch (RuntimeException unreadable) {
            // See sharedText: extras another app built badly are no layout.
            return null;
        }
    }

    /** The most of an opened file that is read: a layout is small, and this is not a file viewer. */
    private static final int MOST_A_LAYOUT_CAN_BE = 64 * 1024;

    /** The text of a share or a text-selection action, or null if the intent carries none. */
    private static CharSequence sharedText(Intent intent) {
        if (intent == null) {
            return null;
        }
        try {
            CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (text != null) {
                return text;
            }
            // The text-selection menu's own extra, which arrives on Android 6 and later.
            return intent.getCharSequenceExtra("android.intent.extra.PROCESS_TEXT");
        } catch (RuntimeException unreadable) {
            // Reading one extra unparcels them all (API 32 and below), and a Parcelable this app
            // does not have throws BadParcelableException. This activity runs in the keyboard's
            // process, so any app's share would take the keyboard down with it (RFC-0014 F2).
            return null;
        }
    }
}
