package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** The set of layouts the globe key walks, and the walk itself. */
public final class LetterLayoutsTest {
    @Test
    public void anEmptyOrPointlessStoredOrderFallsBackToTheDefault() {
        assertEquals(LetterLayouts.DEFAULT, LetterLayouts.parse(null));
        assertEquals(LetterLayouts.DEFAULT, LetterLayouts.parse(""));
        assertEquals(LetterLayouts.DEFAULT, LetterLayouts.parse("SPECIAL_KEYS,NOT_A_LAYOUT"));
    }

    @Test
    public void storedOrderKeepsItsOrderAndDropsRepeats() {
        List<KeyboardLayoutId> parsed =
            LetterLayouts.parse("EN_DVORAK,KO_CHEONJIIN,EN_DVORAK,MENU");

        assertEquals(
            Arrays.asList(KeyboardLayoutId.EN_DVORAK, KeyboardLayoutId.KO_CHEONJIIN), parsed);
    }

    @Test
    public void formatAndParseRoundTrip() {
        List<KeyboardLayoutId> order = Arrays.asList(
            KeyboardLayoutId.KO_NARATGEUL,
            KeyboardLayoutId.EN_QWERTY,
            KeyboardLayoutId.KO_DUBEOLSIK);

        assertEquals(order, LetterLayouts.parse(LetterLayouts.format(order)));
    }

    @Test
    public void formatIgnoresLayoutsThatAreNotLetterPages() {
        assertEquals("EN_QWERTY", LetterLayouts.format(
            Arrays.asList(KeyboardLayoutId.EN_QWERTY, KeyboardLayoutId.MENU, null)));
    }

    @Test
    public void theGlobeWalksTheOrderAndWraps() {
        List<KeyboardLayoutId> order = Arrays.asList(
            KeyboardLayoutId.KO_DUBEOLSIK,
            KeyboardLayoutId.EN_QWERTY,
            KeyboardLayoutId.KO_CHEONJIIN);

        assertEquals(KeyboardLayoutId.EN_QWERTY,
            LetterLayouts.next(order, KeyboardLayoutId.KO_DUBEOLSIK));
        assertEquals(KeyboardLayoutId.KO_CHEONJIIN,
            LetterLayouts.next(order, KeyboardLayoutId.EN_QWERTY));
        assertEquals(KeyboardLayoutId.KO_DUBEOLSIK,
            LetterLayouts.next(order, KeyboardLayoutId.KO_CHEONJIIN));
    }

    @Test
    public void aLayoutThatWasTurnedOffHandsOverToTheFirstOne() {
        List<KeyboardLayoutId> order = Arrays.asList(KeyboardLayoutId.EN_DVORAK);

        assertEquals(KeyboardLayoutId.EN_DVORAK,
            LetterLayouts.next(order, KeyboardLayoutId.KO_DUBEOLSIK));
        assertEquals(KeyboardLayoutId.EN_DVORAK,
            LetterLayouts.next(order, KeyboardLayoutId.EN_DVORAK));
    }

    @Test
    public void everyOfferedLayoutHasAName() {
        for (KeyboardLayoutId id : LetterLayouts.ALL) {
            assertTrue(id.name(), LetterLayouts.displayName(id).length() > 0);
        }
    }

    /** 언어 묶음: 모든 자판이 태그를 갖고, 태그마다 "tag (Name)" 꼴 제목이 있다. */
    @org.junit.Test
    public void everyLayoutBelongsToALabelledLanguageGroup() {
        for (KeyboardLayoutId id : LetterLayouts.ALL) {
            String tag = LetterLayouts.languageTag(id);
            org.junit.Assert.assertFalse(id.name(), tag.isEmpty());
            String label = LetterLayouts.languageGroupLabel(tag);
            org.junit.Assert.assertTrue(id.name() + " -> " + label,
                label.startsWith(tag + " ("));
        }
        org.junit.Assert.assertEquals("ko", LetterLayouts.languageTag(KeyboardLayoutId.KO_CHEONJIIN));
        org.junit.Assert.assertEquals("ko (Korean)", LetterLayouts.languageGroupLabel("ko"));
        org.junit.Assert.assertEquals("pad", LetterLayouts.languageTag(KeyboardLayoutId.PAD_ARROWS));
    }

    /** 자판 약어는 전부 세 글자다 — 전환 키가 굵은 대문자로 그대로 쓴다(2026-08-29). */
    @org.junit.Test
    public void everyKeyCapNameIsThreeLetters() {
        for (KeyboardLayoutId id : LetterLayouts.ALL) {
            org.junit.Assert.assertEquals(id.name() + " -> " + LetterLayouts.keyCapName(id),
                3, LetterLayouts.keyCapName(id).length());
        }
    }
}