package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class NotepadTextScaleTest {
    @Test
    public void theScaleStaysBetweenLegibleAndUsable() {
        assertEquals(NotepadTextScale.MIN_PERCENT, NotepadTextScale.clamp(10));
        assertEquals(NotepadTextScale.MAX_PERCENT, NotepadTextScale.clamp(10000));
        assertEquals(120, NotepadTextScale.clamp(120));
    }

    @Test
    public void aPinchMultipliesWhereItStarted() {
        assertEquals(200, NotepadTextScale.scaled(100, 2.0f));
        assertEquals(75, NotepadTextScale.scaled(150, 0.5f));
        // Ending where it began leaves the size alone rather than shaving a percent off.
        assertEquals(133, NotepadTextScale.scaled(133, 1.0f));
    }

    @Test
    public void aPinchCannotEscapeTheBounds() {
        assertEquals(NotepadTextScale.MAX_PERCENT, NotepadTextScale.scaled(280, 3.0f));
        assertEquals(NotepadTextScale.MIN_PERCENT, NotepadTextScale.scaled(70, 0.1f));
    }

    @Test
    public void nonsenseFactorsLeaveTheSizeWhereItWas() {
        assertEquals(120, NotepadTextScale.scaled(120, 0f));
        assertEquals(120, NotepadTextScale.scaled(120, -2f));
        assertEquals(120, NotepadTextScale.scaled(120, Float.NaN));
        assertEquals(120, NotepadTextScale.scaled(120, Float.POSITIVE_INFINITY));
    }

    @Test
    public void sizesFollowTheScaleAndNeverVanish() {
        assertEquals(16.0f, NotepadTextScale.sizeOf(16f, 100), 0.001f);
        assertEquals(32.0f, NotepadTextScale.sizeOf(16f, 200), 0.001f);
        assertTrue(NotepadTextScale.sizeOf(0.01f, NotepadTextScale.MIN_PERCENT) >= 1.0f);
    }
}
