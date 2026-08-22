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
    /** The themed layer is a bitmap per density; this is the largest, for inspection. */
    private static final String MONOCHROME = "res/drawable-xxxhdpi/ic_retekey_monochrome.png";
    private static final String[] DENSITIES = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
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
                boolean vector = Files.exists(resources().resolve("res/drawable/" + name + ".xml"));
                boolean bitmap = Files.exists(
                    resources().resolve("res/drawable-xxxhdpi/" + name + ".png"));
                assertTrue("@drawable/" + name + " does not exist as a vector or a bitmap",
                    vector || bitmap);
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
    public void theMonochromeLayerExistsAtEveryDensity() {
        // A themed launcher picks the bitmap for the device's density; a missing bucket falls back
        // to a scaled neighbour, which is blurry rather than broken, but there is no reason to ship
        // it that way.
        for (String density : DENSITIES) {
            assertTrue("no themed layer for " + density, Files.exists(
                resources().resolve("res/drawable-" + density + "/ic_retekey_monochrome.png")));
        }
    }

    @Test
    public void theThemedLayerIsTheCapsWithTheLettersPunchedThrough() throws IOException {
        // A themed launcher fills the background with the theme's light accent and paints whatever
        // is opaque here in its dark on-colour. So the caps are the opaque mark — no plate — and
        // the lettering is a hole through them, so the theme's background shows through in the
        // letter's shape. Read off the pixels: most of a cap is opaque, some of its middle is not,
        // and nothing in the outer margin the launcher mask discards is painted.
        //
        // Decoded by hand: Android unit tests compile against the android.jar stub, which has no
        // AWT/ImageIO, and Bitmap is a stub that throws. PNG is small enough to read directly.
        Png image = Png.read(Files.readAllBytes(resources().resolve(MONOCHROME)));
        int size = image.width;
        assertEquals("square", size, image.height);
        double scale = SAFE_ZONE / 48.0;
        double px = size / VIEWPORT;
        java.util.function.DoubleUnaryOperator toPx = v -> (v * scale + 21.0) * px;
        int left = (int) toPx.applyAsDouble(4.7), right = (int) toPx.applyAsDouble(13.3);
        int top = (int) toPx.applyAsDouble(9.3), bottom = (int) toPx.applyAsDouble(22.7);
        int opaque = 0, clear = 0;
        for (int y = top + 2; y < bottom - 2; y++) {
            for (int x = left + 2; x < right - 2; x++) {
                int alpha = image.alpha(x, y);
                if (alpha > 200) {
                    opaque++;
                } else if (alpha < 55) {
                    clear++;
                }
            }
        }
        assertTrue("the cap is mostly solid", opaque > clear * 2);
        assertTrue("the R is punched through it: " + clear + " clear pixels", clear > 0);
        for (int i = 0; i < size; i += 7) {
            assertEquals("margin must be transparent at (" + i + ",3)", 0, image.alpha(i, 3));
            assertEquals("margin must be transparent at (3," + i + ")", 0, image.alpha(3, i));
        }
    }

    /**
     * Just enough PNG to read the alpha of an 8-bit RGBA image written by Pillow: IHDR, the IDAT
     * stream inflated, and the five filter types undone. Nothing else — palettes, 16-bit, interlace
     * — because nothing else is ever written here, and the assertion that it is 8-bit RGBA is part
     * of the test.
     */
    private static final class Png {
        final int width;
        final int height;
        private final byte[] rgba;

        private Png(int width, int height, byte[] rgba) {
            this.width = width;
            this.height = height;
            this.rgba = rgba;
        }

        int alpha(int x, int y) {
            return rgba[(y * width + x) * 4 + 3] & 0xFF;
        }

        static Png read(byte[] file) throws IOException {
            java.nio.ByteBuffer in = java.nio.ByteBuffer.wrap(file);
            in.position(8); // signature
            int width = 0, height = 0;
            java.io.ByteArrayOutputStream idat = new java.io.ByteArrayOutputStream();
            while (in.remaining() >= 12) {
                int length = in.getInt();
                byte[] type = new byte[4];
                in.get(type);
                String name = new String(type, StandardCharsets.US_ASCII);
                byte[] body = new byte[length];
                in.get(body);
                in.getInt(); // crc
                if (name.equals("IHDR")) {
                    java.nio.ByteBuffer h = java.nio.ByteBuffer.wrap(body);
                    width = h.getInt();
                    height = h.getInt();
                    int depth = h.get() & 0xFF, colour = h.get() & 0xFF;
                    assertEquals("8-bit", 8, depth);
                    assertEquals("RGBA (colour type 6)", 6, colour);
                    assertEquals("not interlaced", 0, body[12]);
                } else if (name.equals("IDAT")) {
                    idat.write(body);
                } else if (name.equals("IEND")) {
                    break;
                }
            }
            java.util.zip.Inflater inflater = new java.util.zip.Inflater();
            inflater.setInput(idat.toByteArray());
            int stride = width * 4;
            byte[] raw = new byte[(stride + 1) * height];
            try {
                int got = 0;
                while (got < raw.length && !inflater.finished()) {
                    int n = inflater.inflate(raw, got, raw.length - got);
                    if (n == 0 && inflater.needsInput()) {
                        break;
                    }
                    got += n;
                }
                assertEquals("inflated the whole image", raw.length, got);
            } catch (java.util.zip.DataFormatException e) {
                throw new IOException(e);
            }
            byte[] out = new byte[stride * height];
            byte[] prior = new byte[stride];
            for (int y = 0; y < height; y++) {
                int filter = raw[y * (stride + 1)] & 0xFF;
                int src = y * (stride + 1) + 1;
                byte[] line = new byte[stride];
                for (int i = 0; i < stride; i++) {
                    int a = i >= 4 ? line[i - 4] & 0xFF : 0;
                    int b = prior[i] & 0xFF;
                    int c = i >= 4 ? prior[i - 4] & 0xFF : 0;
                    int x = raw[src + i] & 0xFF;
                    int v;
                    switch (filter) {
                        case 0: v = x; break;
                        case 1: v = x + a; break;
                        case 2: v = x + b; break;
                        case 3: v = x + ((a + b) >>> 1); break;
                        case 4: {
                            int p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b),
                                pc = Math.abs(p - c);
                            v = x + (pa <= pb && pa <= pc ? a : pb <= pc ? b : c);
                            break;
                        }
                        default: throw new IOException("filter " + filter);
                    }
                    line[i] = (byte) v;
                }
                System.arraycopy(line, 0, out, y * stride, stride);
                prior = line;
            }
            return new Png(width, height, out);
        }
    }

    @Test
    public void theOrdinaryIconStillUsesBothTones() {
        // Guards the test above from passing for the wrong reason — if the icon itself went
        // one-colour, the monochrome check would be trivially satisfied and say nothing.
        assertEquals(2, paintedColours(read(LEGACY)).size());
    }

    @Test
    public void bothLayersDrawInsideTheSafeZone() {
        // The themed layer is a bitmap now; its margin is checked pixel by pixel above. The
        // foreground is still a vector with a viewport to inspect.
        for (String layer : new String[]{FOREGROUND}) {
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
