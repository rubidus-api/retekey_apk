package com.retekey;

import java.util.Collections;
import java.util.EnumSet;

/**
 * What a typed character is on a US keyboard, for remote-desktop mode.
 *
 * <p>The owner's model of the mode (2026-11-18): it is a US keyboard with other caps put on it.
 * Nothing is composed on this side; every key goes out as the US key at its position and the far
 * machine's IME makes of it what its layout says. So a symbol typed from the !# page or a hold —
 * {@code [}, {@code ;}, {@code !} — goes out as the US key that types it, with Shift when a US
 * keyboard needs Shift for it, rather than as finished text the far IME never sees. A far side
 * set to Russian therefore types х for {@code [}, exactly as that keyboard would.
 *
 * <p>Only printable ASCII has a US key; anything else (÷, ×, a Hangul letter) stays text.
 * Android-free, so the table is unit tested.
 */
public final class RemoteDesktopKeys {

    /** The unshifted characters of the US punctuation keys and their keys, in step. */
    private static final String PLAIN = "`-=[]\\;',./ ";
    private static final String SHIFTED = "~_+{}|:\"<>?";
    private static final RawKey[] PUNCTUATION = {
        RawKey.GRAVE, RawKey.MINUS, RawKey.EQUALS, RawKey.LEFT_BRACKET, RawKey.RIGHT_BRACKET,
        RawKey.BACKSLASH, RawKey.SEMICOLON, RawKey.APOSTROPHE, RawKey.COMMA, RawKey.PERIOD,
        RawKey.SLASH, RawKey.SPACE
    };

    /** The shifted digit row, 0 to 9. */
    private static final String SHIFTED_DIGITS = ")!@#$%^&*(";

    private RemoteDesktopKeys() {
    }

    /** The physical US key, Shift included, that types {@code text}; null if there is none. */
    public static SemanticInput forText(String text) {
        if (text == null || text.length() != 1) {
            return null;
        }
        char c = text.charAt(0);
        if (c >= 'a' && c <= 'z') {
            return key(RawKey.values()[RawKey.A.ordinal() + (c - 'a')], false);
        }
        if (c >= 'A' && c <= 'Z') {
            return key(RawKey.values()[RawKey.A.ordinal() + (c - 'A')], true);
        }
        if (c >= '0' && c <= '9') {
            return key(RawKey.values()[RawKey.DIGIT_0.ordinal() + (c - '0')], false);
        }
        int digit = SHIFTED_DIGITS.indexOf(c);
        if (digit >= 0) {
            return key(RawKey.values()[RawKey.DIGIT_0.ordinal() + digit], true);
        }
        int plain = PLAIN.indexOf(c);
        if (plain >= 0) {
            return key(PUNCTUATION[plain], false);
        }
        int shifted = SHIFTED.indexOf(c);
        if (shifted >= 0) {
            return key(PUNCTUATION[shifted], true);
        }
        return null;
    }

    /**
     * What {@code input} becomes in remote-desktop mode: a character as its US key, Backspace and
     * Enter as those keys, and any other key in the shape a plugged-in keyboard sends it. The rest
     * (a character with no US key, a held key's down and up) is left as it is.
     */
    public static SemanticInput asUsKey(SemanticInput input) {
        if (input == null) {
            return null;
        }
        switch (input.kind()) {
            case TEXT: {
                SemanticInput key = forText(input.text());
                return key == null ? input : key;
            }
            case DELETE_BACKWARD:
                return input.isCorrection() ? input : SemanticInput.physicalKey(RawKey.BACKSPACE);
            case PRIMARY_ACTION:
                return SemanticInput.physicalKey(RawKey.ENTER);
            case RAW_KEY:
                if (input.isPhysical() || input.rawKeyPhase() != RawKeyPhase.TAP) {
                    return input;
                }
                return SemanticInput.physicalKey(input.rawKey()).withModifiers(input.modifiers());
            default:
                return input;
        }
    }

    private static SemanticInput key(RawKey rawKey, boolean shift) {
        SemanticInput input = SemanticInput.physicalKey(rawKey);
        return shift
            ? input.withModifiers(EnumSet.of(KeyModifier.SHIFT))
            : input.withModifiers(Collections.<KeyModifier>emptySet());
    }
}
