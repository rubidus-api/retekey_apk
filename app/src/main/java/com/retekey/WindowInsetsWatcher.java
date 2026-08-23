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
        return realScreenSize(view)[1];
    }

    // ---- activities: issue #2 ----

    /**
     * Fits an activity's {@code content} under the furniture. See {@link ScreenFit}: the frame is
     * measured on screen against the status bar, the action bar, the navigation bar and the keyboard,
     * and the content is padded by however much each one overlaps it — zero where the framework has
     * already made room. Re-measured whenever insets change and after every layout, since the frame
     * has no position until it is laid out and the action bar may appear after it.
     */
    static void fitScreen(final View frame, final View content) {
        final int[] base = {
            content.getPaddingLeft(), content.getPaddingTop(),
            content.getPaddingRight(), content.getPaddingBottom()
        };
        frame.setOnApplyWindowInsetsListener((v, insets) -> {
            refit(frame, content, base, insets);
            return v.onApplyWindowInsets(insets);
        });
        frame.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            WindowInsets insets = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? frame.getRootWindowInsets() : null;
            refit(frame, content, base, insets);
        });
        frame.requestApplyInsets();
    }

    private static void refit(View frame, View content, int[] base, WindowInsets insets) {
        if (frame.getWindowToken() == null || frame.getHeight() <= 0 || frame.getWidth() <= 0) {
            return; // Not on screen yet; the layout listener will be back.
        }
        int[] screen = realScreenSize(frame);
        if (screen[1] <= 0) {
            return;
        }
        int[] location = new int[2];
        frame.getLocationOnScreen(location);
        int frameLeft = location[0];
        int frameTop = location[1];
        int frameRight = frameLeft + frame.getWidth();
        int frameBottom = frameTop + frame.getHeight();

        int statusBottom = 0;
        int bottomFurniture = 0;
        int leftFurniture = 0;
        int rightFurniture = 0;
        if (insets != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                statusBottom = bars.top;
                bottomFurniture = Math.max(bars.bottom,
                    insets.getInsets(WindowInsets.Type.ime()).bottom);
                leftFurniture = bars.left;
                rightFurniture = bars.right;
            } else {
                statusBottom = insets.getSystemWindowInsetTop();
                bottomFurniture = insets.getSystemWindowInsetBottom();
                leftFurniture = insets.getSystemWindowInsetLeft();
                rightFurniture = insets.getSystemWindowInsetRight();
            }
        }
        int topFurnitureBottom = Math.max(statusBottom, actionBarBottomOnScreen(frame, statusBottom));

        int left = base[0] + ScreenFit.leftOverlap(leftFurniture, frameLeft);
        int top = base[1] + ScreenFit.topOverlap(topFurnitureBottom, frameTop);
        int right = base[2] + ScreenFit.rightOverlap(rightFurniture, screen[0], frameRight);
        int bottom = base[3] + ScreenFit.bottomOverlap(bottomFurniture, screen[1], frameBottom);
        if (left != content.getPaddingLeft() || top != content.getPaddingTop()
                || right != content.getPaddingRight() || bottom != content.getPaddingBottom()) {
            content.setPadding(left, top, right, bottom);
        }
    }

    /**
     * Where the activity's own action bar ends, in screen pixels, or 0 when it has none showing.
     * Measured from the bar's container view when the decor exposes one (it does on every ROM seen,
     * Samsung's included); otherwise taken as the bar's height under the status bar.
     */
    private static int actionBarBottomOnScreen(View frame, int statusBottom) {
        View bar = actionBarContainer(frame);
        if (bar != null && bar.getVisibility() == View.VISIBLE && bar.getHeight() > 0) {
            int[] location = new int[2];
            bar.getLocationOnScreen(location);
            return location[1] + bar.getHeight();
        }
        android.content.Context context = frame.getContext();
        if (context instanceof android.app.Activity) {
            android.app.ActionBar actionBar = ((android.app.Activity) context).getActionBar();
            if (actionBar != null && actionBar.isShowing()) {
                return statusBottom + actionBar.getHeight();
            }
        }
        return 0;
    }

    private static View actionBarContainer(View frame) {
        int id = frame.getResources().getIdentifier("action_bar_container", "id", "android");
        return id == 0 ? null : frame.getRootView().findViewById(id);
    }

    /** The display's real width and height, system furniture included — not the app area's. */
    private static int[] realScreenSize(View view) {
        try {
            android.view.WindowManager windows = (android.view.WindowManager)
                view.getContext().getSystemService(android.content.Context.WINDOW_SERVICE);
            if (windows == null) {
                return new int[] {0, 0};
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Rect bounds = windows.getMaximumWindowMetrics().getBounds();
                return new int[] {bounds.width(), bounds.height()};
            }
            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            windows.getDefaultDisplay().getRealMetrics(metrics);
            return new int[] {metrics.widthPixels, metrics.heightPixels};
        } catch (RuntimeException unavailable) {
            return new int[] {0, 0};
        }
    }
}
