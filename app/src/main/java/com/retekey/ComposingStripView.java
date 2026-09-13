package com.retekey;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

/**
 * The strip that shows the syllable being built when the editor cannot show it itself — a
 * terminal. It is the IME's candidates view, which is the one surface Android puts on screen even
 * while the keyboard itself is hidden, so it works with a plugged-in keyboard too.
 */
public final class ComposingStripView extends TextView {
    private KeyboardPalette palette;

    public ComposingStripView(Context context) {
        super(context);
        palette = KeyboardPalette.resolve(context);
        setGravity(Gravity.CENTER_VERTICAL);
        setSingleLine(true);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        int pad = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 8f, context.getResources().getDisplayMetrics());
        setPadding(pad, pad / 2, pad, pad / 2);
        applyPalette();
    }

    /** Re-reads the keyboard's colours, for a theme change while the keyboard is up. */
    public void reloadPalette() {
        palette = KeyboardPalette.resolve(getContext());
        applyPalette();
    }

    /** Shows the syllable being built; an empty text leaves the strip blank. */
    public void showComposing(String text) {
        setText(text == null ? "" : text);
    }

    private void applyPalette() {
        setBackgroundColor(palette.background);
        setTextColor(palette.keyText);
    }
}
