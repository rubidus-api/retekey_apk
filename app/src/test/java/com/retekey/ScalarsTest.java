package com.retekey;

import org.junit.Assert;
import org.junit.Test;

/**
 * Cutting text by a number of UTF-16 units splits a character made of two of them: 4000 units of
 * "…😀" ended in half an emoji, which is not text any more (review finding R08). Every cut in the
 * keyboard goes through here instead.
 */
public final class ScalarsTest {
    private static final String EMOJI = "😀";

    @Test
    public void shortTextIsNotCut() {
        Assert.assertEquals("abc", Scalars.truncate("abc", 4));
        Assert.assertEquals("abc", Scalars.truncate("abc", 3));
        Assert.assertNull(Scalars.truncate(null, 3));
    }

    @Test
    public void aCutThatWouldSplitACharacterStopsBeforeIt() {
        String text = "abc" + EMOJI;
        Assert.assertEquals("abc", Scalars.truncate(text, 4));
        Assert.assertEquals("abc" + EMOJI, Scalars.truncate(text, 5));
        Assert.assertEquals("abc", Scalars.truncate(text, 3));
    }

    @Test
    public void everyCutOfEmojiTextIsWholeCharacters() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            text.append('a').append(EMOJI);
        }
        for (int limit = 0; limit <= text.length(); limit++) {
            String cut = Scalars.truncate(text.toString(), limit);
            Assert.assertTrue("limit " + limit, cut.length() <= limit);
            for (int i = 0; i < cut.length(); i++) {
                char ch = cut.charAt(i);
                if (Character.isHighSurrogate(ch)) {
                    Assert.assertTrue("high surrogate at the end, limit " + limit,
                        i + 1 < cut.length() && Character.isLowSurrogate(cut.charAt(i + 1)));
                }
                Assert.assertFalse("lone low surrogate, limit " + limit,
                    Character.isLowSurrogate(ch) && (i == 0
                        || !Character.isHighSurrogate(cut.charAt(i - 1))));
            }
        }
    }

    /** Malformed input is not repaired, only left un-split: a lone surrogate is not carried over. */
    @Test
    public void aLoneSurrogateIsNotKeptAtACut() {
        Assert.assertEquals("a", Scalars.truncate("a\uD83Db", 2));
    }

    @Test
    public void deletingTakesAWholeCharacter() {
        Assert.assertEquals("a", Scalars.withoutLastScalar("a" + EMOJI));
        Assert.assertEquals("", Scalars.withoutLastScalar(EMOJI));
        Assert.assertEquals("ab", Scalars.withoutLastScalar("abc"));
        Assert.assertEquals("", Scalars.withoutLastScalar(""));
        Assert.assertEquals("", Scalars.withoutLastScalar("\uD83D"));
    }
}
