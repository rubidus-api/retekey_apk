package com.retekey;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
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
    /** Whether the band has been worked out with this window's own geometry, not insets alone. */
    private boolean measuredWithGeometry;
    private final Paint bandPaint = new Paint();

    SystemBandFrame(Context context, View content) {
        super(context);
        // No background of its own. This frame wraps *everything* the IME shows, and a colour here
        // would sit behind the floating keyboard and the Hanja and code-point panels — which are
        // translucent on purpose, so that the app underneath stays readable. Painting the whole
        // window opaque took that away; only the reserved band is painted now, and only when there
        // is one.
        bandPaint.setColor(KeyboardPalette.resolve(context).background);
        setWillNotDraw(false);
        addView(content, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        tellTheChildWhatIsNotItsToFill();
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
        refreshBand();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            measuredWithGeometry = false;
            // The answer is only true of the moment it was taken. An input view built while the
            // keyboard was off screen — which is what happens when a setting is changed from the
            // settings screen — reads the insets of whatever *was* on screen, and on a phone whose
            // navigation bar is visible in an app but not under a keyboard, that is a band this
            // keyboard does not need. Ask again now that this window is the one being shown.
            refreshBand();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!measuredWithGeometry && getHeight() > 0) {
            // The first answer is taken before this window has a size, so it cannot include how far
            // the window reaches into the navigation bar — the question that decides whether there
            // is anything to reserve at all. Ask once more now that there is a geometry to measure.
            measuredWithGeometry = true;
            post(this::refreshBand);
        }
    }

    /** Re-reads the insets and applies them, where the platform has any to read. */
    void refreshBand() {
        if (Build.VERSION.SDK_INT < SystemBarInsets.ANY_INSETS_SDK) {
            return;
        }
        int current = WindowInsetsWatcher.currentBand(this);
        if (current >= 0) {
            setBand(current);
        }
        // API 20-22 have no getRootWindowInsets; asking for a dispatch is the only way to be told.
        requestApplyInsets();
    }

    /** The reserved band in pixels; zero when the system takes nothing at the bottom. */
    int band() {
        return band;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int reserved = getPaddingBottom();
        if (reserved <= 0 || isFloating()) {
            return;
        }
        // The strip the system's buttons sit over, in the keyboard's own colour so it reads as part
        // of the keyboard rather than as a gap under it.
        canvas.drawRect(0, getHeight() - reserved, getWidth(), getHeight(), bandPaint);
    }

    /**
     * Whether what is inside is a floating panel. A floating keyboard covers the app rather than
     * pushing it, and painting anything behind it — even a strip — would show through the panel's
     * own translucency as a block of colour.
     */
    private boolean isFloating() {
        return getChildCount() > 0 && getChildAt(0) instanceof FloatingKeyboardFrame;
    }

    private void setBand(int px) {
        if (px == band) {
            return;
        }
        band = px;
        tellTheChildWhatIsNotItsToFill();
        requestLayout();
    }

    /**
     * A child that measures itself to the whole screen has to know how much of the screen this
     * frame has already given away, or it comes out a band too tall and loses its bottom row off
     * the edge of the window.
     */
    private void tellTheChildWhatIsNotItsToFill() {
        if (getChildCount() > 0 && getChildAt(0) instanceof BottomReserving) {
            ((BottomReserving) getChildAt(0)).setBottomReserved(band);
        }
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
