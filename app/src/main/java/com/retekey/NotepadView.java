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
    /** The id of the note open in the editor, or null. */
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
    /** Undo history, one per field: the title and the body are edited independently. */
    private final NoteHistory titleHistory = new NoteHistory();
    private final NoteHistory bodyHistory = new NoteHistory();
    /** Set while undo/redo writes into a field, so restoring a state is not recorded as an edit. */
    private boolean restoring;

    /** Text views that follow the pinch, with the size each was designed at. */
    private final java.util.List<TextView> scaledViews = new java.util.ArrayList<>();
    private final java.util.List<Float> scaledBases = new java.util.ArrayList<>();
    private final android.view.ScaleGestureDetector pinch;
    private int textPercent;
    private int pinchStartPercent;

    private Runnable onClose;
    private Runnable onChanged;
    private TextTaker onSendToApp;
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
        textPercent = NotepadTextScale.clamp(prefs(context)
            .getInt(TEXT_PERCENT_KEY, NotepadTextScale.DEFAULT_PERCENT));
        // Two fingers set the text size, on both screens. One finger is left alone: it belongs to
        // the list rows and to the cursor in the note.
        pinch = new android.view.ScaleGestureDetector(context,
            new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScaleBegin(android.view.ScaleGestureDetector detector) {
                    pinchStartPercent = textPercent;
                    return true;
                }

                @Override
                public boolean onScale(android.view.ScaleGestureDetector detector) {
                    setTextPercent(
                        NotepadTextScale.scaled(pinchStartPercent, detector.getScaleFactor()));
                    return true;
                }

                @Override
                public void onScaleEnd(android.view.ScaleGestureDetector detector) {
                    // Written when the fingers lift rather than on every frame of the gesture.
                    prefs(getContext()).edit().putInt(TEXT_PERCENT_KEY, textPercent).apply();
                }
            });
        // Opaque, as the clipboard list is: two panels that take the same place over an app should
        // not differ in how much of it they let through (issue #14). It was translucent, and in
        // an app that paints its own picture behind the keyboard that picture came through the
        // notes and not through the clipboard.
        setBackgroundColor(palette.background);

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

        // One row of links, wrapping onto a second when the screen is too narrow for them all.
        // Close comes last because it is where the row ends, not because it is least used.
        FlowRow noteLinks = new FlowRow(context);
        noteLinks.addView(toolButton(context, "List", new OnClickListener() {
            @Override
            public void onClick(View v) {
                showList();
            }
        }));
        noteLinks.addView(toolButton(context, "SelA", new OnClickListener() {
            @Override
            public void onClick(View v) {
                selectBody();
            }
        }));

        // The editing set, on the selection when there is one and on the whole field when there
        // is not. The keyboard's own edit keys, Ctrl chords and the action bar do the same here
        // (applyKey); these links stay for a hand that is already on the panel.
        noteLinks.addView(toolButton(context, "Cp", new OnClickListener() {
            @Override
            public void onClick(View v) {
                copySelection(false);
            }
        }));
        noteLinks.addView(toolButton(context, "Cut", new OnClickListener() {
            @Override
            public void onClick(View v) {
                copySelection(true);
            }
        }));
        noteLinks.addView(toolButton(context, "Paste", new OnClickListener() {
            @Override
            public void onClick(View v) {
                pasteClipboard();
            }
        }));
        // Into the app, not into the note: the selection if there is one, otherwise the whole
        // note, typed at the app's own cursor the way a key is — so it lands in a terminal or a
        // remote desktop as well, where a paste would not (the user asked for this, 2026-09-16).
        noteLinks.addView(toolButton(context, "Send", new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendToApp();
            }
        }));
        noteLinks.addView(toolButton(context, "Del", new OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteBackward();
            }
        }));
        noteLinks.addView(toolButton(context, "Un", new OnClickListener() {
            @Override
            public void onClick(View v) {
                undoEdit();
            }
        }));
        noteLinks.addView(toolButton(context, "Re", new OnClickListener() {
            @Override
            public void onClick(View v) {
                redoEdit();
            }
        }));
                noteLinks.addView(toolButton(context, "Close", new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onClose != null) {
                    onClose.run();
                }
            }
        }));
        noteScreen.addView(noteLinks, wide());

        // The first line: the stamp, which cannot be edited, and the title, which can.
        LinearLayout firstLine = row(context);
        stampLabel = new TextView(context);
        stampLabel.setTextColor(palette.keyText);
        stampLabel.setTypeface(android.graphics.Typeface.MONOSPACE);
        scaleText(stampLabel, 12f);
        firstLine.addView(stampLabel, cell(dp(140)));
        titleField = field(context, "title");
        titleField.setSingleLine(true);
        scaleText(titleField, 16f);
        firstLine.addView(titleField, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        noteScreen.addView(firstLine, wide());

        bodyField = field(context, "");
        bodyField.setGravity(Gravity.TOP | Gravity.START);
        scaleText(bodyField, 16f);
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

    /** Takes a piece of text out of the panel and into whatever the keyboard is typing into. */
    public interface TextTaker {
        void take(String text);
    }

    /**
     * Called when the user asks for the note to be typed into the app at its cursor. The host
     * sends it the way a key does, so it arrives in a terminal or a remote desktop too.
     */
    public void setOnSendToApp(TextTaker listener) {
        this.onSendToApp = listener;
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
        recordState();
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
    /** The character just before the cursor in the focused field, or null when there is none. */
    public String lastCharacter() {
        EditText target = focusedField();
        int start = target.getSelectionStart();
        if (start <= 0 || target.getSelectionEnd() != start) {
            return null;
        }
        return String.valueOf(target.getText().charAt(start - 1));
    }

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
        recordState();
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

    /**
     * Carries out an editing key on the focused field — the chords and movement keys that, before
     * this, went past the note to the app behind it. {@code extend} is Shift: a movement grows the
     * selection instead of collapsing it.
     */
    public void applyKey(NotepadKeys.Command command, boolean extend) {
        EditText target = focusedField();
        Editable editable = target.getText();
        switch (command) {
            case SELECT_ALL:
                selectBodyOrTitle(target);
                return;
            case COPY:
                copySelection(false);
                return;
            case CUT:
                copySelection(true);
                return;
            case PASTE:
                pasteClipboard();
                return;
            case UNDO:
                undoEdit();
                return;
            case REDO:
                redoEdit();
                return;
            case TAB:
                type("\t");
                return;
            case BACKSPACE:
                deleteBackward();
                return;
            case FORWARD_DELETE:
                deleteForward();
                return;
            case NEW_LINE:
                newLine();
                return;
            default:
                break;
        }
        if (!NotepadKeys.isMovement(command)) {
            return;
        }
        preeditLength = 0;
        int anchor = Math.max(0, target.getSelectionStart());
        int cursor = Math.max(0, target.getSelectionEnd());
        int length = editable.length();
        android.text.Layout layout = target.getLayout();
        int moved;
        switch (command) {
            case LEFT:
                moved = !extend && anchor != cursor ? Math.min(anchor, cursor) : cursor - 1;
                break;
            case RIGHT:
                moved = !extend && anchor != cursor ? Math.max(anchor, cursor) : cursor + 1;
                break;
            case WORD_LEFT:
                moved = NotepadKeys.wordLeft(editable, cursor);
                break;
            case WORD_RIGHT:
                moved = NotepadKeys.wordRight(editable, cursor);
                break;
            case UP:
            case DOWN:
            case PAGE_UP:
            case PAGE_DOWN:
                moved = verticalMove(layout, cursor, command, length);
                break;
            case LINE_START:
                moved = layout == null ? lineEdge(editable, cursor, false)
                    : layout.getLineStart(layout.getLineForOffset(cursor));
                break;
            case LINE_END:
                moved = layout == null ? lineEdge(editable, cursor, true)
                    : visibleLineEnd(layout, editable, cursor);
                break;
            case TEXT_START:
                moved = 0;
                break;
            default:
                moved = length;
                break;
        }
        moved = Math.max(0, Math.min(length, moved));
        if (extend) {
            target.setSelection(anchor, moved);
        } else {
            target.setSelection(moved);
        }
    }

    /** Runs a context-menu editing command — the action bar's and the ☰ page's edit keys. */
    public boolean editCommand(int contextMenuId) {
        if (contextMenuId == android.R.id.selectAll) {
            applyKey(NotepadKeys.Command.SELECT_ALL, false);
        } else if (contextMenuId == android.R.id.copy) {
            applyKey(NotepadKeys.Command.COPY, false);
        } else if (contextMenuId == android.R.id.cut) {
            applyKey(NotepadKeys.Command.CUT, false);
        } else if (contextMenuId == android.R.id.paste) {
            applyKey(NotepadKeys.Command.PASTE, false);
        } else if (android.os.Build.VERSION.SDK_INT >= 24
                && contextMenuId == android.R.id.undo) {
            applyKey(NotepadKeys.Command.UNDO, false);
        } else if (android.os.Build.VERSION.SDK_INT >= 24
                && contextMenuId == android.R.id.redo) {
            applyKey(NotepadKeys.Command.REDO, false);
        } else {
            return false;
        }
        return true;
    }

    /** Selects the word the cursor is in, the action bar's word key. */
    public void selectWord() {
        EditText target = focusedField();
        Editable editable = target.getText();
        int cursor = Math.max(0, target.getSelectionEnd());
        int start = NotepadKeys.wordLeft(editable, cursor);
        if (cursor < editable.length() && Character.isLetterOrDigit(editable.charAt(cursor))) {
            start = cursor == 0 || !Character.isLetterOrDigit(editable.charAt(cursor - 1))
                ? cursor : start;
        }
        int end = NotepadKeys.wordRight(editable, start);
        if (end > start) {
            target.setSelection(start, end);
        }
    }

    private void selectBodyOrTitle(EditText target) {
        if (target == titleField) {
            titleField.setSelection(0, titleField.getText().length());
        } else {
            selectBody();
        }
    }

    private void deleteForward() {
        EditText target = focusedField();
        Editable editable = target.getText();
        int start = Math.max(0, target.getSelectionStart());
        int end = Math.max(start, target.getSelectionEnd());
        if (start != end) {
            editable.delete(start, end);
        } else if (end < editable.length()) {
            editable.delete(start, start + 1);
        } else {
            return;
        }
        preeditLength = 0;
        recordState();
        storeOpenNote();
    }

    private int verticalMove(android.text.Layout layout, int cursor,
                             NotepadKeys.Command command, int length) {
        if (layout == null) {
            return command == NotepadKeys.Command.UP || command == NotepadKeys.Command.PAGE_UP
                ? 0 : length;
        }
        int line = layout.getLineForOffset(cursor);
        int step = command == NotepadKeys.Command.UP || command == NotepadKeys.Command.DOWN
            ? 1 : Math.max(1, visibleLines());
        boolean up = command == NotepadKeys.Command.UP || command == NotepadKeys.Command.PAGE_UP;
        int target = up ? line - step : line + step;
        if (target < 0) {
            return 0;
        }
        if (target >= layout.getLineCount()) {
            return length;
        }
        return layout.getOffsetForHorizontal(target, layout.getPrimaryHorizontal(cursor));
    }

    private int visibleLines() {
        EditText target = focusedField();
        int lineHeight = Math.max(1, target.getLineHeight());
        View scroller = (View) bodyField.getParent();
        int height = scroller == null ? target.getHeight() : scroller.getHeight();
        return Math.max(1, height / lineHeight - 1);
    }

    private static int visibleLineEnd(android.text.Layout layout, CharSequence text, int cursor) {
        int end = layout.getLineEnd(layout.getLineForOffset(cursor));
        // A wrapped or ended line reports the offset after its newline; End stops before it.
        if (end > 0 && end <= text.length() && text.charAt(end - 1) == '\n') {
            end--;
        }
        return end;
    }

    private static int lineEdge(CharSequence text, int cursor, boolean forward) {
        int i = Math.max(0, Math.min(cursor, text.length()));
        if (forward) {
            while (i < text.length() && text.charAt(i) != '\n') {
                i++;
            }
        } else {
            while (i > 0 && text.charAt(i - 1) != '\n') {
                i--;
            }
        }
        return i;
    }

    /** Selects the body — the note without its first line, which is what "all" means here. */
    public void selectBody() {
        bodyField.requestFocus();
        bodyField.setSelection(0, bodyField.getText().length());
    }

    private EditText focusedField() {
        return titleField.hasFocus() ? titleField : bodyField;
    }

    private NoteHistory historyFor(EditText field) {
        return field == titleField ? titleHistory : bodyHistory;
    }

    /** Remembers what the focused field says now, so undo has somewhere to go back to. */
    private void recordState() {
        if (restoring) {
            return;
        }
        EditText target = focusedField();
        historyFor(target).record(
            target.getText().toString(), Math.max(0, target.getSelectionStart()));
    }

    /** Starts both histories at what the note says as it opens. */
    private void resetHistories() {
        titleHistory.reset();
        bodyHistory.reset();
        titleHistory.record(titleField.getText().toString(), 0);
        bodyHistory.record(bodyField.getText().toString(), 0);
    }

    /**
     * Copies the selection, or the whole field when nothing is selected — which is what "copy the
     * body" means when the cursor is just sitting in it. Cutting is the same, and then removes it.
     */
    /**
     * Hands the note to the host to type into the app at its cursor: the selected part if the
     * user selected one, the whole note otherwise. The host closes the panel first — while the
     * notepad is up it takes every key for itself, and the text is meant for what is behind it.
     */
    private void sendToApp() {
        if (onSendToApp == null) {
            return;
        }
        EditText target = focusedField();
        Editable editable = target.getText();
        int start = Math.max(0, target.getSelectionStart());
        int end = Math.max(start, target.getSelectionEnd());
        if (start == end) {
            start = 0;
            end = editable.length();
        }
        if (end <= start) {
            return;
        }
        onSendToApp.take(editable.subSequence(start, end).toString());
    }

    private void copySelection(boolean cut) {
        EditText target = focusedField();
        Editable editable = target.getText();
        int start = Math.max(0, target.getSelectionStart());
        int end = Math.max(start, target.getSelectionEnd());
        if (start == end) {
            start = 0;
            end = editable.length();
        }
        if (end <= start) {
            return;
        }
        CharSequence taken = editable.subSequence(start, end);
        android.content.ClipboardManager clipboard = clipboard();
        if (clipboard != null) {
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("note", taken));
        }
        if (cut) {
            editable.delete(start, end);
            preeditLength = 0;
            recordState();
            storeOpenNote();
        }
    }

    /** Puts the clipboard in at the cursor, replacing the selection if there is one. */
    private void pasteClipboard() {
        android.content.ClipboardManager clipboard = clipboard();
        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            return;
        }
        android.content.ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            return;
        }
        CharSequence text = clip.getItemAt(0).coerceToText(getContext());
        if (text == null || text.length() == 0) {
            return;
        }
        type(text.toString());
    }

    private android.content.ClipboardManager clipboard() {
        return (android.content.ClipboardManager)
            getContext().getSystemService(Context.CLIPBOARD_SERVICE);
    }

    private void undoEdit() {
        applySnapshot(historyFor(focusedField()).undo());
    }

    private void redoEdit() {
        applySnapshot(historyFor(focusedField()).redo());
    }

    /** Puts a remembered state back without recording it as a new edit. */
    private void applySnapshot(NoteHistory.Snapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        EditText target = focusedField();
        restoring = true;
        try {
            target.setText(snapshot.text());
            target.setSelection(Math.min(snapshot.cursor(), target.getText().length()));
        } finally {
            restoring = false;
        }
        preeditLength = 0;
        storeOpenNote();
    }

    // ---- the list ----

    private void newNote() {
        Note note = Note.made(System.currentTimeMillis(), "", "");
        notes = notes.added(note);
        changed();
        open(note.id());
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
            open(notes.notes().get(0).id());
        }
    }

    /** Forgets any half-written syllable, e.g. when the panel changes screens. */
    public void endComposition() {
        preeditLength = 0;
    }

    private void open(String id) {
        Note note = notes.byId(id);
        if (note == null) {
            return;
        }
        openStamp = id;
        screen = Screen.NOTE;
        stampLabel.setText(note.stamp());
        titleField.setText(note.title());
        bodyField.setText(note.body());
        bodyField.requestFocus();
        bodyField.setSelection(bodyField.getText().length());
        preeditLength = 0;
        // A different note is a different history: undo must not walk back into another note.
        resetHistories();
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
        Note note = notes.byId(openStamp);
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
        button.setTextColor(palette.keyText);
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
        tick.setChecked(notes.isSelected(note.id()));
        tick.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                notes = notes.toggledSelection(note.id());
                refresh();
            }
        });
        row.addView(tick, cell(dp(34)));

        row.addView(moveButton("▲", note.id(), -1), cell(dp(28)));
        row.addView(moveButton("▼", note.id(), 1), cell(dp(28)));

        TextView stamp = new TextView(getContext());
        stamp.setText(note.stamp());
        stamp.setTextColor(palette.keyTextMuted);
        stamp.setTypeface(android.graphics.Typeface.MONOSPACE);
        scaleText(stamp, 12f);
        row.addView(stamp, cell(dp(120)));

        TextView title = new TextView(getContext());
        title.setText(note.title().isEmpty() ? "(untitled)" : note.title());
        title.setTextColor(palette.keyText);
        scaleText(title, 14f);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                open(note.id());
            }
        });
        row.addView(title, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private Button moveButton(String glyph, final String id, final int delta) {
        Button button = new Button(getContext(), null, android.R.attr.borderlessButtonStyle);
        button.setText(glyph);
        button.setTextColor(palette.keyText);
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
                notes = notes.moved(id, delta);
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
        // The platform's borderless default is a washed-out grey on this translucent panel;
        // the body text is palette-coloured, and the toolbar must read as sharply as it does.
        button.setTextColor(palette.keyText);
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

    private static final String TEXT_PERCENT_KEY = "notepad_text_percent";

    private static android.content.SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("retekey_view", Context.MODE_PRIVATE);
    }

    /** Registers a text view to follow the pinch, and sizes it for where the pinch is now. */
    private void scaleText(TextView view, float baseSp) {
        // Rows are rebuilt whenever the list changes; forget the ones that went with the old ones.
        for (int i = scaledViews.size() - 1; i >= 0; i--) {
            if (scaledViews.get(i).getParent() == null && scaledViews.get(i) != view) {
                scaledViews.remove(i);
                scaledBases.remove(i);
            }
        }
        scaledViews.add(view);
        scaledBases.add(baseSp);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP,
            NotepadTextScale.sizeOf(baseSp, textPercent));
    }

    private void setTextPercent(int percent) {
        int clamped = NotepadTextScale.clamp(percent);
        if (clamped == textPercent) {
            return;
        }
        textPercent = clamped;
        for (int i = 0; i < scaledViews.size(); i++) {
            scaledViews.get(i).setTextSize(TypedValue.COMPLEX_UNIT_SP,
                NotepadTextScale.sizeOf(scaledBases.get(i), textPercent));
        }
    }

    @Override
    public boolean onInterceptTouchEvent(android.view.MotionEvent event) {
        pinch.onTouchEvent(event);
        // Only a second finger takes the gesture away from the children.
        return event.getPointerCount() >= 2 || pinch.isInProgress();
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        pinch.onTouchEvent(event);
        return true;
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
