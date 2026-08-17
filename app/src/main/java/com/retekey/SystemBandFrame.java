package com.retekey;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Wraps whatever the input view is and keeps the system's bottom button band clear of it.
 *
 * <p>An IME window is a dialog window, and one belonging to an app that targets a recent SDK is
 * drawn edge to edge — so it reaches the physical bottom of the screen, underneath the hide-keyboard
 * and switch-keyboard buttons the system draws there. Nothing pads it for us. This frame asks
 * {@link WindowInsetsWatcher} for the band and turns the answer into padding below the keyboard,
 * painted in the keyboard's own background so it reads as part of it rather than as a gap.
 *
 * <p>Where insets never arrive — an old platform, or a ROM that does not dispatch them to an IME
 * window — the band stays zero and the keyboard behaves exactly as it did before.
 */
final class SystemBandFrame extends FrameLayout {
    private int band;

    SystemBandFrame(Context context, View content) {
        super(context);
        setBackgroundColor(KeyboardPalette.resolve(context).background);
        addView(content, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        if (Build.VERSION.SDK_INT >= SystemBarInsets.ANY_INSETS_SDK) {
            setFitsSystemWindows(false);
            WindowInsetsWatcher.attach(this, this::setBand);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // A listener only fires when the insets change, and they may have settled before this view
        // existed. Ask once on the way in, where the platform can be asked at all.
        if (Build.VERSION.SDK_INT >= SystemBarInsets.ANY_INSETS_SDK) {
            int current = WindowInsetsWatcher.currentBand(this);
            if (current >= 0) {
                setBand(current);
            }
        }
    }

    /** The reserved band in pixels; zero when the system takes nothing at the bottom. */
    int band() {
        return band;
    }

    private void setBand(int px) {
        if (px == band) {
            return;
        }
        band = px;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Padding first, so the frame's own height comes out as the keyboard's plus the band and
        // the keyboard is measured against what is left. The height setting stays a share of the
        // screen given to *keys*: the band is added below them, not taken out of them.
        int applied = SystemBarInsets.clampToHeight(band, MeasureSpec.getSize(heightMeasureSpec));
        if (getPaddingBottom() != applied) {
            setPadding(0, 0, 0, applied);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
