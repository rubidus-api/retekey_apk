package com.retekey;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

/**
 * The keyboard with the notepad above it, filling the screen. The bottom block is the keyboard as
 * it is docked — action bar included, when the user has one.
 *
 * <p>An IME window is normally exactly as tall as its keyboard. While the notepad is open the
 * window takes the whole screen instead, and everything above the keys is the notepad — translucent,
 * so the app being copied from is still readable behind it. The keyboard keeps its own height at
 * the bottom, unchanged, because the notepad is a place to put what you type rather than a
 * different way of typing it.
 */
final class NotepadFrame extends LinearLayout implements BottomReserving {
    private final NotepadView notepad;
    private final View keyboard;
    private int bottomReserved;

    @Override
    public void setBottomReserved(int px) {
        if (px == bottomReserved) {
            return;
        }
        bottomReserved = px;
        requestLayout();
    }

    NotepadFrame(Context context, NotepadView notepad, View keyboard) {
        super(context);
        this.notepad = notepad;
        this.keyboard = keyboard;
        setOrientation(VERTICAL);
        addView(notepad, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        addView(keyboard, new LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    NotepadView notepad() {
        return notepad;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // The window is the screen: the notepad takes whatever the keyboard does not.
        int screenHeight = getResources().getDisplayMetrics().heightPixels - bottomReserved;
        super.onMeasure(widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(Math.max(0, screenHeight), MeasureSpec.EXACTLY));
    }

    /** Whether a touch at this point is on the notepad's own area rather than the keyboard's. */
    boolean isOverNotepad(float y) {
        View panel = notepad;
        return y >= panel.getTop() && y < panel.getBottom();
    }
}
