package com.retekey;

/**
 * A US physical keyboard typing Persian while the Persian layout is up, on the Windows Persian
 * layout's positions (the standard a US-keyboard Persian typist knows): the letter rows carry
 * ض ص ث ق ف غ ع ه خ ح ج چ پ / ش س ی ب ل ا ت ن م ک گ / ظ ط ز ر ذ د ئ و, and the Shift layer the
 * tashkil, ژ, the hamza family, the guillemets and the Persian marks. Shift+Space is the ZWNJ —
 * ISIRI's half-space. Keys the table does not name are left to the editor.
 */
final class PersianHardwareMapper implements HardwareSemanticMapper {
    static final PersianHardwareMapper INSTANCE = new PersianHardwareMapper();

    private PersianHardwareMapper() {
    }

    @Override
    public SemanticInput map(String stableKeyId, boolean shift) {
        if (stableKeyId == null) {
            return null;
        }
        String text = shift ? shifted(stableKeyId) : base(stableKeyId);
        return text == null ? null : SemanticInput.text(text);
    }

    private static String base(String id) {
        switch (id) {
            case "hardware.key.q": return "ض";
            case "hardware.key.w": return "ص";
            case "hardware.key.e": return "ث";
            case "hardware.key.r": return "ق";
            case "hardware.key.t": return "ف";
            case "hardware.key.y": return "غ";
            case "hardware.key.u": return "ع";
            case "hardware.key.i": return "ه";
            case "hardware.key.o": return "خ";
            case "hardware.key.p": return "ح";
            case "hardware.keycode.71": return "ج";  // [
            case "hardware.keycode.72": return "چ";  // ]
            case "hardware.keycode.73": return "پ";  // backslash
            case "hardware.key.a": return "ش";
            case "hardware.key.s": return "س";
            case "hardware.key.d": return "ی";
            case "hardware.key.f": return "ب";
            case "hardware.key.g": return "ل";
            case "hardware.key.h": return "ا";
            case "hardware.key.j": return "ت";
            case "hardware.key.k": return "ن";
            case "hardware.key.l": return "م";
            case "hardware.keycode.74": return "ک";  // ;
            case "hardware.keycode.75": return "گ";  // '
            case "hardware.key.z": return "ظ";
            case "hardware.key.x": return "ط";
            case "hardware.key.c": return "ز";
            case "hardware.key.v": return "ر";
            case "hardware.key.b": return "ذ";
            case "hardware.key.n": return "د";
            case "hardware.key.m": return "ئ";
            case "hardware.keycode.55": return "و";  // comma
            default: return null;
        }
    }

    private static String shifted(String id) {
        switch (id) {
            case "hardware.keycode.62": return "‌"; // Shift+Space: the ZWNJ half-space
            case "hardware.key.q": return "ً";
            case "hardware.key.w": return "ٌ";
            case "hardware.key.e": return "ٍ";
            case "hardware.key.t": return "،";
            case "hardware.key.y": return "؛";
            case "hardware.key.a": return "َ";
            case "hardware.key.s": return "ُ";
            case "hardware.key.d": return "ِ";
            case "hardware.key.f": return "ّ";
            case "hardware.key.g": return "ۀ";
            case "hardware.key.h": return "آ";
            case "hardware.key.j": return "ـ";
            case "hardware.key.k": return "«";
            case "hardware.key.l": return "»";
            case "hardware.key.z": return "ة";
            case "hardware.key.x": return "ي";
            case "hardware.key.c": return "ژ";
            case "hardware.key.v": return "ؤ";
            case "hardware.key.b": return "إ";
            case "hardware.key.n": return "أ";
            case "hardware.key.m": return "ء";
            case "hardware.keycode.76": return "؟";  // slash
            default: return null;
        }
    }
}
