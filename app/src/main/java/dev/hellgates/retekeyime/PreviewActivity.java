package dev.hellgates.retekeyime;

import android.app.Activity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        hint.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium);
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

        setContentView(root);
    }

    /** A tap-to-select list row with the platform's selectable-item touch feedback. */
    private void addListItem(LinearLayout list, int textRes, View.OnClickListener onClick) {
        TextView row = new TextView(this);
        row.setText(textRes);
        row.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium);
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
        field.requestFocus();
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (imm != null) {
            imm.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void showKeyboardPicker(View view) {
        InputMethodManager imm = getSystemService(InputMethodManager.class);
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
