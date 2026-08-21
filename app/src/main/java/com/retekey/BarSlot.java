package com.retekey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * One place on the action bar.
 *
 * <p>Three kinds live here, and they are one type rather than three lists because the bar is one
 * strip whose order the user decides — a built-in action, a piece of text they wrote, and a key
 * combination they assembled all have to sit beside each other and be dragged past each other.
 *
 * <ul>
 *   <li><b>Built-in</b> — a {@link BarAction}: select the word, paste, the clipboard list.</li>
 *   <li><b>Text</b> — anything they typed into the box. A tap types it; holding repeats it, the way
 *       holding a key on the keyboard does.</li>
 *   <li><b>Chord</b> — modifiers plus a key: Ctrl+B, Alt+F4, Esc on its own. A tap sends it once;
 *       holding presses the key and leaves it down until the slot is pressed again, which is the
 *       only way an on-screen key can be held.</li>
 * </ul>
 *
 * <p>Android-free and immutable.
 */
final class BarSlot {
    enum Kind { BUILT_IN, TEXT, CHORD }

    private final Kind kind;
    private final BarAction action;
    private final String text;
    private final RawKey key;
    private final Set<KeyModifier> modifiers;
    private final String label;

    private BarSlot(Kind kind, BarAction action, String text, RawKey key,
            Set<KeyModifier> modifiers, String label) {
        this.kind = kind;
        this.action = action;
        this.text = text;
        this.key = key;
        this.modifiers = modifiers == null
            ? Collections.<KeyModifier>emptySet()
            : Collections.unmodifiableSet(new TreeSet<>(modifiers));
        this.label = label;
    }

    static BarSlot of(BarAction action) {
        return new BarSlot(Kind.BUILT_IN, action, null, null, null, null);
    }

    /** A slot that types {@code text}. The label is what it says on the bar; blank means the text. */
    static BarSlot text(String text, String label) {
        return new BarSlot(Kind.TEXT, null, text, null, null, label);
    }

    /** A slot that presses {@code key} with {@code modifiers} held. */
    static BarSlot chord(RawKey key, Set<KeyModifier> modifiers, String label) {
        return new BarSlot(Kind.CHORD, null, null, key, modifiers, label);
    }

    Kind kind() {
        return kind;
    }

    BarAction action() {
        return action;
    }

    String text() {
        return text;
    }

    RawKey key() {
        return key;
    }

    Set<KeyModifier> modifiers() {
        return modifiers;
    }

    /** What the slot says on the bar, and in the settings list. */
    String label() {
        if (label != null && !label.isEmpty()) {
            return label;
        }
        switch (kind) {
            case BUILT_IN:
                return action.label();
            case TEXT:
                return oneLine(text);
            default:
                return chordName();
        }
    }

    /** The label the user gave it, or null: kept apart from {@link #label()} for the settings form. */
    String customLabel() {
        return label;
    }

    /** "Ctrl+Shift+B", in the order a keyboard's own caps are read. */
    String chordName() {
        StringBuilder out = new StringBuilder();
        for (KeyModifier modifier : KeyModifier.values()) {
            if (modifiers.contains(modifier)) {
                out.append(name(modifier)).append('+');
            }
        }
        out.append(keyName(key));
        return out.toString();
    }

    /** Whether holding this slot does something other than repeating the tap. */
    boolean canLatch() {
        return kind == Kind.CHORD;
    }

    private static String name(KeyModifier modifier) {
        switch (modifier) {
            case CTRL:
                return "Ctrl";
            case ALT:
                return "Alt";
            case SHIFT:
                return "Shift";
            default:
                return "Meta";
        }
    }

    /** A key's name as a key cap reads it, not as the enum spells it. */
    static String keyName(RawKey key) {
        if (key == null) {
            return "?";
        }
        switch (key) {
            case ENTER: return "Enter";
            case BACKSPACE: return "Backspace";
            case ESCAPE: return "Esc";
            case TAB: return "Tab";
            case FORWARD_DELETE: return "Del";
            case INSERT: return "Ins";
            case LEFT: return "←";
            case RIGHT: return "→";
            case UP: return "↑";
            case DOWN: return "↓";
            case HOME: return "Home";
            case END: return "End";
            case PAGE_UP: return "PgUp";
            case PAGE_DOWN: return "PgDn";
            case PRINT_SCREEN: return "PrtSc";
            case SCROLL_LOCK: return "ScrLk";
            case BREAK: return "Pause";
            case MENU: return "Menu";
            case SEARCH: return "Search";
            case SPACE: return "Space";
            default:
                break;
        }
        String name = key.name();
        if (name.startsWith("DIGIT_")) {
            return name.substring("DIGIT_".length());
        }
        return name;
    }

    /** A clip of text as a bar cap can show it: one line, and short. */
    private static String oneLine(String value) {
        if (value == null) {
            return "";
        }
        String flat = value.replace('\n', ' ').replace('\r', ' ').trim();
        // Trimmed before the ellipsis, so a cap does not read "a very long …".
        return flat.length() > 12 ? flat.substring(0, 12).trim() + "…" : flat;
    }

    /** The keys a chord can be built on, in the order the picker offers them. */
    static List<RawKey> chordKeys() {
        List<RawKey> keys = new ArrayList<>();
        for (RawKey key : RawKey.values()) {
            keys.add(key);
        }
        return keys;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BarSlot)) {
            return false;
        }
        BarSlot slot = (BarSlot) other;
        return kind == slot.kind
            && action == slot.action
            && key == slot.key
            && modifiers.equals(slot.modifiers)
            && equal(text, slot.text)
            && equal(label, slot.label);
    }

    private static boolean equal(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    @Override
    public int hashCode() {
        int hash = kind.hashCode();
        hash = hash * 31 + (action == null ? 0 : action.hashCode());
        hash = hash * 31 + (key == null ? 0 : key.hashCode());
        hash = hash * 31 + modifiers.hashCode();
        hash = hash * 31 + (text == null ? 0 : text.hashCode());
        hash = hash * 31 + (label == null ? 0 : label.hashCode());
        return hash;
    }

    @Override
    public String toString() {
        return kind + ":" + label();
    }
}
