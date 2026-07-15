package dev.hellgates.retekeyime;

import org.junit.Assert;
import org.junit.Test;

public final class KeyboardHeightScaleTest {
    private static final float EPS = 0.0001f;

    @Test
    public void clampsScaleIntoTheSupportedRange() {
        Assert.assertEquals(KeyboardHeightScale.MIN_SCALE,
            KeyboardHeightScale.clamp(0.1f), EPS);
        Assert.assertEquals(KeyboardHeightScale.MAX_SCALE,
            KeyboardHeightScale.clamp(9.0f), EPS);
        Assert.assertEquals(1.0f, KeyboardHeightScale.clamp(1.0f), EPS);
    }

    @Test
    public void fallsBackToDefaultOnNaN() {
        Assert.assertEquals(KeyboardHeightScale.DEFAULT_SCALE,
            KeyboardHeightScale.clamp(Float.NaN), EPS);
    }

    @Test
    public void baseHeightIsPerRowTimesRowsAtDensity() {
        int base = KeyboardHeightScale.baseHeightPx(4, 2.0f);
        int perRow = Math.round(KeyboardHeightScale.BASE_ROW_DP * 2.0f);
        Assert.assertEquals(perRow * 4, base);
    }

    @Test
    public void baseHeightTreatsZeroRowsAsOne() {
        Assert.assertEquals(
            KeyboardHeightScale.baseHeightPx(1, 1.5f),
            KeyboardHeightScale.baseHeightPx(0, 1.5f));
    }

    @Test
    public void heightForScaleThenScaleForHeightRoundTrips() {
        int base = KeyboardHeightScale.baseHeightPx(4, 3.0f);
        int h = KeyboardHeightScale.heightForScale(1.2f, base);
        float recovered = KeyboardHeightScale.scaleForHeight(h, base);
        Assert.assertEquals(1.2f, recovered, 0.01f);
    }

    @Test
    public void scaleForHeightClampsAndGuardsZeroBase() {
        Assert.assertEquals(KeyboardHeightScale.DEFAULT_SCALE,
            KeyboardHeightScale.scaleForHeight(500, 0), EPS);
        // A tall request against a small base is capped at the maximum.
        Assert.assertEquals(KeyboardHeightScale.MAX_SCALE,
            KeyboardHeightScale.scaleForHeight(100_000, 200), EPS);
    }
}
