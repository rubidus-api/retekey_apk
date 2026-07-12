package dev.hellgates.retekeyime;

import android.view.inputmethod.InputConnection;
import java.lang.reflect.Proxy;
import org.junit.Assert;
import org.junit.Test;

public final class InputConnectionEditorBridgeTest {
    @Test
    public void mapsBooleanResultsWithoutRetainingExceptionDetails() {
        InputConnection rejected = connectionReturning(false, null, false);
        InputConnection throwing = connectionReturning(false, null, true);

        Assert.assertTrue(new InputConnectionEditorBridge(rejected)
            .commitText("private", 1).isRejected());
        Assert.assertEquals(
            EditorCallResult.Kind.RUNTIME_FAILURE,
            new InputConnectionEditorBridge(throwing).finishComposingText().kind()
        );
    }

    @Test
    public void mapsSurroundingTextValueNullAndRuntimeFailureWithoutLoggingText() {
        String privateText = "private-surrounding-text";
        EditorTextResult value = new InputConnectionEditorBridge(
            connectionReturning(true, privateText, false)
        ).getTextBeforeCursor(2, 0);
        EditorTextResult nullValue = new InputConnectionEditorBridge(
            connectionReturning(true, null, false)
        ).getTextBeforeCursor(2, 0);
        EditorTextResult failure = new InputConnectionEditorBridge(
            connectionReturning(true, null, true)
        ).getTextBeforeCursor(2, 0);

        Assert.assertEquals(privateText, value.value());
        Assert.assertEquals(EditorTextResult.Kind.NULL_VALUE, nullValue.kind());
        Assert.assertEquals(EditorTextResult.Kind.RUNTIME_FAILURE, failure.kind());
        Assert.assertFalse(value.toString().contains(privateText));
    }

    private static InputConnection connectionReturning(
        boolean booleanResult,
        CharSequence textResult,
        boolean throwRuntime
    ) {
        return (InputConnection) Proxy.newProxyInstance(
            InputConnection.class.getClassLoader(),
            new Class<?>[]{InputConnection.class},
            (proxy, method, arguments) -> {
                if (throwRuntime) {
                    throw new IllegalStateException("must-not-escape");
                }
                if (method.getName().equals("getTextBeforeCursor")) {
                    return textResult;
                }
                if (method.getReturnType() == boolean.class) {
                    return booleanResult;
                }
                if (method.getName().equals("toString")) {
                    return "redacted-input-connection";
                }
                return null;
            }
        );
    }
}
