package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** When a materialised syllable is let go of after a pause (issue #7). */
public final class IdleSyllableSettleTest {
    @Test
    public void onlyAnEditorThatMaterialisesCompositionWaitsOnTheClock() {
        assertTrue("a terminal or a remote desktop, mid-syllable",
            IdleSyllableSettle.shouldArm(true, true));
        assertFalse("nothing being built", IdleSyllableSettle.shouldArm(true, false));
        assertFalse("an ordinary editor keeps its composing region and needs no clock",
            IdleSyllableSettle.shouldArm(false, true));
    }

    @Test
    public void theWaitSitsOutsideOrdinaryTyping() {
        // Long enough that nobody building a syllable hits it, short enough to expire before a
        // hand reaches a pointer and clicks elsewhere.
        assertEquals(1500L, IdleSyllableSettle.DELAY_MS);
    }
}
