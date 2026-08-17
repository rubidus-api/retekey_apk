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
}
