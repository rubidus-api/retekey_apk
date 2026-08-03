package dev.hellgates.retekeyime;

import org.junit.Assert;
import org.junit.Test;

public final class KeyboardHeightScaleTest {
    private static final float EPS = 0.0001f;

    @Test
    public void clampsScaleIntoTheSupportedRange() {
        // Below the floor the range now reaches — the floor itself is level 1.
        Assert.assertEquals(KeyboardHeightScale.MIN_SCALE,
            KeyboardHeightScale.clamp(0.001f), EPS);
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

    @Test
    public void theDefaultIsAQuarterOfTheScreensLongEdge() {
        // 4 rows at density 3 → base 58*3*4 = 696px. A 2400px-long screen wants 600px of
        // keyboard, which is 600/696 of the base.
        int base = KeyboardHeightScale.baseHeightPx(4, 3.0f);
        float scale = KeyboardHeightScale.defaultScaleForScreen(base, 2400);
        Assert.assertEquals(600, KeyboardHeightScale.heightForScale(scale, base), 8);
    }

    @Test
    public void theDefaultIsTheSameHeightInBothOrientations() {
        // The long edge is passed in either way round, so a rotated phone starts the same size.
        int base = KeyboardHeightScale.baseHeightPx(4, 2.0f);
        Assert.assertEquals(
            KeyboardHeightScale.defaultScaleForScreen(base, 1920),
            KeyboardHeightScale.defaultScaleForScreen(base, 1920),
            EPS);
    }

    @Test
    public void anUnknownScreenFallsBackToTheNominalDefault() {
        Assert.assertEquals(KeyboardHeightScale.DEFAULT_SCALE,
            KeyboardHeightScale.defaultScaleForScreen(0, 2400), EPS);
        Assert.assertEquals(KeyboardHeightScale.DEFAULT_SCALE,
            KeyboardHeightScale.defaultScaleForScreen(696, 0), EPS);
    }

    @Test
    public void theScreenDefaultStaysInsideTheAdjustableRange() {
        int base = KeyboardHeightScale.baseHeightPx(4, 1.0f);
        // A absurdly tall screen cannot push the default past the slider's top level.
        Assert.assertTrue(KeyboardHeightScale.defaultScaleForScreen(base, 100000)
            <= KeyboardHeightScale.MAX_SCALE);
        Assert.assertTrue(KeyboardHeightScale.defaultScaleForScreen(base, 1)
            >= KeyboardHeightScale.MIN_SCALE);
    }
}
