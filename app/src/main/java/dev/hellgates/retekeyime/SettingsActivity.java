package dev.hellgates.retekeyime;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import dev.hellgates.retekeyime.HardwareKeyBindings.Binding;
import java.util.ArrayList;
import java.util.List;

/**
 * ReteKey's settings screen. Reachable from the app launcher, from the ☰ menu key on the keyboard,
 * and from the system keyboard settings gear (declared as the input method's settingsActivity).
 *
 * <p>It uses only stock controls (text, sliders, a checkbox, buttons) and no hardcoded colors, so
 * it follows the device's light/dark system theme. Values are stored in the same
 * {@code retekey_view} preferences the keyboard reads, so changes take effect next time it shows.
 */
public final class SettingsActivity extends Activity {
    private static final String PREFS = "retekey_view";
    private static final String KEY_HEIGHT_SCALE = "height_scale";

    /** Which screen's settings this page is showing; the device's own, until the user picks. */
    private ScreenOrientation editing;
    private SeekBar slider;
    private TextView valueLabel;
    private String capturingKey;
    private TextView captureStatus;
    private LinearLayout hanyeongList;
    private LinearLayout hanjaList;
    private LinearLayout layoutList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.settings_title);
        editing = OrientedPrefs.current(this);
        buildUi();
    }

    /**
     * Builds the whole screen. Called again when the user switches which orientation they are
     * setting, because every oriented control then has a different value to show.
     */
    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);

        // The screen is reachable from the launcher, the keyboard's menu, and the system keyboard
        // settings, and the hardware Back key is not always at hand — a tablet with an external
        // keyboard and gesture navigation has no visible one. Put the way out on the screen.
        root.addView(backButton(), matchWidth());
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        addOrientationControls(root);

        root.addView(sectionHeader(R.string.settings_height_label));
        root.addView(sectionHint(R.string.settings_height_hint));

        valueLabel = new TextView(this);
        Compat.setTextAppearance(valueLabel, android.R.style.TextAppearance_DeviceDefault_Medium);
        root.addView(valueLabel);

        slider = new SeekBar(this);
        rangeSlider(slider, KeyboardHeightScale.MIN_LEVEL, KeyboardHeightScale.MAX_LEVEL);
        setSliderValue(slider, KeyboardHeightScale.MIN_LEVEL, currentLevel());
        slider.setPadding(dp(4), dp(12), dp(4), dp(12));
        slider.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                applyLevel(sliderValue(progress, KeyboardHeightScale.MIN_LEVEL));
            }
        });
        root.addView(slider, matchWidth());

        root.addView(sectionHeader(R.string.settings_feedback_label));
        root.addView(sectionHint(R.string.settings_feedback_hint));
        addPercentSlider(root, R.string.settings_visual,
            KeyFeedback.KEY_VISUAL, KeyFeedback.DEFAULT_VISUAL);
        addPercentSlider(root, R.string.settings_haptic,
            KeyFeedback.KEY_HAPTIC, KeyFeedback.DEFAULT_HAPTIC);
        addPercentSlider(root, R.string.settings_sound,
            KeyFeedback.KEY_SOUND, KeyFeedback.DEFAULT_SOUND);

        addLayoutControls(root);
        addFloatingControls(root);
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
        applyLevel(currentLevel());
    }

    /**
     * Picks which screen the oriented settings below are for. A keyboard held sideways wants a
     * different height and often a different set of layouts, and rotating the phone to change them
     * would mean losing sight of the setting you came for.
     */
    private void addOrientationControls(LinearLayout root) {
        root.addView(sectionHeader(R.string.settings_orientation_label));
        root.addView(sectionHint(R.string.settings_orientation_hint));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(orientationButton(
            ScreenOrientation.PORTRAIT, R.string.settings_orientation_portrait));
        row.addView(orientationButton(
            ScreenOrientation.LANDSCAPE, R.string.settings_orientation_landscape));
        root.addView(row, matchWidth());
    }

    private Button orientationButton(ScreenOrientation orientation, int titleRes) {
        Button button = new Button(this);
        String title = getString(titleRes);
        // The one being edited is marked in the label itself: no colour is hardcoded anywhere on
        // this screen, so the mark has to be something the theme cannot take away.
        button.setText(editing == orientation ? "● " + title : title);
        button.setAllCaps(false);
        button.setEnabled(editing != orientation);
        button.setOnClickListener(view -> {
            editing = orientation;
            buildUi();
        });
        return button;
    }

    /** Returns to the app's main screen, whichever entry point opened these settings. */
    private Button backButton() {
        Button back = new Button(this);
        back.setText(R.string.settings_back);
        back.setAllCaps(false);
        back.setOnClickListener(view -> goToMainScreen());
        return back;
    }

    private void goToMainScreen() {
        android.content.Intent intent = new android.content.Intent(this, PreviewActivity.class);
        // Reuse the existing main screen when it is already in the task instead of stacking a
        // second copy behind this one.
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            startActivity(intent);
        } catch (RuntimeException ignored) {
            // Nothing to fall back to; finishing still leaves the settings screen.
        }
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            goToMainScreen();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** How solid the floating keyboard is; it is translucent so the app stays readable under it. */
    /**
     * A slider over {@code [min, max]}. {@code setMin} is API 26; below it the bar runs from zero
     * and the caller shifts, which is what {@link #sliderValue} and {@link #setSliderValue} do.
     */
    private static void rangeSlider(SeekBar bar, int min, int max) {
        if (Compat.canSetSeekBarMin()) {
            bar.setMin(min);
            bar.setMax(max);
        } else {
            bar.setMax(max - min);
        }
    }

    private static int sliderValue(SeekBar bar, int min) {
        return Compat.canSetSeekBarMin() ? bar.getProgress() : bar.getProgress() + min;
    }

    private static void setSliderValue(SeekBar bar, int min, int value) {
        bar.setProgress(Compat.canSetSeekBarMin() ? value : value - min);
    }

    private static int sliderValue(int progress, int min) {
        return Compat.canSetSeekBarMin() ? progress : progress + min;
    }

    private void addFloatingControls(LinearLayout root) {
        root.addView(sectionHeader(R.string.settings_floating_label));
        root.addView(sectionHint(R.string.settings_floating_hint));

        CheckBox floating = new CheckBox(this);
        floating.setText(R.string.settings_floating_enabled);
        floating.setChecked(FloatingKeyboardSettings.isEnabled(prefs(), editing));
        floating.setOnCheckedChangeListener((b, checked) ->
            FloatingKeyboardSettings.setEnabled(prefs(), editing, checked));
        root.addView(floating);

        TextView label = new TextView(this);
        Compat.setTextAppearance(label, android.R.style.TextAppearance_DeviceDefault_Medium);
        root.addView(label);

        SeekBar bar = new SeekBar(this);
        int floor = FloatingKeyboardSettings.MIN_OPACITY_PERCENT;
        rangeSlider(bar, floor, FloatingKeyboardSettings.MAX_OPACITY_PERCENT);
        setSliderValue(bar, floor, FloatingKeyboardSettings.opacityPercent(prefs(), editing));
        bar.setPadding(dp(4), dp(12), dp(4), dp(12));
        label.setText(getString(R.string.settings_floating_value, sliderValue(bar, floor)));
        bar.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int percent = sliderValue(progress, floor);
                FloatingKeyboardSettings.setOpacityPercent(prefs(), editing, percent);
                label.setText(getString(R.string.settings_floating_value, percent));
            }
        });
        root.addView(bar, matchWidth());
    }

    /**
     * Which letter layouts the globe key visits, and in what order. Each row can be turned on or
     * off and moved up or down; the order shown is the order the globe key walks.
     */
    private void addLayoutControls(LinearLayout root) {
        root.addView(sectionHeader(R.string.settings_layouts_label));
        root.addView(sectionHint(R.string.settings_layouts_hint));
        layoutList = new LinearLayout(this);
        layoutList.setOrientation(LinearLayout.VERTICAL);
        root.addView(layoutList, matchWidth());
        refreshLayoutList();
    }

    private void refreshLayoutList() {
        layoutList.removeAllViews();
        List<KeyboardLayoutId> order = orderedLayouts();
        for (KeyboardLayoutId id : order) {
            layoutList.addView(layoutRow(id, order, true), matchWidth());
        }
        for (KeyboardLayoutId id : LetterLayouts.ALL) {
            if (!order.contains(id)) {
                layoutList.addView(layoutRow(id, order, false), matchWidth());
            }
        }
    }

    private LinearLayout layoutRow(KeyboardLayoutId id, List<KeyboardLayoutId> order, boolean on) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        CheckBox enabled = new CheckBox(this);
        enabled.setText(LetterLayouts.displayName(id));
        enabled.setChecked(on);
        enabled.setOnClickListener(view -> toggleLayout(id));
        row.addView(enabled, new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (on) {
            row.addView(moveButton("▲", id, -1, order.indexOf(id) > 0));
            row.addView(moveButton("▼", id, 1, order.indexOf(id) < order.size() - 1));
        }
        return row;
    }

    private Button moveButton(String glyph, KeyboardLayoutId id, int delta, boolean usable) {
        Button button = new Button(this);
        button.setText(glyph);
        button.setEnabled(usable);
        button.setOnClickListener(view -> moveLayout(id, delta));
        return button;
    }

    private void toggleLayout(KeyboardLayoutId id) {
        List<KeyboardLayoutId> order = new ArrayList<>(orderedLayouts());
        if (order.contains(id)) {
            // The globe key must always have somewhere to go, so the last one stays on.
            if (order.size() > 1) {
                order.remove(id);
            }
        } else {
            order.add(id);
        }
        storeLayouts(order);
    }

    private void moveLayout(KeyboardLayoutId id, int delta) {
        List<KeyboardLayoutId> order = new ArrayList<>(orderedLayouts());
        int from = order.indexOf(id);
        int to = from + delta;
        if (from < 0 || to < 0 || to >= order.size()) {
            return;
        }
        order.remove(from);
        order.add(to, id);
        storeLayouts(order);
    }

    private void storeLayouts(List<KeyboardLayoutId> order) {
        OrientedPrefs.putString(prefs(), LetterLayouts.KEY_ORDER, editing, LetterLayouts.format(order));
        refreshLayoutList();
    }

    private List<KeyboardLayoutId> orderedLayouts() {
        return LetterLayouts.parse(
            OrientedPrefs.getString(prefs(), LetterLayouts.KEY_ORDER, editing, null));
    }

    private int currentLevel() {
        return KeyboardHeightScale.levelForScale(KeyboardHeightScale.clamp(OrientedPrefs.getFloat(
            prefs(), KEY_HEIGHT_SCALE, editing, KeyboardHeightScale.DEFAULT_SCALE)));
    }

    private static final int KEYBOARD_ROWS = 4;

    private void applyLevel(int level) {
        int clamped = KeyboardHeightScale.clampLevel(level);
        float scale = KeyboardHeightScale.scaleForLevel(clamped);
        OrientedPrefs.putFloat(prefs(), KEY_HEIGHT_SCALE, editing, scale);
        valueLabel.setText(getString(R.string.settings_height_value, clamped, screenPercent(scale)));
    }

    /** The keyboard's height at {@code scale} as a percentage of the screen height. */
    private int screenPercent(float scale) {
        float density = getResources().getDisplayMetrics().density;
        int keyboardPx = KeyboardHeightScale.heightForScale(
            scale, KeyboardHeightScale.baseHeightPx(KEYBOARD_ROWS, density));
        int screenPx = getResources().getDisplayMetrics().heightPixels;
        return screenPx > 0 ? Math.round(keyboardPx * 100.0f / screenPx) : 0;
    }

    /** Adds a titled 0–100% slider bound to a 0–1 float preference. */
    private void addPercentSlider(LinearLayout root, int titleRes, String prefKey, float defaultValue) {
        TextView label = new TextView(this);
        label.setPadding(0, dp(10), 0, 0);
        root.addView(label);

        SeekBar bar = new SeekBar(this);
        bar.setMax(100);
        int start = Math.round(clampUnit(prefs().getFloat(prefKey, defaultValue)) * 100);
        bar.setProgress(start);
        bar.setPadding(dp(4), dp(8), dp(4), dp(8));
        label.setText(getString(titleRes) + "  " + getString(R.string.settings_percent_value, start));
        bar.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar b, int progress, boolean fromUser) {
                prefs().edit().putFloat(prefKey, progress / 100.0f).apply();
                label.setText(getString(titleRes) + "  "
                    + getString(R.string.settings_percent_value, progress));
            }
        });
        root.addView(bar, matchWidth());
    }

    private static float clampUnit(float value) {
        if (Float.isNaN(value)) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private void resetHeight(View view) {
        setSliderValue(slider, KeyboardHeightScale.MIN_LEVEL, KeyboardHeightScale.DEFAULT_LEVEL);
        applyLevel(KeyboardHeightScale.DEFAULT_LEVEL);
    }

    // ---- Held-key auto-repeat ----

    private void addRepeatControls(LinearLayout root) {
        root.addView(sectionHeader(R.string.settings_repeat_label));
        root.addView(sectionHint(R.string.settings_repeat_hint));

        CheckBox enabled = new CheckBox(this);
        enabled.setText(R.string.settings_repeat_enabled);
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
        label.setPadding(0, dp(10), 0, 0);
        root.addView(label);

        SeekBar bar = new SeekBar(this);
        rangeSlider(bar, min, max);
        int start = Math.max(min, Math.min(max, prefs().getInt(prefKey, def)));
        setSliderValue(bar, min, start);
        bar.setPadding(dp(4), dp(8), dp(4), dp(8));
        label.setText(getString(titleRes) + "  " + getString(R.string.settings_ms_value, start));
        bar.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar b, int progress, boolean fromUser) {
                int value = sliderValue(progress, min);
                prefs().edit().putInt(prefKey, value).apply();
                label.setText(getString(titleRes) + "  "
                    + getString(R.string.settings_ms_value, value));
            }
        });
        root.addView(bar, matchWidth());
    }

    // ---- Physical-keyboard 한/영 and 한자 shortcuts ----

    private void addHardwareControls(LinearLayout root) {
        root.addView(sectionHeader(R.string.settings_hw_label));
        root.addView(sectionHint(R.string.settings_hw_hint));

        captureStatus = new TextView(this);
        captureStatus.setPadding(0, dp(4), 0, dp(4));
        captureStatus.setVisibility(View.GONE);
        captureStatus.setOnClickListener(v -> stopCapture());
        root.addView(captureStatus);

        hanyeongList = addBindingGroup(root, R.string.settings_hw_hanyeong,
            HardwareKeyBindings.KEY_HANYEONG);
        hanjaList = addBindingGroup(root, R.string.settings_hw_hanja,
            HardwareKeyBindings.KEY_HANJA);

        root.addView(sectionHint(R.string.settings_hw_hanja_note));

        refreshBindings(HardwareKeyBindings.KEY_HANYEONG);
        refreshBindings(HardwareKeyBindings.KEY_HANJA);
    }

    private LinearLayout addBindingGroup(LinearLayout root, int titleRes, String prefKey) {
        TextView label = new TextView(this);
        label.setText(titleRes);
        label.setPadding(0, dp(14), 0, 0);
        root.addView(label);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);

        Button add = new Button(this);
        add.setText(R.string.settings_hw_add);
        add.setOnClickListener(v -> startCapture(prefKey));
        root.addView(add, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
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
            Compat.setTextAppearance(name, android.R.style.TextAppearance_DeviceDefault_Medium);
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
        Compat.setTextAppearance(header, android.R.style.TextAppearance_DeviceDefault_Large);
        header.setPadding(0, dp(28), 0, dp(2));
        return header;
    }

    private TextView sectionHint(int textRes) {
        TextView hint = new TextView(this);
        hint.setText(textRes);
        Compat.setTextAppearance(hint, android.R.style.TextAppearance_DeviceDefault_Small);
        hint.setPadding(0, dp(4), 0, dp(8));
        return hint;
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** A {@link SeekBar.OnSeekBarChangeListener} with empty start/stop callbacks. */
    private abstract static class SimpleSeekBarListener
            implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar bar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
        }
    }
}
