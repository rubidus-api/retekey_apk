package dev.hellgates.retekeyime;

import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;

public final class ReteKeyImeService extends InputMethodService {
    private final ReteInputEngine engine = new ReteInputEngine();

    @Override
    public View onCreateInputView() {
        return new ReteKeyboardView(this, action -> applyAction(getCurrentInputConnection(), action));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        KeyAction action = engine.onHardwareKey(KeyEventNormalizer.fromAndroid(keyCode, event));
        if (action.kind() == KeyAction.Kind.NOOP) {
            return super.onKeyDown(keyCode, event);
        }
        applyAction(getCurrentInputConnection(), action);
        return true;
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
            case NOOP:
            default:
                break;
        }
    }
}
