package com.retekey;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Finding a phonetic symbol by name instead of by hunting for it.
 *
 * <p>The request behind the IPA page (issue #11) said the quiet part out loud: the reporter did not
 * use another keyboard's phonetic page because "the symbols are hard to find", and typed saved
 * phrases instead — "palm" to get ɑ. That is a search, and a keyboard with a candidate list already
 * knows how to answer one. So each symbol carries the names people already use for it: its
 * <b>X-SAMPA</b> code (the ASCII notation phoneticians type), its English name (voiced dental
 * fricative), and for the English vowels its <b>lexical set</b> (Wells's keywords: THOUGHT, PALM,
 * KIT — the words a dictionary user has seen all their life).
 *
 * <p>Android-free: the index and the ranking are unit tests.
 */
final class IpaLookup {
    /** How many matches a caller is offered at once — one keyboard row's worth. */
    static final int LIMIT = 10;

    /**
     * One entry: {@code symbol | X-SAMPA | words}. The words are what may be typed to find it,
     * space separated; the first few read as the symbol's name.
     */
    private static final String[] INDEX = {
        // Vowels, with Wells's lexical sets where English has one.
        "ə|@|schwa mid central unstressed comma about",
        "ɚ|@`|schwa rhotic r-coloured letter nurse",
        "ɜ|3|open-mid central unrounded nurse bird",
        "ɝ|3`|open-mid central rhotic nurse bird r-coloured",
        "ɛ|E|open-mid front unrounded dress bed",
        "æ|{|near-open front unrounded trap cat ash",
        "a|a|open front unrounded trap bath",
        "ɑ|A|open back unrounded palm father start",
        "ɒ|Q|open back rounded lot cloth",
        "ɔ|O|open-mid back rounded thought north law",
        "o|o|close-mid back rounded goat",
        "ʊ|U|near-close near-back rounded foot put",
        "u|u|close back rounded goose boot",
        "ʌ|V|open-mid back unrounded strut cup",
        "ɪ|I|near-close near-front unrounded kit bit",
        "i|i|close front unrounded fleece see",
        "e|e|close-mid front unrounded face dress",
        "y|y|close front rounded french tu german ueber",
        "ø|2|close-mid front rounded french eu german oe",
        "œ|9|open-mid front rounded french oeuf",
        "ɶ|&|open front rounded",
        "ɨ|1|close central unrounded russian roses",
        "ʉ|}|close central rounded",
        "ɘ|@\\|close-mid central unrounded",
        "ɵ|8|close-mid central rounded",
        "ɐ|6|near-open central unrounded",
        "ɯ|M|close back unrounded korean eu",
        "ɤ|7|close-mid back unrounded korean eo",
        // Plosives.
        "p|p|voiceless bilabial plosive stop",
        "b|b|voiced bilabial plosive stop",
        "t|t|voiceless alveolar plosive stop",
        "d|d|voiced alveolar plosive stop",
        "ʈ|t`|voiceless retroflex plosive stop",
        "ɖ|d`|voiced retroflex plosive stop",
        "c|c|voiceless palatal plosive stop",
        "ɟ|J\\|voiced palatal plosive stop",
        "k|k|voiceless velar plosive stop",
        "ɡ|g|voiced velar plosive stop script g",
        "q|q|voiceless uvular plosive stop",
        "ɢ|G\\|voiced uvular plosive stop",
        "ʔ|?|glottal stop catch uh-oh",
        // Nasals.
        "m|m|voiced bilabial nasal",
        "ɱ|F|voiced labiodental nasal",
        "n|n|voiced alveolar nasal",
        "ɳ|n`|voiced retroflex nasal",
        "ɲ|J|voiced palatal nasal spanish enye onion",
        "ŋ|N|voiced velar nasal sing ng",
        "ɴ|N\\|voiced uvular nasal",
        // Trills, taps and flaps.
        "ʙ|B\\|voiced bilabial trill",
        "r|r|voiced alveolar trill rolled spanish perro",
        "ʀ|R\\|voiced uvular trill french r",
        "ɾ|4|voiced alveolar tap flap butter korean rieul",
        "ɽ|r`|voiced retroflex flap",
        "ⱱ|v\\|voiced labiodental flap",
        // Fricatives.
        "ɸ|p\\|voiceless bilabial fricative",
        "β|B|voiced bilabial fricative spanish b",
        "f|f|voiceless labiodental fricative",
        "v|v|voiced labiodental fricative",
        "θ|T|voiceless dental fricative thin think theta",
        "ð|D|voiced dental fricative this that eth",
        "s|s|voiceless alveolar fricative",
        "z|z|voiced alveolar fricative",
        "ʃ|S|voiceless postalveolar fricative ship sh esh",
        "ʒ|Z|voiced postalveolar fricative measure vision",
        "ʂ|s`|voiceless retroflex fricative",
        "ʐ|z`|voiced retroflex fricative",
        "ɕ|s\\|voiceless alveolopalatal fricative korean siot chinese x",
        "ʑ|z\\|voiced alveolopalatal fricative chinese",
        "ç|C|voiceless palatal fricative german ich",
        "ʝ|j\\|voiced palatal fricative",
        "x|x|voiceless velar fricative german ach loch",
        "ɣ|G|voiced velar fricative",
        "χ|X|voiceless uvular fricative german bach",
        "ʁ|R|voiced uvular fricative french r paris",
        "ħ|X\\|voiceless pharyngeal fricative arabic",
        "ʕ|?\\|voiced pharyngeal fricative arabic ayin",
        "h|h|voiceless glottal fricative aspirate",
        "ɦ|h\\|voiced glottal fricative breathy",
        "ɬ|K|voiceless lateral fricative welsh ll",
        "ɮ|K\\|voiced lateral fricative",
        // Affricates, the ones English needs.
        "ʧ|tS|voiceless postalveolar affricate church ch",
        "ʤ|dZ|voiced postalveolar affricate judge j",
        "ʦ|ts|voiceless alveolar affricate cats",
        "ʣ|dz|voiced alveolar affricate",
        "ʨ|ts\\|voiceless alveolopalatal affricate korean jieut",
        "ʥ|dz\\|voiced alveolopalatal affricate",
        // Approximants and laterals.
        "ʋ|P|voiced labiodental approximant",
        "ɹ|r\\|voiced alveolar approximant english r red",
        "ɻ|r\\`|voiced retroflex approximant",
        "j|j|voiced palatal approximant yes y",
        "ɰ|M\\|voiced velar approximant",
        "w|w|voiced labial-velar approximant wet",
        "ʍ|W|voiceless labial-velar approximant which",
        "ɥ|H|voiced labial-palatal approximant french huit",
        "l|l|voiced alveolar lateral approximant",
        "ɫ|5|velarised alveolar lateral dark l",
        "ɭ|l`|voiced retroflex lateral",
        "ʎ|L|voiced palatal lateral italian gli",
        "ʟ|L\\|voiced velar lateral",
        // Non-pulmonic.
        "ʘ|O\\|bilabial click",
        "ǀ|\\||dental click tsk",
        "ǃ|!\\|postalveolar click",
        "ǂ|=\\|palatal click",
        "ǁ|\\|\\||lateral click",
        "ɓ|b_<|voiced bilabial implosive",
        "ɗ|d_<|voiced alveolar implosive",
        "ʄ|J\\_<|voiced palatal implosive",
        "ɠ|g_<|voiced velar implosive",
        "ʛ|G\\_<|voiced uvular implosive",
        // Suprasegmentals and the marks a transcription needs around the letters.
        "ˈ|\"|primary stress mark",
        "ˌ|%|secondary stress mark",
        "ː|:|length mark long",
        "ˑ|:\\|half-long mark",
        "̆|_X|extra-short breve",
        "|.|syllable break separator",
        "‿|-\\|linking tie undertie",
        "|||minor group foot break",
        // Diacritics that ride a letter: shown with a dotted circle so they can be read alone.
        "ʰ|_h|aspirated",
        "ʲ|'|palatalised",
        "ʷ|_w|labialised",
        "ˠ|_G|velarised",
        "ˤ|_?\\|pharyngealised",
        "ʼ|_>|ejective",
        "̃|~|nasalised tilde",
        "̥|_0|voiceless ring",
        "̬|_v|voiced",
        "̪|_d|dental",
        "̩|=|syllabic",
        "͡|_|tie bar affricate",
    };

    /** One symbol and the words that find it. */
    static final class Entry {
        final String symbol;
        final String xsampa;
        final String words;

        Entry(String symbol, String xsampa, String words) {
            this.symbol = symbol;
            this.xsampa = xsampa;
            this.words = words;
        }

        /** The name to show beside the symbol: the first words of its description. */
        String name() {
            return words;
        }
    }

    private static final List<Entry> ENTRIES = read();

    private IpaLookup() {
    }

    static List<Entry> entries() {
        return ENTRIES;
    }

    private static List<Entry> read() {
        List<Entry> entries = new ArrayList<>(INDEX.length);
        for (String line : INDEX) {
            int first = line.indexOf('|');
            int second = line.indexOf('|', first + 1);
            if (first < 0 || second < 0) {
                continue;
            }
            String symbol = line.substring(0, first);
            if (symbol.isEmpty()) {
                continue;
            }
            entries.add(new Entry(
                symbol, line.substring(first + 1, second), line.substring(second + 1)));
        }
        return entries;
    }

    /**
     * The symbols a query names, best first and at most {@link #LIMIT}.
     *
     * <p>Ranking, in the order a user means them: the X-SAMPA code typed exactly; a word of the
     * description typed whole (THOUGHT, schwa, nasal); a word it begins (fric → fricative); and
     * finally anything the query appears inside at all.
     */
    static List<String> search(String query) {
        List<String> exactCode = new ArrayList<>();
        List<String> sameCode = new ArrayList<>();
        List<String> wholeWord = new ArrayList<>();
        List<String> wordStart = new ArrayList<>();
        List<String> anywhere = new ArrayList<>();
        if (query == null) {
            return exactCode;
        }
        String typed = query.trim();
        String wanted = typed.toLowerCase(Locale.ROOT);
        if (wanted.isEmpty()) {
            return exactCode;
        }
        for (Entry entry : ENTRIES) {
            String code = entry.xsampa.toLowerCase(Locale.ROOT);
            // X-SAMPA's capitals are not decoration: T is θ and t is t, S is ʃ and s is s. The
            // code typed exactly comes first, and only then the same letters in another case.
            if (entry.xsampa.equals(typed)) {
                exactCode.add(entry.symbol);
                continue;
            }
            if (code.equals(wanted)) {
                sameCode.add(entry.symbol);
                continue;
            }
            boolean whole = false;
            boolean starts = false;
            for (String word : entry.words.split(" ")) {
                if (word.equals(wanted)) {
                    whole = true;
                    break;
                }
                if (word.startsWith(wanted)) {
                    starts = true;
                }
            }
            if (whole) {
                wholeWord.add(entry.symbol);
            } else if (starts) {
                wordStart.add(entry.symbol);
            } else if (entry.words.contains(wanted) || code.contains(wanted)) {
                anywhere.add(entry.symbol);
            }
        }
        List<String> found = new ArrayList<>(LIMIT);
        for (List<String> tier : java.util.Arrays.asList(
                exactCode, sameCode, wholeWord, wordStart, anywhere)) {
            for (String symbol : tier) {
                if (found.size() >= LIMIT) {
                    return found;
                }
                if (!found.contains(symbol)) {
                    found.add(symbol);
                }
            }
        }
        return found;
    }
}
