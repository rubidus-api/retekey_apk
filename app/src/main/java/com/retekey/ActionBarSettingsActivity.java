package com.retekey;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The action bar's own settings screen.
 *
 * <p>It used to be a section on the main settings page, and it outgrew it: the bar carries built-in
 * actions, text the user wrote and key combinations they assembled, each with its own way of being
 * added and edited. A list that long, on a page that long, was hard to find and harder to work in.
 *
 * <p>Stock controls and no hardcoded colours, like every other screen here, so it follows the
 * device's light/dark theme and the user's own colour choice.
 */
public final class ActionBarSettingsActivity extends Activity {
    private static final String PREFS = "retekey_view";
    private static final int ROW_HEIGHT_DP = 48;
    private static final int MOVE_BUTTON_DP = 44;

    private LinearLayout slotList;
    private LinearLayout spareList;
    /** The slot a finger is carrying while a row is dragged to a new place. */
    private Integer draggingIndex;
    /** The row the drop would land on, drawn with a heavy line across its top while dragging. */
    private Integer dropTarget;
    /** The thin rule between rows; thickened where the drop will land. */
    private final java.util.List<View> dividers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ScreenTheme.apply(this);
        super.onCreate(savedInstanceState);
        setTitle(R.string.bar_settings_title);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);

        Button back = new Button(this);
        back.setText(R.string.bar_settings_back);
        back.setAllCaps(false);
        back.setOnClickListener(view -> finish());
        root.addView(back, matchWidth());
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        root.addView(hint(R.string.bar_settings_hint));

        CheckBox enabled = new CheckBox(this);
        enabled.setText(R.string.settings_bar_enabled);
        enabled.setChecked(prefs().getBoolean(
            ActionBarSlots.KEY_ENABLED, ActionBarSlots.DEFAULT_ENABLED));
        enabled.setOnCheckedChangeListener((b, checked) ->
            prefs().edit().putBoolean(ActionBarSlots.KEY_ENABLED, checked).apply());
        root.addView(enabled);

        root.addView(header(R.string.bar_settings_on_the_bar));
        slotList = new LinearLayout(this);
        slotList.setOrientation(LinearLayout.VERTICAL);
        root.addView(slotList, matchWidth());

        LinearLayout adders = new LinearLayout(this);
        adders.setOrientation(LinearLayout.HORIZONTAL);
        adders.addView(wideButton(R.string.bar_settings_add_text, view -> askForText(null)));
        adders.addView(wideButton(R.string.bar_settings_add_chord, view -> askForChord(null)));
        root.addView(adders, matchWidth());

        root.addView(header(R.string.bar_settings_spare));
        root.addView(hint(R.string.bar_settings_spare_hint));
        spareList = new LinearLayout(this);
        spareList.setOrientation(LinearLayout.VERTICAL);
        root.addView(spareList, matchWidth());

        Button reset = new Button(this);
        reset.setText(R.string.settings_bar_reset);
        reset.setAllCaps(false);
        reset.setOnClickListener(view -> store(ActionBarSlots.defaults()));
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        resetParams.topMargin = dp(20);
        resetParams.gravity = Gravity.END;
        root.addView(reset, resetParams);

        ScrollView scroller = new ScrollView(this);
        scroller.addView(root);
        setContentView(scroller);
        ScreenFit.apply(scroller, root);
        refresh();
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---- the list ----

    private void refresh() {
        List<BarSlot> slots = slots();
        slotList.removeAllViews();
        dividers.clear();
        for (int i = 0; i < slots.size(); i++) {
            // A rule above every row, and one after the last: the drop indicator lives on these.
            slotList.addView(divider(), dividerParams());
            slotList.addView(slotRow(slots.get(i), i, slots.size()), matchWidth());
        }
        slotList.addView(divider(), dividerParams());
        spareList.removeAllViews();
        Set<BarAction> carried = new LinkedHashSet<>();
        for (BarSlot slot : slots) {
            if (slot.kind() == BarSlot.Kind.BUILT_IN) {
                carried.add(slot.action());
            }
        }
        for (BarAction action : BarAction.values()) {
            if (!carried.contains(action)) {
                spareList.addView(spareRow(action), matchWidth());
            }
        }
    }

    private LinearLayout slotRow(BarSlot slot, int index, int count) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(ROW_HEIGHT_DP));
        row.setOnDragListener(dropListener(index));

        row.addView(dragHandle(index), moveParams(true));

        TextView name = new TextView(this);
        name.setText(describe(slot));
        Compat.setTextAppearance(name, android.R.style.TextAppearance_DeviceDefault_Medium);
        name.setPadding(dp(4), 0, dp(4), 0);
        name.setClickable(true);
        name.setOnClickListener(view -> edit(slot, index));
        row.addView(name, new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(glyphButton("▲", index > 0, view -> move(index, index - 1)), moveParams(true));
        row.addView(glyphButton("▼", index < count - 1, view -> move(index, index + 1)),
            moveParams(false));
        row.addView(glyphButton("✕", true, view -> remove(index)), moveParams(false));
        return row;
    }

    private LinearLayout spareRow(BarAction action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(ROW_HEIGHT_DP));

        TextView name = new TextView(this);
        name.setText(action.label());
        Compat.setTextAppearance(name, android.R.style.TextAppearance_DeviceDefault_Medium);
        name.setPadding(dp(4), 0, dp(4), 0);
        row.addView(name, new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(glyphButton("＋", true, view -> add(BarSlot.of(action))), moveParams(false));
        return row;
    }

    /** What a row says: the label, and under it what the slot actually does. */
    private String describe(BarSlot slot) {
        switch (slot.kind()) {
            case TEXT:
                return slot.label() + "   —   " + getString(R.string.bar_settings_kind_text);
            case CHORD:
                return slot.label() + "   —   " + slot.chordName();
            default:
                return slot.label();
        }
    }

    // ---- moving ----

    private Button dragHandle(int index) {
        Button handle = glyphButton("≡", true, null);
        handle.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() != MotionEvent.ACTION_DOWN) {
                return false;
            }
            draggingIndex = index;
            // The shadow is the whole row, not the handle that was touched: what follows the
            // finger should look like the thing being moved.
            View row = (View) view.getParent();
            Compat.startDrag(view, new View.DragShadowBuilder(row));
            return true;
        });
        return handle;
    }

    private View.OnDragListener dropListener(int target) {
        return (view, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return draggingIndex != null;
                case DragEvent.ACTION_DRAG_ENTERED:
                case DragEvent.ACTION_DRAG_LOCATION:
                    // Whether the drop would go above or below this row depends on which half of
                    // it the finger is over: the line is drawn where the slot will actually land.
                    boolean upperHalf = event.getY() < view.getHeight() / 2f;
                    showDropAt(upperHalf ? target : target + 1);
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    return true;
                case DragEvent.ACTION_DROP:
                    if (draggingIndex != null) {
                        boolean above = event.getY() < view.getHeight() / 2f;
                        int landing = above ? target : target + 1;
                        // Taking the row out first shifts everything below it up by one.
                        if (landing > draggingIndex) {
                            landing--;
                        }
                        move(draggingIndex, landing);
                        draggingIndex = null;
                    }
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    draggingIndex = null;
                    showDropAt(null);
                    return true;
                default:
                    return false;
            }
        };
    }

    /**
     * Thickens the rule where the carried row would be put down, and thins the one it was on. Rule
     * {@code i} sits above row {@code i}; the rule after the last row is where "append" lands.
     */
    private void showDropAt(Integer gap) {
        if (gap != null && gap.equals(dropTarget)) {
            return;
        }
        dropTarget = gap;
        for (int i = 0; i < dividers.size(); i++) {
            boolean active = gap != null && i == gap;
            View line = dividers.get(i);
            android.view.ViewGroup.LayoutParams params = line.getLayoutParams();
            params.height = dp(active ? 4 : 1);
            line.setLayoutParams(params);
            line.setAlpha(active ? 1f : 0.35f);
        }
    }

    /** One horizontal rule, painted in the theme's own text colour so it is visible in either scheme. */
    private View divider() {
        View line = new View(this);
        TypedValue colour = new TypedValue();
        int ink = getTheme().resolveAttribute(android.R.attr.textColorPrimary, colour, true)
            ? getResources().getColor(colour.resourceId)
            : 0xFF808080;
        line.setBackgroundColor(ink);
        line.setAlpha(0.35f);
        dividers.add(line);
        return line;
    }

    private LinearLayout.LayoutParams dividerParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        params.leftMargin = dp(8);
        params.rightMargin = dp(8);
        return params;
    }

    private void move(int from, int to) {
        store(new ArrayList<>(ActionBarSlots.moved(slots(), from, to)));
    }

    private void remove(int index) {
        List<BarSlot> slots = new ArrayList<>(slots());
        if (index >= 0 && index < slots.size()) {
            slots.remove(index);
            store(slots);
        }
    }

    private void add(BarSlot slot) {
        List<BarSlot> slots = new ArrayList<>(slots());
        slots.add(slot);
        store(slots);
    }

    private void replace(int index, BarSlot slot) {
        List<BarSlot> slots = new ArrayList<>(slots());
        if (index >= 0 && index < slots.size()) {
            slots.set(index, slot);
            store(slots);
        }
    }

    private void edit(BarSlot slot, int index) {
        if (slot.kind() == BarSlot.Kind.TEXT) {
            askForText(index);
        } else if (slot.kind() == BarSlot.Kind.CHORD) {
            askForChord(index);
        }
    }

    // ---- adding a piece of text ----

    /** @param index the slot being edited, or null to add a new one */
    private void askForText(Integer index) {
        BarSlot existing = index == null ? null : slots().get(index);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        form.setPadding(pad, pad, pad, 0);

        final EditText text = new EditText(this);
        text.setHint(R.string.bar_settings_text_hint);
        text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        if (existing != null) {
            text.setText(existing.text());
        }
        form.addView(text, matchWidth());

        final EditText label = new EditText(this);
        label.setHint(R.string.bar_settings_label_hint);
        label.setInputType(InputType.TYPE_CLASS_TEXT);
        if (existing != null && existing.customLabel() != null) {
            label.setText(existing.customLabel());
        }
        form.addView(label, matchWidth());

        new AlertDialog.Builder(this)
            .setTitle(R.string.bar_settings_add_text)
            .setView(form)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                String typed = text.getText().toString();
                if (typed.isEmpty()) {
                    return;
                }
                BarSlot slot = BarSlot.text(typed, label.getText().toString().trim());
                if (index == null) {
                    add(slot);
                } else {
                    replace(index, slot);
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    // ---- adding a key combination ----

    /** @param index the slot being edited, or null to add a new one */
    private void askForChord(final Integer index) {
        BarSlot existing = index == null ? null : slots().get(index);
        final Set<KeyModifier> modifiers = new LinkedHashSet<>();
        if (existing != null) {
            modifiers.addAll(existing.modifiers());
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        form.setPadding(pad, pad, pad, 0);

        LinearLayout modifierRow = new LinearLayout(this);
        modifierRow.setOrientation(LinearLayout.HORIZONTAL);
        for (final KeyModifier modifier : KeyModifier.values()) {
            CheckBox box = new CheckBox(this);
            box.setText(modifier.name().charAt(0) + modifier.name().substring(1).toLowerCase(
                java.util.Locale.US));
            box.setChecked(modifiers.contains(modifier));
            box.setOnCheckedChangeListener((b, checked) -> {
                if (checked) {
                    modifiers.add(modifier);
                } else {
                    modifiers.remove(modifier);
                }
            });
            modifierRow.addView(box, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }
        form.addView(modifierRow, matchWidth());

        final List<RawKey> keys = BarSlot.chordKeys();
        final String[] keyNames = new String[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            keyNames[i] = BarSlot.keyName(keys.get(i));
        }
        final int[] chosen = {existing != null && existing.key() != null
            ? keys.indexOf(existing.key()) : keys.indexOf(RawKey.ESCAPE)};

        final Button key = new Button(this);
        key.setAllCaps(false);
        key.setText(getString(R.string.bar_settings_key_is, keyNames[Math.max(0, chosen[0])]));
        key.setOnClickListener(view -> new AlertDialog.Builder(this)
            .setTitle(R.string.bar_settings_pick_key)
            .setItems(keyNames, (dialog, which) -> {
                chosen[0] = which;
                key.setText(getString(R.string.bar_settings_key_is, keyNames[which]));
            })
            .show());
        form.addView(key, matchWidth());

        final EditText label = new EditText(this);
        label.setHint(R.string.bar_settings_label_hint);
        label.setInputType(InputType.TYPE_CLASS_TEXT);
        if (existing != null && existing.customLabel() != null) {
            label.setText(existing.customLabel());
        }
        form.addView(label, matchWidth());

        form.addView(hint(R.string.bar_settings_chord_note));

        new AlertDialog.Builder(this)
            .setTitle(R.string.bar_settings_add_chord)
            .setView(form)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                if (chosen[0] < 0) {
                    return;
                }
                BarSlot slot = BarSlot.chord(
                    keys.get(chosen[0]), modifiers, label.getText().toString().trim());
                if (index == null) {
                    add(slot);
                } else {
                    replace(index, slot);
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    // ---- storage ----

    private List<BarSlot> slots() {
        return ActionBarSlots.parse(prefs().getString(ActionBarSlots.KEY_SLOTS, null));
    }

    private void store(List<BarSlot> slots) {
        if (slots.isEmpty()) {
            // An empty bar reads back as the default, which would look like the slots came back by
            // themselves; emptiness is the bar being off.
            prefs().edit().putBoolean(ActionBarSlots.KEY_ENABLED, false).apply();
        }
        prefs().edit().putString(ActionBarSlots.KEY_SLOTS, ActionBarSlots.format(slots)).apply();
        refresh();
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    // ---- small views ----

    private TextView header(int textRes) {
        TextView view = new TextView(this);
        view.setText(textRes);
        Compat.setTextAppearance(view, android.R.style.TextAppearance_DeviceDefault_Large);
        view.setPadding(0, dp(24), 0, dp(2));
        return view;
    }

    private TextView hint(int textRes) {
        TextView view = new TextView(this);
        view.setText(textRes);
        Compat.setTextAppearance(view, android.R.style.TextAppearance_DeviceDefault_Small);
        view.setPadding(0, dp(4), 0, dp(8));
        return view;
    }

    private Button wideButton(int textRes, View.OnClickListener onClick) {
        Button button = new Button(this);
        button.setText(textRes);
        button.setAllCaps(false);
        button.setOnClickListener(onClick);
        button.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return button;
    }

    private Button glyphButton(String glyph, boolean usable, View.OnClickListener onClick) {
        Button button = new Button(this, null, android.R.attr.borderlessButtonStyle);
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

    private LinearLayout.LayoutParams moveParams(boolean first) {
        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(dp(MOVE_BUTTON_DP), dp(MOVE_BUTTON_DP));
        params.leftMargin = dp(first ? 8 : 4);
        params.rightMargin = first ? 0 : dp(4);
        return params;
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
