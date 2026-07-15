package dev.hellgates.retekeyime;

import org.junit.Assert;
import org.junit.Test;

public final class KeyLabelFitTest {
    private static final float EPS = 0.001f;

    @Test
    public void keepsTheCapWhenTheLabelAlreadyFits() {
        // A narrow label under the allowed width paints at the full height-derived cap.
        float size = KeyLabelFit.fitSize(30.0f, 40.0f, 80.0f, 12.0f);
        Assert.assertEquals(40.0f, size, EPS);
    }

    @Test
    public void shrinksProportionallyWhenTheLabelIsTooWide() {
        // Measured 100 at cap 40 but only 80 allowed -> 40 * 80/100 = 32.
        float size = KeyLabelFit.fitSize(100.0f, 40.0f, 80.0f, 12.0f);
        Assert.assertEquals(32.0f, size, EPS);
    }

    @Test
    public void neverDropsBelowTheLegibilityFloor() {
        // A very wide label would compute tiny; the floor holds it up.
        float size = KeyLabelFit.fitSize(400.0f, 40.0f, 20.0f, 12.0f);
        Assert.assertEquals(12.0f, size, EPS);
    }

    @Test
    public void fontTracksCellSizeSoLargerCellsGetLargerText() {
        // Same label proportions, a taller cell cap yields a larger fitted size.
        float small = KeyLabelFit.fitSize(50.0f, 30.0f, 60.0f, 8.0f);
        float large = KeyLabelFit.fitSize(75.0f, 45.0f, 90.0f, 8.0f);
        Assert.assertTrue("larger cell should fit larger text", large > small);
    }

    @Test
    public void treatsZeroWidthMeasurementAsFitting() {
        float size = KeyLabelFit.fitSize(0.0f, 40.0f, 80.0f, 12.0f);
        Assert.assertEquals(40.0f, size, EPS);
    }
}
