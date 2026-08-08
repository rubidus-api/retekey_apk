package com.retekey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The special characters a consonant reaches, the way a Korean IME has offered them since the
 * days of KS X 1001: type a consonant on its own, press the Hanja key, and the row of symbols
 * that consonant stands for appears — ㅁ the general symbols, ㅅ the Greek alphabet, ㅇ the
 * circled numbers, ㄹ the units, and so on.
 *
 * <p>The sets are ported from the Jamotong project (MIT; see THIRD_PARTY_NOTICES.md), which is
 * where this keyboard's Hangul automaton comes from as well. They are a practical subset of the
 * standard's rows rather than the whole of it: what people actually reach for, in the order the
 * old tables put it.
 *
 * <p>Android-free, so the table is unit-tested on the JVM.
 */
public final class SpecialCharTable {
    /** ㄱ — punctuation and marks. */
    private static final String[] GIYEOK = {
        "！", "＇", "，", "．", "／", "：", "；", "？", "＾", "＿",
        "｀", "｜", "～", "´", "～", "ˇ", "˘", "˝", "¨", "°",
        "·", "‥", "…", "¸", "˛", "‘", "’", "“", "”", "〔",
        "〕", "§", "※", "☆", "★", "○", "●", "◎", "◇", "◆"
    };

    /** ㄴ — brackets. */
    private static final String[] NIEUN = {
        "（", "）", "｛", "｝", "〔", "〕", "【", "】", "〈", "〉",
        "《", "》", "「", "」", "『", "』", "〖", "〗", "［", "］",
        "‹", "›", "«", "»"
    };

    /** ㄷ — mathematics. */
    private static final String[] DIGEUT = {
        "＋", "－", "＜", "＝", "＞", "±", "×", "÷", "≠", "≤",
        "≥", "∞", "∴", "♂", "♀", "∠", "⊥", "⌒", "∂", "∇",
        "≡", "≒", "≪", "≫", "√", "∽", "∝", "∵", "∫", "∬",
        "∈", "∋", "⊆", "⊇", "⊂", "⊃", "∪", "∩", "∧", "∨",
        "￢", "⇒", "⇔", "∀", "∃"
    };

    /** ㄹ — units and currency. */
    private static final String[] RIEUL = {
        "㎜", "㎝", "㎞", "㎎", "㎏", "㏈", "㎧", "㎨", "㎡", "㎥",
        "㎠", "㎢", "㎣", "㎤", "㎦", "㎖", "㎗", "ℓ", "㏄", "℃",
        "℉", "°", "′", "″", "㏊", "㎍", "㎉", "㎾", "㎿", "Ω",
        "㏀", "㎐", "㎑", "㎒", "㎓", "㎔", "＄", "￡", "￥", "₩"
    };

    /** ㅁ — general symbols. */
    private static final String[] MIEUM = {
        "※", "☆", "★", "○", "●", "◎", "◇", "◆", "□", "■",
        "△", "▲", "▽", "▼", "→", "←", "↑", "↓", "↔", "↕",
        "↗", "↘", "↙", "↖", "◁", "◀", "▷", "▶", "♠", "♣",
        "♥", "♡", "♧", "♤", "⊙", "◈", "▣", "◐", "◑", "▒",
        "§", "¶", "†", "‡", "♨", "☏", "☎", "☜", "☞", "♭",
        "♪", "♩", "♬", "㉿", "㈜", "№", "™", "㏂", "㏘", "℡"
    };

    /** ㅂ — circled and bracketed letters. */
    private static final String[] BIEUP = {
        "㉠", "㉡", "㉢", "㉣", "㉤", "㉥", "㉦", "㉧", "㉨", "㉩",
        "㉪", "㉫", "㉬", "㉭", "㉮", "㉯", "㉰", "㉱", "㉲", "㉳",
        "㈀", "㈁", "㈂", "㈃", "㈄", "㈅", "㈆", "㈇", "㈈", "㈉",
        "⑴", "⑵", "⑶", "⑷", "⑸", "⑹", "⑺", "⑻", "⑼", "⑽"
    };

    /** ㅅ — Greek. */
    private static final String[] SIOT = {
        "Α", "Β", "Γ", "Δ", "Ε", "Ζ", "Η", "Θ", "Ι", "Κ",
        "Λ", "Μ", "Ν", "Ξ", "Ο", "Π", "Ρ", "Σ", "Τ", "Υ",
        "Φ", "Χ", "Ψ", "Ω", "α", "β", "γ", "δ", "ε", "ζ",
        "η", "θ", "ι", "κ", "λ", "μ", "ν", "ξ", "ο", "π",
        "ρ", "σ", "τ", "υ", "φ", "χ", "ψ", "ω"
    };

    /** ㅇ — circled numbers. */
    private static final String[] IEUNG = {
        "①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨", "⑩",
        "⑪", "⑫", "⑬", "⑭", "⑮", "⑯", "⑰", "⑱", "⑲", "⑳",
        "㉮", "㉯", "㉰", "㉱", "㉲", "㉳", "㉴", "㉵", "㉶", "㉷",
        "⓵", "⓶", "⓷", "⓸", "⓹", "⓺", "⓻", "⓼", "⓽", "⓾"
    };

    /** ㅈ — Roman numerals. */
    private static final String[] JIEUT = {
        "ⅰ", "ⅱ", "ⅲ", "ⅳ", "ⅴ", "ⅵ", "ⅶ", "ⅷ", "ⅸ", "ⅹ",
        "Ⅰ", "Ⅱ", "Ⅲ", "Ⅳ", "Ⅴ", "Ⅵ", "Ⅶ", "Ⅷ", "Ⅸ", "Ⅹ"
    };

    /** ㅊ — fractions and superscripts. */
    private static final String[] CHIEUT = {
        "½", "⅓", "⅔", "¼", "¾", "⅛", "⅜", "⅝", "⅞", "¹",
        "²", "³", "⁴", "ⁿ", "₁", "₂", "₃", "₄", "‰", "℅"
    };

    /** ㅋ — Hangul letters. */
    private static final String[] KIEUK = {
        "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄸ", "ㄹ", "ㄺ",
        "ㄻ", "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅃ", "ㅄ",
        "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ",
        "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅜ"
    };

    /** ㅌ — extended Latin. */
    private static final String[] TIEUT = {
        "Æ", "Ð", "ª", "Ħ", "Ĳ", "Ŀ", "Ł", "Ø", "Œ", "º",
        "Þ", "Ŧ", "Ŋ", "æ", "đ", "ð", "ħ", "ı", "ĳ", "ĸ",
        "ŀ", "ł", "ø", "œ", "ß", "þ", "ŧ", "ŋ", "ŉ"
    };

    /** ㅍ — kana. */
    private static final String[] PIEUP = {
        "あ", "い", "う", "え", "お", "か", "き", "く", "け", "こ",
        "さ", "し", "す", "せ", "そ", "た", "ち", "つ", "て", "と",
        "ア", "イ", "ウ", "エ", "オ", "カ", "キ", "ク", "ケ", "コ",
        "サ", "シ", "ス", "セ", "ソ", "タ", "チ", "ツ", "テ", "ト"
    };

    /** ㅎ — Cyrillic. */
    private static final String[] HIEUT = {
        "А", "Б", "В", "Г", "Д", "Е", "Ж", "З", "И", "Й",
        "К", "Л", "М", "Н", "О", "П", "Р", "С", "Т", "У",
        "а", "б", "в", "г", "д", "е", "ж", "з", "и", "й",
        "к", "л", "м", "н", "о", "п", "р", "с", "т", "у"
    };

    private SpecialCharTable() {
    }

    /**
     * The characters this consonant reaches, or an empty list when it reaches none. Tense
     * consonants answer with their plain partner's row, as the old IMEs do: ㄲ gives ㄱ's.
     */
    public static List<String> candidatesFor(char consonant) {
        String[] set = setFor(consonant);
        if (set == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(java.util.Arrays.asList(set)));
    }

    /** Whether this character is a consonant with a row of its own. */
    public static boolean hasCandidates(char consonant) {
        return setFor(consonant) != null;
    }

    private static String[] setFor(char consonant) {
        switch (consonant) {
            case 'ㄱ':
            case 'ㄲ':
                return GIYEOK;
            case 'ㄴ':
                return NIEUN;
            case 'ㄷ':
            case 'ㄸ':
                return DIGEUT;
            case 'ㄹ':
                return RIEUL;
            case 'ㅁ':
                return MIEUM;
            case 'ㅂ':
            case 'ㅃ':
                return BIEUP;
            case 'ㅅ':
            case 'ㅆ':
                return SIOT;
            case 'ㅇ':
                return IEUNG;
            case 'ㅈ':
            case 'ㅉ':
                return JIEUT;
            case 'ㅊ':
                return CHIEUT;
            case 'ㅋ':
                return KIEUK;
            case 'ㅌ':
                return TIEUT;
            case 'ㅍ':
                return PIEUP;
            case 'ㅎ':
                return HIEUT;
            default:
                return null;
        }
    }
}
