package com.retekey;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.widget.Toast;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReteKeyImeService extends InputMethodService {
    private final HangulInputProcessor inputProcessor =
        new HangulInputProcessor(this::currentEditorProfile, this::characterBeforeCursor);
    private final InputDispatcher dispatcher = new InputDispatcher(inputProcessor);
    private final InputSessionController<ScaffoldSessionState> sessionController =
        new InputSessionController<>();
    private HardwareSemanticMapper hardwareMapper = HardwareSemanticMapper.none();
    private EditorProfile editorProfile = EditorProfile.unsupported();
    private boolean sessionActive;
    private Toast editorFailureToast;
    private ReteKeyboardView keyboardView;
    private SoftKeyboardVisibilityPolicy.Mode softKeyboardMode =
        SoftKeyboardVisibilityPolicy.Mode.HIDE_WHEN_HARDWARE;
    // Whether physical-keyboard letter keys compose Hangul; toggled by a user-bound 한/영 key and
    // initialised from the current subtype. A bound 한자 key runs the same conversion as the
    // on-screen one.
    private boolean hardwareKoreanMode;
    private List<HardwareKeyBindings.Binding> hanyeongBindings = java.util.Collections.emptyList();
    private List<HardwareKeyBindings.Binding> hanjaBindings = java.util.Collections.emptyList();
    private List<HardwareKeyBindings.Binding> unicodeBindings = java.util.Collections.emptyList();
    /** The code point being typed on the U+ key, or null when that entry is not open. */
    private UnicodeEntry unicodeEntry;
    private Toast functionToast;
    private static final int HANJA_LOOKBEHIND = 8;
    /** The candidate list while it is up, living in a floating panel of its own. */
    private HanjaCandidatesView hanjaView;
    private String pendingReading;
    private List<HanjaCandidatesView.Item> pendingCandidates;
    private boolean pendingFromSelection;
    private int pendingDeleteLength;
    private boolean hanjaCandidatesShown;
    private final android.os.Handler mainHandler =
        new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean floatingMode;
    /** The notepad panel above the keyboard, or null when it is not open. */
    private NotepadView notepad;
    /** The notepad's own Hangul composer: what is typed there is not going through the editor. */
    private final HangulComposer notepadComposer = new HangulComposer();
    /** The notepad's own Latin composers, used while their layout is the one showing. */
    private final TelexComposer notepadTelex = new TelexComposer();
    private final RomajiKanaComposer notepadRomaji = new RomajiKanaComposer();
    /** The editor's Latin composers; one is handed to the processor while its layout is up. */
    private final TelexComposer telex = new TelexComposer();
    private final RomajiKanaComposer romaji = new RomajiKanaComposer();
    /** The orientation the current input view was built for; a rotation rebuilds it. */
    private ScreenOrientation builtFor;
    private FloatingKeyboardBounds floatingBounds;
    private FloatingKeyboardBounds unicodeBounds;
    private FloatingKeyboardBounds hanjaBounds;
    /** True while the candidate list is the floating panel, in place of the keyboard. */
    private boolean hanjaFloating;
    private FloatingKeyboardFrame floatingFrame;
    /** True while the code-point pad is the floating panel, in place of the keyboard. */
    private boolean unicodeFloating;
    /**
     * Rebuilds the input view when the action bar is turned on or off, or its slots change.
     *
     * <p>The framework builds the input view once and keeps it: without this, ticking "Show the
     * action bar" in settings did nothing visible until the keyboard happened to be rebuilt for
     * some other reason — a rotation, a restart — which is exactly what it looked like on a Note 20.
     */
    private final SharedPreferences.OnSharedPreferenceChangeListener barPrefsListener =
        (changed, key) -> {
            if (ActionBarSlots.KEY_ENABLED.equals(key) || ActionBarSlots.KEY_SLOTS.equals(key)) {
                rebuildInputView();
            }
        };

    /** The clipboard panel while it is open, or null. */
    private ClipboardPanelView clipboardPanel;
    /** Whether the input view now on screen was built with the action bar. */
    private boolean builtWithBar;
    /** The frame that reserves the system's bottom band, so it can be asked to measure again. */
    private SystemBandFrame bandFrame;
    /** What the keyboard remembers of what was cut and copied through it. */
    private ClipHistory clips = ClipHistory.empty();

    @Override
    public View onCreateInputView() {
        // Everything the IME shows goes inside the band frame, which keeps the system's own bottom
        // buttons — hide keyboard, switch keyboard — off the keys. Issue #1: without it the bottom
        // row is drawn underneath them and cannot be pressed at all.
        bandFrame = new SystemBandFrame(this, buildInputView());
        return bandFrame;
    }

    private View buildInputView() {
        builtFor = OrientedPrefs.current(this);
        // The code-point pad floats whatever the keyboard is doing: it is not a page of the
        // keyboard but a small panel of its own, and it replaces the keyboard while it is open.
        floatingMode = unicodeFloating || hanjaFloating
            || FloatingKeyboardSettings.isEnabled(viewPrefs(), builtFor);
        keyboardView = new ReteKeyboardView(this, this::dispatchSoftwareInput);
        keyboardView.setOnOpenSettings(this::openSettings);
        keyboardView.setOnEditCommand(this::performEditCommand);
        keyboardView.setOnInsertDate(this::insertCurrentDate);
        keyboardView.setOnSwitchIme(this::showImePicker);
        keyboardView.setOnManageIme(this::openKeyboardManagement);
        keyboardView.setOnHanja(this::handleHanja);
        keyboardView.setOnUnicodeInput(this::startUnicodeEntry);
        keyboardView.setOnNotepad(this::toggleNotepad);
        keyboardView.setOnFloatingToggle(this::toggleFloatingMode);
        keyboardView.setOnThemeCycle(this::cycleTheme);
        keyboardView.setOnKanaModifier(this::applyKanaModifier);
        keyboardView.setOnLayoutChanged(this::announceLayout);
        reloadHardwareBindings();
        HanjaDictionary.preload(this);
        if (clipboardPanel != null) {
            // The clipboard owns the window while it is open, the way the notepad does.
            floatingFrame = null;
            return new PanelFrame(this, buildClipboardPanel(), withActionBar(keyboardView));
        }
        if (notepad != null) {
            // The notepad owns the window while it is open: the keyboard keeps its height at the
            // bottom and the panel takes the rest of the screen.
            floatingFrame = null;
            NotepadView panel = buildNotepad();
            return new NotepadFrame(this, panel, withActionBar(keyboardView));
        }
        if (!floatingMode) {
            floatingFrame = null;
            return withActionBar(keyboardView);
        }
        // The bar rides on the floating panel too: it is part of the keyboard, not part of being
        // docked, and a floating keyboard is where reaching for the menu page costs most.
        floatingFrame = new FloatingKeyboardFrame(this, withActionBar(keyboardView));
        // One opacity for floating panels, whichever panel it is: how see-through something
        // hovering over your document should be is a single preference, not one per panel.
        floatingFrame.setOpacityPercent(
            FloatingKeyboardSettings.opacityPercent(viewPrefs(), OrientedPrefs.current(this)));
        if (hanjaFloating) {
            hanjaView = new HanjaCandidatesView(this);
            hanjaView.setOnPick(this::commitHanja);
            hanjaView.setOnDismiss(this::hideHanjaCandidates);
            hanjaView.show(pendingReading, pendingCandidates);
            floatingFrame = new FloatingKeyboardFrame(this, hanjaView);
            floatingFrame.setOpacityPercent(
                FloatingKeyboardSettings.opacityPercent(viewPrefs(), OrientedPrefs.current(this)));
            if (hanjaBounds == null) {
                hanjaBounds = FloatingKeyboardSettings.load(
                    viewPrefs(), FloatingKeyboardSettings.HANJA_PREFIX);
            }
            floatingFrame.setOnClose(this::hideHanjaCandidates);
            floatingFrame.setOnBoundsChanged(this::onHanjaBoundsChanged);
            if (hanjaBounds != null) {
                floatingFrame.setBounds(hanjaBounds);
            }
            return floatingFrame;
        }
        if (unicodeFloating) {
            keyboardView.setUnicodeEntry(true);
            if (unicodeBounds == null) {
                unicodeBounds = FloatingKeyboardSettings.load(
                    viewPrefs(), FloatingKeyboardSettings.UNICODE_PREFIX);
            }
            // The pad's ✕ is the way out of the entry, not a way to turn the floating keyboard off.
            floatingFrame.setOnClose(this::endUnicodeEntry);
            floatingFrame.setOnBoundsChanged(this::onUnicodeBoundsChanged);
            if (unicodeBounds != null) {
                floatingFrame.setBounds(unicodeBounds);
            }
            return floatingFrame;
        }
        if (floatingBounds == null) {
            floatingBounds = FloatingKeyboardSettings.load(viewPrefs());
        }
        floatingFrame.setOnClose(this::leaveFloatingMode);
        floatingFrame.setOnBoundsChanged(this::onFloatingBoundsChanged);
        if (floatingBounds != null) {
            floatingFrame.setBounds(floatingBounds);
        }
        return floatingFrame;
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ScreenOrientation now = OrientedPrefs.current(this);
        if (now == builtFor || keyboardView == null) {
            return;
        }
        // Turning the device sideways is a different keyboard: its height, the layouts the globe
        // key visits, and whether there is a floating panel at all are settings of their own.
        setInputView(onCreateInputView());
        updateInputViewShown();
    }

    /**
     * The keyboard with the action bar above it, when the user has asked for one. The bar is a
     * strip of the actions that are not letters — selection, the clipboard, cursor movement — kept
     * visible while typing instead of behind a page change.
     */
    private View withActionBar(ReteKeyboardView keyboard) {
        builtWithBar = viewPrefs().getBoolean(
            ActionBarSlots.KEY_ENABLED, ActionBarSlots.DEFAULT_ENABLED);
        if (!builtWithBar) {
            return keyboard;
        }
        ActionBarView bar = new ActionBarView(this);
        bar.setSlots(ActionBarSlots.parse(viewPrefs().getString(ActionBarSlots.KEY_SLOTS, null)));
        bar.setRepeatTimings(
            KeyRepeatSettings.clampDelay(viewPrefs().getInt(
                KeyRepeatSettings.KEY_DELAY_MS, KeyRepeatSettings.DEFAULT_DELAY_MS)),
            KeyRepeatSettings.clampInterval(viewPrefs().getInt(
                KeyRepeatSettings.KEY_INTERVAL_MS, KeyRepeatSettings.DEFAULT_INTERVAL_MS)));
        bar.setListener(new ActionBarView.Listener() {
            @Override
            public void onAction(BarAction action) {
                performBarAction(action);
            }

            @Override
            public void onText(String text) {
                typeBarText(text);
            }

            @Override
            public void onChord(BarSlot slot) {
                sendBarChord(slot, RawKeyPhase.TAP);
            }

            @Override
            public void onChordLatch(BarSlot slot, boolean down) {
                sendBarChord(slot, down ? RawKeyPhase.HOLD : RawKeyPhase.RELEASE);
            }
        });
        return new ActionBarFrame(this, bar, keyboard);
    }

    /** One press on the action bar. */
    /** A text slot: what the user wrote, typed as if the keys had been pressed. */
    private void typeBarText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        try {
            dispatchSoftwareInput(
                ProjectKeyEvent.softwareDown("touch.bar.text", SemanticInput.text(text)));
        } catch (RuntimeException ignored) {
            // A bar press must never crash the keyboard, whatever the editor does with it.
        }
    }

    /**
     * A chord slot: a key with modifiers held, sent once on a tap and left down on a hold.
     *
     * <p>A latched chord is two halves of one press separated by however long the user leaves it
     * that way, which is what makes Shift+arrow selection or a held Ctrl possible from a screen.
     */
    private void sendBarChord(BarSlot slot, RawKeyPhase phase) {
        if (slot == null || slot.key() == null) {
            return;
        }
        try {
            dispatchSoftwareInput(ProjectKeyEvent.softwareDown(
                "touch.bar.chord",
                SemanticInput.rawKey(slot.key(), slot.modifiers(), phase)));
        } catch (RuntimeException ignored) {
            // As above: a chord the editor refuses must not take the keyboard with it.
        }
    }

    private void performBarAction(BarAction action) {
        try {
            switch (action) {
                case SELECT_WORD:
                    selectWordAroundCursor();
                    break;
                case SELECT_ALL:
                    performEditCommand(android.R.id.selectAll);
                    break;
                case CUT:
                    performEditCommand(android.R.id.cut);
                    rememberClipSoon();
                    break;
                case COPY:
                    performEditCommand(android.R.id.copy);
                    rememberClipSoon();
                    break;
                case PASTE:
                    performEditCommand(android.R.id.paste);
                    break;
                case CLIPBOARD:
                    toggleClipboardPanel();
                    break;
                case SYMBOLS:
                    if (keyboardView != null) {
                        keyboardView.showSpecialChars();
                    }
                    break;
                case NOTEPAD:
                    toggleNotepad();
                    break;
                default:
                    RawKey key = action.rawKey();
                    if (key != null) {
                        dispatchSoftwareInput(ProjectKeyEvent.softwareDown(
                            "touch.bar." + action.stored(), SemanticInput.rawKey(key)));
                    }
                    break;
            }
        } catch (RuntimeException ignored) {
            // A bar press must never crash the keyboard, whatever the editor does with it.
        }
    }

    /**
     * Selects the word the cursor is in. There is no context-menu id for it, so the keyboard reads
     * the text either side, asks {@link WordBoundary} where the word ends, and sets the selection
     * itself. Where the editor will not say where the cursor is, or there is no word touching it,
     * nothing happens — which is better than selecting the wrong thing.
     */
    private void selectWordAroundCursor() {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null) {
            return;
        }
        CharSequence before = inputConnection.getTextBeforeCursor(WORD_LOOKAROUND, 0);
        CharSequence after = inputConnection.getTextAfterCursor(WORD_LOOKAROUND, 0);
        WordBoundary word = WordBoundary.of(before, after);
        if (word.isEmpty()) {
            return;
        }
        EditorBounds bounds = sessionController.workingBounds();
        if (!bounds.hasSelection()) {
            // A terminal that never reports where the cursor is cannot be given an absolute
            // selection, and guessing one would select somewhere else entirely.
            return;
        }
        int cursor = bounds.selectionEnd();
        inputConnection.setSelection(
            Math.max(0, cursor - word.before), cursor + word.after);
    }

    /**
     * Reads what the cut or copy just put on the system clipboard, and remembers it.
     *
     * <p>A moment later, not at once: {@code performContextMenuAction} is a request to the editor,
     * and the clipboard is only what the editor made of it once it has acted. Nothing is remembered
     * from a password or otherwise sensitive field — a clipboard list is exactly the sort of place
     * those must not survive in.
     */
    private void rememberClipSoon() {
        if (editorProfile.capabilities().isSensitive()) {
            return;
        }
        mainHandler.postDelayed(this::rememberCurrentClip, CLIP_READ_DELAY_MS);
    }

    private void rememberCurrentClip() {
        try {
            android.content.ClipboardManager manager = Compat.systemService(
                this, Context.CLIPBOARD_SERVICE, android.content.ClipboardManager.class);
            if (manager == null || !manager.hasPrimaryClip()) {
                return;
            }
            android.content.ClipData data = manager.getPrimaryClip();
            if (data == null || data.getItemCount() == 0) {
                return;
            }
            CharSequence text = data.getItemAt(0).coerceToText(this);
            ClipHistory updated = clips.record(text, editorProfile.capabilities().isSensitive());
            if (updated != clips) {
                clips = updated;
                ClipStore.save(this, clips);
            }
        } catch (RuntimeException ignored) {
            // Reading the clipboard is best-effort: a ROM that refuses must not break Copy.
        }
    }

    /** How long the editor is given to act on a cut or copy before the clipboard is read. */
    private static final int CLIP_READ_DELAY_MS = 120;

    /** Opens the clipboard list, or closes it if it is already open. */
    private void toggleClipboardPanel() {
        if (clipboardPanel != null) {
            closeClipboardPanel();
            return;
        }
        clips = ClipStore.load(this);
        clipboardPanel = new ClipboardPanelView(this);
        setInputView(onCreateInputView());
        updateInputViewShown();
    }

    private void closeClipboardPanel() {
        clipboardPanel = null;
        setInputView(onCreateInputView());
        updateInputViewShown();
    }

    private ClipboardPanelView buildClipboardPanel() {
        final ClipboardPanelView panel = clipboardPanel;
        panel.setListener(new ClipboardPanelView.Listener() {
            @Override
            public void onPaste(String text) {
                dispatchSoftwareInput(ProjectKeyEvent.softwareDown(
                    "touch.bar.clip.paste", SemanticInput.text(text)));
                closeClipboardPanel();
            }

            @Override
            public void onPin(String text, boolean pinned) {
                clips = clips.setPinned(text, pinned);
                ClipStore.save(ReteKeyImeService.this, clips);
                panel.show(clips.clips());
            }

            @Override
            public void onForget(String text) {
                clips = clips.remove(text);
                ClipStore.save(ReteKeyImeService.this, clips);
                panel.show(clips.clips());
            }

            @Override
            public void onClearAll() {
                clips = clips.clearUnpinned();
                ClipStore.save(ReteKeyImeService.this, clips);
                panel.show(clips.clips());
            }

            @Override
            public void onClose() {
                closeClipboardPanel();
            }
        });
        panel.show(clips.clips());
        return panel;
    }

    /** How far either side of the cursor a word is looked for. Longer than any word worth one. */
    private static final int WORD_LOOKAROUND = 64;

    /** Names the layout the globe key just moved to, so a five-way cycle is not a guessing game. */
    private void announceLayout(KeyboardLayoutId id) {
        showFunctionToast(LetterLayouts.displayName(id));
    }

    private SharedPreferences viewPrefs() {
        return getSharedPreferences("retekey_view", MODE_PRIVATE);
    }

    /**
     * The menu page's Theme key: system → light → dark → system. The keyboard repaints itself off
     * the preference change; the toast is how the user knows which of the three they landed on,
     * since two of them can look identical on a device that is already set that way.
     */
    private void cycleTheme() {
        ThemeMode[] modes = ThemeMode.values();
        ThemeMode next = modes[(ScreenTheme.mode(this).ordinal() + 1) % modes.length];
        ScreenTheme.setMode(this, next);
        showFunctionToast(getString(themeLabel(next)));
        // No rebuild: the keyboard's own preference listener repaints it, which keeps the user on
        // the menu page they pressed the key from. The frames around it — the floating panel, the
        // candidate strip — resolve the palette when they are next created.
    }

    private static int themeLabel(ThemeMode mode) {
        switch (mode) {
            case LIGHT:
                return R.string.settings_theme_light;
            case DARK:
                return R.string.settings_theme_dark;
            default:
                return R.string.settings_theme_system;
        }
    }

    /** Flips the floating half-screen keyboard on or off and rebuilds the input view in place. */
    private void toggleFloatingMode() {
        setFloatingMode(!floatingMode);
    }

    /** The floating panel's ✕ key: back to the ordinary docked keyboard. */
    private void leaveFloatingMode() {
        setFloatingMode(false);
    }

    private void setFloatingMode(boolean enabled) {
        FloatingKeyboardSettings.setEnabled(viewPrefs(), OrientedPrefs.current(this), enabled);
        // Rebuilding is the whole switch: onCreateInputView re-reads the mode and returns either
        // the bare keyboard or the floating frame around it.
        setInputView(onCreateInputView());
        updateInputViewShown();
    }

    private void onFloatingBoundsChanged(FloatingKeyboardBounds bounds) {
        floatingBounds = bounds;
        FloatingKeyboardSettings.store(viewPrefs(), bounds);
    }

    /** The candidate list is its own shape too, and is put where reading it is comfortable. */
    private void onHanjaBoundsChanged(FloatingKeyboardBounds bounds) {
        hanjaBounds = bounds;
        FloatingKeyboardSettings.store(viewPrefs(), FloatingKeyboardSettings.HANJA_PREFIX, bounds);
    }

    /** The code-point pad is a different size and belongs elsewhere, so it remembers its own place. */
    private void onUnicodeBoundsChanged(FloatingKeyboardBounds bounds) {
        unicodeBounds = bounds;
        FloatingKeyboardSettings.store(
            viewPrefs(), FloatingKeyboardSettings.UNICODE_PREFIX, bounds);
    }

    /** Opens ReteKey's settings screen from the menu's 설정 tile, hiding the keyboard behind it. */
    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        launchFromKeyboard(intent);
    }

    /** Runs an editor context-menu command (copy/paste/undo) on the focused editor. */
    private void performEditCommand(int contextMenuId) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            inputConnection.performContextMenuAction(contextMenuId);
        }
    }

    /** Commits the current date and time as text, e.g. "2026. 12. 23.(월) 13:59". */
    private void insertCurrentDate() {
        // SimpleDateFormat rather than java.time, which only exists from API 26.
        String stamp = new SimpleDateFormat("yyyy. MM. dd.(E) HH:mm", Locale.KOREAN)
            .format(new Date());
        dispatchSoftwareInput(
            ProjectKeyEvent.softwareDown("touch.menu.date", SemanticInput.text(stamp)));
    }

    /** Opens the system input-method picker (keyboard chooser) from the 키보드전환 tile. */
    private void showImePicker() {
        InputMethodManager manager = Compat.systemService(
            this, Context.INPUT_METHOD_SERVICE, InputMethodManager.class);
        if (manager != null) {
            try {
                manager.showInputMethodPicker();
            } catch (RuntimeException ignored) {
                // Opening the picker must never crash the keyboard.
            }
        }
    }

    /** Starts an activity from the keyboard, hiding it, without ever crashing on a bad intent. */
    private void launchFromKeyboard(Intent intent) {
        try {
            startActivity(intent);
        } catch (RuntimeException ignored) {
            // ActivityNotFound / security failures must not take down the IME.
        }
        requestHideSelf(0);
    }

    /** Opens the system screen for enabling/disabling installed keyboards, from the 키보드관리 tile. */
    private void openKeyboardManagement() {
        Intent intent = new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launchFromKeyboard(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getRepeatCount() == 0 && handleHardwareFunctionKey(keyCode, event)) {
            return true;
        }
        if (isBoundFunctionKey(keyCode, event)) {
            // A held/repeating bound key: swallow the extra downs so the app sees nothing.
            return true;
        }
        if (unicodeEntry != null && handleUnicodeKey(keyCode, event)) {
            return true;
        }
        if (hanjaCandidatesShown && handleHanjaSelectionKey(keyCode)) {
            return true;
        }
        hideHanjaCandidatesIfShown();
        if (keyboardView != null) {
            // A physical key ends any 12-key run on screen, the same way an on-screen key that is
            // not part of the run does; otherwise the next tap would continue a run the user left.
            keyboardView.resetPhoneInterpreters();
        }
        if (passThroughChord(event)) {
            return super.onKeyDown(keyCode, event);
        }
        if (usesRawKeyCompatibility()) {
            return super.onKeyDown(keyCode, event);
        }
        // A physical keyboard types through the same composers as the screen: with the Vietnamese
        // layout up, its letters are Telex too (RFC-0011: one layout definition, both ways in).
        syncLatinComposer();
        ProjectKeyEvent projectEvent = KeyEventNormalizer.fromAndroid(
            keyCode,
            event,
            hardwareMapper
        );
        DispatchResult result = dispatcher.dispatch(projectEvent);
        if (result.actions().isEmpty()) {
            if (!result.isHandled() && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getRepeatCount() == 0) {
                endLatinWordBeforeDelegating(event);
            }
            return result.isHandled() || super.onKeyDown(keyCode, event);
        }
        ExecutionResult execution = execute(result);
        if (shouldDelegateHandled(result, execution)) {
            dispatcher.releaseForDelegation(projectEvent);
            return super.onKeyDown(keyCode, event);
        }
        if (!result.isHandled() && !mustBlockDelegation(execution)) {
            return super.onKeyDown(keyCode, event);
        }
        showFailureIfNeeded(execution);
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isBoundFunctionKey(keyCode, event)) {
            return true;
        }
        if (passThroughChord(event)) {
            return super.onKeyUp(keyCode, event);
        }
        if (usesRawKeyCompatibility()) {
            return super.onKeyUp(keyCode, event);
        }
        // A physical keyboard types through the same composers as the screen: with the Vietnamese
        // layout up, its letters are Telex too (RFC-0011: one layout definition, both ways in).
        syncLatinComposer();
        ProjectKeyEvent projectEvent = KeyEventNormalizer.fromAndroid(
            keyCode,
            event,
            hardwareMapper
        );
        DispatchResult result = dispatcher.dispatch(projectEvent);
        if (result.actions().isEmpty()) {
            return result.isHandled() || super.onKeyUp(keyCode, event);
        }
        ExecutionResult execution = execute(result);
        if (shouldDelegateHandled(result, execution)) {
            return super.onKeyUp(keyCode, event);
        }
        if (!result.isHandled() && !mustBlockDelegation(execution)) {
            return super.onKeyUp(keyCode, event);
        }
        showFailureIfNeeded(execution);
        return true;
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
        if (isBoundFunctionKey(keyCode, event)) {
            return true;
        }
        if (passThroughChord(event)) {
            return super.onKeyMultiple(keyCode, count, event);
        }
        if (usesRawKeyCompatibility()) {
            return super.onKeyMultiple(keyCode, count, event);
        }
        // A physical keyboard types through the same composers as the screen: with the Vietnamese
        // layout up, its letters are Telex too (RFC-0011: one layout definition, both ways in).
        syncLatinComposer();
        ProjectKeyEvent projectEvent = KeyEventNormalizer.fromAndroid(
            keyCode,
            event,
            hardwareMapper
        );
        DispatchResult result = dispatcher.dispatch(projectEvent);
        if (result.actions().isEmpty()) {
            return result.isHandled() || super.onKeyMultiple(keyCode, count, event);
        }
        ExecutionResult execution = execute(result);
        if (shouldDelegateHandled(result, execution)) {
            return super.onKeyMultiple(keyCode, count, event);
        }
        if (!result.isHandled() && !mustBlockDelegation(execution)) {
            return super.onKeyMultiple(keyCode, count, event);
        }
        showFailureIfNeeded(execution);
        return true;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        if (keyboardView != null) {
            // A field that takes a phone number or an amount opens on the keypad, the way other
            // keyboards answer one; the layout key walks the user's own list from there.
            keyboardView.setNumericField(
                attribute != null && NumericFieldPolicy.wantsKeypad(attribute.inputType));
        }
        dispatcher.reset();
        inputProcessor.reset();
        editorProfile = AndroidEditorProfileClassifier.classify(
            attribute,
            Build.VERSION.SDK_INT
        );
        sessionController.start(
            ScaffoldSessionState.EMPTY,
            initialBounds(attribute),
            editorProfile.capabilities()
        );
        sessionActive = true;
        if (keyboardView != null) {
            keyboardView.resetLayerState();
            keyboardView.resetPhoneInterpreters();
        }
        reloadHardwareBindings();
        if (keyboardView != null) {
            // Settings may have turned the current layout off while the keyboard was away.
            keyboardView.reloadLetterLayouts();
        }
        if (floatingFrame != null) {
            floatingFrame.setOpacityPercent(FloatingKeyboardSettings.opacityPercent(viewPrefs(), OrientedPrefs.current(this)));
        }
        hideHanjaCandidatesIfShown();
        updateHardwareMapper(currentSubtype());
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype newSubtype) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype);
        // A language switch must not carry a half-formed jamo into the new subtype; finalize and
        // reset the composer so no stale syllable surfaces on a later, unrelated keystroke.
        finishComposingInEditor();
        inputProcessor.reset();
        dispatcher.reset();
        updateHardwareMapper(newSubtype);
    }

    @Override
    public void onFinishInput() {
        // Finalize any half-formed preedit into the editor before tearing down, so leaving a field
        // mid-syllable doesn't drop the underlined composing text.
        finishComposingInEditor();
        if (sessionActive) {
            sessionController.stopAccepting();
        }
        dispatcher.reset();
        try {
            super.onFinishInput();
        } finally {
            finishSession();
        }
    }

    /** Commits any active composing region as normal text; a no-op when nothing is composing. */
    private void finishComposingInEditor() {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            inputConnection.finishComposingText();
        }
    }

    @Override
    public void onUnbindInput() {
        dispatcher.reset();
        hardwareMapper = HardwareSemanticMapper.none();
        finishSession();
        super.onUnbindInput();
    }

    @Override
    public void onDestroy() {
        dispatcher.reset();
        finishSession();
        try {
            viewPrefs().unregisterOnSharedPreferenceChangeListener(barPrefsListener);
        } catch (RuntimeException ignored) {
            // Never registered, or the preferences are already gone; nothing to unhook.
        }
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Held for the life of the service: the registration in SharedPreferences is weak, and a
        // listener that is collected is a setting that appears not to work.
        viewPrefs().registerOnSharedPreferenceChangeListener(barPrefsListener);
    }

    /**
     * Puts a freshly built input view on screen. The framework keeps the one it was given, so any
     * change to what the view is made of — the action bar arriving, the clipboard opening — has to
     * be pushed rather than waited for.
     */
    private void rebuildInputView() {
        try {
            setInputView(onCreateInputView());
            updateInputViewShown();
        } catch (RuntimeException ignored) {
            // Rebuilding while the window is going away must not take the keyboard with it.
        }
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (bandFrame != null) {
            // The band was possibly measured while this window was not the one on screen — every
            // rebuild from the settings screen is such a moment — and the insets that mattered then
            // were the settings screen's, not the keyboard's.
            bandFrame.refreshBand();
        }
        // A safety net for the same problem: if the bar was switched on while this view was off
        // screen, the listener may have fired when there was nothing to rebuild.
        boolean wanted = viewPrefs().getBoolean(
            ActionBarSlots.KEY_ENABLED, ActionBarSlots.DEFAULT_ENABLED);
        if (wanted != builtWithBar) {
            rebuildInputView();
        }
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        // Finalize any preedit when the keyboard is dismissed so a hidden view never strands
        // underlined composing text; the composer restarts clean when the view returns.
        finishComposingInEditor();
        inputProcessor.reset();
        hideHanjaCandidatesIfShown();
    }

    @Override
    public void onUpdateSelection(
        int oldSelStart,
        int oldSelEnd,
        int newSelStart,
        int newSelEnd,
        int candidatesStart,
        int candidatesEnd
    ) {
        super.onUpdateSelection(
            oldSelStart,
            oldSelEnd,
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd
        );
        if (!sessionActive) {
            return;
        }
        try {
            if (newSelStart < 0 || newSelEnd < 0) {
                sessionController.updateSelection(
                    sessionController.generation(),
                    EditorBounds.unknown()
                );
                return;
            }
            // A remote-desktop editor never has a composing region — composition is
            // materialised as committed text — and its dummy buffer reports selection changes of
            // its own accord, so a cursor-move verdict there is noise that resets the composer
            // mid-syllable (일 became 이ㄹ, 전 stopped at 저 depending on when the report landed).
            boolean remoteDesktop = editorProfile != null
                && editorProfile.capabilities().deleteByKeyEvents();
            boolean abandon = !remoteDesktop && CursorMovePolicy.shouldAbandonComposition(
                inputProcessor.isComposing(),
                newSelStart,
                newSelEnd,
                candidatesStart,
                candidatesEnd
            );
            if (!abandon && !remoteDesktop && candidatesStart < 0 && inputProcessor.isComposing()) {
                // Editors that never report a composing region (Compose text fields — Google
                // Keep) get the text-based verdict: composing leaves the cursor right after the
                // preedit, so anything else there means the user moved it.
                String preedit = inputProcessor.composingText();
                InputConnection connection = getCurrentInputConnection();
                abandon = CursorMovePolicy.shouldAbandonWithoutRegion(
                    true,
                    newSelStart,
                    newSelEnd,
                    preedit,
                    connection == null || preedit.isEmpty()
                        ? null
                        : connection.getTextBeforeCursor(preedit.length(), 0)
                );
            }
            if (abandon) {
                // The user moved the cursor away from the syllable being composed. Settle that
                // syllable where it is — it must not follow the cursor — and start clean, so the
                // next key types at the new position instead of repainting the stale composition.
                finishComposingInEditor();
                inputProcessor.reset();
                if (keyboardView != null) {
                    keyboardView.resetPhoneInterpreters();
                }
            }
            int composingStart = candidatesStart >= 0
                && candidatesEnd >= candidatesStart ? candidatesStart : -1;
            int composingEnd = composingStart >= 0 ? candidatesEnd : -1;
            sessionController.updateSelection(
                sessionController.generation(),
                EditorBounds.of(
                    newSelStart,
                    newSelEnd,
                    composingStart,
                    composingEnd
                )
            );
        } catch (RuntimeException ignored) {
            // A selection update must never crash the IME and make the keyboard disappear.
            inputProcessor.reset();
        }
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public void onComputeInsets(Insets outInsets) {
        super.onComputeInsets(outInsets);
        applyFloatingInsets(outInsets);
    }

    @Override
    public boolean onEvaluateInputViewShown() {
        super.onEvaluateInputViewShown();
        if (hanjaFloating) {
            // The candidates are a panel of this IME's own now, so they need the window shown even
            // when a hardware keyboard would otherwise keep the keyboard hidden.
            return true;
        }
        if (unicodeFloating) {
            // A hardware keyboard normally hides the on-screen keyboard, and it can go on typing
            // the digits — but then nothing would show the code being built. The pad is the
            // feedback, so it comes up regardless of what is typing into it.
            return true;
        }
        // Hide the on-screen keyboard when a hardware keyboard is usable; input still passes
        // through the service. The mode will be user-configurable once settings land (RFC-0007).
        return SoftKeyboardVisibilityPolicy.shouldShow(
            hasActiveHardwareKeyboard(),
            softKeyboardMode
        );
    }

    private boolean hasActiveHardwareKeyboard() {
        Configuration config = getResources().getConfiguration();
        return config.keyboard != Configuration.KEYBOARD_NOKEYS
            && config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
    }

    private void dispatchSoftwareInput(ProjectKeyEvent event) {
        if (unicodeEntry != null && consumeForUnicodeEntry(event)) {
            return;
        }
        if (consumeForNotepad(event)) {
            return;
        }
        hideHanjaCandidatesIfShown();
        syncLatinComposer();
        // A single misbehaving editor must never crash the IME and make the keyboard vanish.
        try {
            ExecutionResult result = execute(dispatcher.dispatch(event));
            if (result == null || result.isFailure()) {
                showEditorFailure();
            }
        } catch (RuntimeException crash) {
            dispatcher.reset();
            inputProcessor.reset();
        }
    }

    private EditorProfile currentEditorProfile() {
        return editorProfile;
    }

    /**
     * A physical key the Latin composer does not take — space, punctuation, Enter, a digit — is
     * about to go to the editor itself. The word composing must be committed first, or the editor
     * would type after a composing region the next letter then replaces.
     */
    private void endLatinWordBeforeDelegating(KeyEvent event) {
        LatinComposer latin = inputProcessor.latinComposer();
        if (latin == null || !latin.isComposing()) {
            return;
        }
        int keyCode = event.getKeyCode();
        boolean printable = event.getUnicodeChar() != 0
            || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_TAB;
        if (!printable) {
            return;
        }
        ExecutionResult result = execute(dispatcher.dispatch(
            ProjectKeyEvent.softwareDown("hardware.flush", SemanticInput.flush())));
        if (result == null || result.isFailure()) {
            latin.reset();
        }
    }

    /**
     * The kana pad's ゛゜小 key: the character before the cursor moves along its cycle — か to が,
     * は through ば to ぱ, a vowel to its small form. Nothing before the cursor, or a character
     * with no cycle, does nothing, like a drag at an empty guide cell. The notepad's text is
     * edited the same way when a note is open.
     */
    private void applyKanaModifier() {
        if (notepad != null && notepad.isWriting()) {
            String last = notepad.lastCharacter();
            String turned = last != null && last.length() == 1
                ? KanaFlick.modified(last.charAt(0)) : null;
            if (turned != null) {
                notepad.deleteBackward();
                notepad.type(turned);
            }
            return;
        }
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }
        try {
            CharSequence before = ic.getTextBeforeCursor(1, 0);
            if (before == null || before.length() != 1) {
                return;
            }
            String turned = KanaFlick.modified(before.charAt(0));
            if (turned == null) {
                return;
            }
            ic.beginBatchEdit();
            ic.deleteSurroundingText(1, 0);
            ic.commitText(turned, 1);
            ic.endBatchEdit();
        } catch (RuntimeException ignored) {
            // A misbehaving editor must never crash the keyboard.
        }
    }

    /** The Latin composer the current letter layout wants, or null for none. */
    private LatinComposer wantedLatinComposer() {
        if (keyboardView == null) {
            return null;
        }
        switch (keyboardView.letterLayoutId()) {
            case VI_TELEX:
                return telex;
            case JA_ROMAJI:
                return romaji;
            default:
                return null;
        }
    }

    /** The notepad's own composer for the current letter layout, or null. */
    private LatinComposer wantedNotepadLatin() {
        if (keyboardView == null) {
            return null;
        }
        switch (keyboardView.letterLayoutId()) {
            case VI_TELEX:
                return notepadTelex;
            case JA_ROMAJI:
                return notepadRomaji;
            default:
                return null;
        }
    }

    /**
     * Hands the letter keys to the Telex composer while the Vietnamese layout is up and takes
     * them back otherwise. The switch itself commits whatever the outgoing composer had, so a
     * half-made word is not left behind as composing text the next layout cannot finish.
     */
    private void syncLatinComposer() {
        LatinComposer wanted = wantedLatinComposer();
        LatinComposer current = inputProcessor.latinComposer();
        if (wanted == current) {
            return;
        }
        applyHardwareMode();
        if (current != null && current.isComposing()) {
            ExecutionResult result = execute(dispatcher.dispatch(
                ProjectKeyEvent.softwareDown("layout.switch", SemanticInput.flush())));
            if (result == null || result.isFailure()) {
                current.reset();
            }
        }
        inputProcessor.setLatinComposer(wanted);
    }

    private void updateHardwareMapper(InputMethodSubtype subtype) {
        hardwareKoreanMode = isKoreanSubtype(subtype);
        applyHardwareMode();
    }

    /** Selects the physical-key mapper for the current Hangul mode and editor kind. */
    private void applyHardwareMode() {
        if (!usesRawKeyCompatibility() && hardwareKoreanMode) {
            hardwareMapper = DubeolsikHardwareMapper.INSTANCE;
        } else if (!usesRawKeyCompatibility() && wantedLatinComposer() != null) {
            hardwareMapper = LatinHardwareMapper.INSTANCE;
        } else {
            hardwareMapper = HardwareSemanticMapper.none();
        }
    }

    /** Re-reads the user's physical-key shortcuts for 한/영 and 한자 from preferences. */
    private void reloadHardwareBindings() {
        SharedPreferences prefs = getSharedPreferences("retekey_view", MODE_PRIVATE);
        hanyeongBindings = HardwareKeyBindings.parse(
            prefs.getString(HardwareKeyBindings.KEY_HANYEONG, ""));
        hanjaBindings = HardwareKeyBindings.parse(
            prefs.getString(HardwareKeyBindings.KEY_HANJA, ""));
        unicodeBindings = HardwareKeyBindings.parse(
            prefs.getString(HardwareKeyBindings.KEY_UNICODE, ""));
        // A physical key held down repeats on the platform's own clock; only the user's on/off
        // choice can carry over from the soft keyboard's auto-repeat setting.
        dispatcher.setHardwareRepeatEnabled(
            prefs.getBoolean(KeyRepeatSettings.KEY_ENABLED, KeyRepeatSettings.DEFAULT_ENABLED));
    }

    private static int pressedMods(KeyEvent event) {
        int meta = event.getMetaState();
        return HardwareKeyBindings.modsOf(
            (meta & KeyEvent.META_SHIFT_ON) != 0,
            (meta & KeyEvent.META_CTRL_ON) != 0,
            (meta & KeyEvent.META_ALT_ON) != 0,
            (meta & KeyEvent.META_META_ON) != 0);
    }

    private boolean isBoundFunctionKey(int keyCode, KeyEvent event) {
        int mods = pressedMods(event);
        return HardwareKeyBindings.matches(hanyeongBindings, keyCode, mods)
            || HardwareKeyBindings.matches(hanjaBindings, keyCode, mods)
            || HardwareKeyBindings.matches(unicodeBindings, keyCode, mods);
    }

    /** Runs the 한/영 or 한자 action for a matching physical key; returns true when it acted. */
    private boolean handleHardwareFunctionKey(int keyCode, KeyEvent event) {
        int mods = pressedMods(event);
        if (HardwareKeyBindings.matches(hanyeongBindings, keyCode, mods)) {
            toggleHardwareKorean();
            return true;
        }
        if (HardwareKeyBindings.matches(unicodeBindings, keyCode, mods)) {
            startUnicodeEntry();
            return true;
        }
        if (HardwareKeyBindings.matches(hanjaBindings, keyCode, mods)) {
            handleHanja();
            return true;
        }
        return false;
    }

    /** Flips physical-keyboard Hangul composing on/off, finalising any half-formed syllable. */
    private void toggleHardwareKorean() {
        finishComposingInEditor();
        inputProcessor.reset();
        dispatcher.reset();
        hardwareKoreanMode = !hardwareKoreanMode;
        applyHardwareMode();
        showFunctionToast(getString(hardwareKoreanMode ? R.string.mode_korean : R.string.mode_english));
    }

    /**
     * Converts a Hangul reading to Hanja: a live selection is converted whole, otherwise the
     * reading immediately before the cursor. Candidates are offered in the candidates strip; the
     * key is a no-op when nothing converts.
     */
    /**
     * Opens the code-point entry: type hex digits and the character they name appears as the one
     * candidate, with its U+ number beside it. Enter or the candidate commits it, Esc leaves.
     * Reached from the menu's Uni key or from a physical key the user has bound to it.
     */
    private void startUnicodeEntry() {
        finishComposingInEditor();
        inputProcessor.reset();
        unicodeEntry = UnicodeEntry.empty();
        unicodeFloating = true;
        setInputView(onCreateInputView());
        updateInputViewShown();
        showUnicodeEntry();
    }

    /**
     * Shows what has been typed so far on the pad itself: the digits, and the character they name
     * once they name one. A window of its own put four characters of feedback somewhere other than
     * the keys producing them, which is the scattered thing this replaced.
     */
    private void showUnicodeEntry() {
        if (unicodeEntry == null) {
            return;
        }
        if (keyboardView == null) {
            return;
        }
        String character = unicodeEntry.character();
        keyboardView.setUnicodePreview(character == null
            ? unicodeEntry.display()
            : unicodeEntry.display() + "   " + character);
    }

    /** Feeds one character to the open entry. Returns false when the entry is not open. */
    private boolean feedUnicodeEntry(char typed) {
        if (unicodeEntry == null || !UnicodeEntry.isHexDigit(typed)) {
            return false;
        }
        unicodeEntry = unicodeEntry.append(typed);
        showUnicodeEntry();
        return true;
    }

    /**
     * Puts the character in the document and closes the entry; does nothing without one. Nothing
     * is typed while the digits are being entered — the code is a composition, and only this
     * finishes it — so Cancel can leave at any point with the document untouched.
     */
    private void commitUnicodeEntry() {
        String character = unicodeEntry == null ? null : unicodeEntry.character();
        endUnicodeEntry();
        if (character == null) {
            return;
        }
        dispatchSoftwareInput(
            ProjectKeyEvent.softwareDown("touch.menu.unicode", SemanticInput.text(character)));
    }

    /**
     * A physical key while the code-point entry is open. Hex digits build it, backspace takes one
     * back, Enter commits and Esc leaves — and the digits are claimed here rather than by the
     * candidate window's number keys, which would otherwise swallow 1 to 9.
     */
    private boolean handleUnicodeKey(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_SPACE:
                commitUnicodeEntry();
                return true;
            case KeyEvent.KEYCODE_ESCAPE:
                endUnicodeEntry();
                return true;
            case KeyEvent.KEYCODE_DEL:
                unicodeEntry = unicodeEntry.backspace();
                showUnicodeEntry();
                return true;
            default:
                break;
        }
        int unicodeChar = event == null ? 0 : event.getUnicodeChar(0);
        return unicodeChar > 0 && feedUnicodeEntry((char) unicodeChar);
    }

    /**
     * An on-screen key while the entry is open. The same four answers as a physical key, so the
     * Uni key works with no hardware keyboard attached: the letters and the keypad both type hex.
     */
    private boolean consumeForUnicodeEntry(ProjectKeyEvent event) {
        SemanticInput input = event == null ? null : event.semanticInput();
        if (input == null) {
            return false;
        }
        switch (input.kind()) {
            case TEXT: {
                String text = input.text();
                if (text.length() == 1 && UnicodeEntry.isHexDigit(text.charAt(0))) {
                    return feedUnicodeEntry(text.charAt(0));
                }
                if (" ".equals(text)) {
                    commitUnicodeEntry();
                    return true;
                }
                return false;
            }
            case DELETE_BACKWARD:
                unicodeEntry = unicodeEntry.backspace();
                showUnicodeEntry();
                return true;
            case PRIMARY_ACTION:
                commitUnicodeEntry();
                return true;
            case RAW_KEY:
                if (input.rawKey() == RawKey.ESCAPE) {
                    endUnicodeEntry();
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    private void endUnicodeEntry() {
        unicodeEntry = null;
        hideHanjaCandidatesIfShown();
        if (!unicodeFloating) {
            if (keyboardView != null) {
                keyboardView.setUnicodeEntry(false);
            }
            return;
        }
        // Rebuilding puts back whatever was there before: the docked keyboard, or the user's own
        // floating one, on the layout it was left on.
        unicodeFloating = false;
        setInputView(onCreateInputView());
        updateInputViewShown();
    }

    /** The menu's Memo key: opens the notepad over the app, or closes it again. */
    private void toggleNotepad() {
        if (notepad != null) {
            closeNotepad();
            return;
        }
        notepad = new NotepadView(this, NoteStore.load(this));
        setInputView(onCreateInputView());
        updateInputViewShown();
    }

    private void closeNotepad() {
        flushNotepadComposition();
        notepadComposer.reset();
        notepadTelex.reset();
        notepadRomaji.reset();
        if (notepad != null) {
            NoteStore.save(this, notepad.notes());
            notepad = null;
        }
        setInputView(onCreateInputView());
        updateInputViewShown();
    }

    /** Wires a freshly built panel to its store and its way out. */
    private NotepadView buildNotepad() {
        final NotepadView panel = notepad;
        panel.setOnClose(this::closeNotepad);
        panel.setOnChanged(() -> NoteStore.save(this, panel.notes()));
        return panel;
    }

    /**
     * While a note is open the keys write into it rather than into the app behind. Everything the
     * notepad does not claim — the layout keys, the menu, the height keys — still reaches the
     * keyboard, because those are the keyboard's own controls rather than text.
     */
    private boolean consumeForNotepad(ProjectKeyEvent event) {
        if (notepad == null || !notepad.isWriting()) {
            return false;
        }
        SemanticInput input = event == null ? null : event.semanticInput();
        if (input == null) {
            return false;
        }
        switch (input.kind()) {
            case JAMO: {
                HangulComposer.Result result = notepadComposer.input(input.jamo());
                notepad.typeComposed(result.commit(), result.preedit());
                return true;
            }
            case TEXT: {
                LatinComposer latin = wantedNotepadLatin();
                if (latin != null && latin.accepts(input.text())) {
                    // A composer letter: the Hangul syllable, if any, settles first.
                    flushNotepadHangul();
                    LatinComposer.Result result = latin.input(input.text());
                    notepad.typeComposed(result.commit, result.preedit);
                    return true;
                }
                flushNotepadComposition();
                notepad.type(input.text());
                return true;
            }
            case DELETE_BACKWARD: {
                LatinComposer notepadLatin = notepadTelex.isComposing() ? notepadTelex
                    : notepadRomaji.isComposing() ? notepadRomaji : null;
                if (notepadLatin != null) {
                    LatinComposer.Result result = notepadLatin.backspace();
                    notepad.typeComposed("", result == null ? "" : result.preedit);
                    return true;
                }
                if (notepad.isComposing()) {
                    // Inside a syllable, backspace takes it apart rather than deleting it whole.
                    HangulComposer.Result result = notepadComposer.backspace();
                    notepad.typeComposed("", result == null ? "" : result.preedit());
                    return true;
                }
                notepadComposer.reset();
        notepadTelex.reset();
        notepadRomaji.reset();
                notepad.deleteBackward();
                return true;
            }
            case PRIMARY_ACTION:
                flushNotepadComposition();
                notepad.newLine();
                return true;
            case FLUSH:
                flushNotepadComposition();
                return true;
            default:
                return false;
        }
    }

    /** Settles whatever syllable the notepad was building, so the next thing types after it. */
    private void flushNotepadComposition() {
        String flushed = notepadComposer.flush() + notepadTelex.flush() + notepadRomaji.flush();
        if (notepad == null) {
            return;
        }
        if (!flushed.isEmpty()) {
            notepad.typeComposed(flushed, "");
        }
        notepad.endComposition();
    }

    /** Settles only the Hangul syllable, leaving a Telex word to carry on. */
    private void flushNotepadHangul() {
        String flushed = notepadComposer.flush();
        if (notepad != null && !flushed.isEmpty()) {
            notepad.typeComposed(flushed, "");
        }
    }

    private void handleHanja() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }
        MappedHanjaTable dictionary = HanjaDictionary.get(this);
        // A live selection converts in place, in whichever direction its script implies.
        CharSequence selection = null;
        try {
            selection = ic.getSelectedText(0);
        } catch (RuntimeException ignored) {
            // Some editors refuse selection reads; fall through to the cursor path.
        }
        if (selection != null && selection.length() > 0 && selection.length() <= 16) {
            if (convertFromSelection(dictionary, selection.toString())) {
                return;
            }
            hideHanjaCandidatesIfShown();
            return;
        }
        // Otherwise convert what is before the cursor. Finish any composing first so the syllable is
        // committed text we can read back and replace.
        finishComposingInEditor();
        inputProcessor.reset();
        CharSequence before = ic.getTextBeforeCursor(HANJA_LOOKBEHIND, 0);
        if (before == null || before.length() == 0) {
            hideHanjaCandidatesIfShown();
            return;
        }
        String text = before.toString();
        int lastCodePoint = text.codePointBefore(text.length());
        if (lastCodePoint <= 0xFFFF && SpecialCharTable.hasCandidates((char) lastCodePoint)) {
            // A consonant on its own, then the Hanja key: the special characters that consonant
            // has stood for since the KS X 1001 tables. ㅁ the general symbols, ㅅ the Greek
            // alphabet, ㅇ the circled numbers — the convention every Korean IME has carried.
            pendingFromSelection = false;
            pendingDeleteLength = 1;
            showHanjaCandidates(String.valueOf((char) lastCodePoint),
                codePointItems(SpecialCharTable.candidatesFor((char) lastCodePoint)));
        } else if (HanjaTable.isHangul(lastCodePoint)) {
            HanjaTable.Match match = dictionary.longestSuffixMatch(text, HANJA_LOOKBEHIND);
            if (match == null) {
                hideHanjaCandidatesIfShown();
                return;
            }
            pendingFromSelection = false;
            pendingDeleteLength = match.length;
            showHanjaCandidates(match.reading, forwardItems(match.candidates));
        } else if (HanjaTable.isHanja(lastCodePoint)) {
            HanjaTable.Match match = dictionary.longestSuffixReverseMatch(text, HANJA_LOOKBEHIND);
            if (match == null) {
                hideHanjaCandidatesIfShown();
                return;
            }
            pendingFromSelection = false;
            pendingDeleteLength = match.length;
            showHanjaCandidates(match.reading, reverseItems(match.candidates));
        } else {
            hideHanjaCandidatesIfShown();
        }
    }

    /** Converts a selection: Hangul → Hanja, or Hanja → Hangul. Returns false when nothing matches. */
    private boolean convertFromSelection(MappedHanjaTable dictionary, String selection) {
        int lastCodePoint = selection.codePointBefore(selection.length());
        if (HanjaTable.isHangul(lastCodePoint)) {
            List<String> candidates = dictionary.candidates(selection);
            if (candidates.isEmpty()) {
                return false;
            }
            pendingFromSelection = true;
            pendingDeleteLength = 0;
            showHanjaCandidates(selection, forwardItems(candidates));
            return true;
        }
        if (HanjaTable.isHanja(lastCodePoint)) {
            List<String> readings = dictionary.readings(selection);
            if (readings.isEmpty()) {
                return false;
            }
            pendingFromSelection = true;
            pendingDeleteLength = 0;
            showHanjaCandidates(selection, reverseItems(readings));
            return true;
        }
        return false;
    }

    /** Hanja candidates with their 훈음 gloss (한글 → 한자). */
    private List<HanjaCandidatesView.Item> forwardItems(List<String> hanja) {
        MappedHanjaTable dictionary = HanjaDictionary.get(this);
        List<HanjaCandidatesView.Item> items = new ArrayList<>(hanja.size());
        for (String candidate : hanja) {
            items.add(new HanjaCandidatesView.Item(candidate, dictionary.gloss(candidate)));
        }
        return items;
    }

    /**
     * Candidates glossed with their code point. A symbol has no reading to show beside it, and
     * the number is the one thing about it worth knowing — it is what you would look it up by,
     * and what you would type it by on the Unicode key.
     */
    private List<HanjaCandidatesView.Item> codePointItems(List<String> characters) {
        List<HanjaCandidatesView.Item> items = new ArrayList<>(characters.size());
        for (String character : characters) {
            items.add(new HanjaCandidatesView.Item(character, UnicodeEntry.labelOf(character)));
        }
        return items;
    }

    /** Reading candidates for reverse conversion (한자 → 한글); no gloss. */
    private List<HanjaCandidatesView.Item> reverseItems(List<String> readings) {
        List<HanjaCandidatesView.Item> items = new ArrayList<>(readings.size());
        for (String reading : readings) {
            items.add(new HanjaCandidatesView.Item(reading, null));
        }
        return items;
    }

    /** Replaces the source reading with the chosen Hanja and hides the strip. */
    private void commitHanja(String hanja) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            try {
                ic.beginBatchEdit();
                if (!pendingFromSelection && pendingDeleteLength > 0) {
                    ic.deleteSurroundingText(pendingDeleteLength, 0);
                }
                // With a live selection, commitText replaces it; otherwise it follows the delete.
                ic.commitText(hanja, 1);
            } finally {
                ic.endBatchEdit();
            }
        }
        hideHanjaCandidates();
    }

    private void showHanjaCandidates(String reading, List<HanjaCandidatesView.Item> candidates) {
        pendingReading = reading;
        pendingCandidates = candidates;
        hanjaCandidatesShown = true;
        if (hanjaFloating && hanjaView != null) {
            // Already up: a new reading is new contents, not a new panel — rebuilding would make
            // it jump and lose the place the user dragged it to.
            hanjaView.show(reading, candidates);
            return;
        }
        // The candidates are a floating panel like the code-point pad: they belong over the
        // document rather than in a popup pinned to a keyboard that may not even be on screen.
        hanjaFloating = true;
        setInputView(onCreateInputView());
        updateInputViewShown();
    }

    /**
     * In floating mode the IME window covers the screen but only the panel may take touches, and
     * the app underneath must not be resized for it. Returns false when the mode is off.
     */
    private boolean applyFloatingInsets(Insets outInsets) {
        if (!floatingMode || floatingFrame == null) {
            return false;
        }
        android.graphics.Rect panel = floatingFrame.panelBounds();
        if (panel.isEmpty()) {
            return false;
        }
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION;
        outInsets.touchableRegion.set(panel);
        // The editor keeps its full height: a floating keyboard covers the app rather than
        // pushing it, which is the point of being able to move it out of the way.
        int windowBottom = floatingFrame.getHeight();
        outInsets.contentTopInsets = windowBottom;
        outInsets.visibleTopInsets = windowBottom;
        return true;
    }

    private void hideHanjaCandidatesIfShown() {
        if (hanjaCandidatesShown) {
            hideHanjaCandidates();
        }
    }

    private void hideHanjaCandidates() {
        pendingReading = null;
        pendingCandidates = null;
        hanjaCandidatesShown = false;
        if (!hanjaFloating) {
            return;
        }
        hanjaFloating = false;
        hanjaView = null;
        // Back to whatever was there before, on the layout it was left on.
        setInputView(onCreateInputView());
        updateInputViewShown();
    }

    /**
     * While the candidate strip is up, a number key 1–9 picks that candidate, the page keys/arrows
     * turn the page, and Escape dismisses. Returns true when the key was used for the strip.
     */
    private boolean handleHanjaSelectionKey(int keyCode) {
        if (hanjaView == null) {
            return false;
        }
        int number = digitFromKeyCode(keyCode);
        if (number >= 1) {
            hanjaView.selectByNumber(number);
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_PAGE_DOWN:
                hanjaView.nextPage();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_PAGE_UP:
                hanjaView.prevPage();
                return true;
            case KeyEvent.KEYCODE_ESCAPE:
                hideHanjaCandidates();
                return true;
            default:
                return false;
        }
    }

    /** Maps a top-row or numpad digit key to 1–9, or -1 when it is not a digit. */
    private static int digitFromKeyCode(int keyCode) {
        if (keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_9) {
            return keyCode - KeyEvent.KEYCODE_1 + 1;
        }
        if (keyCode >= KeyEvent.KEYCODE_NUMPAD_1 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9) {
            return keyCode - KeyEvent.KEYCODE_NUMPAD_1 + 1;
        }
        return -1;
    }

    private void showFunctionToast(String text) {
        try {
            if (functionToast != null) {
                functionToast.cancel();
            }
            functionToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
            functionToast.show();
        } catch (RuntimeException ignored) {
            // A toast failure must never affect input.
        }
    }

    @SuppressWarnings("deprecation")
    private InputMethodSubtype currentSubtype() {
        InputMethodManager manager = Compat.systemService(
            this, Context.INPUT_METHOD_SERVICE, InputMethodManager.class);
        return manager == null ? null : manager.getCurrentInputMethodSubtype();
    }

    @SuppressWarnings("deprecation")
    private static boolean isKoreanSubtype(InputMethodSubtype subtype) {
        if (subtype == null) {
            return false;
        }
        String languageTag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            ? subtype.getLanguageTag()
            : null;
        if (languageTag == null || languageTag.isEmpty()) {
            languageTag = subtype.getLocale().replace('_', '-');
        }
        return "ko".equals(Compat.languageOf(languageTag));
    }

    private ExecutionResult execute(DispatchResult result) {
        if (!sessionActive) {
            return null;
        }
        try {
            EditorBounds predicted = EditorBoundsPredictor.after(
                sessionController.workingBounds(),
                result.actions()
            );
            TransitionPlan<ScaffoldSessionState> plan = sessionController.plan(
                result,
                ScaffoldSessionState.EMPTY,
                predicted
            );
            return sessionController.execute(plan, this::currentEndpoint);
        } catch (RuntimeException crash) {
            // The keyboard must survive any single bad editor interaction.
            inputProcessor.reset();
            return null;
        }
    }

    /**
     * The character before the cursor, for a 나랏글 transformation arriving with nothing
     * composing. Null when there is no connection or the editor cannot say (terminals).
     */
    private CharSequence characterBeforeCursor() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return null;
        }
        try {
            return ic.getTextBeforeCursor(1, 0);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private EditorEndpoint currentEndpoint() {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null) {
            return null;
        }
        return EditorEndpoint.of(
            sessionController.generation(), new InputConnectionEditorBridge(inputConnection));
    }

    private boolean usesRawKeyCompatibility() {
        return editorProfile.capabilities().deletionMode()
            == EditorCapabilities.DeletionMode.RAW_KEY;
    }

    /** Modifier keys and Ctrl/Alt/Meta chords are app shortcuts; the IME must not consume them. */
    private static boolean passThroughChord(KeyEvent event) {
        return ModifierChordPolicy.passThroughToApp(
            KeyEvent.isModifierKey(event.getKeyCode()),
            event.isCtrlPressed(),
            event.isAltPressed(),
            event.isMetaPressed()
        );
    }

    private static EditorBounds initialBounds(EditorInfo editorInfo) {
        if (editorInfo == null
            || editorInfo.initialSelStart < 0
            || editorInfo.initialSelEnd < 0) {
            return EditorBounds.unknown();
        }
        return EditorBounds.of(
            editorInfo.initialSelStart,
            editorInfo.initialSelEnd,
            -1,
            -1
        );
    }

    private void finishSession() {
        if (sessionActive) {
            sessionController.finish();
            sessionActive = false;
        }
        inputProcessor.reset();
        editorProfile = EditorProfile.unsupported();
        if (editorFailureToast != null) {
            editorFailureToast.cancel();
            editorFailureToast = null;
        }
    }

    private static boolean shouldDelegateHandled(
        DispatchResult dispatch,
        ExecutionResult execution
    ) {
        return dispatch.isHandled()
            && (execution == null
                || (execution.outcome() == ExecutionResult.Outcome.NOT_DISPATCHED
                    && !execution.remoteMutationMayHaveOccurred()));
    }

    private static boolean mustBlockDelegation(ExecutionResult execution) {
        return execution != null
            && execution.isFailure()
            && execution.remoteMutationMayHaveOccurred();
    }

    private void showFailureIfNeeded(ExecutionResult execution) {
        if (execution != null && execution.isFailure()) {
            showEditorFailure();
        }
    }

    private void showEditorFailure() {
        if (editorFailureToast != null) {
            editorFailureToast.cancel();
        }
        editorFailureToast = Toast.makeText(
            this,
            R.string.editor_unavailable,
            Toast.LENGTH_SHORT
        );
        editorFailureToast.show();
    }

    protected final long observedSessionGeneration() {
        return sessionController.generation();
    }

    protected final boolean isObservedSessionActive() {
        return sessionActive;
    }

    protected final SynchronizationState observedSynchronizationState() {
        return sessionController.syncState();
    }

    protected final int observedPendingExpectationCount() {
        return sessionController.pendingExpectationCount();
    }

    private enum ScaffoldSessionState {
        EMPTY
    }
}
