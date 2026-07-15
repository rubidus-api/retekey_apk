package dev.hellgates.retekeyime;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * ReteKey's settings screen. Reachable from the app launcher, from the ☰ menu key on the keyboard,
 * and from the system keyboard settings gear (declared as the input method's settingsActivity).
 *
 * <p>Today it hosts the keyboard-height slider; voice and AI entries will join it under RFC-0007.
 * The height is stored in the same {@code retekey_view} preferences the keyboard reads at measure
 * time, so a change here takes effect the next time the keyboard is shown.
 */
public final class SettingsActivity extends Activity {
    private static final String PREFS = "retekey_view";
    private static final String KEY_HEIGHT_SCALE = "height_scale";
    private static final int PREVIEW_BASE_DP = 108;

    private SeekBar slider;
    private TextView valueLabel;
    private View previewBar;
    private ViewGroup.LayoutParams previewParams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.settings_title);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(250, 250, 252));
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText(R.string.settings_height_label);
        title.setTextSize(20);
        title.setTextColor(Color.rgb(22, 27, 34));
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText(R.string.settings_height_hint);
        hint.setTextColor(Color.rgb(90, 98, 110));
        hint.setPadding(0, dp(6), 0, dp(16));
        root.addView(hint);

        valueLabel = new TextView(this);
        valueLabel.setTextSize(16);
        valueLabel.setTextColor(Color.rgb(40, 90, 170));
        root.addView(valueLabel);

        slider = new SeekBar(this);
        int minPercent = Math.round(KeyboardHeightScale.MIN_SCALE * 100);
        int maxPercent = Math.round(KeyboardHeightScale.MAX_SCALE * 100);
        slider.setMin(minPercent);
        slider.setMax(maxPercent);
        slider.setProgress(Math.round(currentScale() * 100));
        slider.setPadding(dp(4), dp(12), dp(4), dp(12));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                applyPercent(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
        root.addView(slider, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView previewCaption = new TextView(this);
        previewCaption.setText(R.string.settings_preview);
        previewCaption.setTextColor(Color.rgb(90, 98, 110));
        previewCaption.setPadding(0, dp(16), 0, dp(6));
        root.addView(previewCaption);

        previewBar = new View(this);
        GradientDrawable fill = new GradientDrawable();
        fill.setColor(Color.rgb(221, 225, 231));
        fill.setStroke(dp(1), Color.rgb(180, 188, 198));
        fill.setCornerRadius(dp(8));
        previewBar.setBackground(fill);
        previewParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(PREVIEW_BASE_DP));
        root.addView(previewBar, previewParams);

        Button reset = new Button(this);
        reset.setText(R.string.settings_reset);
        reset.setOnClickListener(this::resetHeight);
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        resetParams.topMargin = dp(20);
        resetParams.gravity = Gravity.END;
        root.addView(reset, resetParams);

        setContentView(root);
        applyPercent(slider.getProgress());
    }

    private float currentScale() {
        return KeyboardHeightScale.clamp(
            prefs().getFloat(KEY_HEIGHT_SCALE, KeyboardHeightScale.DEFAULT_SCALE));
    }

    private void applyPercent(int percent) {
        float scale = KeyboardHeightScale.clamp(percent / 100.0f);
        prefs().edit().putFloat(KEY_HEIGHT_SCALE, scale).apply();
        valueLabel.setText(getString(R.string.settings_height_value, Math.round(scale * 100)));
        previewParams.height = Math.round(dp(PREVIEW_BASE_DP) * scale);
        previewBar.setLayoutParams(previewParams);
    }

    private void resetHeight(View view) {
        int percent = Math.round(KeyboardHeightScale.DEFAULT_SCALE * 100);
        slider.setProgress(percent);
        applyPercent(percent);
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
