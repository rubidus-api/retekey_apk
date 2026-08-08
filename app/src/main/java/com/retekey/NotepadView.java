package com.retekey;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * The notepad that opens above the keyboard: a list of notes, and one note open for writing.
 *
 * <p>It is a panel of the keyboard rather than an app of its own, so what is typed on the keys
 * comes to it instead of to the editor behind — the service routes the keystrokes here while it is
 * open. The panel is translucent because what is underneath is the thing you are usually copying
 * from; a note taken while reading is worth more if you can still see what you were reading.
 *
 * <p>The list behaves the way a file manager's details view behaves: a header per column that
 * sorts by it and turns the sort around when pressed again, a checkbox per row, and a checkbox in
 * the header that takes all of them or none.
 *
 * <p>A note's first line is its stamp and its title, and neither is part of the body — which is
 * why "select all" here selects the body alone. The stamp is written when the note is made and
 * never edited: it is the note's name as much as its date.
 */
public final class NotepadView extends LinearLayout {
    /** What the panel is showing. */
    private enum Screen { LIST, NOTE }

    private final KeyboardPalette palette;
    private NoteList notes;
    private Screen screen = Screen.LIST;
    private String openStamp;

    private final LinearLayout listScreen;
    private final LinearLayout listRows;
    private final LinearLayout noteScreen;
    private final CheckBox selectAll;
    private final Button sortByStamp;
    private final Button sortByTitle;
    private final TextView stampLabel;
    private final EditText titleField;
    private final EditText bodyField;

    private Runnable onClose;
    private Runnable onChanged;
    /**
     * How many characters at the cursor are a syllable still being composed. The notepad has no
     * editor to hold a preedit for it, so it keeps the composing text in the note itself and
     * rewrites those characters as the syllable grows.
     */
    private int preeditLength;

    public NotepadView(Context context, NoteList initial) {
        super(context);
        this.palette = KeyboardPalette.resolve(context);
        this.notes = initial == null ? NoteList.empty() : initial;
        setOrientation(VERTICAL);
        // Translucent: the panel is over an app the user is often reading from.
        setBackgroundColor(withAlpha(palette.background, 0xE0));

        listScreen = new LinearLayout(context);
        listScreen.setOrientation(VERTICAL);
        listRows = new LinearLayout(context);
        listRows.setOrientation(VERTICAL);

        LinearLayout tools = row(context);
        tools.addView(toolButton(context, "New", new OnClickListener() {
            @Override
            public void onClick(View v) {
                newNote();
            }
        }));
        tools.addView(toolButton(context, "Del", new OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSelected();
            }
        }));
        tools.addView(toolButton(context, "DelAll", new OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAll();
            }
        }));
        tools.addView(spacer(context));
        tools.addView(toolButton(context, "Close", new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onClose != null) {
                    onClose.run();
                }
            }
        }));
        listScreen.addView(tools, wide());

        LinearLayout header = row(context);
        selectAll = new CheckBox(context);
        selectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                notes = notes.toggledSelectAll();
                refresh();
            }
        });
        header.addView(selectAll, cell(dp(34)));
        header.addView(spacer(context, dp(56)));
        sortByStamp = headerButton(context, NoteList.Sort.STAMP);
        header.addView(sortByStamp, cell(dp(120)));
        sortByTitle = headerButton(context, NoteList.Sort.TITLE);
        header.addView(sortByTitle, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        listScreen.addView(header, wide());

        ScrollView scroller = new ScrollView(context);
        scroller.addView(listRows);
        listScreen.addView(scroller, new LayoutParams(
            LayoutParams.MATCH_PARENT, 0, 1f));
        addView(listScreen, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        noteScreen = new LinearLayout(context);
        noteScreen.setOrientation(VERTICAL);
        noteScreen.setVisibility(GONE);

        LinearLayout noteTools = row(context);
        noteTools.addView(toolButton(context, "List", new OnClickListener() {
            @Override
            public void onClick(View v) {
                showList();
            }
        }));
        noteTools.addView(toolButton(context, "SelA", new OnClickListener() {
            @Override
            public void onClick(View v) {
                selectBody();
            }
        }));
        noteTools.addView(spacer(context));
        noteTools.addView(toolButton(context, "Close", new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onClose != null) {
                    onClose.run();
                }
            }
        }));
        noteScreen.addView(noteTools, wide());

        // The first line: the stamp, which cannot be edited, and the title, which can.
        LinearLayout firstLine = row(context);
        stampLabel = new TextView(context);
        stampLabel.setTextColor(palette.keyText);
        stampLabel.setTypeface(android.graphics.Typeface.MONOSPACE);
        firstLine.addView(stampLabel, cell(dp(140)));
        titleField = field(context, "title");
        titleField.setSingleLine(true);
        firstLine.addView(titleField, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        noteScreen.addView(firstLine, wide());

        bodyField = field(context, "");
        bodyField.setGravity(Gravity.TOP | Gravity.START);
        ScrollView bodyScroller = new ScrollView(context);
        bodyScroller.addView(bodyField, new ScrollView.LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        noteScreen.addView(bodyScroller, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        addView(noteScreen, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        refresh();
    }

    /** Called when the panel should be taken down. */
    public void setOnClose(Runnable listener) {
        this.onClose = listener;
    }

    /** Called whenever the notes change, so the host can write them out. */
    public void setOnChanged(Runnable listener) {
        this.onChanged = listener;
    }

    public NoteList notes() {
        return notes;
    }

    /** Whether a note is open for writing, which is when keystrokes belong here. */
    public boolean isWriting() {
        return screen == Screen.NOTE;
    }

    // ---- what the keyboard sends while a note is open ----

    /** Types text into whichever field the cursor is in. */
    public void type(String text) {
        EditText target = focusedField();
        int start = Math.max(0, target.getSelectionStart());
        int end = Math.max(start, target.getSelectionEnd());
        target.getText().replace(start, end, text);
        preeditLength = 0;
        storeOpenNote();
    }

    /**
     * Replaces the syllable being composed with what it has become: the finished text, then
     * whatever is still composing. Called for every jamo, so Hangul builds up in the note the same
     * way it builds up in an app.
     */
    public void typeComposed(String commit, String preedit) {
        EditText target = focusedField();
        Editable editable = target.getText();
        int end = Math.max(0, target.getSelectionEnd());
        int start = Math.max(0, end - preeditLength);
        editable.replace(start, end, commit + preedit);
        preeditLength = preedit.length();
        storeOpenNote();
    }

    /** Whether a syllable is part-written at the cursor. */
    public boolean isComposing() {
        return preeditLength > 0;
    }

    /** Deletes backwards, or the selection when there is one. */
    public void deleteBackward() {
        EditText target = focusedField();
        Editable editable = target.getText();
        int start = Math.max(0, target.getSelectionStart());
        int end = Math.max(start, target.getSelectionEnd());
        if (start != end) {
            editable.delete(start, end);
        } else if (start > 0) {
            editable.delete(start - 1, start);
        }
        preeditLength = 0;
        storeOpenNote();
    }

    /** Enter: a new line in the body, and a jump into the body from the title. */
    public void newLine() {
        if (titleField.hasFocus()) {
            bodyField.requestFocus();
            bodyField.setSelection(bodyField.getText().length());
            return;
        }
        type("\n");
    }

    /** Selects the body — the note without its first line, which is what "all" means here. */
    public void selectBody() {
        bodyField.requestFocus();
        bodyField.setSelection(0, bodyField.getText().length());
    }

    private EditText focusedField() {
        return titleField.hasFocus() ? titleField : bodyField;
    }

    // ---- the list ----

    private void newNote() {
        Note note = new Note(Note.stampOf(System.currentTimeMillis()), "", "");
        notes = notes.added(note);
        changed();
        open(note.stamp());
    }

    private void deleteSelected() {
        notes = notes.withoutSelected();
        changed();
        refresh();
    }

    private void deleteAll() {
        notes = notes.cleared();
        changed();
        refresh();
    }

    /**
     * Opens the first note, for the debug screenshot screen: the note screen is reached by tapping
     * a row, and a picture has no finger. Package-private, and referenced only from the
     * instrumentation source set.
     */
    void showPreviewNote() {
        if (!notes.isEmpty()) {
            open(notes.notes().get(0).stamp());
        }
    }

    /** Forgets any half-written syllable, e.g. when the panel changes screens. */
    public void endComposition() {
        preeditLength = 0;
    }

    private void open(String stamp) {
        Note note = notes.byStamp(stamp);
        if (note == null) {
            return;
        }
        openStamp = stamp;
        screen = Screen.NOTE;
        stampLabel.setText(note.stamp());
        titleField.setText(note.title());
        bodyField.setText(note.body());
        bodyField.requestFocus();
        bodyField.setSelection(bodyField.getText().length());
        preeditLength = 0;
        refresh();
    }

    private void showList() {
        storeOpenNote();
        screen = Screen.LIST;
        openStamp = null;
        refresh();
    }

    /** Writes the open note back into the list, so nothing is lost between screens. */
    private void storeOpenNote() {
        if (openStamp == null) {
            return;
        }
        Note note = notes.byStamp(openStamp);
        if (note == null) {
            return;
        }
        Note edited = note.withTitle(titleField.getText().toString())
            .withBody(bodyField.getText().toString());
        if (!edited.equals(note)) {
            notes = notes.replaced(edited);
            changed();
        }
    }

    private void changed() {
        if (onChanged != null) {
            onChanged.run();
        }
    }

    private void refresh() {
        listScreen.setVisibility(screen == Screen.LIST ? VISIBLE : GONE);
        noteScreen.setVisibility(screen == Screen.NOTE ? VISIBLE : GONE);
        if (screen != Screen.LIST) {
            return;
        }
        selectAll.setChecked(notes.allSelected());
        sortByStamp.setText(headerText("Date", NoteList.Sort.STAMP));
        sortByTitle.setText(headerText("Title", NoteList.Sort.TITLE));
        listRows.removeAllViews();
        for (final Note note : notes.notes()) {
            listRows.addView(listRow(note), wide());
        }
        if (notes.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("No notes yet — New starts one.");
            empty.setTextColor(palette.keyTextMuted);
            empty.setPadding(dp(8), dp(12), dp(8), dp(12));
            listRows.addView(empty, wide());
        }
    }

    /** The header's own text: its name, and the arrow for the direction it is sorted in. */
    private String headerText(String name, NoteList.Sort column) {
        if (notes.sort() != column) {
            return name;
        }
        return name + (notes.ascending() ? " ▲" : " ▼");
    }

    private Button headerButton(Context context, final NoteList.Sort column) {
        Button button = new Button(context, null, android.R.attr.borderlessButtonStyle);
        button.setAllCaps(false);
        button.setPadding(dp(4), 0, dp(4), 0);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                notes = notes.sortedBy(column);
                refresh();
            }
        });
        return button;
    }

    private LinearLayout listRow(final Note note) {
        LinearLayout row = row(getContext());
        row.setMinimumHeight(dp(44));

        CheckBox tick = new CheckBox(getContext());
        tick.setChecked(notes.isSelected(note.stamp()));
        tick.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                notes = notes.toggledSelection(note.stamp());
                refresh();
            }
        });
        row.addView(tick, cell(dp(34)));

        row.addView(moveButton("▲", note.stamp(), -1), cell(dp(28)));
        row.addView(moveButton("▼", note.stamp(), 1), cell(dp(28)));

        TextView stamp = new TextView(getContext());
        stamp.setText(note.stamp());
        stamp.setTextColor(palette.keyTextMuted);
        stamp.setTypeface(android.graphics.Typeface.MONOSPACE);
        stamp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        row.addView(stamp, cell(dp(120)));

        TextView title = new TextView(getContext());
        title.setText(note.title().isEmpty() ? "(untitled)" : note.title());
        title.setTextColor(palette.keyText);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                open(note.stamp());
            }
        });
        row.addView(title, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private Button moveButton(String glyph, final String stamp, final int delta) {
        Button button = new Button(getContext(), null, android.R.attr.borderlessButtonStyle);
        button.setText(glyph);
        button.setAllCaps(false);
        button.setPadding(0, 0, 0, 0);
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                notes = notes.moved(stamp, delta);
                changed();
                refresh();
            }
        });
        return button;
    }

    // ---- small builders ----

    private LinearLayout row(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), dp(2), dp(4), dp(2));
        return row;
    }

    private Button toolButton(Context context, String label, OnClickListener listener) {
        Button button = new Button(context, null, android.R.attr.borderlessButtonStyle);
        button.setText(label);
        button.setAllCaps(false);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setMinimumWidth(0);
        button.setMinWidth(0);
        button.setOnClickListener(listener);
        return button;
    }

    private EditText field(Context context, String hint) {
        EditText edit = new EditText(context);
        edit.setHint(hint);
        edit.setTextColor(palette.keyText);
        edit.setHintTextColor(palette.keyTextMuted);
        edit.setBackgroundColor(Color.TRANSPARENT);
        edit.setFocusableInTouchMode(true);
        // The keyboard types into this itself; no system keyboard is wanted on top of it, and on
        // an IME window there is no system focus to raise one anyway.
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            edit.setShowSoftInputOnFocus(false);
        }
        return edit;
    }

    private View spacer(Context context) {
        return spacer(context, 0);
    }

    private View spacer(Context context, int width) {
        View view = new View(context);
        view.setLayoutParams(width > 0
            ? new LayoutParams(width, 1)
            : new LayoutParams(0, 1, 1f));
        return view;
    }

    private LayoutParams cell(int width) {
        return new LayoutParams(width, LayoutParams.WRAP_CONTENT);
    }

    private LayoutParams wide() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
