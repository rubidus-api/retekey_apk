package com.retekey;

import java.nio.ByteBuffer;

/**
 * A lookup table that stays on disk. The bytes are sorted {@code key\tvalue} lines, and a search
 * bisects them in place rather than parsing them into a map.
 *
 * <p>The Hanja tables are 190 KB of text that cost about 3.5 MB of Java heap once every entry has
 * become a {@code String} in a {@code HashMap} — an object header and a char array for each of some
 * twenty thousand keys and values. Searched where they lie, they cost the heap nothing: the buffer
 * is file-backed, so the pages are clean and the kernel drops them under pressure and reads them
 * back when a lookup touches them again. A lookup touches a handful.
 *
 * <p>Comment lines at the top carry the data's copyright notice and are skipped. One of them may be
 * the directive {@code #!maxkey N}, the longest key in the file, which callers need to know how far
 * back to look when matching a suffix.
 *
 * <p>Android-free: the buffer comes from wherever the caller got it.
 */
public final class SortedIndex {
    private static final byte NEWLINE = '\n';
    private static final byte TAB = '\t';
    private static final byte HASH = '#';

    private final ByteBuffer bytes;
    private final int start;
    private final int end;
    private final int maxKeyLength;

    private SortedIndex(ByteBuffer bytes, int start, int end, int maxKeyLength) {
        this.bytes = bytes;
        this.start = start;
        this.end = end;
        this.maxKeyLength = maxKeyLength;
    }

    /** Reads the header, then leaves the rest to be searched where it lies. */
    public static SortedIndex over(ByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }
        ByteBuffer bytes = buffer.duplicate();
        int limit = bytes.limit();
        int cursor = 0;
        int maxKey = 1;
        while (cursor < limit && bytes.get(cursor) == HASH) {
            int lineEnd = lineEnd(bytes, cursor, limit);
            String line = text(bytes, cursor, lineEnd);
            if (line.startsWith("#!maxkey ")) {
                try {
                    maxKey = Math.max(1, Integer.parseInt(line.substring(9).trim()));
                } catch (NumberFormatException keepDefault) {
                    // A damaged directive must not stop the table from working.
                }
            }
            cursor = lineEnd + 1;
        }
        return new SortedIndex(bytes, cursor, limit, maxKey);
    }

    /** An index over nothing, for when the data could not be opened. */
    public static SortedIndex empty() {
        return new SortedIndex(ByteBuffer.allocate(0), 0, 0, 1);
    }

    /** The longest key in the file, so a suffix search knows where to start. */
    public int maxKeyLength() {
        return maxKeyLength;
    }

    public boolean isEmpty() {
        return start >= end;
    }

    /** The value stored for {@code key}, or {@code null} when the key is not in the file. */
    public String find(String key) {
        if (key == null || key.isEmpty() || isEmpty()) {
            return null;
        }
        byte[] wanted = key.getBytes(Compat.UTF_8);
        int lineStart = lowerBound(wanted);
        if (lineStart >= end) {
            return null;
        }
        int lineEnd = lineEnd(bytes, lineStart, end);
        int separator = separator(lineStart, lineEnd);
        if (separator < 0 || !keyEquals(lineStart, separator, wanted)) {
            return null;
        }
        return text(bytes, separator + 1, lineEnd);
    }

    /** The start of the first line whose key is not less than {@code wanted}. */
    private int lowerBound(byte[] wanted) {
        int low = start;
        int high = end;
        while (low < high) {
            int probe = (low + high) >>> 1;
            int lineStart = lineStart(probe, low);
            int lineEnd = lineEnd(bytes, lineStart, end);
            if (compareKey(lineStart, lineEnd, wanted) < 0) {
                // Everything up to and including this line is too small.
                low = lineEnd + 1;
            } else {
                high = lineStart;
            }
        }
        return low;
    }

    /** Walks back from an arbitrary byte to the start of the line it sits in. */
    private int lineStart(int probe, int floor) {
        int cursor = probe;
        while (cursor > floor && bytes.get(cursor - 1) != NEWLINE) {
            cursor--;
        }
        return cursor;
    }

    private static int lineEnd(ByteBuffer bytes, int from, int limit) {
        int cursor = from;
        while (cursor < limit && bytes.get(cursor) != NEWLINE) {
            cursor++;
        }
        return cursor;
    }

    private int separator(int lineStart, int lineEnd) {
        for (int cursor = lineStart; cursor < lineEnd; cursor++) {
            if (bytes.get(cursor) == TAB) {
                return cursor;
            }
        }
        return -1;
    }

    /** UTF-8 bytes sort in the same order as the characters they encode, so this is a plain compare. */
    private int compareKey(int lineStart, int lineEnd, byte[] wanted) {
        int separator = separator(lineStart, lineEnd);
        int keyEnd = separator < 0 ? lineEnd : separator;
        int length = Math.min(keyEnd - lineStart, wanted.length);
        for (int i = 0; i < length; i++) {
            int mine = bytes.get(lineStart + i) & 0xFF;
            int theirs = wanted[i] & 0xFF;
            if (mine != theirs) {
                return mine < theirs ? -1 : 1;
            }
        }
        return Integer.compare(keyEnd - lineStart, wanted.length);
    }

    private boolean keyEquals(int lineStart, int separator, byte[] wanted) {
        if (separator - lineStart != wanted.length) {
            return false;
        }
        for (int i = 0; i < wanted.length; i++) {
            if (bytes.get(lineStart + i) != wanted[i]) {
                return false;
            }
        }
        return true;
    }

    private static String text(ByteBuffer bytes, int from, int to) {
        byte[] slice = new byte[Math.max(0, to - from)];
        for (int i = 0; i < slice.length; i++) {
            slice[i] = bytes.get(from + i);
        }
        return new String(slice, Compat.UTF_8);
    }
}
