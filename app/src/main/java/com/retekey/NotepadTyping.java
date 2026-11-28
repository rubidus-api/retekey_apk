package com.retekey;

import com.retekey.core.HangulComposer;
import com.retekey.core.LatinComposer;
import com.retekey.core.RomajiKanaComposer;
import com.retekey.core.SemanticJamo;
import com.retekey.core.TelexComposer;

/**
 * The notepad's own composers: the Hangul one and the two Latin ones, Telex and romaji kana.
 *
 * <p>Only one composition is open at a time, and a letter for another composer settles the open
 * one first, in the order it was typed (RFC-0014 F8). A jamo typed after a Telex word used to land
 * in place of the word, because the note took the word's composition for the syllable's.
 */
final class NotepadTyping {
    /** What the notepad writes into: NotepadView, or a stand-in in tests. */
    interface Sink {
        /** Replaces the composition with {@code commit}, then composes {@code preedit}. */
        void typeComposed(String commit, String preedit);

        /** Types text after the composition, which has been settled. */
        void type(String text);

        /** Ends the composition, keeping what it shows. */
        void endComposition();
    }

    /** Which Latin composer the layout on screen asks for. */
    enum Latin { NONE, TELEX, ROMAJI }

    private final HangulComposer hangul = new HangulComposer();
    private final TelexComposer telex = new TelexComposer();
    private final RomajiKanaComposer romaji = new RomajiKanaComposer();

    void jamo(Sink note, SemanticJamo jamo) {
        settleLatin(note, null);
        HangulComposer.Result result = hangul.input(jamo);
        note.typeComposed(result.commit(), result.preedit());
    }

    void text(Sink note, Latin wanted, String text) {
        LatinComposer latin = composer(wanted);
        if (latin != null && latin.accepts(text)) {
            settleLatin(note, latin);
            settleHangul(note);
            LatinComposer.Result result = latin.input(text);
            note.typeComposed(result.commit, result.preedit);
            return;
        }
        flush(note);
        note.type(text);
    }

    /** Backspace inside a Latin composition; false when none is open. */
    boolean backspaceLatin(Sink note) {
        LatinComposer open = telex.isComposing() ? telex : romaji.isComposing() ? romaji : null;
        if (open == null) {
            return false;
        }
        LatinComposer.Result result = open.backspace();
        note.typeComposed("", result == null ? "" : result.preedit);
        return true;
    }

    /** Backspace inside the syllable: one jamo at a time. */
    void backspaceHangul(Sink note) {
        HangulComposer.Result result = hangul.backspace();
        note.typeComposed("", result == null ? "" : result.preedit());
    }

    /** Settles every composition into the note. */
    void flush(Sink note) {
        String flushed = hangul.flush() + telex.flush() + romaji.flush();
        if (note == null) {
            return;
        }
        if (!flushed.isEmpty()) {
            note.typeComposed(flushed, "");
        }
        note.endComposition();
    }

    /** Settles only the syllable, leaving a Latin word to carry on. */
    void settleHangul(Sink note) {
        String flushed = hangul.flush();
        if (note != null && !flushed.isEmpty()) {
            note.typeComposed(flushed, "");
        }
    }

    void reset() {
        hangul.reset();
        telex.reset();
        romaji.reset();
    }

    /** Settles the Latin compositions other than {@code keep}. */
    private void settleLatin(Sink note, LatinComposer keep) {
        String flushed = (keep == telex ? "" : telex.flush()) + (keep == romaji ? "" : romaji.flush());
        if (note != null && !flushed.isEmpty()) {
            note.typeComposed(flushed, "");
        }
    }

    private LatinComposer composer(Latin wanted) {
        switch (wanted) {
            case TELEX:
                return telex;
            case ROMAJI:
                return romaji;
            default:
                return null;
        }
    }
}
