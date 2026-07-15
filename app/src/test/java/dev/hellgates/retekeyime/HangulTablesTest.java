package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Exhaustive validation of the ported jamo tables against the Unicode Hangul model. A single wrong
 * index in a table would surface here rather than as a mistyped syllable in the field.
 */
public final class HangulTablesTest {
    @Test
    public void everyChoJungComposesToAModernHangulSyllable() {
        for (int cho = 0; cho < HangulTables.CHO_COUNT; cho++) {
            for (int jung = 0; jung < HangulTables.JUNG_COUNT; jung++) {
                char syllable = HangulTables.compose(cho, jung, -1);
                assertTrue(
                    "cho " + cho + " jung " + jung + " is a syllable",
                    syllable >= 0xAC00 && syllable <= 0xD7A3
                );
                // The Unicode decomposition must recover the same indices.
                int index = syllable - 0xAC00;
                assertEquals(cho, index / (21 * 28));
                assertEquals(jung, (index / 28) % 21);
                assertEquals(0, index % 28);
            }
        }
    }

    @Test
    public void everyJongPlacesInTheSyllable() {
        for (int jong = 0; jong < HangulTables.JONG_COUNT; jong++) {
            char syllable = HangulTables.compose(0, 0, jong);
            assertEquals(jong, (syllable - 0xAC00) % 28);
        }
    }

    @Test
    public void composeIsZeroWhenIncomplete() {
        assertEquals(0, HangulTables.compose(-1, 0, -1));
        assertEquals(0, HangulTables.compose(0, -1, -1));
    }

    @Test
    public void compoundVowelsAreSymmetricWithTheirSplit() {
        int[][] vowels = {{8, 0, 9}, {8, 1, 10}, {8, 20, 11}, {13, 4, 14},
            {13, 5, 15}, {13, 20, 16}, {18, 20, 19}};
        for (int[] v : vowels) {
            assertEquals("combine", v[2], HangulTables.combineJung(v[0], v[1]));
            int[] split = HangulTables.splitJung(v[2]);
            assertNotNull(split);
            assertEquals("split first", v[0], split[0]);
            assertEquals("split second", v[1], split[1]);
        }
    }

    @Test
    public void nonCompoundVowelsDoNotSplit() {
        for (int jung = 0; jung < HangulTables.JUNG_COUNT; jung++) {
            boolean compound = jung == 9 || jung == 10 || jung == 11
                || jung == 14 || jung == 15 || jung == 16 || jung == 19;
            if (!compound) {
                assertNull("jung " + jung, HangulTables.splitJung(jung));
            }
        }
    }

    @Test
    public void compoundFinalsAreSymmetricWithTheirSplit() {
        // jong index, cho index, resulting jong index.
        int[][] finals = {{1, 9, 3}, {4, 12, 5}, {4, 18, 6}, {8, 0, 9}, {8, 6, 10},
            {8, 7, 11}, {8, 9, 12}, {8, 16, 13}, {8, 17, 14}, {8, 18, 15}, {17, 9, 18}};
        for (int[] f : finals) {
            assertEquals("combine", f[2], HangulTables.combineJong(f[0], f[1]));
            int[] split = HangulTables.splitJong(f[2]);
            assertEquals("split keeps first part", f[0], split[0]);
            assertEquals("split moves second part", f[1], split[1]);
        }
    }

    @Test
    public void theThreeConsonantsThatCannotBeBatchimReportMinusOne() {
        // ㄸ(4), ㅃ(8), ㅉ(13) have no jongseong form.
        assertEquals(-1, HangulTables.choToJong(4));
        assertEquals(-1, HangulTables.choToJong(8));
        assertEquals(-1, HangulTables.choToJong(13));
    }

    @Test
    public void everyOtherConsonantHasABatchimForm() {
        for (int cho = 0; cho < HangulTables.CHO_COUNT; cho++) {
            if (cho == 4 || cho == 8 || cho == 13) {
                continue;
            }
            int jong = HangulTables.choToJong(cho);
            assertTrue("cho " + cho + " -> jong " + jong, jong >= 1 && jong < 28);
        }
    }

    @Test
    public void splittingASingleFinalLeavesNoBatchimAndMovesItsConsonant() {
        // A plain ㄱ batchim (jong 1) splits to no batchim + ㄱ choseong (0).
        int[] split = HangulTables.splitJong(1);
        assertEquals(0, split[0]);
        assertEquals(0, split[1]);
    }
}
