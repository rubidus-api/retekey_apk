package dev.hellgates.retekeyime;

final class UnicodeScalar {
    private UnicodeScalar() {
    }

    static boolean isValid(int codePoint) {
        return codePoint >= Character.MIN_CODE_POINT &&
            codePoint <= Character.MAX_CODE_POINT &&
            !(codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE);
    }

    static boolean isWellFormed(String text) {
        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            if (Character.isHighSurrogate(value)) {
                if (index + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(index + 1))) {
                    return false;
                }
                index++;
            } else if (Character.isLowSurrogate(value)) {
                return false;
            }
        }
        return true;
    }
}
