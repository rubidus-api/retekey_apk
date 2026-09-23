package com.retekey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * The stored form of the notes, the clip history and the shared-text stash. The old form separated
 * records with U+001E and fields with U+001F — characters no keyboard types, but which arrive by
 * paste, and then a note became two notes or a clip vanished on save (review findings R06, R07).
 * Here a field says how long it is, so nothing in it can end it.
 */
public final class RecordCodecTest {
    private static final String EMOJI = "😀";

    @Test
    public void theEncodedFormIsWhatItSays() {
        // Written out by hand: the codec must not be the only judge of its own format.
        Assert.assertEquals(
            "retekey-store 2 2\n1:a3:bcd",
            RecordCodec.encode(Collections.singletonList(new String[] {"a", "bcd"}), 2));
        Assert.assertEquals(
            "retekey-store 2 2\n0:0:",
            RecordCodec.encode(Collections.singletonList(new String[] {"", ""}), 2));
        Assert.assertEquals(
            "retekey-store 2 2\n1:\u001E3:a\u001Fb",
            RecordCodec.encode(Collections.singletonList(new String[] {"\u001E", "a\u001Fb"}), 2));
    }

    @Test
    public void everythingThatCanBeTypedOrPastedSurvives() {
        List<String[]> records = Arrays.asList(
            new String[] {"note\u001Ebody", "title\u001Fwith units"},
            new String[] {"line\r\nbreaks\tand tabs", ""},
            new String[] {EMOJI + "at the front", "10:not a length"},
            new String[] {"", "retekey-store 2 2\n1:a"});
        List<String[]> back = RecordCodec.decode(RecordCodec.encode(records, 2), 2);
        Assert.assertEquals(records.size(), back.size());
        for (int i = 0; i < records.size(); i++) {
            Assert.assertArrayEquals(records.get(i), back.get(i));
        }
    }

    @Test
    public void anEmptyStoreIsAnEmptyList() {
        Assert.assertTrue(RecordCodec.decode(null, 2).isEmpty());
        Assert.assertTrue(RecordCodec.decode("", 2).isEmpty());
        Assert.assertTrue(RecordCodec.decode(RecordCodec.encode(
            new ArrayList<String[]>(), 2), 2).isEmpty());
        Assert.assertTrue(RecordCodec.isNewFormat(RecordCodec.encode(new ArrayList<String[]>(), 2)));
    }

    @Test
    public void theOldFormatIsRecognisedAsNotThisOne() {
        Assert.assertFalse(RecordCodec.isNewFormat("20260713-1448\ttitle\nbody"));
        Assert.assertFalse(RecordCodec.isNewFormat(""));
        Assert.assertFalse(RecordCodec.isNewFormat(null));
        Assert.assertTrue(RecordCodec.isNewFormat("retekey-store 2 4\n"));
    }

    /** A store cut short keeps the records that are whole; the rest is not guessed at. */
    @Test
    public void adamagedStoreKeepsWhatItCanRead() {
        String whole = RecordCodec.encode(Arrays.asList(
            new String[] {"one", "first"}, new String[] {"two", "second"}), 2);
        List<String[]> partial = RecordCodec.decode(whole.substring(0, whole.length() - 3), 2);
        Assert.assertEquals(1, partial.size());
        Assert.assertArrayEquals(new String[] {"one", "first"}, partial.get(0));
        Assert.assertTrue(RecordCodec.decode("retekey-store 2 2\nnonsense", 2).isEmpty());
        Assert.assertTrue(RecordCodec.decode("retekey-store 2 2\n-1:x1:y", 2).isEmpty());
        // A length that would overflow when added to the position must not read as a short one.
        Assert.assertTrue(RecordCodec.decode("retekey-store 2 2\n2147483647:x", 2).isEmpty());
    }

    /** The header says how many fields a record has, so a store of notes is not read as clips. */
    @Test
    public void aStoreWrittenForOtherRecordsIsNotRead() {
        String threeFields = RecordCodec.encode(Collections.singletonList(
            new String[] {"a", "b", "c"}), 3);
        Assert.assertTrue(RecordCodec.decode(threeFields, 2).isEmpty());
        Assert.assertEquals(1, RecordCodec.decode(threeFields, 3).size());
    }
}
