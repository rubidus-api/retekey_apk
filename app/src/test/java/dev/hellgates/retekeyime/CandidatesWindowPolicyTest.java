package dev.hellgates.retekeyime;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class CandidatesWindowPolicyTest {
    @Test
    public void unrequestedSoftInputForcesTheWindowOpen() {
        // A hardware keyboard suppresses the on-screen keyboard, and then the IME window is never
        // displayed; the candidate strip would be invisible unless the service asks for it.
        assertTrue(CandidatesWindowPolicy.mustForceWindow(false));
    }

    @Test
    public void requestedSoftInputNeedsNoRequest() {
        assertFalse(CandidatesWindowPolicy.mustForceWindow(true));
    }

    @Test
    public void aForcedWindowIsGivenBackWhenTheStripCloses() {
        assertTrue(CandidatesWindowPolicy.mustReleaseWindow(true));
    }

    @Test
    public void aWindowThatWasAlreadyUpIsLeftAlone() {
        // Hiding the strip must never pull the keyboard out from under a user who is typing on it.
        assertFalse(CandidatesWindowPolicy.mustReleaseWindow(false));
    }
}
