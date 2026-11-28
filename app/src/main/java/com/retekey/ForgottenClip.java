package com.retekey;

/**
 * A clip taken off the list while it is still on the clipboard stays off until the clipboard moves
 * on (RFC-0014 F9). The mark was never cleared, so the same text copied again later, after
 * something else, was never recorded again while the service lived.
 */
final class ForgottenClip {
    private String text;

    void forget(String normalized) {
        text = normalized;
    }

    /** Whether {@code current} is the forgotten clip; any other clipboard clears the mark. */
    boolean holdsBack(String current) {
        if (text != null && text.equals(current)) {
            return true;
        }
        text = null;
        return false;
    }
}
