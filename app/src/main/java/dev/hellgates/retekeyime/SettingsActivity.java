package dev.hellgates.retekeyime;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import dev.hellgates.retekeyime.HardwareKeyBindings.Binding;
import java.util.List;

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
    private String capturingKey;
    private TextView captureStatus;
    private LinearLayout hanyeongList;
    private LinearLayout hanjaList;

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

        TextView feedbackHeader = new TextView(this);
        feedbackHeader.setText(R.string.settings_feedback_label);
        feedbackHeader.setTextSize(20);
        feedbackHeader.setTextColor(Color.rgb(22, 27, 34));
        feedbackHeader.setPadding(0, dp(28), 0, 0);
        root.addView(feedbackHeader);

        TextView feedbackHint = new TextView(this);
        feedbackHint.setText(R.string.settings_feedback_hint);
        feedbackHint.setTextColor(Color.rgb(90, 98, 110));
        feedbackHint.setPadding(0, dp(6), 0, dp(8));
        root.addView(feedbackHint);

        addPercentSlider(root, R.string.settings_visual,
            KeyFeedback.KEY_VISUAL, KeyFeedback.DEFAULT_VISUAL);
        addPercentSlider(root, R.string.settings_haptic,
            KeyFeedback.KEY_HAPTIC, KeyFeedback.DEFAULT_HAPTIC);
        addPercentSlider(root, R.string.settings_sound,
            KeyFeedback.KEY_SOUND, KeyFeedback.DEFAULT_SOUND);

        addRepeatControls(root);
        addHardwareControls(root);

        Button reset = new Button(this);
        reset.setText(R.string.settings_reset);
        reset.setOnClickListener(this::resetHeight);
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        resetParams.topMargin = dp(20);
        resetParams.gravity = Gravity.END;
        root.addView(reset, resetParams);

        // The controls are taller than a phone screen, so make the whole screen scroll.
        ScrollView scroller = new ScrollView(this);
        scroller.addView(root);
        setContentView(scroller);
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

    /** Adds a titled 0–100% slider bound to a 0–1 float preference. */
    private void addPercentSlider(LinearLayout root, int titleRes, String prefKey, float defaultValue) {
        TextView label = new TextView(this);
        label.setTextColor(Color.rgb(40, 90, 170));
        label.setPadding(0, dp(10), 0, 0);
        root.addView(label);

        SeekBar bar = new SeekBar(this);
        bar.setMax(100);
        int start = Math.round(clampUnit(prefs().getFloat(prefKey, defaultValue)) * 100);
        bar.setProgress(start);
        bar.setPadding(dp(4), dp(8), dp(4), dp(8));
        label.setText(getString(titleRes) + "  " + getString(R.string.settings_percent_value, start));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar b, int progress, boolean fromUser) {
                prefs().edit().putFloat(prefKey, progress / 100.0f).apply();
                label.setText(getString(titleRes) + "  "
                    + getString(R.string.settings_percent_value, progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar b) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar b) {
            }
        });
        root.addView(bar, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private static float clampUnit(float value) {
        if (Float.isNaN(value)) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private void resetHeight(View view) {
        int percent = Math.round(KeyboardHeightScale.DEFAULT_SCALE * 100);
        slider.setProgress(percent);
        applyPercent(percent);
    }

    // ---- Held-key auto-repeat ----

    private void addRepeatControls(LinearLayout root) {
        root.addView(sectionHeader(R.string.settings_repeat_label));
        root.addView(sectionHint(R.string.settings_repeat_hint));

        CheckBox enabled = new CheckBox(this);
        enabled.setText(R.string.settings_repeat_enabled);
        enabled.setTextColor(Color.rgb(40, 90, 170));
        enabled.setChecked(prefs().getBoolean(
            KeyRepeatSettings.KEY_ENABLED, KeyRepeatSettings.DEFAULT_ENABLED));
        enabled.setOnCheckedChangeListener((b, checked) ->
            prefs().edit().putBoolean(KeyRepeatSettings.KEY_ENABLED, checked).apply());
        root.addView(enabled);

        addMsSlider(root, R.string.settings_repeat_delay, KeyRepeatSettings.KEY_DELAY_MS,
            KeyRepeatSettings.MIN_DELAY_MS, KeyRepeatSettings.MAX_DELAY_MS,
            KeyRepeatSettings.DEFAULT_DELAY_MS);
        addMsSlider(root, R.string.settings_repeat_interval, KeyRepeatSettings.KEY_INTERVAL_MS,
            KeyRepeatSettings.MIN_INTERVAL_MS, KeyRepeatSettings.MAX_INTERVAL_MS,
            KeyRepeatSettings.DEFAULT_INTERVAL_MS);
    }

    /** A titled millisecond slider bound to an int preference clamped to [min, max]. */
    private void addMsSlider(LinearLayout root, int titleRes, String prefKey,
            int min, int max, int def) {
        TextView label = new TextView(this);
        label.setTextColor(Color.rgb(40, 90, 170));
        label.setPadding(0, dp(10), 0, 0);
        root.addView(label);

        SeekBar bar = new SeekBar(this);
        bar.setMin(min);
        bar.setMax(max);
        int start = Math.max(min, Math.min(max, prefs().getInt(prefKey, def)));
        bar.setProgress(start);
        bar.setPadding(dp(4), dp(8), dp(4), dp(8));
        label.setText(getString(titleRes) + "  " + getString(R.string.settings_ms_value, start));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar b, int progress, boolean fromUser) {
                prefs().edit().putInt(prefKey, progress).apply();
                label.setText(getString(titleRes) + "  "
                    + getString(R.string.settings_ms_value, progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar b) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar b) {
            }
        });
        root.addView(bar, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    // ---- Physical-keyboard 한/영 and 한자 shortcuts ----

    private void addHardwareControls(LinearLayout root) {
        root.addView(sectionHeader(R.string.settings_hw_label));
        root.addView(sectionHint(R.string.settings_hw_hint));

        captureStatus = new TextView(this);
        captureStatus.setTextColor(Color.rgb(200, 80, 60));
        captureStatus.setPadding(0, dp(4), 0, dp(4));
        captureStatus.setVisibility(View.GONE);
        captureStatus.setOnClickListener(v -> stopCapture());
        root.addView(captureStatus);

        hanyeongList = addBindingGroup(root, R.string.settings_hw_hanyeong,
            HardwareKeyBindings.KEY_HANYEONG);
        hanjaList = addBindingGroup(root, R.string.settings_hw_hanja,
            HardwareKeyBindings.KEY_HANJA);

        TextView note = new TextView(this);
        note.setText(R.string.settings_hw_hanja_note);
        note.setTextColor(Color.rgb(150, 120, 60));
        note.setPadding(0, dp(6), 0, 0);
        root.addView(note);

        refreshBindings(HardwareKeyBindings.KEY_HANYEONG);
        refreshBindings(HardwareKeyBindings.KEY_HANJA);
    }

    private LinearLayout addBindingGroup(LinearLayout root, int titleRes, String prefKey) {
        TextView label = new TextView(this);
        label.setText(titleRes);
        label.setTextColor(Color.rgb(40, 90, 170));
        label.setPadding(0, dp(14), 0, 0);
        root.addView(label);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);

        Button add = new Button(this);
        add.setText(R.string.settings_hw_add);
        add.setOnClickListener(v -> startCapture(prefKey));
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        root.addView(add, addParams);
        return list;
    }

    private void refreshBindings(String prefKey) {
        LinearLayout list = prefKey.equals(HardwareKeyBindings.KEY_HANYEONG)
            ? hanyeongList : hanjaList;
        list.removeAllViews();
        List<Binding> bindings = HardwareKeyBindings.parse(prefs().getString(prefKey, ""));
        if (bindings.isEmpty()) {
            TextView none = new TextView(this);
            none.setText(R.string.settings_hw_none);
            none.setTextColor(Color.rgb(150, 150, 150));
            none.setPadding(0, dp(4), 0, dp(4));
            list.addView(none);
            return;
        }
        for (Binding binding : bindings) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            TextView name = new TextView(this);
            name.setText(bindingLabel(binding));
            name.setTextColor(Color.rgb(22, 27, 34));
            name.setTextSize(16);
            row.addView(name, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Button remove = new Button(this);
            remove.setText(R.string.settings_hw_remove);
            remove.setOnClickListener(v -> removeBinding(prefKey, binding));
            row.addView(remove);

            list.addView(row);
        }
    }

    private void startCapture(String prefKey) {
        capturingKey = prefKey;
        captureStatus.setText(R.string.settings_hw_capture);
        captureStatus.setVisibility(View.VISIBLE);
    }

    private void stopCapture() {
        capturingKey = null;
        if (captureStatus != null) {
            captureStatus.setVisibility(View.GONE);
        }
    }

    private void addBinding(String prefKey, Binding binding) {
        List<Binding> bindings = HardwareKeyBindings.parse(prefs().getString(prefKey, ""));
        HardwareKeyBindings.add(bindings, binding);
        prefs().edit().putString(prefKey, HardwareKeyBindings.format(bindings)).apply();
        refreshBindings(prefKey);
    }

    private void removeBinding(String prefKey, Binding binding) {
        List<Binding> bindings = HardwareKeyBindings.parse(prefs().getString(prefKey, ""));
        bindings.remove(binding);
        prefs().edit().putString(prefKey, HardwareKeyBindings.format(bindings)).apply();
        refreshBindings(prefKey);
    }

    private String bindingLabel(Binding binding) {
        StringBuilder sb = new StringBuilder();
        if ((binding.mods & HardwareKeyBindings.MOD_CTRL) != 0) {
            sb.append("Ctrl+");
        }
        if ((binding.mods & HardwareKeyBindings.MOD_SHIFT) != 0) {
            sb.append("Shift+");
        }
        if ((binding.mods & HardwareKeyBindings.MOD_ALT) != 0) {
            sb.append("Alt+");
        }
        if ((binding.mods & HardwareKeyBindings.MOD_META) != 0) {
            sb.append("Meta+");
        }
        String name = KeyEvent.keyCodeToString(binding.keyCode);
        if (name != null && name.startsWith("KEYCODE_")) {
            name = name.substring("KEYCODE_".length());
        }
        sb.append(name);
        return sb.toString();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (capturingKey != null) {
            if (KeyEvent.isModifierKey(keyCode)) {
                // Wait: a plain modifier may be the start of a chord, or a lone-modifier binding
                // captured on its release.
                return true;
            }
            addBinding(capturingKey, new Binding(modsOf(event), keyCode));
            stopCapture();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (capturingKey != null && KeyEvent.isModifierKey(keyCode)) {
            addBinding(capturingKey, new Binding(0, keyCode));
            stopCapture();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private static int modsOf(KeyEvent event) {
        int meta = event.getMetaState();
        return HardwareKeyBindings.modsOf(
            (meta & KeyEvent.META_SHIFT_ON) != 0,
            (meta & KeyEvent.META_CTRL_ON) != 0,
            (meta & KeyEvent.META_ALT_ON) != 0,
            (meta & KeyEvent.META_META_ON) != 0);
    }

    private TextView sectionHeader(int textRes) {
        TextView header = new TextView(this);
        header.setText(textRes);
        header.setTextSize(20);
        header.setTextColor(Color.rgb(22, 27, 34));
        header.setPadding(0, dp(28), 0, 0);
        return header;
    }

    private TextView sectionHint(int textRes) {
        TextView hint = new TextView(this);
        hint.setText(textRes);
        hint.setTextColor(Color.rgb(90, 98, 110));
        hint.setPadding(0, dp(6), 0, dp(8));
        return hint;
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
