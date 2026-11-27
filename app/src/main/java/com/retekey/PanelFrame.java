package com.retekey;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

/**
 * A panel above the keyboard. The bottom block is the keyboard as it is docked — action bar
 * included, when the user has one: opening a panel must not take it away.
 *
 * <p>Full-height panels (the notepad, the clipboard) take the available screen above the keyboard;
 * compact panels (docked Hanja or IPA candidates) wrap their own content height up to that bound.
 */
final class PanelFrame extends LinearLayout implements BottomReserving {
    private final View panel;
    private final View keyboard;
    private final boolean compact;
    private int bottomReserved;

    @Override
    public void setBottomReserved(int px) {
        if (px == bottomReserved) {
            return;
        }
        bottomReserved = px;
        requestLayout();
    }

    PanelFrame(Context context, View panel, View keyboard) {
        this(context, panel, keyboard, false);
    }

    PanelFrame(Context context, View panel, View keyboard, boolean compact) {
        super(context);
        this.panel = panel;
        this.keyboard = keyboard;
        this.compact = compact;
        setOrientation(VERTICAL);
        // No background: the bar and the keyboard each paint their own, and a colour here would
        // sit behind a floating panel and cancel the translucency it exists for.
        addView(panel, compact
            ? new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            : new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        addView(keyboard, new LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    View panel() {
        return panel;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // The window is what the screen leaves an input method: the band the system draws at the
        // top is not ours to write in, and asking for it put the top of the panel underneath the
        // status bar and made some apps drop the keyboard the moment it opened (issue #8).
        int maxHeight = PanelHeight.forPanel(getResources().getDisplayMetrics().heightPixels,
            PanelHeight.topInset(this), bottomReserved);
        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            maxHeight = PanelHeight.clampToSpec(maxHeight, MeasureSpec.getSize(heightMeasureSpec));
        }
        if (!compact) {
            super.onMeasure(widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY));
            return;
        }
        keyboard.measure(widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST));
        int kbHeight = keyboard.getMeasuredHeight();
        int availForPanel = Math.max(0, maxHeight - kbHeight);
        panel.measure(widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(availForPanel, MeasureSpec.AT_MOST));
        int total = Math.min(maxHeight, kbHeight + panel.getMeasuredHeight());
        super.onMeasure(widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(total, MeasureSpec.EXACTLY));
    }
}
