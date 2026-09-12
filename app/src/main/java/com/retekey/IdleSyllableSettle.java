package com.retekey;

/**
 * When a syllable being built in a terminal or a remote-desktop window stops waiting for its next
 * jamo (issue #7, the owner's remote-desktop report).
 *
 * <p>Those editors materialise composition as committed text, so the syllable is already on the
 * far side; what this keyboard still holds is only the claim that it may take those characters
 * back to redraw them. The claim is safe while the user is typing and wrong the moment the cursor
 * moves somewhere this keyboard cannot see — a click in a remote desktop, Termux's own arrow
 * buttons. Neither reaches an IME: a terminal reports no cursor at all, and a remote client's
 * dummy buffer reports only its own echoes (which is why the cursor-move verdict is off there).
 *
 * <p>Time is the only signal left. After a pause the claim is dropped: nothing is written, the
 * text stays exactly where it is, and the next key starts a new syllable wherever the cursor now
 * is. The cost is that a syllable cannot be resumed across a pause in those editors — 바, a
 * pause, then ㄷ gives 바ㄷ rather than 받 — which is why the delay is long enough to sit well
 * outside ordinary typing.
 */
public final class IdleSyllableSettle {
    /**
     * How long a materialised syllable may stay open with no input. Measured against typing: a
     * jamo every 1.5 seconds is slower than anyone composing a syllable, and faster than the
     * pause before reaching for a pointer.
     */
    public static final long DELAY_MS = 1500L;

    private IdleSyllableSettle() {
    }

    /**
     * @param materialises the editor takes composition as commits — a terminal, remote desktop
     * @param composing    a syllable or word is still being built
     */
    public static boolean shouldArm(boolean materialises, boolean composing) {
        return materialises && composing;
    }
}
