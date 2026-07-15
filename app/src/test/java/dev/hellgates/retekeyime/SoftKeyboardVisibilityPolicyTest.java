package dev.hellgates.retekeyime;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SoftKeyboardVisibilityPolicyTest {
    @Test
    public void hideWhenHardwareHidesOnlyWhileAHardwareKeyboardIsActive() {
        assertFalse(SoftKeyboardVisibilityPolicy.shouldShow(
            true, SoftKeyboardVisibilityPolicy.Mode.HIDE_WHEN_HARDWARE));
        assertTrue(SoftKeyboardVisibilityPolicy.shouldShow(
            false, SoftKeyboardVisibilityPolicy.Mode.HIDE_WHEN_HARDWARE));
    }

    @Test
    public void alwaysShowKeepsTheKeyboardRegardlessOfHardware() {
        assertTrue(SoftKeyboardVisibilityPolicy.shouldShow(
            true, SoftKeyboardVisibilityPolicy.Mode.ALWAYS_SHOW));
        assertTrue(SoftKeyboardVisibilityPolicy.shouldShow(
            false, SoftKeyboardVisibilityPolicy.Mode.ALWAYS_SHOW));
    }

    @Test(expected = IllegalArgumentException.class)
    public void aNullModeIsRejected() {
        SoftKeyboardVisibilityPolicy.shouldShow(true, null);
    }
}
