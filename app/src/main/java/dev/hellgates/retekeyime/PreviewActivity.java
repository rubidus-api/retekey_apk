package dev.hellgates.retekeyime;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A tiny launcher screen for trying ReteKey. It uses the system theme and standard controls — no
 * hardcoded colors — so it follows the device's light/dark colour scheme, and lays the actions out
 * as a plain spaced menu of full-width buttons.
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

        // Menu order 2, 1, 3: manage keyboards, pick keyboard, ReteKey settings.
        addMenuButton(root, R.string.preview_manage_keyboards, this::manageKeyboards);
        addMenuButton(root, R.string.preview_pick_keyboard, this::showKeyboardPicker);
        addMenuButton(root, R.string.preview_open_settings, this::openSettings);

        setContentView(root);
    }

    /** Adds a full-width button with a gap above it, so the actions read as a spaced menu list. */
    private void addMenuButton(LinearLayout root, int textRes, View.OnClickListener onClick) {
        Button button = new Button(this);
        button.setText(textRes);
        button.setAllCaps(false);
        button.setOnClickListener(onClick);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(12);
        root.addView(button, params);
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
