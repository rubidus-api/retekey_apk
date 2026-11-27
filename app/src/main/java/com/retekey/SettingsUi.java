package com.retekey;

import android.app.Activity;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Shared stock-control builders for ReteKey's settings activities so formatting, slider bounds,
 * and icon-button sizing stay consistent across screens.
 */
final class SettingsUi {
    private static final int MOVE_BUTTON_DP = 44;

    private SettingsUi() {
    }

    static TextView sectionHeader(Activity activity, int textRes, int topPadDp) {
        TextView header = new TextView(activity);
        header.setText(textRes);
        Compat.setTextAppearance(header, android.R.style.TextAppearance_DeviceDefault_Large);
        header.setPadding(0, dp(activity, topPadDp), 0, dp(activity, 2));
        return header;
    }

    static TextView sectionHint(Activity activity, int textRes) {
        TextView hint = new TextView(activity);
        hint.setText(textRes);
        Compat.setTextAppearance(hint, android.R.style.TextAppearance_DeviceDefault_Small);
        hint.setPadding(0, dp(activity, 4), 0, dp(activity, 8));
        return hint;
    }

    /** A single-glyph borderless button sized as a square touch target. */
    static Button glyphButton(
            Activity activity, String glyph, boolean usable, View.OnClickListener onClick) {
        Button button = new Button(activity, null, android.R.attr.borderlessButtonStyle);
        button.setText(glyph);
        button.setEnabled(usable);
        button.setAllCaps(false);
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(0, 0, 0, 0);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        if (onClick != null) {
            button.setOnClickListener(onClick);
        }
        return button;
    }

    static LinearLayout.LayoutParams moveButtonParams(Activity activity, boolean first) {
        int size = dp(activity, MOVE_BUTTON_DP);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.leftMargin = dp(activity, first ? 8 : 4);
        params.rightMargin = first ? 0 : dp(activity, 4);
        return params;
    }

    static LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    static void rangeSlider(SeekBar bar, int min, int max) {
        if (Compat.canSetSeekBarMin()) {
            bar.setMin(min);
            bar.setMax(max);
        } else {
            bar.setMax(max - min);
        }
    }

    static int sliderValue(SeekBar bar, int min) {
        return Compat.canSetSeekBarMin() ? bar.getProgress() : bar.getProgress() + min;
    }

    static int sliderValue(int progress, int min) {
        return Compat.canSetSeekBarMin() ? progress : progress + min;
    }

    static void setSliderValue(SeekBar bar, int min, int value) {
        bar.setProgress(Compat.canSetSeekBarMin() ? value : value - min);
    }

    static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
