package com.retekey;

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
final class TelexComposer {
    /** What a key did: text to commit (always empty here — a word commits on flush) and the preedit. */
    static final class Result {
        final String commit;
        final String preedit;

        Result(String commit, String preedit) {
            this.commit = commit;
            this.preedit = preedit;
        }
    }

    private enum Mark { NONE, CIRCUMFLEX, BREVE, HORN }
    private enum Tone { NONE, ACUTE, GRAVE, HOOK, TILDE, DOT }

    private static final class Letter {
        final char base;       // lowercase a-z
        final boolean upper;
        Mark mark = Mark.NONE;
        boolean stroke;        // d with stroke: đ

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
    boolean accepts(String text) {
        if (text == null || text.length() != 1) {
            return false;
        }
        char c = text.charAt(0);
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    Result input(String text) {
        raw.add(text.charAt(0));
        return new Result("", preeditText());
    }

    /** Drops the last keystroke, or returns null when nothing is composing. */
    Result backspace() {
        if (raw.isEmpty()) {
            return null;
        }
        raw.remove(raw.size() - 1);
        return new Result("", preeditText());
    }

    /** The word so far, committed; the composer is empty afterwards. */
    String flush() {
        String word = preeditText();
        raw.clear();
        return word;
    }

    void reset() {
        raw.clear();
    }

    boolean isComposing() {
        return !raw.isEmpty();
    }

    String preeditText() {
        return render(derive());
    }

    // ---- derivation ----

    private static final class Word {
        final List<Letter> letters = new ArrayList<>();
        Tone tone = Tone.NONE;

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
        Word word = new Word();
        for (char ch : raw) {
            char lower = Character.toLowerCase(ch);
            boolean upper = Character.isUpperCase(ch);
            Letter last = word.last();
            Tone tone = toneOf(lower);
            if (tone != Tone.NONE && word.hasVowel()) {
                if (word.tone == tone) {
                    word.tone = Tone.NONE;             // pressed again: off, and the letter itself
                    word.letters.add(new Letter(lower, upper));
                } else {
                    word.tone = tone;
                }
                continue;
            }
            if (lower == 'z' && word.tone != Tone.NONE) {
                word.tone = Tone.NONE;
                continue;
            }
            if ((lower == 'a' || lower == 'e' || lower == 'o') && last != null && last.base == lower) {
                if (last.mark == Mark.NONE) {
                    last.mark = Mark.CIRCUMFLEX;
                    continue;
                }
                if (last.mark == Mark.CIRCUMFLEX) {
                    last.mark = Mark.NONE;
                    word.letters.add(new Letter(lower, upper));
                    continue;
                }
            }
            if (lower == 'w') {
                Letter before = word.at(1);
                if (last != null && before != null && before.base == 'u' && last.base == 'o'
                        && before.mark == Mark.NONE && last.mark == Mark.NONE) {
                    before.mark = Mark.HORN;
                    last.mark = Mark.HORN;
                    continue;
                }
                if (last != null && last.mark == Mark.NONE
                        && (last.base == 'a' || last.base == 'o' || last.base == 'u')) {
                    last.mark = last.base == 'a' ? Mark.BREVE : Mark.HORN;
                    continue;
                }
                if (last != null && ((last.base == 'a' && last.mark == Mark.BREVE)
                        || ((last.base == 'o' || last.base == 'u') && last.mark == Mark.HORN))) {
                    last.mark = Mark.NONE;
                    word.letters.add(new Letter(lower, upper));
                    continue;
                }
                Letter u = new Letter('u', upper);
                u.mark = Mark.HORN;
                word.letters.add(u);
                continue;
            }
            if (lower == 'd' && last != null && last.base == 'd') {
                if (!last.stroke) {
                    last.stroke = true;
                    continue;
                }
                last.stroke = false;
                word.letters.add(new Letter(lower, upper));
                continue;
            }
            word.letters.add(new Letter(lower, upper));
        }
        return word;
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
