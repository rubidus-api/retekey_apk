package dev.hellgates.retekeyime;

import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import java.util.Locale;

public final class ReteKeyImeService extends InputMethodService {
    private final InputDispatcher dispatcher = new InputDispatcher();
    private HardwareSemanticMapper hardwareMapper = HardwareSemanticMapper.none();

    @Override
    public View onCreateInputView() {
        return new ReteKeyboardView(this, this::dispatchSoftwareInput);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        DispatchResult result = dispatcher.dispatch(
            KeyEventNormalizer.fromAndroid(keyCode, event, hardwareMapper)
        );
        applyActions(result);
        if (result.isHandled()) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        DispatchResult result = dispatcher.dispatch(
            KeyEventNormalizer.fromAndroid(keyCode, event, hardwareMapper)
        );
        applyActions(result);
        if (result.isHandled()) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
        DispatchResult result = dispatcher.dispatch(
            KeyEventNormalizer.fromAndroid(keyCode, event, hardwareMapper)
        );
        applyActions(result);
        if (result.isHandled()) {
            return true;
        }
        return super.onKeyMultiple(keyCode, count, event);
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        dispatcher.reset();
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
        dispatcher.reset();
        super.onFinishInput();
    }

    @Override
    public void onUnbindInput() {
        dispatcher.reset();
        hardwareMapper = HardwareSemanticMapper.none();
        super.onUnbindInput();
    }

    @Override
    public void onDestroy() {
        dispatcher.reset();
        super.onDestroy();
    }

    private void dispatchSoftwareInput(ProjectKeyEvent event) {
        applyActions(dispatcher.dispatch(event));
    }

    private void updateHardwareMapper(InputMethodSubtype subtype) {
        hardwareMapper = isKoreanSubtype(subtype)
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

    private void applyActions(DispatchResult result) {
        InputConnection inputConnection = getCurrentInputConnection();
        for (KeyAction action : result.actions()) {
            applyAction(inputConnection, action);
        }
    }

    private void applyAction(InputConnection inputConnection, KeyAction action) {
        if (inputConnection == null) {
            return;
        }

        switch (action.kind()) {
            case COMMIT_TEXT:
                inputConnection.commitText(action.text(), 1);
                break;
            case DELETE_BACKWARD:
                inputConnection.deleteSurroundingText(1, 0);
                break;
            case SET_COMPOSING_TEXT:
                inputConnection.setComposingText(action.text(), 1);
                break;
            case FINISH_COMPOSING:
                inputConnection.finishComposingText();
                break;
            default:
                break;
        }
    }
}
