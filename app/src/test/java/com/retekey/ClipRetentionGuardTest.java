package com.retekey;

import org.junit.Assert;
import org.junit.Test;

/**
 * What the keyboard may keep from Android's clipboard without being asked. A copy made in a
 * password field (visible or not) was kept in the clip history, unmarked, because the clipboard
 * listener judged a clip only by the copying app's own marking (review finding R18). The field
 * the user is in, and the one they have just left, now count too; and a clip withheld once stays
 * withheld while it is still the clipboard's, however the keyboard comes back to it later.
 */
public final class ClipRetentionGuardTest {
    @Test
    public void anOrdinaryFieldKeepsAnOrdinaryClip() {
        ClipRetentionGuard guard = new ClipRetentionGuard();
        guard.onField(false, 1_000);
        Assert.assertTrue(guard.mayKeep("hello", false, 1_100));
    }

    @Test
    public void aMarkedClipIsNeverKept() {
        ClipRetentionGuard guard = new ClipRetentionGuard();
        guard.onField(false, 1_000);
        Assert.assertFalse(guard.mayKeep("hunter2", true, 1_100));
    }

    @Test
    public void nothingIsKeptWhileAPrivateFieldIsTheOneBeingTyped() {
        ClipRetentionGuard guard = new ClipRetentionGuard();
        guard.onField(true, 1_000);
        Assert.assertFalse(guard.mayKeep("hunter2", false, 1_100));
    }

    /** The listener can be told after the focus has already moved on. */
    @Test
    public void aClipArrivingJustAfterLeavingAPrivateFieldIsWithheld() {
        ClipRetentionGuard guard = new ClipRetentionGuard();
        guard.onField(true, 1_000);
        guard.onField(false, 5_000);
        Assert.assertFalse(guard.mayKeep("hunter2", false, 5_000 + 900));
    }

    @Test
    public void aClipLongAfterLeavingIsJudgedByTheFieldItWasCopiedIn() {
        ClipRetentionGuard guard = new ClipRetentionGuard();
        guard.onField(true, 1_000);
        guard.onField(false, 5_000);
        Assert.assertTrue(guard.mayKeep("hello", false, 5_000 + 1_500));
    }

    /** Withheld once, it is not picked up later when the keyboard catches up in another field. */
    @Test
    public void aWithheldClipStaysWithheldWhileItIsStillOnTheClipboard() {
        ClipRetentionGuard guard = new ClipRetentionGuard();
        guard.onField(true, 1_000);
        Assert.assertFalse(guard.mayKeep("hunter2", false, 1_100));
        guard.onField(false, 2_000);
        Assert.assertFalse(guard.mayKeep("hunter2", false, 60_000));
        // The clipboard moved on: the next clip is an ordinary one again.
        Assert.assertTrue(guard.mayKeep("hello", false, 61_000));
    }

    @Test
    public void theGuardHoldsNoCopyOfAWithheldClip() {
        ClipRetentionGuard guard = new ClipRetentionGuard();
        guard.onField(true, 1_000);
        guard.mayKeep("hunter2", false, 1_100);
        Assert.assertFalse(guard.toString().contains("hunter2"));
    }

    @Test
    public void beforeAnyFieldOnlyTheClipDecides() {
        // Started, not yet told about any field: there is no field to hold against the clip.
        Assert.assertTrue(new ClipRetentionGuard().mayKeep("hello", false, 10));
        Assert.assertFalse(new ClipRetentionGuard().mayKeep(null, false, 10));
    }

    @Test
    public void passwordsAndNoLearningFieldsArePrivate() {
        Assert.assertTrue(ClipRetentionGuard.isPrivateField(true, 0));
        Assert.assertTrue(ClipRetentionGuard.isPrivateField(false,
            ClipRetentionGuard.IME_FLAG_NO_PERSONALIZED_LEARNING | 0x6));
        Assert.assertFalse(ClipRetentionGuard.isPrivateField(false, 0x6));
        // The constant is the platform's own.
        Assert.assertEquals(android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING,
            ClipRetentionGuard.IME_FLAG_NO_PERSONALIZED_LEARNING);
    }
}
