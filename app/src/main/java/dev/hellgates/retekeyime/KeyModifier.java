package dev.hellgates.retekeyime;

/**
 * A platform-neutral modifier that can ride along with a raw key to form a chord. The Android
 * editor bridge folds these into a {@code KeyEvent} meta state.
 */
public enum KeyModifier {
    CTRL,
    ALT,
    SHIFT,
    META
}
