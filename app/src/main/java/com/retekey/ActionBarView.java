package com.retekey;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.retekey.core.KeyRepeatSettings;

/**
 * The strip above or below the keys: the actions that are not letters, visible while you type.
 *
 * <p>Everything on it exists elsewhere already — the menu page has tiles, the pads have overlays —
 * and what none of those has is being reachable without hiding the text you are working on. The bar
 * costs the height of one short row and is never a mode.
 *
 * <p>Three kinds of slot sit side by side, and holding one does something different for each:
 * a built-in action does it once — except the keys, arrows to page keys, which repeat — text
 * repeats while the finger is down the way a held key does,
 * and a key combination latches — pressed and left down until the slot is pressed again, which is
 * the only way an on-screen key can be held at all.
 */
final class ActionBarView extends HorizontalScrollView {
    /** What the bar asks the service to do. */
    interface Listener {
        void onAction(BarAction action);

        void onText(String text);

        /** A chord pressed and released at once. */
        void onChord(BarSlot slot);

        /** A chord pressed and left down, or let up again. */
        void onChordLatch(BarSlot slot, boolean down);

        /**
         * A finger has come down on a slot that types ({@code true}) or lifted off it again. What
         * modifiers are down is read once, here, so a held arrow keeps its Ctrl for every repeat
         * and a one-shot toggle is spent by the press rather than by its first repeat.
         */
        void onPress(boolean down);
    }

    private static final int GAP_DP = 3;
    /** The height the bar's labels were sized for. */
    private static final int USUAL_HEIGHT_DP = 40;
    private static final int LABEL_SP = 15;
    private static final int EDGE_EXCLUSION_DP = 24;

    private final LinearLayout row;
    private final KeyboardPalette palette;
    /** No press shade when the keys are to stay still (issue #15). */
    private final boolean still;
    /** The bar's height as set for this orientation. */
    private final int heightPx;
    private final Handler handler = new Handler(Looper.getMainLooper());
    /** The chords held down right now. They stay down until pressed again. */
    private final Set<BarSlot> latched = new HashSet<>();
    private final Rect leftExclusion = new Rect();
    private final Rect rightExclusion = new Rect();
    private final List<Rect> exclusionRects = Arrays.asList(leftExclusion, rightExclusion);
    private Listener listener;
    private SlotTouch activeTouch;
    private int repeatDelayMs = KeyRepeatSettings.DEFAULT_DELAY_MS;
    private int repeatIntervalMs = KeyRepeatSettings.DEFAULT_INTERVAL_MS;

    ActionBarView(Context context) {
        super(context);
        palette = KeyboardPalette.resolve(context);
        still = ScreenTheme.still(context);
        android.content.SharedPreferences prefs =
            context.getSharedPreferences("retekey_view", Context.MODE_PRIVATE);
        ScreenOrientation orientation = OrientedPrefs.current(context);
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        int usualHeightPx = ActionBarHeightSettings.usualPx(
            OrientedPrefs.getInt(prefs, ActionBarHeightSettings.LEGACY_KEY_PERCENT, orientation,
                ActionBarHeightSettings.LEGACY_DEFAULT_PERCENT),
            metrics.density);
        int percent = OrientedPrefs.getInt(prefs, ActionBarHeightSettings.KEY_SCREEN_PERCENT,
            orientation, ActionBarHeightSettings.UNSET);
        heightPx = percent < 0
            ? usualHeightPx
            : ActionBarHeightSettings.heightPx(
                percent, KeyboardHeightPrefs.screenHeightPx(orientation, metrics));
        setFillViewport(true);
        setHorizontalScrollBarEnabled(false);
        setBackgroundColor(palette.background);
        row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        addView(row, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    /** The auto-repeat timings the keyboard itself uses, so a held slot feels like a held key. */
    void setRepeatTimings(int delayMs, int intervalMs) {
        repeatDelayMs = delayMs;
        repeatIntervalMs = intervalMs;
    }

    /** Fills the bar. Called again whenever the slot list changes. */
    void setSlots(List<BarSlot> slots) {
        releaseAllLatchedAndActive();
        row.removeAllViews();
        for (BarSlot slot : slots) {
            row.addView(button(slot), slotParams());
        }
    }

    /** Lets every latched chord up through the listener, while this bar is still on screen. */
    void releaseHeldKeys() {
        releaseAllLatchedAndActive();
    }

    private void releaseAllLatchedAndActive() {
        if (activeTouch != null) {
            activeTouch.cancelActive();
            activeTouch = null;
        }
        handler.removeCallbacksAndMessages(null);
        if (!latched.isEmpty()) {
            List<BarSlot> snapshot = new ArrayList<>(latched);
            latched.clear();
            if (listener != null) {
                for (BarSlot slot : snapshot) {
                    listener.onChordLatch(slot, false);
                }
            }
        }
    }

    /** The strip's height in pixels, so the frame around it can account for it. */
    int barHeightPx() {
        return heightPx;
    }

    private LinearLayout.LayoutParams slotParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        params.setMargins(dp(GAP_DP), dp(GAP_DP), 0, dp(GAP_DP));
        return params;
    }

    private View button(BarSlot slot) {
        TextView view = new TextView(getContext());
        view.setText(slot.label());
        view.setGravity(Gravity.CENTER);
        float usual = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, LABEL_SP, getResources().getDisplayMetrics());
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, ActionBarHeightSettings.labelPx(
            heightPx, dp(USUAL_HEIGHT_DP), usual, barHeightPx() - 2 * dp(GAP_DP)));
        view.setTextColor(palette.keyText);
        view.setBackgroundColor(palette.keyFace);
        int pad = dp(14);
        view.setPadding(pad, 0, pad, 0);
        view.setMinimumWidth(dp(44));
        view.setClickable(true);
        view.setFocusable(true);
        view.setOnTouchListener(new SlotTouch(slot, view));
        return view;
    }

    /**
     * A slot's press, hold and release.
     *
     * <p>Written by hand rather than as a click listener, because "what a hold does" is the whole
     * point of two of the three kinds and a click listener cannot see a hold at all.
     */
    private final class SlotTouch implements View.OnTouchListener {
        private final BarSlot slot;
        private final TextView view;
        private final Runnable repeat = new Runnable() {
            @Override
            public void run() {
                repeated = true;
                ensurePressStarted();
                if (listener != null) {
                    if (slot.kind() == BarSlot.Kind.TEXT) {
                        listener.onText(slot.text());
                    } else {
                        listener.onAction(slot.action());
                    }
                }
                handler.postDelayed(this, Math.max(20, repeatIntervalMs));
            }
        };
        private final Runnable latch = new Runnable() {
            @Override
            public void run() {
                held = true;
                setLatched(true);
            }
        };
        private boolean held;
        private boolean inside;
        private boolean pressStarted;
        /** Set once a hold has repeated: the lift then types nothing more, as with a real key. */
        private boolean repeated;

        SlotTouch(BarSlot slot, TextView view) {
            this.slot = slot;
            this.view = view;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    press();
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    boolean nowInside = isInside(v, event);
                    if (nowInside != inside) {
                        inside = nowInside;
                        paint(inside);
                        if (!inside) {
                            handler.removeCallbacks(repeat);
                            handler.removeCallbacks(latch);
                        }
                    }
                    return true;
                }
                case MotionEvent.ACTION_UP:
                    boolean commit = inside && isInside(v, event);
                    release(commit);
                    if (commit) {
                        v.performClick();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    release(false);
                    return true;
                default:
                    return true;
            }
        }

        private static boolean isInside(View v, MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            return x >= 0.0f && x <= v.getWidth() && y >= 0.0f && y <= v.getHeight();
        }

        /** Whether this slot types something a hold should repeat. */
        private boolean repeats() {
            return slot.kind() == BarSlot.Kind.TEXT
                || (slot.kind() == BarSlot.Kind.BUILT_IN && slot.action() != null
                    && slot.action().rawKey() != null);
        }

        private void ensurePressStarted() {
            if (repeats() && !pressStarted && listener != null) {
                pressStarted = true;
                activeTouch = this;
                listener.onPress(true);
            }
        }

        private void press() {
            if (activeTouch != null && activeTouch != this) {
                activeTouch.cancelActive();
            }
            activeTouch = this;
            held = false;
            repeated = false;
            pressStarted = false;
            inside = true;
            paint(true);
            if (repeats()) {
                handler.postDelayed(repeat, Math.max(50, repeatDelayMs));
            } else if (slot.canLatch() && !latched.contains(slot)) {
                handler.postDelayed(latch, Math.max(50, repeatDelayMs));
            }
        }

        private void release(boolean insideThePress) {
            handler.removeCallbacks(repeat);
            handler.removeCallbacks(latch);
            inside = false;
            paint(false);
            try {
                deliver(insideThePress);
            } finally {
                if (pressStarted && listener != null) {
                    pressStarted = false;
                    listener.onPress(false);
                }
                if (activeTouch == this) {
                    activeTouch = null;
                }
            }
        }

        void cancelActive() {
            handler.removeCallbacks(repeat);
            handler.removeCallbacks(latch);
            inside = false;
            paint(false);
            if (pressStarted && listener != null) {
                pressStarted = false;
                listener.onPress(false);
            }
        }

        private void deliver(boolean insideThePress) {
            if (!insideThePress || listener == null) {
                return;
            }
            if (held || repeated) {
                // The hold already did what it does; the lift only ends it.
                return;
            }
            switch (slot.kind()) {
                case BUILT_IN:
                    if (slot.canLatch() && latched.contains(slot)) {
                        // A locked modifier: this press lets it up. Going through onAction would
                        // clear the latch the keyboard holds and leave this view still believing
                        // the slot was down — painted locked, and never latchable again.
                        setLatched(false);
                        break;
                    }
                    ensurePressStarted();
                    listener.onAction(slot.action());
                    break;
                case TEXT:
                    ensurePressStarted();
                    listener.onText(slot.text());
                    break;
                default:
                    if (latched.contains(slot)) {
                        setLatched(false);
                    } else {
                        listener.onChord(slot);
                    }
                    break;
            }
        }

        private void setLatched(boolean down) {
            if (down) {
                latched.add(slot);
            } else {
                latched.remove(slot);
            }
            paint(false);
            if (listener != null) {
                listener.onChordLatch(slot, down);
            }
        }

        /** A pressed slot is tinted; a latched one keeps the accent until it is let up. */
        private void paint(boolean pressed) {
            if (latched.contains(slot)) {
                view.setBackgroundColor(palette.keyAccent);
                view.setTextColor(palette.keyLatchedInk());
                return;
            }
            view.setTextColor(palette.keyText);
            view.setBackgroundColor(pressed && !still
                ? KeyPressTint.pressed(palette.keyFace, palette.pressTint)
                : palette.keyFace);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            updateGestureExclusion();
        }
    }

    @TargetApi(29)
    private void updateGestureExclusion() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        int edge = Math.min(dp(EDGE_EXCLUSION_DP), width / 4);
        leftExclusion.set(0, 0, edge, height);
        rightExclusion.set(width - edge, 0, width, height);
        setSystemGestureExclusionRects(exclusionRects);
    }

    @Override
    protected void onDetachedFromWindow() {
        releaseAllLatchedAndActive();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // A frame that has worked out the bar's band says so exactly, and is believed: it may be
        // less than the bar asks for, where the keys would otherwise be left with nothing.
        int height = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY
            ? Math.min(barHeightPx(), MeasureSpec.getSize(heightMeasureSpec))
            : barHeightPx();
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
