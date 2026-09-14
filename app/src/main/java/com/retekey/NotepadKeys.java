package com.retekey;

import java.util.Set;

/**
 * What a key does inside the notepad. The notepad is a panel of the keyboard's own, not the app
 * behind it, so the keys an app would interpret — Ctrl+A/C/X/V/Z/Y, the arrows, Home/End, the
 * page keys, Tab, Delete — have to be interpreted here, or they fall through to the app while the
 * note stays untouched. Android-free so the table can be unit-tested; {@link NotepadView} carries
 * each command out on its fields.
 */
public final class NotepadKeys {
    /** One editing command on the focused note field. */
    public enum Command {
        NONE,
        SELECT_ALL,
        COPY,
        CUT,
        PASTE,
        UNDO,
        REDO,
        LEFT,
        RIGHT,
        WORD_LEFT,
        WORD_RIGHT,
        UP,
        DOWN,
        LINE_START,
        LINE_END,
        TEXT_START,
        TEXT_END,
        PAGE_UP,
        PAGE_DOWN,
        TAB,
        FORWARD_DELETE,
        BACKSPACE,
        NEW_LINE
    }

    private NotepadKeys() {
    }

    /** The command for a key pressed with these modifiers held; NONE when it means nothing here. */
    public static Command of(RawKey key, Set<KeyModifier> modifiers) {
        if (key == null) {
            return Command.NONE;
        }
        boolean ctrl = modifiers != null && modifiers.contains(KeyModifier.CTRL);
        boolean shift = modifiers != null && modifiers.contains(KeyModifier.SHIFT);
        boolean altOrMeta = modifiers != null
            && (modifiers.contains(KeyModifier.ALT) || modifiers.contains(KeyModifier.META));
        if (ctrl && !altOrMeta) {
            switch (key) {
                case A: return Command.SELECT_ALL;
                case C: return Command.COPY;
                case X: return Command.CUT;
                case V: return Command.PASTE;
                case Z: return shift ? Command.REDO : Command.UNDO;
                case Y: return Command.REDO;
                case INSERT: return Command.COPY;
                case LEFT: return Command.WORD_LEFT;
                case RIGHT: return Command.WORD_RIGHT;
                case HOME: return Command.TEXT_START;
                case END: return Command.TEXT_END;
                default: break;
            }
        }
        if (shift && !ctrl && !altOrMeta) {
            // The old clipboard keys, still on many keyboards and in many hands.
            if (key == RawKey.INSERT) {
                return Command.PASTE;
            }
            if (key == RawKey.FORWARD_DELETE) {
                return Command.CUT;
            }
        }
        if (ctrl || altOrMeta) {
            // Arrows and the page keys keep working with Alt or Meta held; anything else chorded
            // is a shortcut the notepad does not have.
            if (!isNavigation(key)) {
                return Command.NONE;
            }
        }
        switch (key) {
            case LEFT: return Command.LEFT;
            case RIGHT: return Command.RIGHT;
            case UP: return Command.UP;
            case DOWN: return Command.DOWN;
            case HOME: return Command.LINE_START;
            case END: return Command.LINE_END;
            case PAGE_UP: return Command.PAGE_UP;
            case PAGE_DOWN: return Command.PAGE_DOWN;
            case TAB: return Command.TAB;
            case FORWARD_DELETE: return Command.FORWARD_DELETE;
            case BACKSPACE: return Command.BACKSPACE;
            case ENTER: return Command.NEW_LINE;
            case SPACE: return Command.NONE;
            default: return Command.NONE;
        }
    }

    /** Whether a movement should grow the selection rather than move the cursor. */
    public static boolean extendsSelection(Set<KeyModifier> modifiers) {
        return modifiers != null && modifiers.contains(KeyModifier.SHIFT);
    }

    /** Whether a command moves the cursor, which is what Shift turns into selecting. */
    public static boolean isMovement(Command command) {
        switch (command) {
            case LEFT:
            case RIGHT:
            case WORD_LEFT:
            case WORD_RIGHT:
            case UP:
            case DOWN:
            case LINE_START:
            case LINE_END:
            case TEXT_START:
            case TEXT_END:
            case PAGE_UP:
            case PAGE_DOWN:
                return true;
            default:
                return false;
        }
    }

    private static boolean isNavigation(RawKey key) {
        switch (key) {
            case LEFT:
            case RIGHT:
            case UP:
            case DOWN:
            case HOME:
            case END:
            case PAGE_UP:
            case PAGE_DOWN:
                return true;
            default:
                return false;
        }
    }

    /** Where the previous word starts, from {@code cursor} in {@code text}. */
    public static int wordLeft(CharSequence text, int cursor) {
        int i = Math.max(0, Math.min(cursor, text.length()));
        while (i > 0 && !Character.isLetterOrDigit(text.charAt(i - 1))) {
            i--;
        }
        while (i > 0 && Character.isLetterOrDigit(text.charAt(i - 1))) {
            i--;
        }
        return i;
    }

    /** Where the next word ends, from {@code cursor} in {@code text}. */
    public static int wordRight(CharSequence text, int cursor) {
        int n = text.length();
        int i = Math.max(0, Math.min(cursor, n));
        while (i < n && !Character.isLetterOrDigit(text.charAt(i))) {
            i++;
        }
        while (i < n && Character.isLetterOrDigit(text.charAt(i))) {
            i++;
        }
        return i;
    }
}
