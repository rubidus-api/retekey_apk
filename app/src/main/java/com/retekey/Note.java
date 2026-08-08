package com.retekey;

import java.util.Locale;

/**
 * One note: a stamp, a title and a body.
 *
 * <p>The stamp is the note's identity as well as its date — it is written the moment the note is
 * made, in {@code 20260713-1448} form, and never edited afterwards. The first line of a note on
 * screen is that stamp and the title beside it; everything from the second line down is the body.
 * Keeping the three apart in the model is what lets "select all" mean the body alone, which is
 * what someone reaching for it in a note actually wants.
 */
public final class Note {
    private final String stamp;
    private final String title;
    private final String body;

    public Note(String stamp, String title, String body) {
        if (stamp == null || stamp.isEmpty()) {
            throw new IllegalArgumentException("a note is stamped when it is made");
        }
        this.stamp = stamp;
        this.title = title == null ? "" : title;
        this.body = body == null ? "" : body;
    }

    /** The stamp for a moment, in the form the first line carries: {@code 20260713-1448}. */
    public static String stampOf(long epochMillis) {
        return new java.text.SimpleDateFormat("yyyyMMdd-HHmm", Locale.US)
            .format(new java.util.Date(epochMillis));
    }

    public String stamp() {
        return stamp;
    }

    public String title() {
        return title;
    }

    public String body() {
        return body;
    }

    /** The note with a different title; the stamp is fixed for the note's life. */
    public Note withTitle(String newTitle) {
        return new Note(stamp, newTitle, body);
    }

    /** The note with a different body. */
    public Note withBody(String newBody) {
        return new Note(stamp, title, newBody);
    }

    /** The first line as it is shown: the stamp, then the title when there is one. */
    public String headline() {
        return title.isEmpty() ? stamp : stamp + " " + title;
    }

    /** The whole note as text — the headline, then the body — for copying it out. */
    public String fullText() {
        return body.isEmpty() ? headline() : headline() + "\n" + body;
    }

    /** What the title column sorts on: an untitled note sorts by its stamp instead. */
    public String sortTitle() {
        return title.isEmpty() ? stamp : title;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Note)) {
            return false;
        }
        Note that = (Note) other;
        return stamp.equals(that.stamp) && title.equals(that.title) && body.equals(that.body);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(stamp, title, body);
    }

    @Override
    public String toString() {
        return "Note{" + stamp + " " + title + " (" + body.length() + " chars)}";
    }
}
