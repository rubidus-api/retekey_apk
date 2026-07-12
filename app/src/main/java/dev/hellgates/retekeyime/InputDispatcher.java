package dev.hellgates.retekeyime;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class InputDispatcher {
    private static final int MAX_TRACKED_HARDWARE_KEYS = 32;

    private final StatelessInputProcessor processor;
    private final Set<PressedKey> consumedHardwareKeys = new LinkedHashSet<>();

    public InputDispatcher() {
        this(new ScaffoldInputProcessor());
    }

    public InputDispatcher(StatelessInputProcessor processor) {
        this.processor = Objects.requireNonNull(processor, "processor");
    }

    public DispatchResult dispatch(ProjectKeyEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (event.source() == InputSource.HARDWARE) {
            return dispatchHardware(event);
        }
        if (event.action() != InputAction.DOWN || event.repeatCount() != 0 || event.canceled()) {
            return DispatchResult.handled();
        }
        DispatchResult result = dispatchSemantic(event);
        return result.isHandled() ? result : DispatchResult.handled(result.actions());
    }

    public void reset() {
        consumedHardwareKeys.clear();
    }

    private DispatchResult dispatchHardware(ProjectKeyEvent event) {
        PressedKey key = new PressedKey(event.deviceId(), event.keyCode());

        if (event.action() == InputAction.UP) {
            return consumedHardwareKeys.remove(key)
                ? DispatchResult.handled()
                : DispatchResult.delegate();
        }
        if (event.action() == InputAction.MULTIPLE) {
            boolean tracked = consumedHardwareKeys.contains(key);
            if (tracked && event.canceled()) {
                consumedHardwareKeys.remove(key);
            }
            return tracked ? DispatchResult.handled() : DispatchResult.delegate();
        }
        if (event.action() != InputAction.DOWN) {
            return DispatchResult.delegate();
        }
        if (event.canceled()) {
            return consumedHardwareKeys.remove(key)
                ? DispatchResult.handled()
                : DispatchResult.delegate();
        }
        if (event.repeatCount() != 0) {
            return consumedHardwareKeys.contains(key)
                ? DispatchResult.handled()
                : DispatchResult.delegate();
        }

        consumedHardwareKeys.remove(key);
        if (event.keyCode() == 0) {
            return DispatchResult.delegate();
        }

        DispatchResult result = dispatchSemantic(event);
        if (result.isHandled()) {
            if (consumedHardwareKeys.size() >= MAX_TRACKED_HARDWARE_KEYS) {
                return DispatchResult.delegate();
            }
            consumedHardwareKeys.add(key);
        }
        return result;
    }

    private DispatchResult dispatchSemantic(ProjectKeyEvent event) {
        if (event.ctrl() || event.alt() || event.meta() || event.function() || event.sym() ||
            event.hasDeadKey()) {
            return DispatchResult.delegate();
        }
        if (!event.hasSemanticInput()) {
            return DispatchResult.delegate();
        }

        return Objects.requireNonNull(
            processor.process(event.semanticInput()),
            "processor result"
        );
    }

    private static final class PressedKey {
        private final int deviceId;
        private final int keyCode;

        private PressedKey(int deviceId, int keyCode) {
            this.deviceId = deviceId;
            this.keyCode = keyCode;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof PressedKey)) {
                return false;
            }
            PressedKey that = (PressedKey) other;
            return deviceId == that.deviceId && keyCode == that.keyCode;
        }

        @Override
        public int hashCode() {
            return Objects.hash(deviceId, keyCode);
        }
    }
}
