package com.retekey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Which letter layouts the globe key cycles through, and in what order.
 *
 * <p>The user chooses both in settings: a layout can be turned off entirely, and the ones left on
 * are visited in the order they appear here. The globe key walks the list, so two enabled layouts
 * behave exactly like the old KO/EN toggle and five behave like a carousel.
 *
 * <p>Android-free: the list is stored as a comma-separated string of enum names, and this class
 * owns parsing, repair, and the walk. Unknown or duplicated names are dropped rather than throwing,
 * because a preference file can outlive the build that wrote it.
 */
public final class LetterLayouts {
    static final String KEY_ORDER = "letter_layout_order";

    /** Every layout the globe key can reach, in the order settings offers them. */
    public static final List<KeyboardLayoutId> ALL = Arrays.asList(
        KeyboardLayoutId.KO_DUBEOLSIK,
        KeyboardLayoutId.EN_QWERTY,
        KeyboardLayoutId.EN_DVORAK,
        KeyboardLayoutId.EN_COLEMAK,
        KeyboardLayoutId.ES_QWERTY,
        KeyboardLayoutId.PT_QWERTY,
        KeyboardLayoutId.IT_QWERTY,
        KeyboardLayoutId.PL_QWERTY,
        KeyboardLayoutId.VI_TELEX,
        KeyboardLayoutId.DE_QWERTZ,
        KeyboardLayoutId.TR_QWERTY,
        KeyboardLayoutId.FR_AZERTY,
        KeyboardLayoutId.EL_QWERTY,
        KeyboardLayoutId.HE_STANDARD,
        KeyboardLayoutId.JA_ROMAJI,
        KeyboardLayoutId.JA_FLICK,
        KeyboardLayoutId.FA_ISIRI,
        KeyboardLayoutId.TH_KEDMANEE,
        KeyboardLayoutId.HI_INSCRIPT,
        KeyboardLayoutId.RU_JCUKEN,
        KeyboardLayoutId.UK_JCUKEN,
        KeyboardLayoutId.BG_PHONETIC,
        KeyboardLayoutId.MK_STANDARD,
        KeyboardLayoutId.SR_CYRILLIC,
        KeyboardLayoutId.AR_101,
        KeyboardLayoutId.UR_PHONETIC,
        KeyboardLayoutId.KA_QWERTY,
        KeyboardLayoutId.HY_EASTERN,
        KeyboardLayoutId.KO_CHEONJIIN,
        KeyboardLayoutId.KO_NARATGEUL,
        KeyboardLayoutId.PAD_ARROWS,
        KeyboardLayoutId.PAD_KEYPAD
    );

    /** What a user gets before touching settings: the two full keyboards. */
    public static final List<KeyboardLayoutId> DEFAULT = Arrays.asList(
        KeyboardLayoutId.KO_DUBEOLSIK,
        KeyboardLayoutId.EN_QWERTY
    );

    private LetterLayouts() {
    }

    /** Human-readable name for settings and for the toast shown when the globe key switches. */
    /**
     * The name that fits on a key. The layout-walking key is captioned with where it goes, not
     * with where it is: a globe says "some language", and the layout you are on is already on the
     * screen in front of you — what you cannot see is what the next press will give you.
     */
    public static String keyCapName(KeyboardLayoutId id) {
        if (id == null) {
            return "";
        }
        switch (id) {
            case EN_QWERTY:
                return "qw";
            case EN_DVORAK:
                return "dv";
            case EN_COLEMAK:
                return "cm";
            case ES_QWERTY:
                return "es";
            case PT_QWERTY:
                return "pt";
            case IT_QWERTY:
                return "it";
            case PL_QWERTY:
                return "pl";
            case VI_TELEX:
                return "vi";
            case DE_QWERTZ:
                return "de";
            case TR_QWERTY:
                return "tr";
            case FR_AZERTY:
                return "fr";
            case EL_QWERTY:
                return "el";
            case HE_STANDARD:
                return "he";
            case JA_ROMAJI:
                return "rj";
            case JA_FLICK:
                return "ja";
            case FA_ISIRI:
                return "fa";
            case TH_KEDMANEE:
                return "th";
            case HI_INSCRIPT:
                return "hi";
            case RU_JCUKEN:
                return "ru";
            case UK_JCUKEN:
                return "uk";
            case BG_PHONETIC:
                return "bg";
            case MK_STANDARD:
                return "mk";
            case SR_CYRILLIC:
                return "sr";
            case AR_101:
                return "ar";
            case UR_PHONETIC:
                return "ur";
            case KA_QWERTY:
                return "ka";
            case HY_EASTERN:
                return "hy";
            case KO_DUBEOLSIK:
                return "2b";
            case KO_CHEONJIIN:
                return "cj";
            case KO_NARATGEUL:
                return "ng";
            case PAD_ARROWS:
                return "arw";
            case PAD_KEYPAD:
                return "num";
            default:
                return baseName(id);
        }
    }

    /**
     * The name settings and the layout toast show: the key-cap abbreviation, a hyphen, then the
     * English name with the language's own spelling in parentheses — {@code es-Spanish(Español)},
     * {@code 2b-2beolsik(2벌식)} — so the two-letter cap on the layout key and the full name are
     * always seen together (owner's format, 2026-08-24).
     */
    public static String displayName(KeyboardLayoutId id) {
        if (id == null) {
            return "";
        }
        return keyCapName(id) + "-" + baseName(id);
    }

    private static String baseName(KeyboardLayoutId id) {
        switch (id) {
            case EN_QWERTY:
                return "QWERTY";
            case EN_DVORAK:
                return "Dvorak";
            case EN_COLEMAK:
                return "Colemak";
            case ES_QWERTY:
                return "Spanish(Español)";
            case PT_QWERTY:
                return "Portuguese(Português)";
            case IT_QWERTY:
                return "Italian(Italiano)";
            case PL_QWERTY:
                return "Polish(Polski)";
            case VI_TELEX:
                return "Vietnamese Telex(Tiếng Việt)";
            case DE_QWERTZ:
                return "German(Deutsch)";
            case TR_QWERTY:
                return "Turkish(Türkçe)";
            case FR_AZERTY:
                return "French AZERTY(Français)";
            case EL_QWERTY:
                return "Greek(Ελληνικά)";
            case HE_STANDARD:
                return "Hebrew(עברית)";
            case JA_ROMAJI:
                return "Japanese Romaji(ローマ字)";
            case JA_FLICK:
                return "Japanese Flick(フリック)";
            case FA_ISIRI:
                return "Persian(فارسی)";
            case TH_KEDMANEE:
                return "Thai(ไทย)";
            case HI_INSCRIPT:
                return "Hindi(हिन्दी)";
            case RU_JCUKEN:
                return "Russian(Русский)";
            case UK_JCUKEN:
                return "Ukrainian(Українська)";
            case BG_PHONETIC:
                return "Bulgarian(Български)";
            case MK_STANDARD:
                return "Macedonian(Македонски)";
            case SR_CYRILLIC:
                return "Serbian(Српски)";
            case AR_101:
                return "Arabic(العربية)";
            case UR_PHONETIC:
                return "Urdu(اردو)";
            case KA_QWERTY:
                return "Georgian(ქართული)";
            case HY_EASTERN:
                return "Armenian(Հայերեն)";
            case KO_DUBEOLSIK:
                return "2beolsik(2벌식)";
            case KO_CHEONJIIN:
                return "Cheonjiin(천지인)";
            case KO_NARATGEUL:
                return "Naratgeul(나랏글)";
            case PAD_ARROWS:
                return "Arrows";
            case PAD_KEYPAD:
                return "Keypad";
            default:
                return id.name();
        }
    }

    /**
     * Reads a stored order, keeping only known letter layouts and dropping repeats. An empty or
     * unusable list falls back to {@link #DEFAULT}, so the globe key always has somewhere to go.
     */
    public static List<KeyboardLayoutId> parse(String stored) {
        LinkedHashSet<KeyboardLayoutId> order = new LinkedHashSet<>();
        if (stored != null) {
            for (String raw : stored.split(",")) {
                String name = raw.trim();
                if (name.isEmpty()) {
                    continue;
                }
                for (KeyboardLayoutId candidate : ALL) {
                    if (candidate.name().equals(name)) {
                        order.add(candidate);
                        break;
                    }
                }
            }
        }
        return order.isEmpty() ? DEFAULT : new ArrayList<>(order);
    }

    /** The stored form of an order. */
    public static String format(List<KeyboardLayoutId> order) {
        StringBuilder text = new StringBuilder();
        if (order != null) {
            for (KeyboardLayoutId id : order) {
                if (id == null || !ALL.contains(id)) {
                    continue;
                }
                if (text.length() > 0) {
                    text.append(',');
                }
                text.append(id.name());
            }
        }
        return text.toString();
    }

    /**
     * The layout after {@code current} in {@code order}, wrapping at the end. A layout that is not
     * in the order — it was just turned off in settings — hands over to the first one.
     */
    public static KeyboardLayoutId next(List<KeyboardLayoutId> order, KeyboardLayoutId current) {
        List<KeyboardLayoutId> walk = order == null || order.isEmpty() ? DEFAULT : order;
        int index = walk.indexOf(current);
        if (index < 0) {
            return walk.get(0);
        }
        return walk.get((index + 1) % walk.size());
    }

    /** The layout to start from: the first enabled one, or the stored one when still enabled. */
    public static KeyboardLayoutId firstOf(List<KeyboardLayoutId> order) {
        List<KeyboardLayoutId> walk = order == null || order.isEmpty() ? DEFAULT : order;
        return walk.get(0);
    }
}
