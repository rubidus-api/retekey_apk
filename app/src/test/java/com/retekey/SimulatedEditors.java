package com.retekey;

import java.util.ArrayList;
import java.util.List;

/**
 * Editors as the keyboard meets them, each modelled on what was measured: what reaches the
 * screen, and nothing else. The interaction matrix types the same keys into every one and compares
 * the screens, so a model that flatters an editor hides a defect — each rule below cites the
 * behaviour it copies.
 */
final class SimulatedEditors {
    private SimulatedEditors() {
    }

    /** What a simulated editor shows, and the chords that reached it. */
    interface Screen extends EditorBridge {
        String screen();

        List<String> chords();

        /** The bounds the keyboard is told, or unknown where the editor reports none. */
        EditorBounds bounds();
    }

    /** A text field with a composing region: the ordinary case, modelled by FakeEditorBridge. */
    static Screen richText() {
        return new RichText();
    }

    /**
     * Termux (§15.30 of the manual; TerminalHostActivity mirrors the same rules): commits and
     * finishComposingText reach the screen, a composing update alone shows nothing, DEL erases one
     * character, a backspace key erases one, deleteSurroundingText is turned into a backspace, and
     * deleteSurroundingTextInCodePoints only touches the connection's dummy buffer.
     */
    static Screen terminal() {
        return new Terminal();
    }

    /**
     * A remote-desktop client (manual §15a): commits and key events cross to the far side; a
     * code-point delete crosses only while the characters are still in the client's dummy buffer —
     * what this keyboard itself committed since the last key event — and a composing region never
     * reaches the far side at all.
     */
    static Screen remoteDesktop() {
        return new RemoteDesktop();
    }

    private abstract static class Base implements Screen {
        final StringBuilder text = new StringBuilder();
        final List<String> chords = new ArrayList<>();

        @Override public String screen() { return text.toString(); }
        @Override public List<String> chords() { return chords; }
        @Override public EditorCallResult beginBatchEdit() { return EditorCallResult.succeeded(); }
        @Override public EditorCallResult endBatchEdit() { return EditorCallResult.succeeded(); }
        @Override public EditorCallResult performEditorAction(int actionId) {
            return EditorCallResult.succeeded();
        }
        @Override public EditorTextResult getTextBeforeCursor(int max, int flags) {
            return EditorTextResult.value("");
        }

        void eraseOne() {
            if (text.length() > 0) {
                text.setLength(text.offsetByCodePoints(text.length(), -1));
            }
        }

        /** A key event: Backspace erases, Enter breaks the line, a chord is recorded. */
        EditorCallResult key(RawEditorKey key) {
            if (key.action() != RawEditorKey.Action.DOWN) {
                return EditorCallResult.succeeded();
            }
            if (isModifierKey(key.key())) {
                return EditorCallResult.succeeded();       // the modifier's own press
            }
            if (!key.modifiers().isEmpty() && !key.modifiers().equals(
                    java.util.EnumSet.of(KeyModifier.SHIFT))) {
                chords.add(key.modifiers() + "+" + key.key());
                return EditorCallResult.succeeded();
            }
            switch (key.key()) {
                case BACKSPACE: eraseOne(); break;
                case ENTER: text.append('\n'); break;
                case SPACE: text.append(' '); break;
                default: break;
            }
            return EditorCallResult.succeeded();
        }
    }

    private static final class RichText implements Screen {
        private final FakeEditorBridge fake = new FakeEditorBridge();
        private final List<String> chords = new ArrayList<>();

        RichText() {
            fake.setModel("", EditorBounds.of(0, 0, -1, -1));
        }

        @Override public String screen() { return fake.modelText(); }
        @Override public List<String> chords() { return chords; }
        @Override public EditorBounds bounds() { return fake.modelBounds(); }
        @Override public EditorCallResult beginBatchEdit() { return fake.beginBatchEdit(); }
        @Override public EditorCallResult endBatchEdit() { return fake.endBatchEdit(); }
        @Override public EditorCallResult commitText(String t, int c) { return fake.commitText(t, c); }
        @Override public EditorCallResult setComposingText(String t, int c) {
            return fake.setComposingText(t, c);
        }
        @Override public EditorCallResult finishComposingText() { return fake.finishComposingText(); }
        @Override public EditorCallResult deleteSurroundingTextInCodePoints(int b, int a) {
            return fake.deleteSurroundingTextInCodePoints(b, a);
        }
        @Override public EditorTextResult getTextBeforeCursor(int m, int f) {
            return fake.getTextBeforeCursor(m, f);
        }
        @Override public EditorCallResult deleteSurroundingText(int b, int a) {
            return fake.deleteSurroundingText(b, a);
        }
        @Override public EditorCallResult performEditorAction(int id) {
            return fake.performEditorAction(id);
        }
        @Override public EditorCallResult sendRawKey(RawEditorKey key) {
            if (isModifierKey(key.key())) {
                return EditorCallResult.succeeded();
            }
            if (key.action() == RawEditorKey.Action.DOWN && !key.modifiers().isEmpty()
                    && !key.modifiers().equals(java.util.EnumSet.of(KeyModifier.SHIFT))) {
                chords.add(key.modifiers() + "+" + key.key());
                return EditorCallResult.succeeded();
            }
            if (key.action() == RawEditorKey.Action.DOWN && key.key() == RawKey.SPACE) {
                // A TextView types a key's character at the cursor and leaves any composing span
                // where it was — it does not grow to take the new character in.
                typeAtCursor(" ");
                return EditorCallResult.succeeded();
            }
            return fake.sendRawKey(key);
        }

        private void typeAtCursor(String typed) {
            EditorBounds b = fake.modelBounds();
            String t = fake.modelText();
            int at = b.selectionEnd();
            fake.setModel(t.substring(0, at) + typed + t.substring(at),
                EditorBounds.of(at + typed.length(), at + typed.length(),
                    b.composingStart(), b.composingEnd()));
        }

        /** The user puts the cursor somewhere else. */
        void moveCursor(int to) {
            EditorBounds b = fake.modelBounds();
            fake.setModel(fake.modelText(),
                EditorBounds.of(to, to, b.composingStart(), b.composingEnd()));
        }
    }

    static boolean isModifierKey(RawKey key) {
        return key == RawKey.CTRL_LEFT || key == RawKey.SHIFT_LEFT || key == RawKey.ALT_LEFT
            || key == RawKey.META_LEFT;
    }

    /** Moves the cursor of a rich-text screen; other editors have none to move. */
    static void moveCursor(Screen screen, int to) {
        if (screen instanceof RichText) {
            ((RichText) screen).moveCursor(to);
        }
    }

    private static final class Terminal extends Base {
        private final StringBuilder pending = new StringBuilder();

        @Override public EditorBounds bounds() { return EditorBounds.unknown(); }

        @Override public EditorCallResult performEditorAction(int actionId) {
            // Termux does not override it: BaseInputConnection sends Enter as key events.
            text.append('\n');
            return EditorCallResult.succeeded();
        }

        @Override public EditorCallResult commitText(String t, int c) {
            pending.setLength(0);
            write(t);
            return EditorCallResult.succeeded();
        }

        @Override public EditorCallResult setComposingText(String t, int c) {
            pending.setLength(0);
            pending.append(t);
            return EditorCallResult.succeeded();
        }

        @Override public EditorCallResult finishComposingText() {
            write(pending.toString());
            pending.setLength(0);
            return EditorCallResult.succeeded();
        }

        @Override public EditorCallResult deleteSurroundingTextInCodePoints(int b, int a) {
            return EditorCallResult.succeeded();          // the dummy buffer only
        }

        @Override public EditorCallResult deleteSurroundingText(int b, int a) {
            for (int i = 0; i < b; i++) {
                eraseOne();
            }
            return EditorCallResult.succeeded();
        }

        @Override public EditorCallResult sendRawKey(RawEditorKey key) {
            return key(key);
        }

        private void write(String t) {
            for (int i = 0; i < t.length(); ) {
                int cp = t.codePointAt(i);
                if (cp == 0x7f) {
                    eraseOne();
                } else {
                    text.appendCodePoint(cp == '\r' ? '\n' : cp);
                }
                i += Character.charCount(cp);
            }
        }
    }

    private static final class RemoteDesktop extends Base {
        /** What the client's dummy buffer still holds of this keyboard's own recent commits. */
        private int recentCommitted;

        @Override public EditorBounds bounds() {
            int end = text.codePointCount(0, text.length());
            return EditorBounds.of(end, end, -1, -1);
        }

        @Override public EditorCallResult commitText(String t, int c) {
            text.append(t);
            recentCommitted += t.codePointCount(0, t.length());
            return EditorCallResult.succeeded();
        }

        @Override public EditorCallResult setComposingText(String t, int c) {
            return EditorCallResult.succeeded();          // never reaches the far side
        }

        @Override public EditorCallResult finishComposingText() {
            return EditorCallResult.succeeded();
        }

        @Override public EditorCallResult deleteSurroundingTextInCodePoints(int b, int a) {
            int crossing = Math.min(b, recentCommitted);
            for (int i = 0; i < crossing; i++) {
                eraseOne();
            }
            recentCommitted -= crossing;
            return EditorCallResult.succeeded();
        }

        @Override public EditorCallResult deleteSurroundingText(int b, int a) {
            return deleteSurroundingTextInCodePoints(b, a);
        }

        @Override public EditorCallResult sendRawKey(RawEditorKey key) {
            recentCommitted = 0;
            return key(key);
        }
    }
}
