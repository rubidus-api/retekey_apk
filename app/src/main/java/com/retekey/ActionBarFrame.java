package com.retekey;

import android.content.Context;
import android.widget.LinearLayout;

/**
 * The keyboard with the action bar above it.
 *
 * <p>The bar adds its height to the window rather than taking it out of the keys, the way the
 * system band below does: turning the bar on must not shrink the typing area, since the height
 * setting is a share of the screen given to keys.
 */
final class ActionBarFrame extends LinearLayout {
    private final ActionBarView bar;
    private final ReteKeyboardView keyboard;

    ActionBarFrame(Context context, ActionBarView bar, ReteKeyboardView keyboard) {
        super(context);
        this.bar = bar;
        this.keyboard = keyboard;
        setOrientation(VERTICAL);
        setBackgroundColor(KeyboardPalette.resolve(context).background);
        addView(bar, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(keyboard, new LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    ActionBarView bar() {
        return bar;
    }

    ReteKeyboardView keyboard() {
        return keyboard;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
            // Docked: the frame is as tall as the bar plus whatever height the keyboard asks for.
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        // Floating: the panel's frame hands down an exact height, and the two children have to
        // divide it. The bar keeps its own height and the keyboard takes the rest, rather than the
        // keyboard asking for its usual share of the screen and being clipped by the bar.
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int bandForBar = Math.min(bar.barHeightPx(), height);
        bar.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(bandForBar, MeasureSpec.EXACTLY));
        keyboard.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height - bandForBar, MeasureSpec.EXACTLY));
        setMeasuredDimension(width, height);
    }
}
