package com.retekey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The two ways to reach a phonetic symbol that is not on the page: by name, and by family.
 *
 * <p>The IPA page holds the symbols a transcription uses most, and that is as many as a page can
 * hold honestly. The rest — a hundred and more — need finding rather than hunting, which is what
 * the request behind the page actually said (issue #11: "the symbols are hard to find"). So:
 *
 * <ul>
 *   <li><b>Find</b>: type what you call it — {@code palm}, {@code schwa}, {@code fricative}, or the
 *       X-SAMPA code {@code T} — and the matches are offered ({@link IpaLookup}).</li>
 *   <li><b>Chart</b>: pick a family — vowels, plosives, fricatives… — and its symbols are offered.
 *       Two taps, and nothing to remember, because the chart's own words are the map.</li>
 * </ul>
 *
 * <p>Both end in the same list of choices, so both use the candidate panel the keyboard already
 * has. Android-free: what is offered for a query or a family is a unit test.
 */
final class IpaPanel {
    /** A family of symbols, in the order the IPA chart reads. */
    static final class Family {
        final String name;
        final String[] symbols;

        Family(String name, String... symbols) {
            this.name = name;
            this.symbols = symbols;
        }
    }

    /**
     * The families, and what is in each. The chart's own grouping, which is what a phonetics user
     * already thinks in; the everyday page stays the everyday page.
     */
    static final List<Family> FAMILIES = Collections.unmodifiableList(Arrays.asList(
        new Family("vowels", "i", "y", "ɪ", "ʏ", "e", "ø", "ɛ", "œ", "æ", "a", "ɶ",
            "ɨ", "ʉ", "ɘ", "ɵ", "ə", "ɜ", "ɞ", "ɐ", "ɚ", "ɝ",
            "ɯ", "u", "ʊ", "ɤ", "o", "ʌ", "ɔ", "ɑ", "ɒ"),
        new Family("plosives", "p", "b", "t", "d", "ʈ", "ɖ", "c", "ɟ", "k", "ɡ", "q", "ɢ", "ʔ"),
        new Family("nasals", "m", "ɱ", "n", "ɳ", "ɲ", "ŋ", "ɴ"),
        new Family("fricatives", "ɸ", "β", "f", "v", "θ", "ð", "s", "z", "ʃ", "ʒ", "ʂ", "ʐ",
            "ɕ", "ʑ", "ç", "ʝ", "x", "ɣ", "χ", "ʁ", "ħ", "ʕ", "h", "ɦ", "ɬ", "ɮ"),
        new Family("affricates", "ʧ", "ʤ", "ʦ", "ʣ", "ʨ", "ʥ"),
        new Family("approximants", "ʋ", "ɹ", "ɻ", "j", "ɰ", "w", "ʍ", "ɥ"),
        new Family("laterals", "l", "ɫ", "ɭ", "ʎ", "ʟ", "ɬ", "ɮ"),
        new Family("trills and taps", "ʙ", "r", "ʀ", "ɾ", "ɽ", "ⱱ"),
        new Family("clicks and implosives", "ʘ", "ǀ", "ǃ", "ǂ", "ǁ", "ɓ", "ɗ", "ʄ", "ɠ", "ʛ"),
        new Family("stress and length", "ˈ", "ˌ", "ː", "ˑ", ".", "‿"),
        new Family("diacritics", "ʰ", "ʲ", "ʷ", "ˠ", "ˤ", "ʼ", "̃", "̥", "̬", "̪", "̩", "͡")
    ));

    /** What the panel is doing: choosing a family, showing one, or answering a query. */
    enum Mode { FAMILIES, FAMILY, FIND }

    private final Mode mode;
    private final int family;
    private final String query;

    private IpaPanel(Mode mode, int family, String query) {
        this.mode = mode;
        this.family = family;
        this.query = query;
    }

    /** The chart's first question: which family? */
    static IpaPanel families() {
        return new IpaPanel(Mode.FAMILIES, -1, "");
    }

    /** An empty query, waiting for letters. */
    static IpaPanel find() {
        return new IpaPanel(Mode.FIND, -1, "");
    }

    Mode mode() {
        return mode;
    }

    String query() {
        return query;
    }

    /** The panel showing the family at {@code index}, or this one when the index is not a family. */
    IpaPanel openFamily(int index) {
        return index < 0 || index >= FAMILIES.size()
            ? this : new IpaPanel(Mode.FAMILY, index, "");
    }

    IpaPanel withQuery(String next) {
        return mode == Mode.FIND ? new IpaPanel(Mode.FIND, -1, next == null ? "" : next) : this;
    }

    IpaPanel append(String typed) {
        return withQuery(query + typed);
    }

    IpaPanel backspace() {
        // One whole character: a supplementary one is two units, and taking one of them would
        // leave the query malformed (R08).
        return query.isEmpty() ? this : withQuery(Scalars.withoutLastScalar(query));
    }

    /** The line above the choices: what was asked, in the user's own words. */
    String label() {
        switch (mode) {
            case FAMILIES:
                return "IPA";
            case FAMILY:
                return FAMILIES.get(family).name;
            default:
                return query.isEmpty() ? "find: type a name" : "find: " + query;
        }
    }

    /**
     * What is on offer: {@code {value, gloss}} pairs. A family's name is its own value, so picking
     * one opens it; a symbol's value is the symbol, and its gloss is what it is called.
     */
    List<String[]> choices() {
        List<String[]> choices = new ArrayList<>();
        switch (mode) {
            case FAMILIES:
                for (Family group : FAMILIES) {
                    choices.add(new String[] {group.name, String.valueOf(group.symbols.length)});
                }
                return choices;
            case FAMILY:
                for (String symbol : FAMILIES.get(family).symbols) {
                    choices.add(new String[] {symbol, glossOf(symbol)});
                }
                return choices;
            default:
                if (query.isEmpty()) {
                    return choices;
                }
                for (String symbol : IpaLookup.search(query)) {
                    choices.add(new String[] {symbol, glossOf(symbol)});
                }
                return choices;
        }
    }

    /** Whether picking {@code value} opens a family rather than typing a symbol. */
    int familyAt(String value) {
        if (mode != Mode.FAMILIES) {
            return -1;
        }
        for (int i = 0; i < FAMILIES.size(); i++) {
            if (FAMILIES.get(i).name.equals(value)) {
                return i;
            }
        }
        return -1;
    }

    /** What a symbol is called, for the line under it; empty when the index has no entry. */
    static String glossOf(String symbol) {
        for (IpaLookup.Entry entry : IpaLookup.entries()) {
            if (entry.symbol.equals(symbol)) {
                return entry.name();
            }
        }
        return "";
    }
}
