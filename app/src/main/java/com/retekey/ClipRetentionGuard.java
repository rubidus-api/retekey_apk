package com.retekey;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Decides whether a clip on Android's clipboard may join the keyboard's history when nobody asked
 * for it to — the clipboard listener, the catch-up when the keyboard appears, the list opening.
 *
 * <p>A clip is withheld when its app marked it sensitive; when the field being typed in is private
 * (a password of any kind, or a field that asked for no personalised learning); or when a private
 * field was left less than {@link #LEFT_PRIVATE_GRACE_MS} ago, because the listener can be told
 * about a copy after the focus has already moved on. The clipboard itself carries no reliable word
 * of where a clip came from (review finding R18), so the field is the evidence.
 *
 * <p>A withheld clip stays withheld until the clipboard moves on, so a later catch-up in an
 * ordinary field cannot pick it up. Only a digest of it is held, never the text.
 */
final class ClipRetentionGuard {
    static final long LEFT_PRIVATE_GRACE_MS = 1_000;
    // Not StandardCharsets: that arrived in API 19, and the legacy build reaches down to 14.
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private boolean privateField;
    private long leftPrivateAt = Long.MIN_VALUE;
    private byte[] withheld;

    /** EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING (API 26), as a number the JVM tests can use. */
    static final int IME_FLAG_NO_PERSONALIZED_LEARNING = 0x1000000;

    /**
     * Whether a field's text is not the keyboard's to keep: a password of any kind, or a field
     * that asked for no personalised learning — an incognito tab (review H05). The flag is a
     * request about learning; keeping a clip history of what was copied there is learning of the
     * plainest sort, so the keyboard takes it as covering that too.
     */
    static boolean isPrivateField(boolean sensitive, int imeOptions) {
        return sensitive || (imeOptions & IME_FLAG_NO_PERSONALIZED_LEARNING) != 0;
    }

    /** The field now being typed in, and whether it is private. */
    void onField(boolean isPrivate, long nowMs) {
        if (privateField && !isPrivate) {
            leftPrivateAt = nowMs;
        }
        privateField = isPrivate;
    }

    /** Whether {@code text}, now on the clipboard, may be kept. */
    boolean mayKeep(CharSequence text, boolean markedSensitive, long nowMs) {
        if (text == null) {
            return false;
        }
        byte[] digest = digestOf(text);
        if (withheld != null && Arrays.equals(withheld, digest)) {
            return false;
        }
        withheld = null;
        if (markedSensitive) {
            return false;
        }
        boolean justLeftPrivate = leftPrivateAt != Long.MIN_VALUE
            && nowMs - leftPrivateAt < LEFT_PRIVATE_GRACE_MS;
        if (privateField || justLeftPrivate) {
            withheld = digest;
            return false;
        }
        return true;
    }

    private static byte[] digestOf(CharSequence text) {
        try {
            return MessageDigest.getInstance("SHA-256")
                .digest(text.toString().getBytes(UTF_8));
        } catch (NoSuchAlgorithmException unavailable) {
            // Every Java platform has SHA-256; without it, compare by the text's own hash.
            int hash = text.toString().hashCode();
            return new byte[] {(byte) (hash >>> 24), (byte) (hash >>> 16), (byte) (hash >>> 8),
                (byte) hash};
        }
    }

    @Override
    public String toString() {
        return "ClipRetentionGuard{private=" + privateField + ", withholding=" + (withheld != null)
            + "}";
    }
}
