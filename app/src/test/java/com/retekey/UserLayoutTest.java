package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;

/**
 * The layout file somebody else writes (issue #11). The format's promise is that it stays readable,
 * so what it accepts and what it ignores are both tested.
 */
public final class UserLayoutTest {
    private static final String GREEK = String.join("\n",
        "retekey-layout 1",
        "name: Greek phonetic",
        "cap: grk",
        "row: α β γ|ϝ δ ε|έ ζ η|ή θ ι|ί κ",
        "row: λ μ ν ξ ο|ό π ρ σ|ς τ",
        "row: υ|ύ φ χ ψ ω|ώ");

    @Test
    public void itReadsTheKeysAndWhatTheyHold() {
        UserLayout layout = UserLayout.parse(GREEK);
        assertNotNull(layout);
        assertEquals("Greek phonetic", layout.name());
        assertEquals("grk", layout.cap());
        assertEquals(3, layout.rows().size());
        assertEquals(10, layout.rows().get(0).size());
        assertEquals("α", layout.rows().get(0).get(0).types);
        assertTrue(layout.rows().get(0).get(0).holds.isEmpty());
        assertEquals("γ", layout.rows().get(0).get(2).types);
        assertEquals(Arrays.asList("ϝ"), layout.rows().get(0).get(2).holds);
        assertEquals("σ", layout.rows().get(1).get(7).types);
        assertEquals(Arrays.asList("ς"), layout.rows().get(1).get(7).holds);
    }

    @Test
    public void aLineFromALaterVersionIsIgnoredRatherThanRefused() {
        // The whole compatibility story in one test: a file written for a future ReteKey still
        // works in this one, minus whatever this one does not know about.
        UserLayout layout = UserLayout.parse(GREEK
            + "\nflick: α=left\nbottom-row: custom\n# a comment\n");
        assertNotNull(layout);
        assertEquals("Greek phonetic", layout.name());
        assertEquals(3, layout.rows().size());
    }

    @Test
    public void theExampleOnTheSettingsScreenIsARealLayout() {
        // The example is shown so the format can be learned from it; if the parser ever stopped
        // accepting it, the screen would be teaching a format the app does not read.
        UserLayout example = UserLayout.parse(UserLayout.EXAMPLE);
        assertNotNull(example);
        assertEquals("Greek phonetic", example.name());
        assertEquals("grk", example.cap());
        assertEquals(UserLayout.ROWS, example.rows().size());
        assertEquals("α", example.rows().get(0).get(0).types);
        assertEquals(Arrays.asList("ϝ"), example.rows().get(0).get(2).holds);
    }

    @Test
    public void whatIsNotALayoutIsNotOne() {
        assertFalse(UserLayout.looksLikeOne("a shared sentence"));
        assertNull(UserLayout.parse("a shared sentence"));
        assertNull("a layout needs a name", UserLayout.parse("retekey-layout 1\nrow: a b c"));
        assertNull("and three rows", UserLayout.parse(
            "retekey-layout 1\nname: Half\nrow: a b c\nrow: d e f"));
    }

    @Test
    public void aRowIsCappedAtTheGridsWidthAndAKeyAtWhatItCanHold() {
        UserLayout layout = UserLayout.parse(String.join("\n",
            "retekey-layout 1",
            "name: Wide",
            "row: a b c d e f g h i j k l m",
            "row: n o p",
            "row: q|1|2|3|4|5|6|7 r"));
        assertNotNull(layout);
        assertEquals(UserLayout.MAX_KEYS_PER_ROW, layout.rows().get(0).size());
        assertEquals(UserLayout.MAX_HOLDS, layout.rows().get(2).get(0).holds.size());
    }

    @Test
    public void theCapIsThreeLettersEvenWhenTheFileForgets() {
        UserLayout named = UserLayout.parse(
            "retekey-layout 1\nname: Ogham\nrow: a b\nrow: c d\nrow: e f");
        assertNotNull(named);
        assertEquals(3, named.cap().length());
        assertEquals("Ogh", named.cap());
        UserLayout tiny = UserLayout.parse(
            "retekey-layout 1\nname: X\nrow: a b\nrow: c d\nrow: e f");
        assertNotNull(tiny);
        assertEquals(3, tiny.cap().length());
    }

    /**
     * The version is the whole word, not its beginning: `retekey-layout 10` is a format this
     * version has never seen, and reading it as version 1 is how a later file would be
     * misunderstood key for key (review finding R09).
     */
    @Test
    public void onlyTheVersionThisBuildKnowsIsRead() {
        assertNull(UserLayout.parse(GREEK.replace("retekey-layout 1", "retekey-layout 10")));
        assertNull(UserLayout.parse(GREEK.replace("retekey-layout 1", "retekey-layout 1.5")));
        assertNull(UserLayout.parse(GREEK.replace("retekey-layout 1", "retekey-layout")));
        assertNotNull(UserLayout.parse(GREEK.replace("retekey-layout 1", "retekey-layout 1 ")));
        assertEquals(UserLayout.Problem.WRONG_VERSION,
            UserLayout.check(GREEK.replace("retekey-layout 1", "retekey-layout 10")).problem());
    }

    /** A Windows editor writes a byte-order mark: invisible, and not the file's fault. */
    @Test
    public void aByteOrderMarkBeforeTheHeaderIsNotAFault() {
        assertNotNull(UserLayout.parse("\uFEFF" + GREEK));
    }

    /**
     * Bounds, so that a file from somewhere else cannot make the keyboard hold a key that types
     * two hundred thousand characters (R09). They are far above anything a layout needs: the
     * largest of the documented examples is 186 characters with keys of two.
     */
    @Test
    public void aLayoutThatIsNotOfALayoutsSizeIsRefused() {
        assertEquals(UserLayout.Problem.NONE, UserLayout.check(GREEK).problem());

        String hugeKey = GREEK.replace("row: α", "row: " + repeat("x", UserLayout.MAX_TYPES + 1));
        assertNull(UserLayout.parse(hugeKey));
        assertEquals(UserLayout.Problem.KEY_TOO_LONG, UserLayout.check(hugeKey).problem());

        String hugeHold = GREEK.replace("γ|ϝ", "γ|" + repeat("y", UserLayout.MAX_HOLD + 1));
        assertEquals(UserLayout.Problem.KEY_TOO_LONG, UserLayout.check(hugeHold).problem());

        String longLine = GREEK + "\nnote: " + repeat("z", UserLayout.MAX_LINE);
        assertEquals(UserLayout.Problem.LINE_TOO_LONG, UserLayout.check(longLine).problem());

        String huge = GREEK + "\n#" + repeat("w", UserLayout.MAX_TEXT);
        assertEquals(UserLayout.Problem.TOO_BIG, UserLayout.check(huge).problem());
        assertNull(UserLayout.parse(huge));
    }

    /** A key at the limit still works: the bound is a ceiling, not a new shape. */
    @Test
    public void aKeyAtTheLimitIsAccepted() {
        String atLimit = GREEK.replace("row: α", "row: " + repeat("x", UserLayout.MAX_TYPES));
        assertNotNull(UserLayout.parse(atLimit));
        String holdAtLimit = GREEK.replace("γ|ϝ", "γ|" + repeat("y", UserLayout.MAX_HOLD));
        assertNotNull(UserLayout.parse(holdAtLimit));
    }

    /** What the user is told when a file will not do. */
    @Test
    public void everyRefusalHasAReasonOfItsOwn() {
        assertEquals(UserLayout.Problem.NOT_A_LAYOUT, UserLayout.check("a shared sentence").problem());
        assertEquals(UserLayout.Problem.NOT_A_LAYOUT, UserLayout.check(null).problem());
        assertEquals(UserLayout.Problem.NO_NAME,
            UserLayout.check("retekey-layout 1\nrow: a\nrow: b\nrow: c").problem());
        assertEquals(UserLayout.Problem.NOT_THREE_ROWS,
            UserLayout.check("retekey-layout 1\nname: two\nrow: a\nrow: b").problem());
        assertNotNull(UserLayout.check(GREEK).layout());
        assertNull(UserLayout.check("a shared sentence").layout());
    }

    private static String repeat(String s, int times) {
        StringBuilder out = new StringBuilder(s.length() * times);
        for (int i = 0; i < times; i++) {
            out.append(s);
        }
        return out.toString();
    }
}
