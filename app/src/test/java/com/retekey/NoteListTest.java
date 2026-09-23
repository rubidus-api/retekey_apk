package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** The notepad's list: how it sorts, what the checkboxes mean, and what deleting leaves. */
public final class NoteListTest {
    private static final Note ALPHA = new Note("id-a", "20260713-1448", "Beta", "one");
    private static final Note BETA = new Note("id-b", "20260714-0900", "alpha", "two");
    private static final Note GAMMA = new Note("id-c", "20260712-2300", "", "three");
    /** Two notes written in the same minute: one stamp, two notes (review R05). */
    private static final Note TWIN = new Note("id-d", "20260713-1448", "same minute", "twin");

    private static NoteList three() {
        return NoteList.of(Arrays.asList(ALPHA, BETA, GAMMA));
    }

    private static List<String> stamps(NoteList list) {
        List<String> out = new java.util.ArrayList<>();
        for (Note note : list.notes()) {
            out.add(note.stamp());
        }
        return out;
    }

    @Test
    public void aStampIsWrittenTheWayTheFirstLineCarriesIt() {
        // 2026-07-13 14:48 local time.
        java.util.Calendar when = java.util.Calendar.getInstance(java.util.Locale.US);
        when.set(2026, java.util.Calendar.JULY, 13, 14, 48, 0);
        when.set(java.util.Calendar.MILLISECOND, 0);
        assertEquals("20260713-1448", Note.stampOf(when.getTimeInMillis()));
    }

    @Test
    public void theFirstLineIsTheStampAndTheTitle() {
        assertEquals("20260713-1448 Beta", ALPHA.headline());
        assertEquals("20260712-2300", GAMMA.headline());
        assertEquals("20260713-1448 Beta\none", ALPHA.fullText());
    }

    /** A column sorts by its field, and pressing it again turns the sort around. */
    @Test
    public void aColumnSortsAndThenReverses() {
        NoteList byDate = three().sortedBy(NoteList.Sort.STAMP);
        assertEquals(Arrays.asList("20260714-0900", "20260713-1448", "20260712-2300"),
            stamps(byDate));
        assertFalse("dates open newest first", byDate.ascending());

        NoteList reversed = byDate.sortedBy(NoteList.Sort.STAMP);
        assertEquals(Arrays.asList("20260712-2300", "20260713-1448", "20260714-0900"),
            stamps(reversed));
        assertTrue(reversed.ascending());
    }

    @Test
    public void titlesSortWithoutCaseAndUntitledNotesSortByTheirStamp() {
        NoteList byTitle = three().sortedBy(NoteList.Sort.TITLE);
        // "alpha" then "Beta" — case is not a sort order anyone means — and the untitled note
        // sorts under its stamp, which is the only name it has, so digits put it first.
        assertEquals(Arrays.asList("20260712-2300", "20260714-0900", "20260713-1448"),
            stamps(byTitle));
        assertTrue(byTitle.ascending());
        assertEquals(Arrays.asList("20260713-1448", "20260714-0900", "20260712-2300"),
            stamps(byTitle.sortedBy(NoteList.Sort.TITLE)));
    }

    @Test
    public void theArrowsMakeAnOrderOfTheirOwn() {
        NoteList list = three().sortedBy(NoteList.Sort.STAMP);
        NoteList moved = list.moved("id-c", -1);
        assertEquals(Arrays.asList("20260714-0900", "20260712-2300", "20260713-1448"),
            stamps(moved));
        assertEquals(NoteList.Sort.MANUAL, moved.sort());
        // And they stop at the ends rather than wrapping.
        assertEquals(stamps(moved), stamps(moved.moved("id-b", -1)));
        assertEquals(stamps(moved), stamps(moved.moved("id-a", 1)));
    }

    @Test
    public void theHeaderCheckboxTakesAllOrNone() {
        NoteList list = three();
        assertFalse(list.allSelected());
        NoteList all = list.toggledSelectAll();
        assertTrue(all.allSelected());
        assertEquals(3, all.selected().size());
        assertFalse(all.toggledSelectAll().allSelected());
        assertTrue(all.toggledSelectAll().selected().isEmpty());
    }

    @Test
    public void oneCheckboxTogglesOneNote() {
        NoteList list = three().toggledSelection("id-a");
        assertTrue(list.isSelected("id-a"));
        assertFalse(list.isSelected("id-b"));
        assertFalse(list.allSelected());
        assertFalse(list.toggledSelection("id-a").isSelected("id-a"));
    }

    @Test
    public void deletingTakesTheTickedOnesAndClearsTheTicks() {
        NoteList list = three().toggledSelection("id-a").toggledSelection("id-c");
        NoteList left = list.withoutSelected();
        assertEquals(Arrays.asList("20260714-0900"), stamps(left));
        assertTrue(left.selected().isEmpty());
    }

    @Test
    public void deleteAllEmptiesTheList() {
        assertTrue(three().cleared().isEmpty());
        assertTrue(three().cleared().selected().isEmpty());
    }

    @Test
    public void aNewNoteArrivesAtTheTopOfTheCurrentOrder() {
        NoteList list = three().sortedBy(NoteList.Sort.STAMP);
        Note fresh = new Note("id-e", "20260715-1000", "newest", "");
        assertEquals("20260715-1000", list.added(fresh).notes().get(0).stamp());
    }

    @Test
    public void editingANoteKeepsItsPlace() {
        NoteList list = three().sortedBy(NoteList.Sort.STAMP);
        NoteList edited = list.replaced(ALPHA.withBody("rewritten"));
        assertEquals(stamps(list), stamps(edited));
        assertEquals("rewritten", edited.byId("id-a").body());
    }

    /** Two notes made in the same minute are two notes, whatever they do (review R05). */
    @Test
    public void notesWithOneStampAreStillSeparate() {
        NoteList list = NoteList.of(Arrays.asList(ALPHA, TWIN));
        assertEquals("one", list.byId("id-a").body());
        assertEquals("twin", list.byId("id-d").body());

        NoteList selected = list.toggledSelection("id-d");
        assertFalse(selected.isSelected("id-a"));
        assertEquals(1, selected.withoutSelected().size());
        assertEquals("one", selected.withoutSelected().notes().get(0).body());

        NoteList edited = list.replaced(ALPHA.withBody("edited"));
        assertEquals("edited", edited.byId("id-a").body());
        assertEquals("twin", edited.byId("id-d").body());
    }

    @Test
    public void aNoteMadeNowHasAnIdentityOfItsOwn() {
        Note first = Note.made(1_000_000L, "", "");
        Note second = Note.made(1_000_000L, "", "");
        assertEquals(first.stamp(), second.stamp());
        assertFalse(first.id().equals(second.id()));
        assertEquals(first.id(), first.withBody("rewritten").id());
        assertEquals(first.id(), first.withTitle("named").id());
    }
}
