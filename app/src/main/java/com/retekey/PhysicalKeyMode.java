package com.retekey;

/**
 * The switch for RFC-0010's first experiment: sending keys the way a physical keyboard does.
 *
 * <p>Some apps take key events and nothing else — remote-desktop clients forward them to the far
 * side, and some games and terminals read them directly — and they tell a soft keyboard's events
 * apart by the {@code FLAG_SOFT_KEYBOARD} flag and by the virtual device id that comes with them.
 * With this on, ReteKey sends its keys without either, and types the characters a US keyboard has
 * keys for as those keys rather than as text.
 *
 * <p>It is off by default and named as an experiment, because whether it works is a question about
 * the app on the other side rather than about this keyboard, and because it costs something real:
 * Hangul has no key code, so in this mode Korean still arrives as text and the far side needs its
 * own input method for it.
 */
final class PhysicalKeyMode {
    static final String KEY_ENABLED = "physical_key_mode";
    static final boolean DEFAULT_ENABLED = false;

    private PhysicalKeyMode() {
    }
}
