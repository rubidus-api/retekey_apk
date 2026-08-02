package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

/** The naming rule that keeps one setting's two values apart. */
public final class ScreenOrientationTest {
    @Test
    public void eachOrientationHasItsOwnKey() {
        assertEquals("height_scale.portrait", ScreenOrientation.PORTRAIT.key("height_scale"));
        assertEquals("height_scale.landscape", ScreenOrientation.LANDSCAPE.key("height_scale"));
        assertNotEquals(
            ScreenOrientation.PORTRAIT.key("x"), ScreenOrientation.LANDSCAPE.key("x"));
    }

    @Test
    public void theKeyFromBeforeTheSplitIsTheBareOne() {
        // What a keyboard already in use wrote, and still reads until each side is set on its own.
        assertEquals("height_scale", ScreenOrientation.legacyKey("height_scale"));
        assertNotEquals(
            ScreenOrientation.legacyKey("height_scale"),
            ScreenOrientation.PORTRAIT.key("height_scale"));
    }

    @Test
    public void aWideScreenIsLandscapeAndASquareOneIsNot() {
        assertEquals(ScreenOrientation.LANDSCAPE, ScreenOrientation.of(1920, 1080));
        assertEquals(ScreenOrientation.PORTRAIT, ScreenOrientation.of(1080, 1920));
        assertEquals(ScreenOrientation.PORTRAIT, ScreenOrientation.of(1000, 1000));
    }

    @Test
    public void eachIsTheOthersOther() {
        assertEquals(ScreenOrientation.LANDSCAPE, ScreenOrientation.PORTRAIT.other());
        assertEquals(ScreenOrientation.PORTRAIT, ScreenOrientation.LANDSCAPE.other());
    }

    @Test
    public void anEmptyKeyIsRefusedRatherThanNamingSomethingUnfindable() {
        for (String bad : new String[] {null, ""}) {
            try {
                ScreenOrientation.PORTRAIT.key(bad);
                fail("expected a refusal for " + bad);
            } catch (IllegalArgumentException expected) {
                // The point.
            }
        }
    }
}
