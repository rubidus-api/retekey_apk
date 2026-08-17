package com.retekey;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

/**
 * The clipboard list, above the keyboard.
 *
 * <p>A tap pastes a clip; the buttons beside it pin it — pinned clips are never aged out — or
 * forget it. The list is the keyboard's own memory of what was cut and copied through it, not a
 * window onto the system clipboard, so nothing appears here that the user did not put there with a
 * key of ours.
 *
 * <p>Drawn from the keyboard's palette rather than an activity theme, since there is no activity:
 * this is a view inside an IME window.
 */
final class ClipboardPanelView extends LinearLayout {
    /** What the panel asks the service to do. */
    interface Listener {
        void onPaste(String text);

        void onPin(String text, boolean pinned);

        void onForget(String text);

        void onClearAll();

        void onClose();
    }

    private final KeyboardPalette palette;
    private final LinearLayout list;
    private Listener listener;

    ClipboardPanelView(Context context) {
        super(context);
        palette = KeyboardPalette.resolve(context);
        setOrientation(VERTICAL);
        setBackgroundColor(palette.background);

        addView(header(), new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        list = new LinearLayout(context);
        list.setOrientation(VERTICAL);
        ScrollView scroller = new ScrollView(context);
        scroller.addView(list, new LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(scroller, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    /** Fills the panel. Called again after every pin, forget or clear. */
    void show(List<ClipHistory.Clip> clips) {
        list.removeAllViews();
        if (clips.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("Nothing copied yet — Cut and Copy on the bar put clips here.");
            empty.setTextColor(palette.keyTextMuted);
            empty.setPadding(dp(12), dp(16), dp(12), dp(16));
            list.addView(empty);
            return;
        }
        for (ClipHistory.Clip clip : clips) {
            list.addView(row(clip), new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }
    }

    private View header() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(palette.keyFace);

        TextView title = new TextView(getContext());
        title.setText("Clipboard");
        title.setTextColor(palette.keyText);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setPadding(dp(12), dp(10), dp(8), dp(10));
        row.addView(title, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        row.addView(action("Clear", () -> {
            if (listener != null) {
                listener.onClearAll();
            }
        }));
        row.addView(action("✕", () -> {
            if (listener != null) {
                listener.onClose();
            }
        }));
        return row;
    }

    private View row(ClipHistory.Clip clip) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView text = new TextView(getContext());
        // One line of it: a clip can be a page long, and the list has to stay a list.
        text.setText(oneLine(clip.text));
        text.setTextColor(palette.keyText);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        text.setPadding(dp(12), dp(12), dp(8), dp(12));
        text.setMaxLines(2);
        text.setClickable(true);
        text.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPaste(clip.text);
            }
        });
        row.addView(text, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        row.addView(action(clip.pinned ? "★" : "☆", () -> {
            if (listener != null) {
                listener.onPin(clip.text, !clip.pinned);
            }
        }));
        row.addView(action("✕", () -> {
            if (listener != null) {
                listener.onForget(clip.text);
            }
        }));
        return row;
    }

    /** Runs when pressed. Not {@code Runnable}-typed API sugar: plain and API 14 safe. */
    private TextView action(String label, final Runnable onPress) {
        TextView view = new TextView(getContext());
        view.setText(label);
        view.setTextColor(palette.keyText);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(14), dp(12), dp(14), dp(12));
        view.setClickable(true);
        view.setOnClickListener(v -> onPress.run());
        return view;
    }

    private static String oneLine(String text) {
        String flat = text.replace('\n', ' ').replace('\r', ' ').trim();
        return flat.length() > 120 ? flat.substring(0, 120) + "…" : flat;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
