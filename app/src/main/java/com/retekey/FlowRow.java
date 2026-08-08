package com.retekey;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * A row of children that becomes several rows when it runs out of width.
 *
 * <p>The notepad's links want to be one line and must not disappear off the edge when they are too
 * many for the screen — which a {@code LinearLayout} cannot do and a wrapping row can. Where the
 * breaks fall is {@link FlowRowMath}'s decision; this class only measures children and places them.
 */
public final class FlowRow extends ViewGroup {
    public FlowRow(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int available = MeasureSpec.getSize(widthMeasureSpec);
        int count = getChildCount();
        int[] widths = new int[count];
        int lineHeight = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            child.measure(
                MeasureSpec.makeMeasureSpec(available, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            widths[i] = child.getMeasuredWidth();
            lineHeight = Math.max(lineHeight, child.getMeasuredHeight());
        }
        int lines = FlowRowMath.lineCount(widths, available);
        setMeasuredDimension(available, lines * lineHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int available = right - left;
        int count = getChildCount();
        int[] widths = new int[count];
        int lineHeight = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            widths[i] = child.getMeasuredWidth();
            lineHeight = Math.max(lineHeight, child.getMeasuredHeight());
        }
        int[] lines = FlowRowMath.lines(widths, available);
        int x = 0;
        int line = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            if (lines[i] != line) {
                line = lines[i];
                x = 0;
            }
            int y = line * lineHeight;
            child.layout(x, y, x + widths[i], y + child.getMeasuredHeight());
            x += widths[i];
        }
    }
}
