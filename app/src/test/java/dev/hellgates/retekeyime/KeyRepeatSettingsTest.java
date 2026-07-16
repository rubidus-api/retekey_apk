package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class KeyRepeatSettingsTest {
    @Test
    public void delayClampsToBounds() {
        assertEquals(KeyRepeatSettings.MIN_DELAY_MS, KeyRepeatSettings.clampDelay(0));
        assertEquals(KeyRepeatSettings.MAX_DELAY_MS, KeyRepeatSettings.clampDelay(99999));
        assertEquals(300, KeyRepeatSettings.clampDelay(300));
    }

    @Test
    public void intervalClampsToBounds() {
        assertEquals(KeyRepeatSettings.MIN_INTERVAL_MS, KeyRepeatSettings.clampInterval(1));
        assertEquals(KeyRepeatSettings.MAX_INTERVAL_MS, KeyRepeatSettings.clampInterval(99999));
        assertEquals(75, KeyRepeatSettings.clampInterval(75));
    }

    @Test
    public void defaultsAreInsideTheirRanges() {
        assertEquals(KeyRepeatSettings.DEFAULT_DELAY_MS,
            KeyRepeatSettings.clampDelay(KeyRepeatSettings.DEFAULT_DELAY_MS));
        assertEquals(KeyRepeatSettings.DEFAULT_INTERVAL_MS,
            KeyRepeatSettings.clampInterval(KeyRepeatSettings.DEFAULT_INTERVAL_MS));
    }
}
