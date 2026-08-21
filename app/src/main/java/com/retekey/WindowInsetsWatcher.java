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
            listener.onBand(bandOf(v, insets));
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
        return insets == null ? -1 : bandOf(view, insets);
    }

    private static int bandOf(View view, WindowInsets insets) {
        int tappable = 0;
        int navigation = 0;
        boolean navigationVisible = true;
        if (Build.VERSION.SDK_INT >= SystemBarInsets.TAPPABLE_INSETS_SDK) {
            tappable = insets.getTappableElementInsets().bottom;
            navigation = insets.getSystemWindowInsetBottom();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            navigation = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
            navigationVisible = insets.isVisible(WindowInsets.Type.navigationBars());
        }
        int lift = liftAboveScreenBottom(view);
        int band = SystemBarInsets.bandPx(
            Build.VERSION.SDK_INT,
            tappable,
            navigation,
            navigationVisible,
            insets.getSystemWindowInsetBottom(),
            lift,
            SystemBandSettings.mode(view.getContext()));
        // What the phone actually reported, so the settings screen can show it rather than leaving
        // the user to guess why their keyboard has a gap under it — or has none.
        SystemBandSettings.remember(
            view.getContext(), tappable, navigation, navigationVisible, lift, band);
        return band;
    }

    /**
     * How far this window's bottom edge sits above the physical bottom of the screen, in pixels.
     *
     * <p>The insets describe the screen; this describes us. A window the framework has already
     * lifted above the navigation bar has a lift equal to the bar's height and needs little or
     * nothing reserved; a window drawn to the very bottom has a lift of zero and needs the whole
     * furniture cleared. The issue #1 reporter's phones were the second kind, the owner's the first,
     * and that single number is the difference between them.
     *
     * @return the lift, or {@link SystemBarInsets#LIFT_UNKNOWN} before the view has a size
     */
    private static int liftAboveScreenBottom(View view) {
        if (view.getHeight() <= 0 || view.getWindowToken() == null) {
            return SystemBarInsets.LIFT_UNKNOWN;
        }
        int screenHeight = realScreenHeight(view);
        if (screenHeight <= 0) {
            return SystemBarInsets.LIFT_UNKNOWN;
        }
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int windowBottom = location[1] + view.getHeight();
        return Math.max(0, screenHeight - windowBottom);
    }

    /** The display's real height, system furniture included — not the app area's. */
    private static int realScreenHeight(View view) {
        try {
            android.view.WindowManager windows = (android.view.WindowManager)
                view.getContext().getSystemService(android.content.Context.WINDOW_SERVICE);
            if (windows == null) {
                return 0;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return windows.getMaximumWindowMetrics().getBounds().height();
            }
            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            windows.getDefaultDisplay().getRealMetrics(metrics);
            return metrics.heightPixels;
        } catch (RuntimeException unavailable) {
            return 0;
        }
    }
}
