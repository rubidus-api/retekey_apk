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
    private int[] visibleWidths = new int[0];
    private int[] visibleChildIdx = new int[0];
    private int[] childLines = new int[0];

    public FlowRow(Context context) {
        super(context);
    }

    private void ensureScratchCapacity(int count) {
        if (visibleWidths.length < count) {
            visibleWidths = new int[count];
            visibleChildIdx = new int[count];
            childLines = new int[count];
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int totalWidth = MeasureSpec.getSize(widthMeasureSpec);
        int padH = getPaddingLeft() + getPaddingRight();
        int padV = getPaddingTop() + getPaddingBottom();
        int available = Math.max(0, totalWidth - padH);
        int count = getChildCount();
        ensureScratchCapacity(count);
        int visibleCount = 0;
        int lineHeight = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            child.measure(
                MeasureSpec.makeMeasureSpec(available, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            visibleWidths[visibleCount] = child.getMeasuredWidth();
            visibleChildIdx[visibleCount] = i;
            visibleCount++;
            lineHeight = Math.max(lineHeight, child.getMeasuredHeight());
        }
        int lines = FlowRowMath.linesInto(visibleWidths, visibleCount, available, childLines);
        setMeasuredDimension(totalWidth, padV + lines * lineHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int padLeft = getPaddingLeft();
        int padTop = getPaddingTop();
        int available = Math.max(0, (right - left) - padLeft - getPaddingRight());
        int count = getChildCount();
        ensureScratchCapacity(count);
        int visibleCount = 0;
        int lineHeight = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            visibleWidths[visibleCount] = child.getMeasuredWidth();
            visibleChildIdx[visibleCount] = i;
            visibleCount++;
            lineHeight = Math.max(lineHeight, child.getMeasuredHeight());
        }
        FlowRowMath.linesInto(visibleWidths, visibleCount, available, childLines);
        int x = padLeft;
        int line = 0;
        for (int idx = 0; idx < visibleCount; idx++) {
            View child = getChildAt(visibleChildIdx[idx]);
            if (childLines[idx] != line) {
                line = childLines[idx];
                x = padLeft;
            }
            int y = padTop + line * lineHeight;
            child.layout(x, y, x + visibleWidths[idx], y + child.getMeasuredHeight());
            x += visibleWidths[idx];
        }
    }
}
