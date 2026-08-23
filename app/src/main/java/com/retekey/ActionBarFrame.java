package com.retekey;

import android.content.Context;
import android.widget.LinearLayout;

/**
 * The keyboard with the action bar above it.
 *
 * <p>The bar is part of the keyboard's height, not added on top of it: the height setting is the
 * share of the screen the whole keyboard takes, bar included, so turning the bar on does not push
 * the keyboard up into the app — the keys give up one short row to it instead. (The system band
 * below is the opposite kind of thing: it is the system's, and is added.) The user asked for it
 * this way after the bar had first been an addition.
 */
final class ActionBarFrame extends LinearLayout {
    private final ActionBarView bar;
    private final ReteKeyboardView keyboard;

    ActionBarFrame(Context context, ActionBarView bar, ReteKeyboardView keyboard) {
        super(context);
        this.bar = bar;
        this.keyboard = keyboard;
        setOrientation(VERTICAL);
        // No background: the bar and the keyboard each paint their own, and a colour here would
        // sit behind a floating panel and cancel the translucency it exists for.
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
        int width = MeasureSpec.getSize(widthMeasureSpec);
        // Docked, the frame is exactly as tall as the keyboard would be on its own; floating, the
        // panel's frame hands down an exact height. Either way the two children divide it: the bar
        // keeps its own height and the keyboard takes the rest.
        int height;
        switch (MeasureSpec.getMode(heightMeasureSpec)) {
            case MeasureSpec.EXACTLY:
                height = MeasureSpec.getSize(heightMeasureSpec);
                break;
            case MeasureSpec.AT_MOST:
                height = Math.min(keyboard.desiredHeightPx(), MeasureSpec.getSize(heightMeasureSpec));
                break;
            default:
                height = keyboard.desiredHeightPx();
                break;
        }
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
