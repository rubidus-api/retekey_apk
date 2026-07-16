package dev.hellgates.retekeyime;

import android.content.Intent;
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

    @Override
    public View onCreateInputView() {
        keyboardView = new ReteKeyboardView(this, this::dispatchSoftwareInput);
        keyboardView.setOnOpenSettings(this::openSettings);
        keyboardView.setOnEditCommand(this::performEditCommand);
        keyboardView.setOnInsertDate(this::insertCurrentDate);
        keyboardView.setOnSwitchIme(this::showImePicker);
        keyboardView.setOnManageIme(this::openKeyboardManagement);
        return keyboardView;
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
        ExecutionResult result = execute(dispatcher.dispatch(event));
        if (result == null || result.isFailure()) {
            showEditorFailure();
        }
    }

    private EditorProfile currentEditorProfile() {
        return editorProfile;
    }

    private void updateHardwareMapper(InputMethodSubtype subtype) {
        hardwareMapper = !usesRawKeyCompatibility() && isKoreanSubtype(subtype)
            ? DubeolsikHardwareMapper.INSTANCE
            : HardwareSemanticMapper.none();
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
