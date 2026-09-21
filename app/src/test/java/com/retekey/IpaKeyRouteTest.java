package com.retekey;

import org.junit.Assert;
import org.junit.Test;

/**
 * A physical key while the phonetic search is open answers the way an on-screen key does
 * (consumeForIpaPanel): a letter builds the query, a delete takes one back, anything else closes
 * the search and is typed as usual. Before this, a physical letter went to the editor while the
 * query stayed empty (review finding R20).
 */
public final class IpaKeyRouteTest {
    private static final int A = 29;          // KeyEvent.KEYCODE_A
    private static final int DEL = 67;        // KeyEvent.KEYCODE_DEL
    private static final int ESCAPE = 111;    // KeyEvent.KEYCODE_ESCAPE
    private static final int ENTER = 66;      // KeyEvent.KEYCODE_ENTER
    private static final int SPACE = 62;      // KeyEvent.KEYCODE_SPACE
    private static final int SHIFT_LEFT = 59; // KeyEvent.KEYCODE_SHIFT_LEFT
    private static final int DIGIT_3 = 10;    // KeyEvent.KEYCODE_3
    private static final int DPAD_RIGHT = 22; // KeyEvent.KEYCODE_DPAD_RIGHT
    private static final int PAGE_DOWN = 93;  // KeyEvent.KEYCODE_PAGE_DOWN

    @Test
    public void aLetterJoinsTheQuery() {
        IpaKeyRoute route = IpaKeyRoute.of(true, false, A, 'a', false, false);
        Assert.assertEquals(IpaKeyRoute.Kind.APPEND, route.kind());
        Assert.assertEquals("a", route.text());
        Assert.assertTrue(route.consumes());
    }

    @Test
    public void aShiftedLetterJoinsAsTheCapitalItTypes() {
        Assert.assertEquals("N", IpaKeyRoute.of(true, false, 42, 'N', false, false).text());
    }

    @Test
    public void aDeleteTakesOneBackAndClosesAnEmptySearch() {
        Assert.assertEquals(IpaKeyRoute.Kind.BACKSPACE,
            IpaKeyRoute.of(true, false, DEL, 0, false, false).kind());
        Assert.assertEquals(IpaKeyRoute.Kind.CLOSE,
            IpaKeyRoute.of(true, true, DEL, 0, false, false).kind());
        Assert.assertTrue(IpaKeyRoute.of(true, true, DEL, 0, false, false).consumes());
    }

    @Test
    public void escapeClosesAndIsNotTyped() {
        IpaKeyRoute route = IpaKeyRoute.of(true, false, ESCAPE, 0x1b, false, false);
        Assert.assertEquals(IpaKeyRoute.Kind.CLOSE, route.kind());
        Assert.assertTrue(route.consumes());
    }

    /** Shift on its own is how a capital is typed: the search stays, the key goes on as usual. */
    @Test
    public void aModifierAloneLeavesTheSearchOpen() {
        IpaKeyRoute route = IpaKeyRoute.of(true, false, SHIFT_LEFT, 0, true, false);
        Assert.assertEquals(IpaKeyRoute.Kind.PASS, route.kind());
        Assert.assertFalse(route.consumes());
    }

    /** The candidate window's own keys pick and turn pages, as they do for Hanja. */
    @Test
    public void digitsArrowsAndPagesStayWithTheCandidates() {
        for (int key : new int[] {DIGIT_3, DPAD_RIGHT, PAGE_DOWN}) {
            IpaKeyRoute route = IpaKeyRoute.of(true, false, key, key == DIGIT_3 ? '3' : 0, false, false);
            Assert.assertEquals(IpaKeyRoute.Kind.PASS, route.kind());
        }
    }

    @Test
    public void enterSpaceAndChordsCloseTheSearchAndGoOnAsUsual() {
        Assert.assertEquals(IpaKeyRoute.Kind.CLOSE_AND_CONTINUE,
            IpaKeyRoute.of(true, false, ENTER, '\n', false, false).kind());
        Assert.assertEquals(IpaKeyRoute.Kind.CLOSE_AND_CONTINUE,
            IpaKeyRoute.of(true, false, SPACE, ' ', false, false).kind());
        IpaKeyRoute chord = IpaKeyRoute.of(true, false, A, 'a', false, true);
        Assert.assertEquals(IpaKeyRoute.Kind.CLOSE_AND_CONTINUE, chord.kind());
        Assert.assertFalse(chord.consumes());
    }

    /** The family list answers taps on itself; a letter there closes it and is typed. */
    @Test
    public void theFamilyListTakesNoLetters() {
        Assert.assertEquals(IpaKeyRoute.Kind.CLOSE_AND_CONTINUE,
            IpaKeyRoute.of(false, false, A, 'a', false, false).kind());
        Assert.assertEquals(IpaKeyRoute.Kind.CLOSE,
            IpaKeyRoute.of(false, false, ESCAPE, 0x1b, false, false).kind());
    }
}
