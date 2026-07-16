package dev.hellgates.retekeyime;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

/**
 * The Hanja candidate strip shown in the IME candidates area when the 한자 key converts a reading.
 * A horizontal, scrollable row: a leading reading label, then one tappable button per candidate.
 * It shows in the candidates area, so it appears with the soft keyboard and with a hardware
 * keyboard (when the on-screen keyboard is hidden) alike.
 */
public final class HanjaCandidatesView extends HorizontalScrollView {
    /** Notified with the chosen Hanja text when a candidate is tapped. */
    public interface OnPick {
        void pick(String hanja);
    }

    private final LinearLayout row;
    private OnPick listener;

    public HanjaCandidatesView(Context context) {
        super(context);
        setFillViewport(true);
        setHorizontalScrollBarEnabled(false);
        setBackgroundColor(Color.rgb(28, 30, 36));
        row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        addView(row, new LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    public void setOnPick(OnPick listener) {
        this.listener = listener;
    }

    /** Replaces the strip with the candidates for {@code reading}, scrolled back to the start. */
    public void show(String reading, List<String> candidates) {
        row.removeAllViews();

        TextView label = new TextView(getContext());
        label.setText(reading + " ▸");
        label.setTextColor(Color.rgb(150, 160, 172));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        label.setPadding(dp(12), dp(8), dp(8), dp(8));
        label.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(label);

        for (String candidate : candidates) {
            Button button = new Button(getContext());
            button.setText(candidate);
            button.setAllCaps(false);
            button.setTextColor(Color.rgb(233, 237, 243));
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            button.setBackgroundColor(Color.rgb(40, 44, 52));
            button.setMinWidth(dp(48));
            button.setPadding(dp(10), dp(4), dp(10), dp(4));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(dp(3), dp(4), dp(3), dp(4));
            final String chosen = candidate;
            button.setOnClickListener(v -> {
                if (listener != null) {
                    listener.pick(chosen);
                }
            });
            row.addView(button, params);
        }
        scrollTo(0, 0);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
