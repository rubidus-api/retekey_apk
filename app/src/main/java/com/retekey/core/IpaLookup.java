package com.retekey.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
public final class IpaLookup {
    /** How many matches a caller is offered at once — one keyboard row's worth. */
    public static final int LIMIT = 10;

    /** One symbol and the words that find it. */
    public static final class Entry {
        public final String symbol;
        public final String xsampa;
        public final String words;

        public Entry(String symbol, String xsampa, String words) {
            this.symbol = symbol;
            this.xsampa = xsampa;
            this.words = words;
        }

        /** The name to show beside the symbol: the first words of its description. */
        public String name() {
            return words;
        }
    }

    private static Entry e(String symbol, String xsampa, String words) {
        return new Entry(symbol, xsampa, words);
    }

    /** Structured symbol index, avoiding delimiter collisions on '|' symbols and X-SAMPA codes. */
    private static final List<Entry> ENTRIES = Collections.unmodifiableList(Arrays.asList(
        // Vowels, with Wells's lexical sets where English has one.
        e("ə", "@", "schwa mid central unstressed comma about"),
        e("ɚ", "@`", "schwa rhotic r-coloured letter nurse"),
        e("ɜ", "3", "open-mid central unrounded nurse bird"),
        e("ɝ", "3`", "open-mid central rhotic nurse bird r-coloured"),
        e("ɛ", "E", "open-mid front unrounded dress bed"),
        e("æ", "{", "near-open front unrounded trap cat ash"),
        e("a", "a", "open front unrounded trap bath"),
        e("ɑ", "A", "open back unrounded palm father start"),
        e("ɒ", "Q", "open back rounded lot cloth"),
        e("ɔ", "O", "open-mid back rounded thought north law"),
        e("o", "o", "close-mid back rounded goat"),
        e("ʊ", "U", "near-close near-back rounded foot put"),
        e("u", "u", "close back rounded goose boot"),
        e("ʌ", "V", "open-mid back unrounded strut cup"),
        e("ɪ", "I", "near-close near-front unrounded kit bit"),
        e("ʏ", "Y", "near-close near-front rounded"),
        e("i", "i", "close front unrounded fleece see"),
        e("e", "e", "close-mid front unrounded face dress"),
        e("y", "y", "close front rounded french tu german ueber"),
        e("ø", "2", "close-mid front rounded french eu german oe"),
        e("œ", "9", "open-mid front rounded french oeuf"),
        e("ɶ", "&", "open front rounded"),
        e("ɨ", "1", "close central unrounded russian roses"),
        e("ʉ", "}", "close central rounded"),
        e("ɘ", "@\\", "close-mid central unrounded"),
        e("ɵ", "8", "close-mid central rounded"),
        e("ɞ", "3\\", "open-mid central rounded"),
        e("ɐ", "6", "near-open central unrounded"),
        e("ɯ", "M", "close back unrounded korean eu"),
        e("ɤ", "7", "close-mid back unrounded korean eo"),
        // Plosives.
        e("p", "p", "voiceless bilabial plosive stop"),
        e("b", "b", "voiced bilabial plosive stop"),
        e("t", "t", "voiceless alveolar plosive stop"),
        e("d", "d", "voiced alveolar plosive stop"),
        e("ʈ", "t`", "voiceless retroflex plosive stop"),
        e("ɖ", "d`", "voiced retroflex plosive stop"),
        e("c", "c", "voiceless palatal plosive stop"),
        e("ɟ", "J\\", "voiced palatal plosive stop"),
        e("k", "k", "voiceless velar plosive stop"),
        e("ɡ", "g", "voiced velar plosive stop script g"),
        e("q", "q", "voiceless uvular plosive stop"),
        e("ɢ", "G\\", "voiced uvular plosive stop"),
        e("ʔ", "?", "glottal stop catch uh-oh"),
        // Nasals.
        e("m", "m", "voiced bilabial nasal"),
        e("ɱ", "F", "voiced labiodental nasal"),
        e("n", "n", "voiced alveolar nasal"),
        e("ɳ", "n`", "voiced retroflex nasal"),
        e("ɲ", "J", "voiced palatal nasal spanish enye onion"),
        e("ŋ", "N", "voiced velar nasal sing ng"),
        e("ɴ", "N\\", "voiced uvular nasal"),
        // Trills, taps and flaps.
        e("ʙ", "B\\", "voiced bilabial trill"),
        e("r", "r", "voiced alveolar trill rolled spanish perro"),
        e("ʀ", "R\\", "voiced uvular trill french r"),
        e("ɾ", "4", "voiced alveolar tap flap butter korean rieul"),
        e("ɽ", "r`", "voiced retroflex flap"),
        e("ⱱ", "v\\", "voiced labiodental flap"),
        // Fricatives.
        e("ɸ", "p\\", "voiceless bilabial fricative"),
        e("β", "B", "voiced bilabial fricative spanish b"),
        e("f", "f", "voiceless labiodental fricative"),
        e("v", "v", "voiced labiodental fricative"),
        e("θ", "T", "voiceless dental fricative thin think theta"),
        e("ð", "D", "voiced dental fricative this that eth"),
        e("s", "s", "voiceless alveolar fricative"),
        e("z", "z", "voiced alveolar fricative"),
        e("ʃ", "S", "voiceless postalveolar fricative ship sh esh"),
        e("ʒ", "Z", "voiced postalveolar fricative measure vision"),
        e("ʂ", "s`", "voiceless retroflex fricative"),
        e("ʐ", "z`", "voiced retroflex fricative"),
        e("ɕ", "s\\", "voiceless alveolopalatal fricative korean siot chinese x"),
        e("ʑ", "z\\", "voiced alveolopalatal fricative chinese"),
        e("ç", "C", "voiceless palatal fricative german ich"),
        e("ʝ", "j\\", "voiced palatal fricative"),
        e("x", "x", "voiceless velar fricative german ach loch"),
        e("ɣ", "G", "voiced velar fricative"),
        e("χ", "X", "voiceless uvular fricative german bach"),
        e("ʁ", "R", "voiced uvular fricative french r paris"),
        e("ħ", "X\\", "voiceless pharyngeal fricative arabic"),
        e("ʕ", "?\\", "voiced pharyngeal fricative arabic ayin"),
        e("ʜ", "H\\", "voiceless epiglottal fricative"),
        e("h", "h", "voiceless glottal fricative aspirate"),
        e("ɦ", "h\\", "voiced glottal fricative breathy"),
        e("ɬ", "K", "voiceless lateral fricative welsh ll"),
        e("ɮ", "K\\", "voiced lateral fricative"),
        // Affricates, the ones English needs.
        e("ʧ", "tS", "voiceless postalveolar affricate church ch"),
        e("ʤ", "dZ", "voiced postalveolar affricate judge j"),
        e("ʦ", "ts", "voiceless alveolar affricate cats"),
        e("ʣ", "dz", "voiced alveolar affricate"),
        e("ʨ", "ts\\", "voiceless alveolopalatal affricate korean jieut"),
        e("ʥ", "dz\\", "voiced alveolopalatal affricate"),
        // Approximants and laterals.
        e("ʋ", "P", "voiced labiodental approximant"),
        e("ɹ", "r\\", "voiced alveolar approximant english r red"),
        e("ɻ", "r\\`", "voiced retroflex approximant"),
        e("j", "j", "voiced palatal approximant yes y"),
        e("ɰ", "M\\", "voiced velar approximant"),
        e("w", "w", "voiced labial-velar approximant wet"),
        e("ʍ", "W", "voiceless labial-velar approximant which"),
        e("ɥ", "H", "voiced labial-palatal approximant french huit"),
        e("l", "l", "voiced alveolar lateral approximant"),
        e("ɫ", "5", "velarised alveolar lateral dark l"),
        e("ɭ", "l`", "voiced retroflex lateral"),
        e("ʎ", "L", "voiced palatal lateral italian gli"),
        e("ʟ", "L\\", "voiced velar lateral"),
        // Non-pulmonic.
        e("ʘ", "O\\", "bilabial click"),
        e("ǀ", "\\|", "dental click tsk"),
        e("ǃ", "!\\", "postalveolar click"),
        e("ǂ", "=\\", "palatal click"),
        e("ǁ", "\\|\\|", "lateral click"),
        e("ɓ", "b_<", "voiced bilabial implosive"),
        e("ɗ", "d_<", "voiced alveolar implosive"),
        e("ʄ", "J\\_<", "voiced palatal implosive"),
        e("ɠ", "g_<", "voiced velar implosive"),
        e("ʛ", "G\\_<", "voiced uvular implosive"),
        // Suprasegmentals and the marks a transcription needs around the letters.
        e("ˈ", "\"", "primary stress mark"),
        e("ˌ", "%", "secondary stress mark"),
        e("ː", ":", "length mark long"),
        e("ˑ", ":\\", "half-long mark"),
        e("̆", "_X", "extra-short breve"),
        e(".", ".", "syllable break separator"),
        e("‿", "-\\", "linking tie undertie"),
        e("|", "|", "minor group foot break"),
        // Diacritics that ride a letter: shown with a dotted circle so they can be read alone.
        e("ʰ", "_h", "aspirated"),
        e("ʲ", "'", "palatalised"),
        e("ʷ", "_w", "labialised"),
        e("ˠ", "_G", "velarised"),
        e("ˤ", "_?\\", "pharyngealised"),
        e("ʼ", "_>", "ejective"),
        e("̃", "~", "nasalised tilde"),
        e("̥", "_0", "voiceless ring"),
        e("̬", "_v", "voiced"),
        e("̪", "_d", "dental"),
        e("̩", "=", "syllabic"),
        e("͡", "_", "tie bar affricate")
    ));

    private IpaLookup() {
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    /**
     * The symbols a query names, best first and at most {@link #LIMIT}.
     *
     * <p>Ranking, in the order a user means them: the X-SAMPA code typed exactly; a word of the
     * description typed whole (THOUGHT, schwa, nasal); a word it begins (fric → fricative); and
     * finally anything the query appears inside at all.
     */
    public static List<String> search(String query) {
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
