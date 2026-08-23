package com.retekey;

/**
 * Gives a physical keyboard's letter keys to the Latin composer: {@code hardware.key.a} … {@code z}
 * become the letter as text (capital with Shift), and everything else is left to the editor. Used
 * while a Latin layout with a composer — Vietnamese Telex — is up, so a USB or Bluetooth keyboard
 * types through the same rules as the screen (RFC-0011 §0: one layout definition, both ways in).
 */
final class LatinHardwareMapper implements HardwareSemanticMapper {
    static final LatinHardwareMapper INSTANCE = new LatinHardwareMapper();

    private LatinHardwareMapper() {
    }

    @Override
    public SemanticInput map(String stableKeyId, boolean shift) {
        if (stableKeyId == null || !stableKeyId.startsWith("hardware.key.")
                || stableKeyId.length() != "hardware.key.".length() + 1) {
            return null;
        }
        char letter = stableKeyId.charAt(stableKeyId.length() - 1);
        if (letter < 'a' || letter > 'z') {
            return null;
        }
        return SemanticInput.text(String.valueOf(shift ? Character.toUpperCase(letter) : letter));
    }
}
