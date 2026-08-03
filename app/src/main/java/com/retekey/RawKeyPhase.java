package com.retekey;

/**
 * How much of a key press a raw key stands for. A {@link #TAP} is the whole of one — down and up
 * together, which is what every ordinary key sends. {@link #HOLD} sends only the down half and
 * leaves the key held; {@link #RELEASE} sends the up half that ends it. The pair exists so a key
 * can be latched down the way a finger holds a real one: the editor sees the key still pressed
 * between them, and nothing else in the pipeline has to know why.
 */
public enum RawKeyPhase {
    TAP,
    HOLD,
    RELEASE
}
