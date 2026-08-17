package com.retekey;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

/**
 * The strip above the keys: the actions that are not letters, visible while you type.
 *
 * <p>Everything on it exists elsewhere already — the menu page has tiles, the pads have overlays —
 * and what none of those has is being reachable without hiding the text you are working on. The bar
 * costs the height of one short row and is never a mode.
 *
 * <p>It scrolls sideways when the slot list is longer than the screen, so a bar can be as long as
 * its owner wants without shrinking its buttons to nothing.
 */
final class ActionBarView extends HorizontalScrollView {
    /** Told which action was pressed. Not {@code Consumer}, which is API 24. */
    interface Listener {
        void onAction(BarAction action);
    }

    /** How tall the strip is. Shorter than a key row: it is a shelf, not another row of keys. */
    private static final int HEIGHT_DP = 40;
    private static final int GAP_DP = 3;
    private static final int LABEL_SP = 15;

    private final LinearLayout row;
    private final KeyboardPalette palette;
    private Listener listener;

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

    /** Fills the bar. Called again whenever the slot list changes. */
    void setSlots(List<BarAction> slots) {
        row.removeAllViews();
        for (BarAction action : slots) {
            row.addView(button(action), slotParams());
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

    private View button(BarAction action) {
        TextView view = new TextView(getContext());
        view.setText(action.label());
        view.setGravity(Gravity.CENTER);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, LABEL_SP);
        view.setTextColor(palette.keyText);
        view.setBackgroundColor(palette.keyFace);
        int pad = dp(14);
        view.setPadding(pad, 0, pad, 0);
        view.setMinimumWidth(dp(44));
        view.setClickable(true);
        view.setFocusable(true);
        view.setOnClickListener(v -> {
            // The press is drawn by hand: the bar takes its colours from the keyboard's palette
            // rather than from the activity theme, which has no say inside an IME window.
            v.setBackgroundColor(KeyPressTint.pressed(palette.keyFace, palette.pressTint));
            v.postDelayed(() -> v.setBackgroundColor(palette.keyFace), 90);
            if (listener != null) {
                listener.onAction(action);
            }
        });
        return view;
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
