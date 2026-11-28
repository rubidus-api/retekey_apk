package com.retekey;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** RFC-0014 F9: a clip taken off the list stays off only while the clipboard still holds it. */
public class ForgottenClipTest {
    @Test
    public void aForgottenClipIsHeldBackUntilTheClipboardMovesOn() {
        ForgottenClip forgotten = new ForgottenClip();
        forgotten.forget("address A");
        assertTrue(forgotten.holdsBack("address A"));
        assertTrue("still on the clipboard, still held back", forgotten.holdsBack("address A"));
        assertFalse(forgotten.holdsBack("phone B"));
        assertFalse("copied again later, it is recorded again", forgotten.holdsBack("address A"));
    }

    @Test
    public void nothingForgottenHoldsNothingBack() {
        ForgottenClip forgotten = new ForgottenClip();
        assertFalse(forgotten.holdsBack("x"));
        assertFalse(forgotten.holdsBack(null));
        forgotten.forget("x");
        assertFalse("an empty clipboard is the clipboard moving on", forgotten.holdsBack(null));
        assertFalse(forgotten.holdsBack("x"));
    }
}
