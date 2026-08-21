package com.retekey;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The strip above the keys: the actions that are not letters, visible while you type.
 *
 * <p>Everything on it exists elsewhere already — the menu page has tiles, the pads have overlays —
 * and what none of those has is being reachable without hiding the text you are working on. The bar
 * costs the height of one short row and is never a mode.
 *
 * <p>Three kinds of slot sit side by side, and holding one does something different for each:
 * a built-in action does it once, text repeats while the finger is down the way a held key does,
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
    }

    /** How tall the strip is. Shorter than a key row: it is a shelf, not another row of keys. */
    private static final int HEIGHT_DP = 40;
    private static final int GAP_DP = 3;
    private static final int LABEL_SP = 15;

    private final LinearLayout row;
    private final KeyboardPalette palette;
    private final Handler handler = new Handler(Looper.getMainLooper());
    /** The chords held down right now. They stay down until pressed again. */
    private final Set<BarSlot> latched = new HashSet<>();
    private Listener listener;
    private int repeatDelayMs = KeyRepeatSettings.DEFAULT_DELAY_MS;
    private int repeatIntervalMs = KeyRepeatSettings.DEFAULT_INTERVAL_MS;

    ActionBarView(Context context) {
        super(context);
        palette = KeyboardPalette.resolve(context);
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
        row.removeAllViews();
        latched.clear();
        for (BarSlot slot : slots) {
            row.addView(button(slot), slotParams());
        }
    }

    /** The strip's height in pixels, so the frame around it can account for it. */
    int barHeightPx() {
        return dp(HEIGHT_DP);
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
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, LABEL_SP);
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
                if (listener != null) {
                    listener.onText(slot.text());
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
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    release(event.getActionMasked() == MotionEvent.ACTION_UP);
                    return true;
                default:
                    return true;
            }
        }

        private void press() {
            held = false;
            paint(true);
            if (slot.kind() == BarSlot.Kind.TEXT) {
                handler.postDelayed(repeat, Math.max(50, repeatDelayMs));
            } else if (slot.canLatch() && !latched.contains(slot)) {
                handler.postDelayed(latch, Math.max(50, repeatDelayMs));
            }
        }

        private void release(boolean insideThePress) {
            handler.removeCallbacks(repeat);
            handler.removeCallbacks(latch);
            paint(false);
            if (!insideThePress || listener == null) {
                return;
            }
            if (held) {
                // The hold already did what it does; the lift only ends it.
                return;
            }
            switch (slot.kind()) {
                case BUILT_IN:
                    listener.onAction(slot.action());
                    break;
                case TEXT:
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
            view.setBackgroundColor(pressed
                ? KeyPressTint.pressed(palette.keyFace, palette.pressTint)
                : palette.keyFace);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(barHeightPx(), MeasureSpec.EXACTLY));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
