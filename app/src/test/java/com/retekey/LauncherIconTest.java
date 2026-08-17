package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * Reads the real launcher icon resources, not fixtures, because what goes wrong with an icon is a
 * missing file rather than a wrong calculation. An adaptive icon whose monochrome layer is absent or
 * misnamed builds, installs and looks perfectly correct; it goes wrong only on Android 13 with
 * themed icons switched on, on somebody else's phone. The same in reverse for the {@code -v26}
 * qualifier: an adaptive icon in an unqualified folder would be handed to the legacy flavour's
 * API 14 floor, which cannot read it, and the app would install with no icon at all.
 */
public final class LauncherIconTest {
    private static final String ADAPTIVE = "res/drawable-anydpi-v26/ic_retekey.xml";
    private static final String FOREGROUND = "res/drawable/ic_retekey_foreground.xml";
    private static final String MONOCHROME = "res/drawable/ic_retekey_monochrome.xml";
    private static final String LEGACY = "res/drawable/ic_retekey.xml";
    private static final String COLORS = "res/values/colors.xml";

    /** Adaptive icon layers are 108 units square, of which only the middle 66 is guaranteed. */
    private static final double VIEWPORT = 108.0;
    private static final double SAFE_ZONE = 66.0;

    private static Path resources() {
        for (String candidate : new String[]{"src/main", "app/src/main"}) {
            Path path = Paths.get(candidate);
            if (Files.isDirectory(path)) {
                return path;
            }
        }
        throw new AssertionError("main sources not found from working dir "
            + Paths.get("").toAbsolutePath());
    }

    private static String read(String relative) {
        Path path = resources().resolve(relative);
        assertTrue(path + " is missing", Files.exists(path));
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static double attribute(String xml, String name) {
        Matcher matcher = Pattern.compile("android:" + name + "=\"([0-9.]+)(dp)?\"").matcher(xml);
        assertTrue("no android:" + name + " in the drawable", matcher.find());
        return Double.parseDouble(matcher.group(1));
    }

    /** Every colour literal the drawable paints with, transparent ones excluded. */
    private static Set<String> paintedColours(String xml) {
        Set<String> colours = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("(?:fillColor|strokeColor)=\"(#[0-9A-Fa-f]+)\"")
            .matcher(xml);
        while (matcher.find()) {
            String colour = matcher.group(1).toUpperCase();
            if (!colour.equals("#00000000")) {
                colours.add(colour);
            }
        }
        return colours;
    }

    @Test
    public void adaptiveIconDeclaresAllThreeLayers() {
        String xml = read(ADAPTIVE);
        assertTrue("no <background>", xml.contains("<background"));
        assertTrue("no <foreground>", xml.contains("<foreground"));
        assertTrue("themed icons need a monochrome layer", xml.contains("<monochrome"));
        assertTrue("layers must point at the foreground drawable",
            xml.contains("@drawable/ic_retekey_foreground"));
        assertTrue("layers must point at the monochrome drawable",
            xml.contains("@drawable/ic_retekey_monochrome"));
    }

    @Test
    public void everyLayerPointsAtSomethingThatExists() {
        String xml = read(ADAPTIVE);
        Matcher matcher = Pattern.compile("android:drawable=\"@(drawable|color)/([A-Za-z0-9_]+)\"")
            .matcher(xml);
        int layers = 0;
        while (matcher.find()) {
            layers++;
            String kind = matcher.group(1);
            String name = matcher.group(2);
            if (kind.equals("drawable")) {
                assertTrue("@drawable/" + name + " does not exist",
                    Files.exists(resources().resolve("res/drawable/" + name + ".xml")));
            } else {
                assertTrue("@color/" + name + " is not defined",
                    read(COLORS).contains("name=\"" + name + "\""));
            }
        }
        assertEquals("an adaptive icon has three layers here", 3, layers);
    }

    @Test
    public void theAdaptiveIconIsOnlyEverBehindTheV26Qualifier() {
        // The legacy flavour goes down to API 14. It must keep being handed the plain vector.
        String legacy = read(LEGACY);
        assertFalse("the unqualified icon must not be an adaptive icon",
            legacy.contains("adaptive-icon"));
        assertFalse("an unqualified adaptive icon would reach API 14",
            Files.exists(resources().resolve("res/drawable/ic_retekey_adaptive.xml")));
    }

    @Test
    public void theMonochromeLayerPaintsInOneColourOnly() {
        // The system tints this layer as a whole. Two colours in it means one of them is a lie:
        // whatever the second tone was meant to say is lost the moment the theme recolours it.
        Set<String> colours = paintedColours(read(MONOCHROME));
        assertEquals("the monochrome layer must paint in exactly one colour: " + colours,
            1, colours.size());
    }

    @Test
    public void theOrdinaryIconStillUsesBothTones() {
        // Guards the test above from passing for the wrong reason — if the icon itself went
        // one-colour, the monochrome check would be trivially satisfied and say nothing.
        assertEquals(2, paintedColours(read(LEGACY)).size());
    }

    @Test
    public void bothLayersDrawInsideTheSafeZone() {
        for (String layer : new String[]{FOREGROUND, MONOCHROME}) {
            String xml = read(layer);
            assertEquals(layer + " is not on the 108-unit canvas",
                VIEWPORT, attribute(xml, "viewportWidth"), 0.001);
            assertEquals(layer + " is not on the 108-unit canvas",
                VIEWPORT, attribute(xml, "viewportHeight"), 0.001);

            // The art is authored 48 units wide and scaled into the middle of the canvas by a
            // group. Check the group actually lands inside the 66 units a launcher's mask keeps.
            Matcher scale = Pattern.compile("android:scaleX=\"([0-9.]+)\"").matcher(xml);
            Matcher offset = Pattern.compile("android:translateX=\"([0-9.]+)\"").matcher(xml);
            assertTrue(layer + " has no scaling group", scale.find());
            assertTrue(layer + " has no offset group", offset.find());
            double drawn = 48.0 * Double.parseDouble(scale.group(1));
            double left = Double.parseDouble(offset.group(1));
            assertTrue(layer + " draws " + drawn + " units wide, wider than the safe zone",
                drawn <= SAFE_ZONE + 0.001);
            assertEquals(layer + " is not centred", (VIEWPORT - drawn) / 2, left, 0.001);
        }
    }
}
