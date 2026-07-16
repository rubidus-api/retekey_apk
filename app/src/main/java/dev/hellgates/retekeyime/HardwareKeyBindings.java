package dev.hellgates.retekeyime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * User-assigned physical-keyboard shortcuts for the 한/영 toggle and the 한자 key. Each function may
 * carry several {@link Binding}s (e.g. 한/영 on both Shift+Space and Right-Ctrl), so a Bluetooth or
 * wired keyboard can drive language switching however the user likes.
 *
 * <p>A binding is a key code plus a required modifier mask; a lone key uses {@code mods == 0}. The
 * parse/format/match logic is Android-free so it is unit-tested directly; the service supplies the
 * live key code and modifier state, and the settings screen captures new bindings.
 */
public final class HardwareKeyBindings {
    static final String KEY_HANYEONG = "hw_hanyeong_bindings";
    static final String KEY_HANJA = "hw_hanja_bindings";

    public static final int MOD_SHIFT = 1;
    public static final int MOD_CTRL = 2;
    public static final int MOD_ALT = 4;
    public static final int MOD_META = 8;

    private HardwareKeyBindings() {
    }

    /** One assigned shortcut: a key code and the modifier bits that must be held with it. */
    public static final class Binding {
        public final int mods;
        public final int keyCode;

        public Binding(int mods, int keyCode) {
            this.mods = mods;
            this.keyCode = keyCode;
        }

        /** {@code "mods:keyCode"}, the stored token form. */
        public String token() {
            return mods + ":" + keyCode;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Binding)) {
                return false;
            }
            Binding that = (Binding) other;
            return mods == that.mods && keyCode == that.keyCode;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mods, keyCode);
        }

        @Override
        public String toString() {
            return token();
        }
    }

    public static int modsOf(boolean shift, boolean ctrl, boolean alt, boolean meta) {
        int mods = 0;
        if (shift) {
            mods |= MOD_SHIFT;
        }
        if (ctrl) {
            mods |= MOD_CTRL;
        }
        if (alt) {
            mods |= MOD_ALT;
        }
        if (meta) {
            mods |= MOD_META;
        }
        return mods;
    }

    /** Parses a stored comma-separated list of {@code "mods:keyCode"} tokens; bad tokens are skipped. */
    public static List<Binding> parse(String stored) {
        List<Binding> result = new ArrayList<>();
        if (stored == null) {
            return result;
        }
        for (String raw : stored.split(",")) {
            String token = raw.trim();
            if (token.isEmpty()) {
                continue;
            }
            int colon = token.indexOf(':');
            if (colon <= 0 || colon == token.length() - 1) {
                continue;
            }
            try {
                int mods = Integer.parseInt(token.substring(0, colon).trim());
                int keyCode = Integer.parseInt(token.substring(colon + 1).trim());
                if (keyCode <= 0 || mods < 0) {
                    continue;
                }
                Binding binding = new Binding(mods, keyCode);
                if (!result.contains(binding)) {
                    result.add(binding);
                }
            } catch (NumberFormatException ignored) {
                // Skip a malformed token rather than dropping the whole list.
            }
        }
        return result;
    }

    public static String format(List<Binding> bindings) {
        StringBuilder sb = new StringBuilder();
        for (Binding binding : bindings) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(binding.token());
        }
        return sb.toString();
    }

    /** Adds {@code binding} unless an identical one is present; returns the (possibly unchanged) list. */
    public static List<Binding> add(List<Binding> bindings, Binding binding) {
        if (!bindings.contains(binding)) {
            bindings.add(binding);
        }
        return bindings;
    }

    /**
     * Whether the pressed key satisfies any binding: the key code matches and every modifier the
     * binding requires is currently held (extra held modifiers are ignored, so a lone-key binding
     * still fires while other modifiers happen to be down).
     */
    public static boolean matches(List<Binding> bindings, int keyCode, int pressedMods) {
        for (Binding binding : bindings) {
            if (binding.keyCode == keyCode && (pressedMods & binding.mods) == binding.mods) {
                return true;
            }
        }
        return false;
    }
}
