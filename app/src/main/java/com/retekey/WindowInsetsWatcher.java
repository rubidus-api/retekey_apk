package com.retekey;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

/**
 * The one class that touches {@link WindowInsets}, so nothing else has to.
 *
 * <p>Insets are API 20, and this app still runs on API 14 — where the class does not exist and a
 * method mentioning it is a method the runtime cannot resolve. Keeping every reference behind one
 * door means the keyboard's own views stay loadable on the oldest devices, and this one is only ever
 * entered after a version check.
 *
 * <p>It reports the bottom band the system takes taps in, computed by {@link SystemBarInsets}: once
 * when the view is attached, since insets may have settled before the view existed, and again
 * whenever they change.
 */
@TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
final class WindowInsetsWatcher {
    /** Told the bottom band in pixels. Not {@code IntConsumer}, which is API 24. */
    interface BandListener {
        void onBand(int px);
    }

    private WindowInsetsWatcher() {
    }

    /** Starts watching {@code view}. Call only when the platform has insets at all. */
    static void attach(View view, BandListener listener) {
        view.setOnApplyWindowInsetsListener((v, insets) -> {
            listener.onBand(bandOf(insets));
            // Not consumed: other views in the window are entitled to the same answer.
            return v.onApplyWindowInsets(insets);
        });
    }

    /** The band from the insets the view already has, or -1 when they cannot be asked for. */
    static int currentBand(View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return -1;
        }
        WindowInsets insets = view.getRootWindowInsets();
        return insets == null ? -1 : bandOf(insets);
    }

    private static int bandOf(WindowInsets insets) {
        int tappable = Build.VERSION.SDK_INT >= SystemBarInsets.TAPPABLE_INSETS_SDK
            ? insets.getTappableElementInsets().bottom
            : 0;
        return SystemBarInsets.bandPx(
            Build.VERSION.SDK_INT, tappable, insets.getSystemWindowInsetBottom());
    }
}
