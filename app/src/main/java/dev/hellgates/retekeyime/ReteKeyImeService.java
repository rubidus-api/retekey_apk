package dev.hellgates.retekeyime;

import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.widget.Toast;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import java.util.Locale;

public class ReteKeyImeService extends InputMethodService {
    private final InputDispatcher dispatcher = new InputDispatcher();
    private final InputSessionController<ScaffoldSessionState> sessionController =
        new InputSessionController<>();
    private HardwareSemanticMapper hardwareMapper = HardwareSemanticMapper.none();
    private EditorProfile editorProfile = EditorProfile.unsupported();
    private boolean sessionActive;
    private Toast editorFailureToast;

    @Override
    public View onCreateInputView() {
        return new ReteKeyboardView(this, this::dispatchSoftwareInput);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
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
        updateHardwareMapper(currentSubtype());
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype newSubtype) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype);
        dispatcher.reset();
        updateHardwareMapper(newSubtype);
    }

    @Override
    public void onFinishInput() {
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
        // View-only state will be cleared here when the P3 pointer state machine lands.
        // Deliberately skip the default composing finish; onFinishInput owns that boundary.
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

    private void dispatchSoftwareInput(ProjectKeyEvent event) {
        ExecutionResult result = execute(dispatcher.dispatch(event));
        if (result == null || result.isFailure()) {
            showEditorFailure();
        }
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
        EditorExpectation expectation = EditorBoundsPredictor.expectationAfter(
            sessionController.workingBounds(),
            result.actions()
        );
        TransitionPlan<ScaffoldSessionState> plan = sessionController.planWithExpectation(
            result,
            ScaffoldSessionState.EMPTY,
            expectation
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
