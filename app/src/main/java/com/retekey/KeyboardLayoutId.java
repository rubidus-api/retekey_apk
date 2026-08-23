package com.retekey;

public enum KeyboardLayoutId {
    EN_QWERTY,
    /** English Dvorak, on the same ten-column grid as QWERTY. */
    EN_DVORAK,
    /** English Colemak, on QWERTY's grid with the letters where Colemak puts them. */
    EN_COLEMAK,
    /** Spanish (Spain and Latin America): QWERTY with ñ ending the home row, accents held. */
    ES_QWERTY,
    /** Portuguese (Portugal and Brazil): QWERTY with the accents and ç held. */
    PT_QWERTY,
    /** Italian: QWERTY with the grave and acute vowels held. */
    IT_QWERTY,
    /** Polish: QWERTY with the ogonek, acute, stroke and dot letters held. */
    PL_QWERTY,
    /** Vietnamese Telex: QWERTY as is; the Telex composer makes the marks and tones. */
    VI_TELEX,
    KO_DUBEOLSIK,
    /** Korean 12-key with grouped consonants and the three vowel elements. */
    KO_CHEONJIIN,
    /** Korean 12-key with a consonant block and stroke/tense transformation keys. */
    KO_NARATGEUL,
    /** The cursor cluster on a 12-key pad, as a layout of its own rather than an overlay. */
    PAD_ARROWS,
    /** The phone keypad's digits on a 12-key pad, as a layout of its own. */
    PAD_KEYPAD,
    /** The special-characters page (reached by holding the period). */
    SPECIAL_CHARS,
    /** The special-keys page: keypad plus the special/function keys (reached by the pad key). */
    SPECIAL_KEYS,
    /** The menu-and-functions page (reached by the ☰ menu key). */
    MENU
}
