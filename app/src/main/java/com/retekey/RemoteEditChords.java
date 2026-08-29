package com.retekey;

import java.util.EnumSet;
import java.util.Set;

/**
 * Which chord an edit command becomes on a remote-desktop editor.
 *
 * <p>The relay has no text view behind its InputConnection, so context-menu actions do nothing
 * there — but it always forwards key events, and the far side is a real operating system. So the
 * commands become the chords that side already understands.
 *
 * <p><b>Paste is one of them.</b> It used to type this phone's clipboard out instead, from a time
 * when Copy was broken over there: the text the user copied never reached the far side's
 * clipboard, so there was nothing to paste with Ctrl+V. With Copy fixed, the text lives exactly
 * where Ctrl+V looks for it (owner's decision, 2026-08-29). A clip the user picks in the
 * clipboard panel still gets typed out — that one really is on this device.
 */
public final class RemoteEditChords {
    private RemoteEditChords() {
    }

    /** The letter for this command's chord, or null when the command has no chord. */
    public static RawKey letterFor(int contextMenuId) {
        if (contextMenuId == android.R.id.selectAll) {
            return RawKey.A;
        }
        if (contextMenuId == android.R.id.copy) {
            return RawKey.C;
        }
        if (contextMenuId == android.R.id.cut) {
            return RawKey.X;
        }
        if (contextMenuId == android.R.id.paste) {
            return RawKey.V;
        }
        if (contextMenuId == EditMenuIds.UNDO) {
            return RawKey.Z;
        }
        if (contextMenuId == EditMenuIds.REDO) {
            return RawKey.Y;
        }
        return null;
    }

    /** Every chord here is Ctrl plus its letter — the shape both Windows and Linux read. */
    public static Set<KeyModifier> modifiers() {
        return EnumSet.of(KeyModifier.CTRL);
    }
}
