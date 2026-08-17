package com.retekey;

/**
 * Decides whether the on-screen keyboard should show when a hardware or Bluetooth keyboard is
 * attached. Platform-neutral: the service supplies the observed hardware-keyboard state and the
 * user's mode, and this returns the decision. Hardware key input passes through the service either
 * way, so hiding the soft view does not stop typing.
 */
public final class SoftKeyboardVisibilityPolicy {
    /** The preference behind {@link Mode#ALWAYS_SHOW}. */
    public static final String KEY_ALWAYS_SHOW = "keyboard_always_shown";
    public static final boolean DEFAULT_ALWAYS_SHOW = false;

    /** The mode a stored flag means. */
    public static Mode modeOf(boolean alwaysShow) {
        return alwaysShow ? Mode.ALWAYS_SHOW : Mode.HIDE_WHEN_HARDWARE;
    }

    public enum Mode {
        /** Hide the on-screen keyboard while a hardware keyboard is usable. */
        HIDE_WHEN_HARDWARE,
        /** Keep the on-screen keyboard even with a hardware keyboard attached. */
        ALWAYS_SHOW
    }

    private SoftKeyboardVisibilityPolicy() {
    }

    /**
     * @param hardwareKeyboardActive a physical/Bluetooth keyboard is attached and not hidden
     * @param mode the user's chosen behavior
     * @return whether the on-screen keyboard view should be shown
     */
    public static boolean shouldShow(boolean hardwareKeyboardActive, Mode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }
        if (mode == Mode.ALWAYS_SHOW) {
            return true;
        }
        return !hardwareKeyboardActive;
    }
}
