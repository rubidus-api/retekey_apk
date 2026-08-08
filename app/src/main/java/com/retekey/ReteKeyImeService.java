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
    private HanjaCandidatesWindow hanjaWindow;
    private String pendingReading;
    private List<HanjaCandidatesView.Item> pendingCandidates;
    private boolean pendingFromSelection;
    private int pendingDeleteLength;
    private boolean hanjaCandidatesShown;
    // Set when the candidate window had to bring the IME window into existence because no
    // on-screen keyboard was up — the external-keyboard case.
    private boolean candidatesWindowForced;
    private int candidateWindowAttempts;
    private final android.os.Handler mainHandler =
        new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean floatingMode;
    /** The notepad panel above the keyboard, or null when it is not open. */
    private NotepadView notepad;
    /** The notepad's own Hangul composer: what is typed there is not going through the editor. */
    private final HangulComposer notepadComposer = new HangulComposer();
    /** The orientation the current input view was built for; a rotation rebuilds it. */
    private ScreenOrientation builtFor;
    private FloatingKeyboardBounds floatingBounds;
    private FloatingKeyboardFrame floatingFrame;

    @Override
    public View onCreateInputView() {
        builtFor = OrientedPrefs.current(this);
        floatingMode = FloatingKeyboardSettings.isEnabled(viewPrefs(), builtFor);
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
        keyboardView.setOnLayoutChanged(this::announceLayout);
        // The view can be created for the first time solely to host the candidate window; it must
        // come up collapsed rather than as a keyboard nobody asked for.
        keyboardView.setCollapsed(!floatingMode && candidatesWindowForced);
        reloadHardwareBindings();
        HanjaDictionary.preload(this);
        if (notepad != null) {
            // The notepad owns the window while it is open: the keyboard keeps its height at the
            // bottom and the panel takes the rest of the screen.
            floatingFrame = null;
            NotepadView panel = buildNotepad();
            return new NotepadFrame(this, panel, keyboardView);
        }
        if (!floatingMode) {
            floatingFrame = null;
            return keyboardView;
        }
        if (floatingBounds == null) {
            floatingBounds = FloatingKeyboardSettings.load(viewPrefs());
        }
        floatingFrame = new FloatingKeyboardFrame(this, keyboardView);
        floatingFrame.setOpacityPercent(FloatingKeyboardSettings.opacityPercent(viewPrefs(), OrientedPrefs.current(this)));
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

    /** Names the layout the globe key just moved to, so a five-way cycle is not a guessing game. */
    private void announceLayout(KeyboardLayoutId id) {
        showFunctionToast(LetterLayouts.displayName(id));
    }

    private SharedPreferences viewPrefs() {
        return getSharedPreferences("retekey_view", MODE_PRIVATE);
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
        ProjectKeyEvent projectEvent = KeyEventNormalizer.fromAndroid(
            keyCode,
            event,
            hardwareMapper
        );
        DispatchResult result = dispatcher.dispatch(projectEvent);
        if (result.actions().isEmpty()) {
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
        super.onDestroy();
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
            boolean abandon = CursorMovePolicy.shouldAbandonComposition(
                inputProcessor.isComposing(),
                newSelStart,
                newSelEnd,
                candidatesStart,
                candidatesEnd
            );
            if (!abandon && candidatesStart < 0 && inputProcessor.isComposing()) {
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
        if (candidatesWindowForced) {
            // The one-pixel input view is what gives the candidate window a window to live on.
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

    private void updateHardwareMapper(InputMethodSubtype subtype) {
        hardwareKoreanMode = isKoreanSubtype(subtype);
        applyHardwareMode();
    }

    /** Selects the physical-key mapper for the current Hangul mode and editor kind. */
    private void applyHardwareMode() {
        hardwareMapper = !usesRawKeyCompatibility() && hardwareKoreanMode
            ? DubeolsikHardwareMapper.INSTANCE
            : HardwareSemanticMapper.none();
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
        if (keyboardView != null && isInputViewShown()) {
            keyboardView.setUnicodeEntry(true);
        }
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
        String character = unicodeEntry.character();
        if (isInputViewShown() && keyboardView != null) {
            keyboardView.setUnicodePreview(character == null
                ? unicodeEntry.display()
                : unicodeEntry.display() + "   " + character);
            return;
        }
        // No keyboard on screen — a hardware keyboard is doing the typing, and the pad that would
        // have shown the code is hidden with it. The candidate window is the one surface an IME
        // can raise without one, so it carries the feedback in that case.
        List<HanjaCandidatesView.Item> items = new ArrayList<>(1);
        if (character != null) {
            items.add(new HanjaCandidatesView.Item(character,
                UnicodeEntry.label(unicodeEntry.codePoint())));
        }
        pendingFromSelection = false;
        pendingDeleteLength = 0;
        showHanjaCandidates(unicodeEntry.display(), items);
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
        if (keyboardView != null) {
            keyboardView.setUnicodeEntry(false);
        }
        hideHanjaCandidatesIfShown();
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
            case TEXT:
                flushNotepadComposition();
                notepad.type(input.text());
                return true;
            case DELETE_BACKWARD: {
                if (notepad.isComposing()) {
                    // Inside a syllable, backspace takes it apart rather than deleting it whole.
                    HangulComposer.Result result = notepadComposer.backspace();
                    notepad.typeComposed("", result == null ? "" : result.preedit());
                    return true;
                }
                notepadComposer.reset();
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
        String flushed = notepadComposer.flush();
        if (notepad == null) {
            return;
        }
        if (!flushed.isEmpty()) {
            notepad.typeComposed(flushed, "");
        }
        notepad.endComposition();
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
        if (showCandidateWindow()) {
            return;
        }
        // Nothing of this IME is on screen — an external keyboard is doing the typing — so there is
        // no window to attach a popup to. Bring one up, one pixel tall, and try again once it is.
        candidatesWindowForced = true;
        if (keyboardView != null) {
            keyboardView.setCollapsed(true);
        }
        updateInputViewShown();
        showSelfForCandidates();
        candidateWindowAttempts = 0;
        retryCandidateWindow();
    }

    /**
     * Brings this IME's own window up so the candidate window has something to attach to.
     * {@code requestShowSelf} is API 28; {@code showWindow} has been there since the beginning and
     * does the same job from inside the service.
     */
    @SuppressWarnings("deprecation")
    private void showSelfForCandidates() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            requestShowSelf(0);
        } else {
            showWindow(true);
        }
    }

    /** Shows the candidate window if this IME already has a window to attach it to. */
    private boolean showCandidateWindow() {
        View anchor = anchorView();
        if (anchor == null || anchor.getWindowToken() == null) {
            return false;
        }
        if (hanjaWindow == null) {
            hanjaWindow = new HanjaCandidatesWindow(this, this::commitHanja, this::hideHanjaCandidates);
        }
        int[] frame = keyboardFrameOnScreen();
        hanjaWindow.show(anchor, pendingReading, pendingCandidates, frame[0], frame[1], frame[2]);
        return true;
    }

    private static final int CANDIDATE_WINDOW_ATTEMPTS = 20;
    private static final long CANDIDATE_WINDOW_RETRY_MS = 50;

    /** Waits for the just-requested window to be attached, then shows the candidates on it. */
    private void retryCandidateWindow() {
        mainHandler.postDelayed(() -> {
            if (!hanjaCandidatesShown) {
                return;
            }
            if (showCandidateWindow()) {
                return;
            }
            if (++candidateWindowAttempts < CANDIDATE_WINDOW_ATTEMPTS) {
                retryCandidateWindow();
            } else {
                // The window never arrived; do not leave a claim standing for a panel that is not
                // going to appear.
                hideHanjaCandidates();
            }
        }, CANDIDATE_WINDOW_RETRY_MS);
    }

    /** Any attached view of this IME; the candidate window only needs its window token. */
    private View anchorView() {
        return floatingFrame != null ? floatingFrame : keyboardView;
    }

    /**
     * Where on screen the keyboard is, as {@code {left, width, top}}, so the candidate window can
     * span it and sit above it. A top of 0 means nothing is on screen to sit above — an external
     * keyboard is doing the typing — and the panel falls back to the foot of the screen at full
     * width.
     */
    private int[] keyboardFrameOnScreen() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int[] location = new int[2];
        if (floatingMode && floatingFrame != null) {
            floatingFrame.getLocationOnScreen(location);
            android.graphics.Rect panel = floatingFrame.panelBounds();
            return new int[] {
                location[0] + panel.left, panel.width(), location[1] + panel.top
            };
        }
        if (keyboardView == null || keyboardView.getHeight() <= 0 || !keyboardView.isShown()) {
            return new int[] {0, screenWidth, 0};
        }
        keyboardView.getLocationOnScreen(location);
        return new int[] {location[0], keyboardView.getWidth(), location[1]};
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
        if (hanjaWindow != null) {
            hanjaWindow.hide();
        }
        if (candidatesWindowForced) {
            // A window brought up only to carry the candidates goes away with them.
            candidatesWindowForced = false;
            if (keyboardView != null) {
                keyboardView.setCollapsed(false);
            }
            updateInputViewShown();
            requestHideSelf(0);
        }
    }

    /**
     * While the candidate strip is up, a number key 1–9 picks that candidate, the page keys/arrows
     * turn the page, and Escape dismisses. Returns true when the key was used for the strip.
     */
    private boolean handleHanjaSelectionKey(int keyCode) {
        if (hanjaWindow == null) {
            return false;
        }
        int number = digitFromKeyCode(keyCode);
        if (number >= 1) {
            hanjaWindow.selectByNumber(number);
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_PAGE_DOWN:
                hanjaWindow.nextPage();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_PAGE_UP:
                hanjaWindow.prevPage();
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
        return inputConnection == null
            ? null
            : EditorEndpoint.of(
                sessionController.generation(),
                new InputConnectionEditorBridge(inputConnection)
            );
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
