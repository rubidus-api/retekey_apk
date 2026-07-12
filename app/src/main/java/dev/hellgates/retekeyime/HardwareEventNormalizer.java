package dev.hellgates.retekeyime;

public final class HardwareEventNormalizer {
    private static final int COMBINING_ACCENT = 0x80000000;
    private static final int COMBINING_ACCENT_MASK = 0x7fffffff;

    private HardwareEventNormalizer() {
    }

    public static ProjectKeyEvent normalize(RawHardwareKeyEvent raw) {
        if (raw == null) {
            throw new IllegalArgumentException("raw event must not be null");
        }

        DecodedUnicode decoded = decodeUnicode(raw.unicodeValue());
        String preservedText = decoded.text.isEmpty() && raw.action() == InputAction.MULTIPLE
            ? raw.characters()
            : decoded.text;
        boolean eligible = raw.action() == InputAction.DOWN &&
            raw.repeatCount() == 0 &&
            !raw.canceled() &&
            !raw.ctrl() &&
            !raw.alt() &&
            !raw.meta() &&
            !raw.function() &&
            !raw.sym() &&
            !decoded.deadKey;

        SemanticInput semanticInput = null;
        if (eligible) {
            if (raw.mappedInput() != null) {
                semanticInput = raw.mappedInput();
            } else if (!decoded.text.isEmpty() && !decoded.control) {
                semanticInput = SemanticInput.text(decoded.text);
            }
        }

        return ProjectKeyEvent.builder(InputSource.HARDWARE, raw.action())
            .stableKeyId(raw.stableKeyId())
            .keyCode(raw.keyCode())
            .scanCode(raw.scanCode())
            .deviceId(raw.deviceId())
            .deviceSource(raw.deviceSource())
            .text(preservedText)
            .shift(raw.shift())
            .ctrl(raw.ctrl())
            .alt(raw.alt())
            .meta(raw.meta())
            .capsLock(raw.capsLock())
            .function(raw.function())
            .sym(raw.sym())
            .rawMetaState(raw.rawMetaState())
            .repeatCount(raw.repeatCount())
            .canceled(raw.canceled())
            .deadKey(decoded.deadKey)
            .combiningAccentCodePoint(decoded.combiningAccentCodePoint)
            .semanticInput(semanticInput)
            .build();
    }

    private static DecodedUnicode decodeUnicode(int rawValue) {
        if (rawValue == 0) {
            return DecodedUnicode.none();
        }
        if ((rawValue & COMBINING_ACCENT) != 0) {
            int accent = rawValue & COMBINING_ACCENT_MASK;
            return UnicodeScalar.isValid(accent) && accent != 0
                ? DecodedUnicode.combiningAccent(accent)
                : DecodedUnicode.invalidDeadKey();
        }
        if (!UnicodeScalar.isValid(rawValue)) {
            return DecodedUnicode.none();
        }
        return DecodedUnicode.text(
            new String(Character.toChars(rawValue)),
            Character.isISOControl(rawValue)
        );
    }

    private static final class DecodedUnicode {
        private final String text;
        private final boolean control;
        private final boolean deadKey;
        private final int combiningAccentCodePoint;

        private DecodedUnicode(
            String text,
            boolean control,
            boolean deadKey,
            int combiningAccentCodePoint
        ) {
            this.text = text;
            this.control = control;
            this.deadKey = deadKey;
            this.combiningAccentCodePoint = combiningAccentCodePoint;
        }

        private static DecodedUnicode none() {
            return new DecodedUnicode("", false, false, 0);
        }

        private static DecodedUnicode text(String text, boolean control) {
            return new DecodedUnicode(text, control, false, 0);
        }

        private static DecodedUnicode combiningAccent(int codePoint) {
            return new DecodedUnicode("", false, true, codePoint);
        }

        private static DecodedUnicode invalidDeadKey() {
            return new DecodedUnicode("", false, true, 0);
        }
    }
}
