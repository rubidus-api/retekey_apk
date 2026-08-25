package com.retekey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Romaji to hiragana, the deterministic half of Japanese input: {@code ka} is か, {@code sha} and
 * {@code sya} are しゃ, a doubled consonant is っ ({@code gakkou} → がっこう), {@code nn} is ん and
 * a lone {@code n} before anything but a vowel or y is too, {@code -} is the long-vowel bar ー,
 * and {@code x}/{@code l} make the small kana. Kana-to-kanji conversion is a dictionary's job and
 * out of scope (RFC-0011 G3); what this types is finished hiragana.
 *
 * <p>Like {@link TelexComposer} it keeps the keystrokes and derives the text from them on every
 * key, so backspace is "drop the last keystroke". The preedit is the kana so far plus whatever
 * tail of romaji is still ambiguous ({@code k} alone might yet be か or きゃ); flushing resolves a
 * trailing {@code n} to ん and lets any other tail stand as it was typed.
 */
final class RomajiKanaComposer implements LatinComposer {
    private static final Map<String, String> TABLE = buildTable();
    private static final int LONGEST_KEY = 4;

    private final List<Character> raw = new ArrayList<>();

    @Override
    public boolean accepts(String text) {
        if (text == null || text.length() != 1) {
            return false;
        }
        char c = text.charAt(0);
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '-';
    }

    @Override
    public Result input(String text) {
        raw.add(text.charAt(0));
        return new Result("", preeditText());
    }

    @Override
    public Result backspace() {
        if (raw.isEmpty()) {
            return null;
        }
        raw.remove(raw.size() - 1);
        return new Result("", preeditText());
    }

    @Override
    public String flush() {
        String text = render(true);
        raw.clear();
        return text;
    }

    @Override
    public void reset() {
        raw.clear();
    }

    @Override
    public boolean isComposing() {
        return !raw.isEmpty();
    }

    @Override
    public String preeditText() {
        return render(false);
    }

    /**
     * Converts the keystrokes left to right, longest table match first. {@code resolveTail} is
     * flushing: a trailing n becomes ん and anything else still pending is emitted as typed.
     */
    private String render(boolean resolveTail) {
        StringBuilder out = new StringBuilder();
        int length = raw.size();
        int i = 0;
        while (i < length) {
            char c = Character.toLowerCase(raw.get(i));
            if (c == '-') {
                out.append('ー');
                i++;
                continue;
            }
            if (c == 'n') {
                if (i + 1 >= length) {
                    if (resolveTail) {
                        out.append('ん');
                    } else {
                        out.append('n');
                    }
                    i++;
                    continue;
                }
                char next = Character.toLowerCase(raw.get(i + 1));
                if (next == 'n') {
                    out.append('ん');
                    i += 2;
                    continue;
                }
                if (!isVowel(next) && next != 'y') {
                    out.append('ん');
                    i++;
                    continue;
                }
                // n before a vowel or y: the na/nya row, through the table below.
            } else if (i + 1 < length && Character.toLowerCase(raw.get(i + 1)) == c
                    && !isVowel(c)) {
                out.append('っ');
                i++;
                continue;
            }
            int matched = 0;
            String kana = null;
            for (int take = Math.min(LONGEST_KEY, length - i); take >= 1; take--) {
                String piece = lower(i, take);
                String found = TABLE.get(piece);
                if (found != null) {
                    matched = take;
                    kana = found;
                    break;
                }
            }
            if (matched > 0) {
                out.append(kana);
                i += matched;
                continue;
            }
            // No kana yet: if the rest could still grow into one, it is the pending tail.
            String rest = lower(i, length - i);
            if (!resolveTail && length - i < LONGEST_KEY && isPrefixOfSomeKey(rest)) {
                out.append(rest);
                break;
            }
            out.append(raw.get(i));
            i++;
        }
        return out.toString();
    }

    private String lower(int from, int count) {
        StringBuilder piece = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            piece.append(Character.toLowerCase(raw.get(from + i)));
        }
        return piece.toString();
    }

    private static boolean isVowel(char c) {
        return "aiueo".indexOf(c) >= 0;
    }

    private static boolean isPrefixOfSomeKey(String piece) {
        for (String key : TABLE.keySet()) {
            if (key.length() > piece.length() && key.startsWith(piece)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> buildTable() {
        Map<String, String> t = new HashMap<>();
        row(t, "", "あいうえお");
        row(t, "k", "かきくけこ");
        row(t, "g", "がぎぐげご");
        row(t, "s", "さしすせそ");
        row(t, "z", "ざじずぜぞ");
        row(t, "t", "たちつてと");
        row(t, "d", "だぢづでど");
        row(t, "n", "なにぬねの");
        row(t, "h", "はひふへほ");
        row(t, "b", "ばびぶべぼ");
        row(t, "p", "ぱぴぷぺぽ");
        row(t, "m", "まみむめも");
        row(t, "r", "らりるれろ");
        t.put("ya", "や"); t.put("yu", "ゆ"); t.put("yo", "よ");
        t.put("wa", "わ"); t.put("wo", "を");
        // The spellings Hepburn uses where the kunrei row above says otherwise.
        t.put("shi", "し"); t.put("chi", "ち"); t.put("tsu", "つ"); t.put("fu", "ふ");
        t.put("ji", "じ");
        // The youon: both spellings of each.
        youon(t, "ky", "き"); youon(t, "gy", "ぎ"); youon(t, "ny", "に"); youon(t, "hy", "ひ");
        youon(t, "by", "び"); youon(t, "py", "ぴ"); youon(t, "my", "み"); youon(t, "ry", "り");
        youon(t, "sy", "し"); youon(t, "sh", "し"); youon(t, "zy", "じ"); youon(t, "j", "じ");
        youon(t, "ty", "ち"); youon(t, "ch", "ち"); youon(t, "dy", "ぢ");
        t.put("fa", "ふぁ"); t.put("fi", "ふぃ"); t.put("fe", "ふぇ"); t.put("fo", "ふぉ");
        // Small kana, by x or l.
        for (String prefix : new String[] {"x", "l"}) {
            t.put(prefix + "a", "ぁ"); t.put(prefix + "i", "ぃ"); t.put(prefix + "u", "ぅ");
            t.put(prefix + "e", "ぇ"); t.put(prefix + "o", "ぉ");
            t.put(prefix + "tu", "っ"); t.put(prefix + "tsu", "っ");
            t.put(prefix + "ya", "ゃ"); t.put(prefix + "yu", "ゅ"); t.put(prefix + "yo", "ょ");
        }
        return t;
    }

    /** One consonant row: the five vowels in order. */
    private static void row(Map<String, String> t, String consonant, String kana) {
        String[] vowels = {"a", "i", "u", "e", "o"};
        for (int i = 0; i < 5; i++) {
            t.put(consonant + vowels[i], String.valueOf(kana.charAt(i)));
        }
    }

    /** The three youon of a consonant: ゃ ゅ ょ after its i-column kana. */
    private static void youon(Map<String, String> t, String spelling, String iKana) {
        t.put(spelling + "a", iKana + "ゃ");
        t.put(spelling + "u", iKana + "ゅ");
        t.put(spelling + "o", iKana + "ょ");
    }
}
