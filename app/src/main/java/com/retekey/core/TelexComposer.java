package com.retekey.core;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Vietnamese Telex: the keyboard stays QWERTY and the letters that Vietnamese does not use — and a
 * repeated vowel — become the marks. {@code aa→â ee→ê oo→ô aw→ă ow→ơ uw→ư dd→đ}, a lone {@code w} is
 * {@code ư}, and the tones are {@code s} (sắc) {@code f} (huyền) {@code r} (hỏi) {@code x} (ngã)
 * {@code j} (nặng), {@code z} taking a tone off. A mark or tone key pressed again gives the mark
 * back and types the letter itself, so {@code aaa} is {@code aa} and {@code toss} is {@code tos}.
 *
 * <p>The composer keeps the <em>keystrokes</em> of the current word and derives the word from them
 * on every key — which is what makes backspace one rule (drop the last keystroke) and keeps a tone
 * typed early in the right place when more vowels follow ({@code hoas}+{@code n} is {@code hoán}).
 * The tone sits on one vowel of the nucleus, chosen the way the orthography does: on the vowel
 * that carries a mark when one does (ươ, uô, iê, yê → the second), on the only vowel when there is
 * one, on the middle of three, and of two on the second when a consonant follows and the first
 * when nothing does ({@code hòa}, {@code thủy}); {@code qu} and {@code gi} before a vowel are not
 * nuclei. Letters are rendered through NFC, so every form is the precomposed one the fonts have.
 *
 * <p>Android-free, like {@link HangulComposer}; the processor calls {@link #accepts} to decide which
 * key presses are the composer's, and everything else flushes it.
 */
public final class TelexComposer implements LatinComposer {
    private enum Mark { NONE, CIRCUMFLEX, BREVE, HORN }
    private enum Tone { NONE, ACUTE, GRAVE, HOOK, TILDE, DOT }

    private static final class Letter {
        final char base;       // lowercase a-z
        final boolean upper;
        Mark mark = Mark.NONE;
        boolean stroke;        // d with stroke: đ
        boolean fromLoneW;     // true when this letter started from a lone 'w'
        boolean hornFromPair;  // true when u's horn was set together with following o via uo+w

        Letter(char base, boolean upper) {
            this.base = base;
            this.upper = upper;
        }

        boolean isVowel() {
            return "aeiouy".indexOf(base) >= 0;
        }
    }

    private final List<Character> raw = new ArrayList<>();

    /** Whether this key press is the composer's: a single Latin letter. */
    @Override
    public boolean accepts(String text) {
        if (text == null || text.length() != 1) {
            return false;
        }
        char c = text.charAt(0);
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    @Override
    public Result input(String text) {
        raw.add(text.charAt(0));
        return new Result("", preeditText());
    }

    /** Drops the last keystroke, or returns null when nothing is composing. */
    @Override
    public Result backspace() {
        if (raw.isEmpty()) {
            return null;
        }
        raw.remove(raw.size() - 1);
        return new Result("", preeditText());
    }

    /** The word so far, committed; the composer is empty afterwards. */
    @Override
    public String flush() {
        String word = preeditText();
        raw.clear();
        return word;
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
        return render(derive());
    }

    // ---- derivation ----

    private static final class Word {
        final List<Letter> letters = new ArrayList<>();
        Tone tone = Tone.NONE;
        boolean trailingLiteralFromUndo;
        boolean usedVietnameseRule;

        Letter last() {
            return letters.isEmpty() ? null : letters.get(letters.size() - 1);
        }

        Letter at(int fromEnd) {
            int i = letters.size() - 1 - fromEnd;
            return i < 0 ? null : letters.get(i);
        }

        boolean hasVowel() {
            for (Letter l : letters) {
                if (l.isVowel()) {
                    return true;
                }
            }
            return false;
        }
    }

    private Word derive() {
        Word word = derivePass(true);
        if (!word.usedVietnameseRule || isValidVietnameseDerived(word)) {
            return word;
        }
        return derivePass(false);
    }

    private Word derivePass(boolean allowVietnamese) {
        Word word = new Word();
        for (char ch : raw) {
            char lower = Character.toLowerCase(ch);
            boolean upper = Character.isUpperCase(ch);
            Letter last = word.last();
            word.trailingLiteralFromUndo = false;
            if (!allowVietnamese) {
                if (lower == 'w' && last != null && last.base == 'w' && last.fromLoneW) {
                    last.fromLoneW = false;
                    continue;
                }
                Letter plain = new Letter(lower, upper);
                plain.fromLoneW = lower == 'w' && (last == null || last.base != 'w');
                word.letters.add(plain);
                continue;
            }
            Tone tone = toneOf(lower);
            if (tone != Tone.NONE && word.hasVowel()) {
                word.usedVietnameseRule = true;
                if (word.tone == tone) {
                    word.tone = Tone.NONE;             // pressed again: off, and the letter itself
                    word.letters.add(new Letter(lower, upper));
                    word.trailingLiteralFromUndo = true;
                } else {
                    word.tone = tone;
                }
                continue;
            }
            if (lower == 'z' && word.tone != Tone.NONE) {
                word.usedVietnameseRule = true;
                word.tone = Tone.NONE;
                continue;
            }
            if ((lower == 'a' || lower == 'e' || lower == 'o') && last != null && last.base == lower) {
                if (last.mark == Mark.NONE) {
                    word.usedVietnameseRule = true;
                    last.mark = Mark.CIRCUMFLEX;
                    continue;
                }
                if (last.mark == Mark.CIRCUMFLEX) {
                    word.usedVietnameseRule = true;
                    last.mark = Mark.NONE;
                    word.letters.add(new Letter(lower, upper));
                    word.trailingLiteralFromUndo = true;
                    continue;
                }
            }
            if (lower == 'w') {
                if (last != null && last.fromLoneW) {
                    word.usedVietnameseRule = true;
                    word.letters.set(word.letters.size() - 1, new Letter('w', last.upper));
                    continue;
                }
                Letter before = word.at(1);
                if (last != null && before != null && before.base == 'u' && last.base == 'o'
                        && before.mark == Mark.NONE && last.mark == Mark.NONE) {
                    word.usedVietnameseRule = true;
                    before.mark = Mark.HORN;
                    before.hornFromPair = true;
                    last.mark = Mark.HORN;
                    continue;
                }
                if (last != null && before != null && before.base == 'u' && before.hornFromPair
                        && last.base == 'o' && last.mark == Mark.HORN) {
                    word.usedVietnameseRule = true;
                    before.mark = Mark.NONE;
                    before.hornFromPair = false;
                    last.mark = Mark.NONE;
                    word.letters.add(new Letter('w', upper));
                    word.trailingLiteralFromUndo = true;
                    continue;
                }
                if (last != null && last.mark == Mark.NONE
                        && (last.base == 'a' || last.base == 'o' || last.base == 'u')) {
                    word.usedVietnameseRule = true;
                    last.mark = last.base == 'a' ? Mark.BREVE : Mark.HORN;
                    continue;
                }
                if (last != null && ((last.base == 'a' && last.mark == Mark.BREVE)
                        || ((last.base == 'o' || last.base == 'u') && last.mark == Mark.HORN))) {
                    word.usedVietnameseRule = true;
                    last.mark = Mark.NONE;
                    last.hornFromPair = false;
                    word.letters.add(new Letter('w', upper));
                    word.trailingLiteralFromUndo = true;
                    continue;
                }
                if (last != null && last.base == 'w') {
                    word.letters.add(new Letter('w', upper));
                    continue;
                }
                if (last != null && (last.base == 'a' || last.base == 'o')
                        && last.mark == Mark.CIRCUMFLEX) {
                    word.usedVietnameseRule = true;
                    word.letters.add(new Letter('w', upper));
                    word.trailingLiteralFromUndo = true;
                    continue;
                }
                if (!word.hasVowel() && isValidVietnameseOnset(word.letters)) {
                    word.usedVietnameseRule = true;
                    Letter u = new Letter('u', upper);
                    u.mark = Mark.HORN;
                    u.fromLoneW = true;
                    word.letters.add(u);
                    continue;
                }
                int[] span = vietnameseSyllableSpan(word.letters);
                if (span != null && applyNonAdjacentW(word, span[0], span[1], upper)) {
                    word.usedVietnameseRule = true;
                    continue;
                }
                word.letters.add(new Letter('w', upper));
                continue;
            }
            if (lower == 'd' && last != null && last.base == 'd') {
                if (!last.stroke) {
                    word.usedVietnameseRule = true;
                    last.stroke = true;
                    continue;
                }
                word.usedVietnameseRule = true;
                last.stroke = false;
                word.letters.add(new Letter(lower, upper));
                word.trailingLiteralFromUndo = true;
                continue;
            }
            if (word.hasVowel() && (lower == 'd' || lower == 'a' || lower == 'e' || lower == 'o')) {
                int[] span = vietnameseSyllableSpan(word.letters);
                if (span != null && applyNonAdjacentMark(word, span[0], span[1], lower, upper)) {
                    word.usedVietnameseRule = true;
                    continue;
                }
            }
            word.letters.add(new Letter(lower, upper));
        }
        return word;
    }

    private static boolean applyNonAdjacentW(Word word, int vowelStart, int vowelEnd, boolean upper) {
        List<Letter> letters = word.letters;
        for (int i = vowelEnd - 2; i >= vowelStart; i--) {
            Letter u = letters.get(i);
            Letter o = letters.get(i + 1);
            if (u.base == 'u' && o.base == 'o' && u.mark == Mark.NONE && o.mark == Mark.NONE) {
                u.mark = Mark.HORN;
                u.hornFromPair = true;
                o.mark = Mark.HORN;
                return true;
            }
            if (u.base == 'u' && u.hornFromPair && o.base == 'o' && o.mark == Mark.HORN) {
                u.mark = Mark.NONE;
                u.hornFromPair = false;
                o.mark = Mark.NONE;
                letters.add(new Letter('w', upper));
                word.trailingLiteralFromUndo = true;
                return true;
            }
        }
        for (int i = vowelEnd - 1; i >= vowelStart; i--) {
            Letter target = letters.get(i);
            if (target.base != 'a' && target.base != 'o' && target.base != 'u') {
                continue;
            }
            if (target.mark == Mark.NONE) {
                target.mark = target.base == 'a' ? Mark.BREVE : Mark.HORN;
                return true;
            }
            if ((target.base == 'a' && target.mark == Mark.BREVE)
                    || ((target.base == 'o' || target.base == 'u') && target.mark == Mark.HORN)) {
                target.mark = Mark.NONE;
                target.hornFromPair = false;
                letters.add(new Letter('w', upper));
                word.trailingLiteralFromUndo = true;
                return true;
            }
        }
        return false;
    }

    private static boolean applyNonAdjacentMark(
        Word word, int vowelStart, int vowelEnd, char lower, boolean upper
    ) {
        List<Letter> letters = word.letters;
        if (lower == 'd') {
            if (vowelStart == 1 && letters.get(0).base == 'd') {
                Letter onset = letters.get(0);
                if (!onset.stroke) {
                    onset.stroke = true;
                    return true;
                }
                onset.stroke = false;
                letters.add(new Letter('d', upper));
                word.trailingLiteralFromUndo = true;
                return true;
            }
            return false;
        }
        for (int i = vowelEnd - 1; i >= vowelStart; i--) {
            Letter target = letters.get(i);
            if (target.base != lower) {
                continue;
            }
            if (target.mark == Mark.NONE) {
                target.mark = Mark.CIRCUMFLEX;
                return true;
            }
            if (target.mark == Mark.CIRCUMFLEX) {
                target.mark = Mark.NONE;
                letters.add(new Letter(lower, upper));
                word.trailingLiteralFromUndo = true;
                return true;
            }
        }
        return false;
    }

    private static boolean isValidVietnameseDerived(Word word) {
        List<Letter> letters = word.letters;
        if (isVietnamesePrefixOrSyllable(letters)) {
            return true;
        }
        return word.trailingLiteralFromUndo
            && letters.size() >= 2
            && isVietnamesePrefixOrSyllable(letters.subList(0, letters.size() - 1));
    }

    private static boolean isVietnamesePrefixOrSyllable(List<Letter> letters) {
        return isValidVietnameseOnset(letters) || vietnameseSyllableSpan(letters) != null;
    }

    private static boolean isValidVietnameseOnset(List<Letter> letters) {
        if (letters.isEmpty()) {
            return true;
        }
        for (Letter l : letters) {
            if (l.isVowel()) {
                return false;
            }
        }
        return matchOnset(letters, 0) == letters.size();
    }

    /**
     * Returns {@code {vowelStart, vowelEnd}} if {@code letters} is a single valid Vietnamese
     * syllable (onset + 1..3 vowels + coda), or {@code null} otherwise.
     */
    private static int[] vietnameseSyllableSpan(List<Letter> letters) {
        int n = letters.size();
        if (n == 0) {
            return null;
        }
        int vowelStart = matchOnset(letters, 0);
        if (vowelStart < 0 || vowelStart >= n) {
            return null;
        }
        // qu + vowel keeps u in onset; gi + vowel keeps i in onset.
        if (vowelStart == 1 && letters.get(0).base == 'q'
                && letters.get(1).base == 'u' && letters.get(1).mark == Mark.NONE
                && 2 < n && letters.get(2).isVowel()) {
            vowelStart = 2;
        } else if (vowelStart == 1 && letters.get(0).base == 'g'
                && letters.get(1).base == 'i' && letters.get(1).mark == Mark.NONE
                && 2 < n && letters.get(2).isVowel()) {
            vowelStart = 2;
        }
        int vowelEnd = vowelStart;
        while (vowelEnd < n && letters.get(vowelEnd).isVowel()) {
            if (letters.get(vowelEnd).stroke) {
                return null;
            }
            vowelEnd++;
        }
        int vowelCount = vowelEnd - vowelStart;
        if (vowelCount < 1 || vowelCount > 3) {
            return null;
        }
        if (!isValidCoda(letters, vowelEnd)) {
            return null;
        }
        return new int[] {vowelStart, vowelEnd};
    }

    private static int matchOnset(List<Letter> letters, int start) {
        int n = letters.size();
        int end = start;
        while (end < n && !letters.get(end).isVowel()) {
            end++;
        }
        int len = end - start;
        if (len == 0) {
            return start;
        }
        for (int i = start + 1; i < end; i++) {
            if (letters.get(i).stroke) {
                return -1;
            }
        }
        char c0 = letters.get(start).base;
        if (letters.get(start).stroke && (len != 1 || c0 != 'd')) {
            return -1;
        }
        if (len == 1) {
            return "bcdghklmnpqrstvx".indexOf(c0) >= 0 ? end : -1;
        }
        char c1 = letters.get(start + 1).base;
        if (len == 2) {
            if ((c0 == 'c' && c1 == 'h') || (c0 == 'g' && c1 == 'h')
                    || (c0 == 'k' && c1 == 'h') || (c0 == 'n' && c1 == 'g')
                    || (c0 == 'n' && c1 == 'h') || (c0 == 'p' && c1 == 'h')
                    || (c0 == 't' && c1 == 'h') || (c0 == 't' && c1 == 'r')) {
                return end;
            }
            return -1;
        }
        if (len == 3 && c0 == 'n' && c1 == 'g' && letters.get(start + 2).base == 'h') {
            return end;
        }
        return -1;
    }

    private static boolean isValidCoda(List<Letter> letters, int start) {
        int n = letters.size();
        int len = n - start;
        if (len == 0) {
            return true;
        }
        for (int i = start; i < n; i++) {
            Letter l = letters.get(i);
            if (l.isVowel() || l.stroke || l.mark != Mark.NONE) {
                return false;
            }
        }
        char c0 = letters.get(start).base;
        if (len == 1) {
            return c0 == 'c' || c0 == 'm' || c0 == 'n' || c0 == 'p' || c0 == 't';
        }
        if (len == 2) {
            char c1 = letters.get(start + 1).base;
            return (c0 == 'c' && c1 == 'h')
                || (c0 == 'n' && c1 == 'g')
                || (c0 == 'n' && c1 == 'h');
        }
        return false;
    }

    private static Tone toneOf(char lower) {
        switch (lower) {
            case 's': return Tone.ACUTE;
            case 'f': return Tone.GRAVE;
            case 'r': return Tone.HOOK;
            case 'x': return Tone.TILDE;
            case 'j': return Tone.DOT;
            default: return Tone.NONE;
        }
    }

    // ---- rendering ----

    private static String render(Word word) {
        int toneAt = word.tone == Tone.NONE ? -1 : toneIndex(word.letters);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < word.letters.size(); i++) {
            Letter l = word.letters.get(i);
            char base = l.stroke ? (l.upper ? 'Đ' : 'đ') : (l.upper ? Character.toUpperCase(l.base) : l.base);
            out.append(base);
            switch (l.mark) {
                case CIRCUMFLEX: out.append('̂'); break;
                case BREVE: out.append('̆'); break;
                case HORN: out.append('̛'); break;
                default: break;
            }
            if (i == toneAt) {
                switch (word.tone) {
                    case ACUTE: out.append('́'); break;
                    case GRAVE: out.append('̀'); break;
                    case HOOK: out.append('̉'); break;
                    case TILDE: out.append('̃'); break;
                    case DOT: out.append('̣'); break;
                    default: break;
                }
            }
        }
        return Normalizer.normalize(out, Normalizer.Form.NFC);
    }

    /** The index of the vowel that carries the tone, or -1 when the word has no nucleus. */
    private static int toneIndex(List<Letter> letters) {
        List<Integer> nucleus = new ArrayList<>();
        for (int i = 0; i < letters.size(); i++) {
            Letter l = letters.get(i);
            if (!l.isVowel()) {
                continue;
            }
            Letter prev = i > 0 ? letters.get(i - 1) : null;
            Letter next = i + 1 < letters.size() ? letters.get(i + 1) : null;
            if (l.base == 'u' && prev != null && prev.base == 'q' && next != null && next.isVowel()) {
                continue; // qu-: the u is the consonant's
            }
            if (l.base == 'i' && prev != null && prev.base == 'g' && next != null && next.isVowel()) {
                continue; // gi- before a vowel: the i is the consonant's
            }
            nucleus.add(i);
        }
        if (nucleus.isEmpty()) {
            return -1;
        }
        int marked = -1;
        for (int i : nucleus) {
            if (letters.get(i).mark != Mark.NONE) {
                marked = i; // the last marked one: ươ, uô, iê, yê all take it on the second
            }
        }
        if (marked >= 0) {
            return marked;
        }
        if (nucleus.size() == 1) {
            return nucleus.get(0);
        }
        if (nucleus.size() >= 3) {
            return nucleus.get(1);
        }
        int second = nucleus.get(1);
        boolean consonantFollows = second + 1 < letters.size() && !letters.get(second + 1).isVowel();
        return consonantFollows ? second : nucleus.get(0);
    }
}
