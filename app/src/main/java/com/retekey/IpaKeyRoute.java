package com.retekey;

import android.view.KeyEvent;

/**
 * Where a physical key goes while the phonetic panel is open. The same answers an on-screen key
 * gets (consumeForIpaPanel): in a search a letter builds the query and a delete takes one back;
 * Escape closes; the candidate window keeps its own keys (digits pick, arrows and page keys turn);
 * a modifier on its own passes, since Shift is how a capital is typed; anything else closes the
 * panel and is then handled as it would have been. Before this, a physical letter was typed into
 * the editor while the query stayed empty (review finding R20).
 *
 * <p>Pure: the caller reads the event, so this holds no Android object and runs on the JVM.
 * Only KeyEvent's key-code constants are used, and those compile to plain numbers.
 */
final class IpaKeyRoute {
    enum Kind {
        /** Add {@link #text()} to the query. */
        APPEND,
        /** Take the last character of the query back. */
        BACKSPACE,
        /** Close the panel; the key is used up. */
        CLOSE,
        /** Close the panel, then handle the key as if it had never been open. */
        CLOSE_AND_CONTINUE,
        /** Leave the panel as it is and handle the key as usual. */
        PASS
    }

    private final Kind kind;
    private final String text;

    private IpaKeyRoute(Kind kind, String text) {
        this.kind = kind;
        this.text = text;
    }

    /**
     * @param searching   the panel is a search (not the family list)
     * @param queryEmpty  nothing typed into the search yet
     * @param unicodeChar what the key types with its meta state, 0 for none
     * @param modifierKey the key is itself a modifier (Shift, Ctrl, Alt, Meta…)
     * @param chorded     Ctrl, Alt or Meta is held with it
     */
    static IpaKeyRoute of(
        boolean searching,
        boolean queryEmpty,
        int keyCode,
        int unicodeChar,
        boolean modifierKey,
        boolean chorded
    ) {
        if (modifierKey) {
            return new IpaKeyRoute(Kind.PASS, null);
        }
        if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            return new IpaKeyRoute(Kind.CLOSE, null);
        }
        if (!chorded && ownedByTheCandidates(keyCode)) {
            return new IpaKeyRoute(Kind.PASS, null);
        }
        if (searching && !chorded) {
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                return new IpaKeyRoute(queryEmpty ? Kind.CLOSE : Kind.BACKSPACE, null);
            }
            if (unicodeChar > ' ' && !Character.isISOControl(unicodeChar)) {
                return new IpaKeyRoute(Kind.APPEND, new String(Character.toChars(unicodeChar)));
            }
        }
        return new IpaKeyRoute(Kind.CLOSE_AND_CONTINUE, null);
    }

    private static boolean ownedByTheCandidates(int keyCode) {
        if (keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_9) {
            return true;
        }
        if (keyCode >= KeyEvent.KEYCODE_NUMPAD_1 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9) {
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_PAGE_UP:
            case KeyEvent.KEYCODE_PAGE_DOWN:
                return true;
            default:
                return false;
        }
    }

    Kind kind() {
        return kind;
    }

    String text() {
        return text;
    }

    /** Whether the key-down is used up here, so its key-up must be too. */
    boolean consumes() {
        return kind == Kind.APPEND || kind == Kind.BACKSPACE || kind == Kind.CLOSE;
    }
}
