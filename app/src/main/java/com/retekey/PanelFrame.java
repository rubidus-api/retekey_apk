package com.retekey;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

/**
 * A panel above the keyboard, filling the window.
 *
 * <p>The same shape as {@link NotepadFrame} — the keyboard keeps its own height at the bottom and
 * the panel takes the rest — for anything that is a list rather than a way of typing: the clipboard
 * today, whatever stage 3 of the action bar brings later.
 */
final class PanelFrame extends LinearLayout implements BottomReserving {
    private final View panel;
    private final ReteKeyboardView keyboard;
    private int bottomReserved;

    @Override
    public void setBottomReserved(int px) {
        if (px == bottomReserved) {
            return;
        }
        bottomReserved = px;
        requestLayout();
    }

    PanelFrame(Context context, View panel, ReteKeyboardView keyboard) {
        super(context);
        this.panel = panel;
        this.keyboard = keyboard;
        setOrientation(VERTICAL);
        // No background: the bar and the keyboard each paint their own, and a colour here would
        // sit behind a floating panel and cancel the translucency it exists for.
        addView(panel, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        addView(keyboard, new LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    View panel() {
        return panel;
    }

    ReteKeyboardView keyboard() {
        return keyboard;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // The window is the screen: the panel takes whatever the keyboard does not.
        int screenHeight = getResources().getDisplayMetrics().heightPixels - bottomReserved;
        super.onMeasure(widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(Math.max(0, screenHeight), MeasureSpec.EXACTLY));
    }
}
