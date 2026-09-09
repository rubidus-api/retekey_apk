package com.retekey;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A tiny launcher screen for trying ReteKey. It uses the system theme and standard views — no
 * hardcoded colors — so it follows the device's light/dark colour scheme, and presents the actions
 * as a plain tap-to-select list rather than buttons.
 */
public final class PreviewActivity extends Activity {
    private EditText field;
    /** The colour scheme these views were made under, so a change made elsewhere is noticed. */
    private ThemeMode builtWith;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // The user's colour-scheme choice, applied before any view is made from it.
        ScreenTheme.apply(this);
        builtWith = ScreenTheme.mode(this);
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );
        // Show the app name and version in the title bar so it is visible right on launch.
        setTitle(getString(R.string.app_name) + "  v" + versionName());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        TextView hint = new TextView(this);
        hint.setText(R.string.preview_hint);
        Compat.setTextAppearance(hint, android.R.style.TextAppearance_DeviceDefault_Medium);
        hint.setPadding(0, 0, 0, dp(12));
        root.addView(hint);

        field = new EditText(this);
        field.setHint(R.string.preview_field_hint);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        field.setMinLines(3);
        field.setGravity(Gravity.TOP | Gravity.START);
        root.addView(field, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // Actions as a tap-to-select list (order 2, 1, 3).
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        listParams.topMargin = dp(16);
        root.addView(list, listParams);

        addDivider(list);
        addListItem(list, R.string.preview_manage_keyboards, this::manageKeyboards);
        addDivider(list);
        addListItem(list, R.string.preview_pick_keyboard, this::showKeyboardPicker);
        addDivider(list);
        addListItem(list, R.string.preview_open_settings, this::openSettings);
        addDivider(list);
        addListItem(list, R.string.preview_open_bar_settings, this::openActionBarSettings);
        addDivider(list);
        // The per-screen pages are reached from here rather than from inside the settings page:
        // one of them is not a part of another, and a door inside a door is a door people do not
        // find (owner's request).
        addListItem(list, R.string.settings_portrait_title,
            view -> openScreenPage(ScreenOrientation.PORTRAIT, false));
        addDivider(list);
        addListItem(list, R.string.settings_landscape_title,
            view -> openScreenPage(ScreenOrientation.LANDSCAPE, false));
        addDivider(list);
        addListItem(list, R.string.settings_layout_portrait_title,
            view -> openScreenPage(ScreenOrientation.PORTRAIT, true));
        addDivider(list);
        addListItem(list, R.string.settings_layout_landscape_title,
            view -> openScreenPage(ScreenOrientation.LANDSCAPE, true));
        addDivider(list);

        // The list outgrew the screen when the per-screen pages joined it, and a row that cannot
        // be scrolled to is a row that is not there. The typing field stays at the top of the
        // scroller, so it is still the first thing under the hint.
        android.widget.ScrollView scroller = new android.widget.ScrollView(this);
        // The height must be WRAP_CONTENT and said out loud: a scroller's default for its child
        // is the viewport's own height, which measures the list to the screen and clips whatever
        // does not fit instead of scrolling to it.
        scroller.addView(root, new android.widget.FrameLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        setContentView(scroller);
        ScreenFit.apply(scroller, root);
    }

    /** A tap-to-select list row with the platform's selectable-item touch feedback. */
    private void addListItem(LinearLayout list, int textRes, View.OnClickListener onClick) {
        TextView row = new TextView(this);
        row.setText(textRes);
        Compat.setTextAppearance(row, android.R.style.TextAppearance_DeviceDefault_Medium);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinHeight(dp(56));
        int h = dp(12);
        row.setPadding(dp(4), h, dp(4), h);
        row.setClickable(true);
        row.setFocusable(true);
        TypedValue background = new TypedValue();
        if (getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground, background, true)) {
            row.setBackgroundResource(background.resourceId);
        }
        row.setOnClickListener(onClick);
        list.addView(row, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    /** A 1px list separator using the platform divider drawable, so it tracks the theme. */
    private void addDivider(LinearLayout list) {
        View divider = new View(this);
        TypedValue drawable = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.listDivider, drawable, true)
                && drawable.resourceId != 0) {
            divider.setBackgroundResource(drawable.resourceId);
        }
        list.addView(divider, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, dp(1))));
    }

    private void openSettings(View view) {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    /** One screen's own page: its settings, or its layouts. */
    private void openScreenPage(ScreenOrientation orientation, boolean layouts) {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra(SettingsActivity.EXTRA_SCREEN, orientation.name());
        if (layouts) {
            intent.putExtra(SettingsActivity.EXTRA_LAYOUTS, true);
        }
        startActivity(intent);
    }

    /** The action bar's own screen, which is long enough to deserve its own way in. */
    private void openActionBarSettings(View view) {
        startActivity(new Intent(this, ActionBarSettingsActivity.class));
    }

    /** Opens the system screen for enabling/disabling installed keyboards. */
    private void manageKeyboards(View view) {
        try {
            startActivity(new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS));
        } catch (RuntimeException ignored) {
            // No settings screen to open on this device.
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Coming back from the settings screen after a colour-scheme change: this screen was kept
        // rather than recreated, so it is still wearing the old theme and has to be built again.
        if (builtWith != ScreenTheme.mode(this)) {
            recreate();
            return;
        }
        field.requestFocus();
        InputMethodManager imm = Compat.systemService(this, Context.INPUT_METHOD_SERVICE, InputMethodManager.class);
        if (imm != null) {
            imm.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void showKeyboardPicker(View view) {
        InputMethodManager imm = Compat.systemService(this, Context.INPUT_METHOD_SERVICE, InputMethodManager.class);
        if (imm != null) {
            imm.showInputMethodPicker();
        }
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException notFound) {
            return "";
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
