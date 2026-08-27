package com.retekey;

import java.util.HashMap;
import java.util.Map;

/**
 * A US physical keyboard typing each layout's language, from the Windows layout of that language
 * (kbdlayout.info's KLC files, read exactly): only the keys whose output is the language's own —
 * non-ASCII — are mapped, so digits, punctuation and plain Latin letters keep doing what the keys
 * say, and dead keys are left alone until there is a composer to serve them (RFC-0011 G2-b) — the
 * Greek tone vowels are the soft keyboard's flicks for now, since the tonos is a dead key.
 * Greek, Hebrew, Thai and Devanagari therefore remap wholesale; German adds its umlauts and ß on
 * the right-hand keys; French puts é è ç à ù where AZERTY has them (the digit row, digits on
 * Shift, as on a real AZERTY); Turkish types ı from the i key the way its own layout does.
 * Persian has a hand-written mapper of its own for the ZWNJ; Korean, Telex and romaji have theirs.
 */
final class HardwareLayoutTables {
    private static final Map<KeyboardLayoutId, Map<String, String[]>> TABLES = new HashMap<>();

    static {
        register(KeyboardLayoutId.EL_QWERTY, new String[] {  // KBDHE, by scan code = US position
            "hardware.key.a|α|Α",
            "hardware.key.b|β|Β",
            "hardware.key.c|ψ|Ψ",
            "hardware.key.d|δ|Δ",
            "hardware.key.e|ε|Ε",
            "hardware.key.f|φ|Φ",
            "hardware.key.g|γ|Γ",
            "hardware.key.h|η|Η",
            "hardware.key.i|ι|Ι",
            "hardware.key.j|ξ|Ξ",
            "hardware.key.k|κ|Κ",
            "hardware.key.l|λ|Λ",
            "hardware.key.m|μ|Μ",
            "hardware.key.n|ν|Ν",
            "hardware.key.o|ο|Ο",
            "hardware.key.p|π|Π",
            "hardware.key.r|ρ|Ρ",
            "hardware.key.s|σ|Σ",
            "hardware.key.t|τ|Τ",
            "hardware.key.u|θ|Θ",
            "hardware.key.v|ω|Ω",
            "hardware.key.w|ς|",
            "hardware.key.x|χ|Χ",
            "hardware.key.y|υ|Υ",
            "hardware.key.z|ζ|Ζ",
        });
        register(KeyboardLayoutId.HE_STANDARD, new String[] {  // KBDHEB, by scan code = US position
            "hardware.key.a|ש|",
            "hardware.key.b|נ|",
            "hardware.key.c|ב|",
            "hardware.key.d|ג|",
            "hardware.key.e|ק|",
            "hardware.key.f|כ|",
            "hardware.key.g|ע|",
            "hardware.key.h|י|",
            "hardware.key.i|ן|",
            "hardware.key.j|ח|",
            "hardware.key.k|ל|",
            "hardware.key.l|ך|",
            "hardware.key.m|צ|",
            "hardware.key.n|מ|",
            "hardware.key.o|ם|",
            "hardware.key.p|פ|",
            "hardware.key.r|ר|",
            "hardware.key.s|ד|",
            "hardware.key.t|א|",
            "hardware.key.u|ו|",
            "hardware.key.v|ה|",
            "hardware.key.x|ס|",
            "hardware.key.y|ט|",
            "hardware.key.z|ז|",
            "hardware.keycode.55|ת|",
            "hardware.keycode.56|ץ|",
            "hardware.keycode.74|ף|",
        });
        register(KeyboardLayoutId.FR_AZERTY, new String[] {  // KBDFR, by scan code = US position
            "hardware.keycode.14|è|",
            "hardware.keycode.16|ç|",
            "hardware.keycode.68|²|",
            "hardware.keycode.69||°",
            "hardware.keycode.72||£",
            "hardware.keycode.73||µ",
            "hardware.keycode.75|ù|",
            "hardware.keycode.76||§",
            "hardware.keycode.7|à|",
            "hardware.keycode.9|é|",
        });
        register(KeyboardLayoutId.DE_QWERTZ, new String[] {  // KBDGR, by scan code = US position
            "hardware.keycode.10||§",
            "hardware.keycode.68||°",
            "hardware.keycode.69|ß|",
            "hardware.keycode.71|ü|Ü",
            "hardware.keycode.74|ö|Ö",
            "hardware.keycode.75|ä|Ä",
        });
        register(KeyboardLayoutId.TR_QWERTY, new String[] {  // KBDTUQ, by scan code = US position
            "hardware.key.i|ı|",
            "hardware.keycode.55|ö|Ö",
            "hardware.keycode.56|ç|Ç",
            "hardware.keycode.68||é",
            "hardware.keycode.71|ğ|Ğ",
            "hardware.keycode.72|ü|Ü",
            "hardware.keycode.74|ş|Ş",
            "hardware.keycode.75||İ",
        });
        register(KeyboardLayoutId.ES_QWERTY, new String[] {  // KBDSP, by scan code = US position
            "hardware.keycode.10||·",
            "hardware.keycode.68|º|ª",
            "hardware.keycode.70|¡|¿",
            "hardware.keycode.73|ç|Ç",
            "hardware.keycode.74|ñ|Ñ",
        });
        register(KeyboardLayoutId.PT_QWERTY, new String[] {  // KBDPO, by scan code = US position
            "hardware.keycode.70|«|»",
            "hardware.keycode.74|ç|Ç",
            "hardware.keycode.75|º|ª",
        });
        register(KeyboardLayoutId.IT_QWERTY, new String[] {  // KBDIT, by scan code = US position
            "hardware.keycode.10||£",
            "hardware.keycode.70|ì|",
            "hardware.keycode.71|è|é",
            "hardware.keycode.73|ù|§",
            "hardware.keycode.74|ò|ç",
            "hardware.keycode.75|à|°",
        });
        register(KeyboardLayoutId.TH_KEDMANEE, new String[] {  // KBDTH0, by scan code = US position
            "hardware.key.a|ฟ|ฤ",
            "hardware.key.b|ิ|ฺ",
            "hardware.key.c|แ|ฉ",
            "hardware.key.d|ก|ฏ",
            "hardware.key.e|ำ|ฎ",
            "hardware.key.f|ด|โ",
            "hardware.key.g|เ|ฌ",
            "hardware.key.h|้|็",
            "hardware.key.i|ร|ณ",
            "hardware.key.j|่|๋",
            "hardware.key.k|า|ษ",
            "hardware.key.l|ส|ศ",
            "hardware.key.m|ท|",
            "hardware.key.n|ื|์",
            "hardware.key.o|น|ฯ",
            "hardware.key.p|ย|ญ",
            "hardware.key.q|ๆ|๐",
            "hardware.key.r|พ|ฑ",
            "hardware.key.s|ห|ฆ",
            "hardware.key.t|ะ|ธ",
            "hardware.key.u|ี|๊",
            "hardware.key.v|อ|ฮ",
            "hardware.key.w|ไ|",
            "hardware.key.x|ป|",
            "hardware.key.y|ั|ํ",
            "hardware.key.z|ผ|",
            "hardware.keycode.10||๒",
            "hardware.keycode.11|ภ|๓",
            "hardware.keycode.12|ถ|๔",
            "hardware.keycode.13|ุ|ู",
            "hardware.keycode.14|ึ|฿",
            "hardware.keycode.15|ค|๕",
            "hardware.keycode.16|ต|๖",
            "hardware.keycode.55|ม|ฒ",
            "hardware.keycode.56|ใ|ฬ",
            "hardware.keycode.69|ข|๘",
            "hardware.keycode.70|ช|๙",
            "hardware.keycode.71|บ|ฐ",
            "hardware.keycode.72|ล|",
            "hardware.keycode.73|ฃ|ฅ",
            "hardware.keycode.74|ว|ซ",
            "hardware.keycode.75|ง|",
            "hardware.keycode.76|ฝ|ฦ",
            "hardware.keycode.7|จ|๗",
            "hardware.keycode.8|ๅ|",
            "hardware.keycode.9||๑",
        });
        register(KeyboardLayoutId.HI_INSCRIPT, new String[] {  // KBDINHIN, by scan code = US position
            "hardware.key.a|ो|ओ",
            "hardware.key.b|व|",
            "hardware.key.c|म|ण",
            "hardware.key.d|्|अ",
            "hardware.key.e|ा|आ",
            "hardware.key.f|ि|इ",
            "hardware.key.g|ु|उ",
            "hardware.key.h|प|फ",
            "hardware.key.i|ग|घ",
            "hardware.key.j|र|ऱ",
            "hardware.key.k|क|ख",
            "hardware.key.l|त|थ",
            "hardware.key.m|स|श",
            "hardware.key.n|ल|ळ",
            "hardware.key.o|द|ध",
            "hardware.key.p|ज|झ",
            "hardware.key.q|ौ|औ",
            "hardware.key.r|ी|ई",
            "hardware.key.s|े|ए",
            "hardware.key.t|ू|ऊ",
            "hardware.key.u|ह|ङ",
            "hardware.key.v|न|",
            "hardware.key.w|ै|ऐ",
            "hardware.key.x|ं|ँ",
            "hardware.key.y|ब|भ",
            "hardware.keycode.55||ष",
            "hardware.keycode.56||।",
            "hardware.keycode.69||ः",
            "hardware.keycode.70|ृ|ऋ",
            "hardware.keycode.71|ड|ढ",
            "hardware.keycode.72|़|ञ",
            "hardware.keycode.73|ॉ|ऑ",
            "hardware.keycode.74|च|छ",
            "hardware.keycode.75|ट|ठ",
            "hardware.keycode.76|य|य़",
            "hardware.keycode.8||ऍ",
            "hardware.keycode.9||ॅ",
        });
    }

    private HardwareLayoutTables() {
    }

    private static void register(KeyboardLayoutId id, String[] entries) {
        Map<String, String[]> table = new HashMap<>();
        for (String entry : entries) {
            String[] parts = entry.split("\\|", -1);
            table.put(parts[0], new String[] {
                parts[1].isEmpty() ? null : parts[1],
                parts[2].isEmpty() ? null : parts[2]
            });
        }
        TABLES.put(id, table);
    }

    /** The mapper for a layout, or null when the layout has no hardware table here. */
    static HardwareSemanticMapper of(KeyboardLayoutId id) {
        final Map<String, String[]> table = TABLES.get(id);
        if (table == null) {
            return null;
        }
        return (stableKeyId, shift) -> {
            String[] pair = stableKeyId == null ? null : table.get(stableKeyId);
            if (pair == null) {
                return null;
            }
            String text = pair[shift ? 1 : 0];
            return text == null ? null : SemanticInput.text(text);
        };
    }
}
