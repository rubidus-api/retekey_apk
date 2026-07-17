package dev.hellgates.retekeyime;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReteKeyImeService extends InputMethodService {
    private final HangulInputProcessor inputProcessor =
        new HangulInputProcessor(this::currentEditorProfile);
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
    // initialised from the current subtype. The 한자 bindings are recognised but conversion is
    // still pending, so they only surface a one-time notice per input session.
    private boolean hardwareKoreanMode;
    private List<HardwareKeyBindings.Binding> hanyeongBindings = java.util.Collections.emptyList();
    private List<HardwareKeyBindings.Binding> hanjaBindings = java.util.Collections.emptyList();
    private Toast functionToast;
    private static final int HANJA_LOOKBEHIND = 8;
    private HanjaCandidatesView candidatesView;
    private String pendingReading;
    private List<HanjaCandidatesView.Item> pendingCandidates;
    private boolean pendingFromSelection;
    private int pendingDeleteLength;
    private boolean hanjaCandidatesShown;

    @Override
    public View onCreateInputView() {
        keyboardView = new ReteKeyboardView(this, this::dispatchSoftwareInput);
        keyboardView.setOnOpenSettings(this::openSettings);
        keyboardView.setOnEditCommand(this::performEditCommand);
        keyboardView.setOnInsertDate(this::insertCurrentDate);
        keyboardView.setOnSwitchIme(this::showImePicker);
        keyboardView.setOnManageIme(this::openKeyboardManagement);
        keyboardView.setOnHanja(this::handleHanja);
        reloadHardwareBindings();
        HanjaDictionary.preload(this);
        return keyboardView;
    }

    @Override
    public View onCreateCandidatesView() {
        candidatesView = new HanjaCandidatesView(this);
        candidatesView.setOnPick(this::commitHanja);
        if (pendingCandidates != null) {
            candidatesView.show(pendingReading, pendingCandidates);
        }
        return candidatesView;
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
        DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy. MM. dd.(E) HH:mm", Locale.KOREAN);
        String stamp = LocalDateTime.now().format(formatter);
        dispatchSoftwareInput(
            ProjectKeyEvent.softwareDown("touch.menu.date", SemanticInput.text(stamp)));
    }

    /** Opens the system input-method picker (keyboard chooser) from the 키보드전환 tile. */
    private void showImePicker() {
        InputMethodManager manager = getSystemService(InputMethodManager.class);
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
        if (hanjaCandidatesShown && handleHanjaSelectionKey(keyCode)) {
            return true;
        }
        hideHanjaCandidatesIfShown();
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
        }
        reloadHardwareBindings();
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
    public boolean onEvaluateInputViewShown() {
        super.onEvaluateInputViewShown();
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
            || HardwareKeyBindings.matches(hanjaBindings, keyCode, mods);
    }

    /** Runs the 한/영 or 한자 action for a matching physical key; returns true when it acted. */
    private boolean handleHardwareFunctionKey(int keyCode, KeyEvent event) {
        int mods = pressedMods(event);
        if (HardwareKeyBindings.matches(hanyeongBindings, keyCode, mods)) {
            toggleHardwareKorean();
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
    private void handleHanja() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }
        HanjaTable dictionary = HanjaDictionary.get(this);
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
        if (HanjaTable.isHangul(lastCodePoint)) {
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
    private boolean convertFromSelection(HanjaTable dictionary, String selection) {
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
        HunumTable glosses = HanjaDictionary.hunum(this);
        List<HanjaCandidatesView.Item> items = new ArrayList<>(hanja.size());
        for (String candidate : hanja) {
            items.add(new HanjaCandidatesView.Item(candidate, glosses.gloss(candidate)));
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
        setCandidatesViewShown(true);
        if (candidatesView != null) {
            candidatesView.show(reading, candidates);
        }
        hanjaCandidatesShown = true;
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
        setCandidatesViewShown(false);
    }

    /**
     * While the candidate strip is up, a number key 1–9 picks that candidate, the page keys/arrows
     * turn the page, and Escape dismisses. Returns true when the key was used for the strip.
     */
    private boolean handleHanjaSelectionKey(int keyCode) {
        if (candidatesView == null) {
            return false;
        }
        int number = digitFromKeyCode(keyCode);
        if (number >= 1) {
            candidatesView.selectByNumber(number);
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_PAGE_DOWN:
                candidatesView.nextPage();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_PAGE_UP:
                candidatesView.prevPage();
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
        InputMethodManager manager = getSystemService(InputMethodManager.class);
        return manager == null ? null : manager.getCurrentInputMethodSubtype();
    }

    @SuppressWarnings("deprecation")
    private static boolean isKoreanSubtype(InputMethodSubtype subtype) {
        if (subtype == null) {
            return false;
        }
        String languageTag = subtype.getLanguageTag();
        if (languageTag == null || languageTag.isEmpty()) {
            languageTag = subtype.getLocale().replace('_', '-');
        }
        return "ko".equals(Locale.forLanguageTag(languageTag).getLanguage());
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
