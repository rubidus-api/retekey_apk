package com.retekey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.retekey.core.DubeolsikHardwareMapper;
import com.retekey.core.HardwareSemanticMapper;
import com.retekey.core.PersianHardwareMapper;
import com.retekey.core.SemanticInput;
import com.retekey.core.SemanticJamo;

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
 * <p>Several layouts can be chosen, and the globe on the page walks them; the list is kept per
 * screen orientation, like the layout list (owner, 2026-11-18). Android-free, so the caps, the
 * list and its fallbacks are unit tested.
 */
public final class RemoteDesktopLayouts {

    /**
     * The one layout the settings chose before 0.2.7, by its enum name. Read only while no list
     * is stored, as the list's starting point ({@link #parseList}); nothing writes it any more.
     */
    public static final String PREF_KEY = "remote_desktop_layout";

    /** The layout the page wears now, the globe's place in the list, by its enum name. */
    public static final String PREF_CURRENT = "remote_desktop_current";

    /**
     * The layouts the globe walks while the mode is on, comma-separated enum names, kept per
     * screen orientation like the layout list (owner, 2026-11-18).
     */
    public static final String PREF_LIST = "remote_desktop_layouts";

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

    /**
     * The chosen list, in the order the settings show the candidates. Nothing (or nothing
     * offered) stored gives 2-beolsik and QWERTY; a single layout chosen before the list existed
     * is kept, with QWERTY beside it, since English is the other thing a far side types.
     */
    public static List<KeyboardLayoutId> parseList(String stored, String legacySingle) {
        java.util.Set<KeyboardLayoutId> chosen = java.util.EnumSet.noneOf(KeyboardLayoutId.class);
        if (stored != null) {
            for (String name : stored.split(",")) {
                KeyboardLayoutId id = offeredByName(name.trim());
                if (id != null) {
                    chosen.add(id);
                }
            }
        }
        if (chosen.isEmpty()) {
            KeyboardLayoutId single = offeredByName(legacySingle);
            chosen.add(single != null ? single : DEFAULT);
            chosen.add(KeyboardLayoutId.EN_QWERTY);
        }
        List<KeyboardLayoutId> ordered = new ArrayList<>(chosen.size());
        for (KeyboardLayoutId id : CANDIDATES) {
            if (chosen.contains(id)) {
                ordered.add(id);
            }
        }
        return Collections.unmodifiableList(ordered);
    }

    /** The list as {@link #parseList} reads it back. */
    public static String encodeList(java.util.Set<KeyboardLayoutId> chosen) {
        StringBuilder out = new StringBuilder();
        for (KeyboardLayoutId id : CANDIDATES) {
            if (chosen.contains(id)) {
                if (out.length() > 0) {
                    out.append(',');
                }
                out.append(id.name());
            }
        }
        return out.toString();
    }

    /** The layout to wear: the stored one if this screen's list has it, else the list's first. */
    public static KeyboardLayoutId current(List<KeyboardLayoutId> list, KeyboardLayoutId stored) {
        if (list == null || list.isEmpty()) {
            return stored != null && CANDIDATES.contains(stored) ? stored : DEFAULT;
        }
        return list.contains(stored) ? stored : list.get(0);
    }

    /** The layout after {@code from} in {@code list}, round to the first after the last. */
    public static KeyboardLayoutId next(List<KeyboardLayoutId> list, KeyboardLayoutId from) {
        if (list == null || list.isEmpty()) {
            return DEFAULT;
        }
        int at = list.indexOf(from);
        return list.get((at + 1) % list.size());
    }

    private static KeyboardLayoutId offeredByName(String name) {
        if (name == null) {
            return null;
        }
        for (KeyboardLayoutId id : CANDIDATES) {
            if (id.name().equals(name)) {
                return id;
            }
        }
        return null;
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
