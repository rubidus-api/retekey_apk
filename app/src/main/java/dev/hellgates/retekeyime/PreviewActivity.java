package dev.hellgates.retekeyime;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
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
 * A tiny screen for trying ReteKey and for capturing screenshots. It is not the product's settings
 * surface; it just gives a focusable field so the keyboard shows over a real editor.
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

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(250, 250, 252));
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText(getString(R.string.app_name));
        title.setTextSize(22);
        title.setTextColor(Color.rgb(22, 27, 34));
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText(R.string.preview_hint);
        hint.setTextColor(Color.rgb(90, 98, 110));
        hint.setPadding(0, dp(6), 0, dp(16));
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

        // Order: system default/added-keyboards settings, then the input-method picker, then
        // ReteKey's own settings (requested top-to-bottom order 2, 1, 3).
        Button manage = new Button(this);
        manage.setText(R.string.preview_manage_keyboards);
        manage.setOnClickListener(this::manageKeyboards);
        root.addView(manage);

        Button chooser = new Button(this);
        chooser.setText(R.string.preview_pick_keyboard);
        chooser.setOnClickListener(this::showKeyboardPicker);
        root.addView(chooser);

        Button settings = new Button(this);
        settings.setText(R.string.preview_open_settings);
        settings.setOnClickListener(this::openSettings);
        root.addView(settings);

        setContentView(root);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
