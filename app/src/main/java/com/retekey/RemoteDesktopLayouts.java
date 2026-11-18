package com.retekey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Which physical layout the remote-desktop page wears, and what each of its caps says.
 *
 * <p>A remote desktop (SPICE, RDP) has an IME of its own on the far machine. Text composed here
 * reaches it as finished characters, which that IME never sees; to type through it, the keys must
 * arrive as keys. So the remote-desktop page sends the US key at every position — the same key a
 * physical keyboard would — and only its caps change: they show what the far side's layout puts
 * there, so ㅂ is drawn where q is sent (owner's request, 2026-11-18).
 *
 * <p>The layouts offered are the ones this keyboard already has a physical-keyboard table for,
 * read through the same mappers a plugged-in keyboard uses. Those tables carry only what a US cap
 * does not already say, so the Latin layouts that move letters — QWERTZ's y and z, AZERTY's a, q,
 * w, z and m — have the moves written out here; the rest of their caps are the US ones, which is
 * what those layouts have there too. Layouts that compose (Telex, romaji) or have no physical
 * form (천지인, a flick grid) are not offered: their keys do not stand for one character each.
 *
 * <p>The choice is one for every screen, not per orientation: the far machine's layout does not
 * turn with the phone. Android-free, so the caps and the fallbacks are unit tested.
 */
public final class RemoteDesktopLayouts {

    /** The preference holding the chosen layout, by its enum name. */
    public static final String PREF_KEY = "remote_desktop_layout";

    /** Whether the remote-desktop page is on. */
    public static final String PREF_MODE = "remote_desktop_mode";

    /** What the page wears until the user picks: 2-beolsik, the far side's Korean IME. */
    public static final KeyboardLayoutId DEFAULT = KeyboardLayoutId.KO_DUBEOLSIK;

    /** The US key positions on the page, in its three letter rows. */
    static final String[] LETTER_ROWS = {"qwertyuiop", "asdfghjkl", "zxcvbnm"};

    /** The stable id of the US period key in the physical tables (KEYCODE_PERIOD). */
    static final String PERIOD_POSITION = "hardware.keycode.56";

    private static final String[] COMPAT_CONSONANTS = {
        "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ",
        "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    };

    private static final String[] COMPAT_VOWELS = {
        "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ",
        "ㅙ", "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ"
    };

    /** Letters the Latin tables leave on their US caps but the layouts move: position → caps. */
    private static final Map<KeyboardLayoutId, Map<String, String[]>> MOVES = new HashMap<>();

    static {
        Map<String, String[]> qwertz = new HashMap<>();
        qwertz.put("hardware.key.y", new String[] {"z", "Z"});
        qwertz.put("hardware.key.z", new String[] {"y", "Y"});
        qwertz.put(PERIOD_POSITION, new String[] {".", ":"});
        MOVES.put(KeyboardLayoutId.DE_QWERTZ, qwertz);

        Map<String, String[]> azerty = new HashMap<>();
        azerty.put("hardware.key.q", new String[] {"a", "A"});
        azerty.put("hardware.key.w", new String[] {"z", "Z"});
        azerty.put("hardware.key.a", new String[] {"q", "Q"});
        azerty.put("hardware.key.z", new String[] {"w", "W"});
        azerty.put("hardware.key.m", new String[] {",", "?"});
        azerty.put(PERIOD_POSITION, new String[] {":", "/"});
        MOVES.put(KeyboardLayoutId.FR_AZERTY, azerty);
    }

    private static final List<KeyboardLayoutId> CANDIDATES;

    static {
        List<KeyboardLayoutId> list = new ArrayList<>();
        list.add(DEFAULT);
        for (KeyboardLayoutId id : KeyboardLayoutId.values()) {
            if (id != DEFAULT && offered(id)) {
                list.add(id);
            }
        }
        CANDIDATES = Collections.unmodifiableList(list);
    }

    private RemoteDesktopLayouts() {
    }

    private static boolean offered(KeyboardLayoutId id) {
        switch (id) {
            case EN_QWERTY:
            case KO_DUBEOLSIK:
            case FA_ISIRI:
                return true;
            case ETC_IPA:
                // A transcription aid, not a layout any remote machine has.
                return false;
            default:
                return HardwareLayoutTables.of(id) != null;
        }
    }

    /** The layouts the page can wear, the default first, the rest in the keyboard's own order. */
    public static List<KeyboardLayoutId> candidates() {
        return CANDIDATES;
    }

    /** The stored choice, or the default when nothing (or nothing offered) is stored. */
    public static KeyboardLayoutId resolve(String stored) {
        if (stored != null) {
            for (KeyboardLayoutId id : CANDIDATES) {
                if (id.name().equals(stored)) {
                    return id;
                }
            }
        }
        return DEFAULT;
    }

    /** The cap for the US letter {@code letter} (lower case) on {@code id}'s page. */
    static String letterCap(KeyboardLayoutId id, char letter, boolean shifted) {
        String us = String.valueOf(letter);
        return cap(id, "hardware.key." + us, shifted,
            shifted ? us.toUpperCase(java.util.Locale.ROOT) : us);
    }

    /** The cap for the US period key on {@code id}'s page. */
    static String periodCap(KeyboardLayoutId id, boolean shifted) {
        return cap(id, PERIOD_POSITION, shifted, shifted ? ">" : ".");
    }

    private static String cap(KeyboardLayoutId id, String position, boolean shifted, String us) {
        Map<String, String[]> moves = MOVES.get(id);
        if (moves != null && moves.containsKey(position)) {
            return moves.get(position)[shifted ? 1 : 0];
        }
        HardwareSemanticMapper mapper = mapperFor(id);
        SemanticInput mapped = mapper == null ? null : mapper.map(position, shifted);
        String text = mapped == null ? null : glyph(mapped);
        return text == null || text.isEmpty() ? us : text;
    }

    private static HardwareSemanticMapper mapperFor(KeyboardLayoutId id) {
        switch (id) {
            case KO_DUBEOLSIK:
                return DubeolsikHardwareMapper.INSTANCE;
            case FA_ISIRI:
                return PersianHardwareMapper.INSTANCE;
            default:
                return HardwareLayoutTables.of(id);
        }
    }

    private static String glyph(SemanticInput input) {
        switch (input.kind()) {
            case TEXT:
                return input.text();
            case JAMO: {
                SemanticJamo jamo = input.jamo();
                return jamo.role() == SemanticJamo.Role.VOWEL
                    ? COMPAT_VOWELS[jamo.index()] : COMPAT_CONSONANTS[jamo.index()];
            }
            default:
                return null;
        }
    }
}
