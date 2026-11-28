package com.retekey;

import static org.junit.Assert.assertEquals;

import com.retekey.core.SemanticJamo;
import org.junit.Test;

/** RFC-0014 F8: the notepad's composers settle in the order the letters were typed. */
public class NotepadTypingTest {
    /** A note as NotepadView keeps it: committed text and the composition after it. */
    private static final class Note implements NotepadTyping.Sink {
        final StringBuilder committed = new StringBuilder();
        String composing = "";

        @Override
        public void typeComposed(String commit, String preedit) {
            committed.append(commit);
            composing = preedit;
        }

        @Override
        public void type(String text) {
            committed.append(composing).append(text);
            composing = "";
        }

        @Override
        public void endComposition() {
            committed.append(composing);
            composing = "";
        }

        String text() {
            return committed + composing;
        }
    }

    private static final SemanticJamo GIYEOK = SemanticJamo.contextualConsonant(0);

    @Test
    public void aJamoAfterATelexWordComesAfterIt() {
        NotepadTyping typing = new NotepadTyping();
        Note note = new Note();
        for (String letter : new String[] {"v", "i", "e"}) {
            typing.text(note, NotepadTyping.Latin.TELEX, letter);
        }
        typing.jamo(note, GIYEOK);
        typing.text(note, NotepadTyping.Latin.NONE, " ");
        assertEquals("vieㄱ ", note.text());
    }

    @Test
    public void aRomajiLetterAfterATelexWordSettlesTheWordFirst() {
        NotepadTyping typing = new NotepadTyping();
        Note note = new Note();
        typing.text(note, NotepadTyping.Latin.TELEX, "a");
        typing.text(note, NotepadTyping.Latin.ROMAJI, "k");
        typing.flush(note);
        assertEquals("ak", note.text());
    }

    @Test
    public void aLetterAfterASyllableSettlesTheSyllableFirst() {
        NotepadTyping typing = new NotepadTyping();
        Note note = new Note();
        typing.jamo(note, GIYEOK);
        typing.text(note, NotepadTyping.Latin.TELEX, "a");
        typing.flush(note);
        assertEquals("ㄱa", note.text());
    }
}
