package com.retekey;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
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

    @Test
    public void theStoredFlagIsTheMode() {
        // The setting is a checkbox — "keep the keyboard on screen" — and this is the only place
        // that turns it into a mode, so a false default keeps the behaviour every version had.
        assertEquals(SoftKeyboardVisibilityPolicy.Mode.ALWAYS_SHOW,
            SoftKeyboardVisibilityPolicy.modeOf(true));
        assertEquals(SoftKeyboardVisibilityPolicy.Mode.HIDE_WHEN_HARDWARE,
            SoftKeyboardVisibilityPolicy.modeOf(false));
        assertFalse(SoftKeyboardVisibilityPolicy.DEFAULT_ALWAYS_SHOW);
    }
}
