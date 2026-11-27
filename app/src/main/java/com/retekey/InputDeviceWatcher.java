package com.retekey;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Handler;
import com.retekey.core.Fn;

/**
 * Isolated API 16+ {@link InputManager.InputDeviceListener} wrapper so {@link ReteKeyImeService}
 * stays free of direct API 16 class references under Dalvik verification on API 14–15.
 */
@TargetApi(16)
final class InputDeviceWatcher {
    private final InputManager.InputDeviceListener listener;

    private InputDeviceWatcher(InputManager.InputDeviceListener listener) {
        this.listener = listener;
    }

    static InputDeviceWatcher start(
            Context context, Handler handler, final Fn.IntConsumer onDeviceGone) {
        try {
            InputManager input = Compat.systemService(
                context, Context.INPUT_SERVICE, InputManager.class);
            if (input == null) {
                return null;
            }
            InputManager.InputDeviceListener listener = new InputManager.InputDeviceListener() {
                @Override
                public void onInputDeviceAdded(int deviceId) {
                }

                @Override
                public void onInputDeviceRemoved(int deviceId) {
                    onDeviceGone.accept(deviceId);
                }

                @Override
                public void onInputDeviceChanged(int deviceId) {
                    onDeviceGone.accept(deviceId);
                }
            };
            input.registerInputDeviceListener(listener, handler);
            return new InputDeviceWatcher(listener);
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    void stop(Context context) {
        try {
            InputManager input = Compat.systemService(
                context, Context.INPUT_SERVICE, InputManager.class);
            if (input != null) {
                input.unregisterInputDeviceListener(listener);
            }
        } catch (RuntimeException ignored) {
            // Nothing to unhook.
        }
    }
}
