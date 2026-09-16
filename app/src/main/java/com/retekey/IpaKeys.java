package com.retekey;

/**
 * Where the phonetic symbols sit: one table, read by both the page on the glass
 * ({@link KeyboardLayouts}) and the physical keyboard ({@link HardwareLayoutTables}), so the two
 * cannot drift apart.
 *
 * <p>The arrangement is the one the request asked for (issue #11): each symbol sits at the key of
 * the letter it sounds like, following X-SAMPA's capital convention — the notation phoneticians
 * already use for ASCII, and nobody's property — so {@code E→ɛ A→ɑ O→ɔ I→ɪ U→ʊ V→ʌ S→ʃ Z→ʒ T→θ
 * D→ð N→ŋ J→ʤ L→ʎ X→χ C→ç B→β W→ʍ} are where a reader of that notation expects them. Three keys
 * differ on purpose: {@code e} types the schwa ə rather than ɛ, because it is the commonest symbol
 * in an English transcription and ɛ is its first hold; {@code a} types æ with ɑ under it, which is
 * the pairing the request named; and {@code r} types ɹ, the English r, with the taps and trills
 * held. Each key holds its neighbours in the same family, so the alphabet is one page and a hold
 * deep rather than a long grid.
 *
 * <p>Shift is not a second set of symbols but the plain Latin letters: a transcription is full of
 * them (the p, t, k, s, m, n, l of /ˈstɹɛŋkθ/), and a page that could not type them would send the
 * user back to another layout every other character.
 */
final class IpaKeys {
    /**
     * The symbol page, by row: ten keys across the top (the {@code q}–{@code p} positions), nine
     * on the home row ({@code a}–{@code l}), and eight below — the {@code z}–{@code m} letters and
     * one cell more, which carries the marks that are not sounds at all (stress and length).
     * Each cell is the symbol the key types, then the symbols it holds, separated by spaces.
     */
    static final String[][] SYMBOLS = {
        {
            "ʔ q ɢ ʛ",      // q: the glottal stop, with the uvular stops it belongs to
            "ʍ ɰ ʷ",        // w: voiceless w (X-SAMPA W), the approximants beside it
            "ə ɛ ɜ ɚ ɝ",    // e: schwa first — the commonest symbol of all — then the e vowels
            "ɹ ɾ ɻ ʁ ʀ",    // r: the English r, then the tap, retroflex and uvular r's
            "θ ʈ t̪",        // t: the th of thin (X-SAMPA T), retroflex and dental t
            "j ʝ ɥ ʲ",      // y: the y-sound is IPA's j, with its fricative and labialised pair
            "ʊ ʉ ɯ ɤ",      // u: the foot vowel, then the central and back unrounded ones
            "ɪ ɨ ɘ",        // i: the kit vowel, then the central ones
            "ɔ ɒ ø œ ɵ",    // o: the thought vowel, then the rounded ones
            "ɸ ʘ ʙ",        // p: the bilabial fricative, click and trill
        },
        {
            "æ ɑ ɐ ɶ",      // a: the trap vowel, with the palm vowel under it (the request's pair)
            "ʃ ʂ ɕ",        // s: the sh of ship, then the retroflex and alveolo-palatal ones
            "ð ɖ ɗ",        // d: the th of this, then the retroflex and implosive d
            "ʋ ɱ ⱱ",        // f/v's labiodentals: approximant, nasal, flap
            "ɡ ɣ ɟ ʄ",      // g: IPA's script g — not the ASCII one — then the velar fricative
            "ɦ ħ ʜ ʰ",      // h: breathy h, pharyngeal, epiglottal, and the aspiration mark
            "ʤ ʒ ʑ",        // j: the j of judge, then the fricatives inside it
            "x χ q",        // k: the ach-sound, then the uvular fricative and stop
            "ʎ ɭ ɬ ɫ",      // l: the palatal l, then retroflex, lateral fricative, dark l
        },
        {
            "ʒ ʐ ʑ",        // z: the s of measure, then the retroflex and alveolo-palatal ones
            "χ ʁ ħ",        // x: the uvular fricatives
            "ʧ ç ɕ c",      // c: the ch of church, then the palatal fricative and stop
            "ʌ ⱱ ʌ̃",        // v: the strut vowel, which is the letter's own shape
            "β ʙ ɓ",        // b: the bilabial fricative, trill and implosive
            "ŋ ɲ ɳ ɴ",      // n: the ng of sing, then palatal, retroflex and uvular nasals
            "ɱ ɰ",          // m: the labiodental nasal and the velar approximant
            "ˈ ˌ ː ˑ",      // and the marks: primary and secondary stress, long and half-long
        },
    };

    /**
     * The same keys with Shift down: the plain letters where they are on a QWERTY keyboard, and
     * the brackets a transcription is written between in the cell the marks had.
     */
    static final String[][] LETTERS = {
        {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
        {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
        {"z", "x", "c", "v", "b", "n", "m", "/ [ ] ˈ ."},
    };

    /** The US keyboard positions the rows sit on, in the same order as the tables above. */
    static final String[][] POSITIONS = {
        {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
        {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
        {"z", "x", "c", "v", "b", "n", "m", "keycode.55"},
    };

    private IpaKeys() {
    }

    /** The part of a cell the key types: everything before the first space. */
    static String typed(String cell) {
        int space = cell.indexOf(' ');
        return space < 0 ? cell : cell.substring(0, space);
    }

    /** The parts of a cell the key holds, which may be empty. */
    static String[] held(String cell) {
        int space = cell.indexOf(' ');
        return space < 0 ? new String[0] : cell.substring(space + 1).split(" ");
    }
}
