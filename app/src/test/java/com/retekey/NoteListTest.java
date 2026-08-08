package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** The notepad's list: how it sorts, what the checkboxes mean, and what deleting leaves. */
public final class NoteListTest {
    private static final Note ALPHA = new Note("20260713-1448", "Beta", "one");
    private static final Note BETA = new Note("20260714-0900", "alpha", "two");
    private static final Note GAMMA = new Note("20260712-2300", "", "three");

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
        NoteList moved = list.moved("20260712-2300", -1);
        assertEquals(Arrays.asList("20260714-0900", "20260712-2300", "20260713-1448"),
            stamps(moved));
        assertEquals(NoteList.Sort.MANUAL, moved.sort());
        // And they stop at the ends rather than wrapping.
        assertEquals(stamps(moved), stamps(moved.moved("20260714-0900", -1)));
        assertEquals(stamps(moved), stamps(moved.moved("20260713-1448", 1)));
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
        NoteList list = three().toggledSelection("20260713-1448");
        assertTrue(list.isSelected("20260713-1448"));
        assertFalse(list.isSelected("20260714-0900"));
        assertFalse(list.allSelected());
        assertFalse(list.toggledSelection("20260713-1448").isSelected("20260713-1448"));
    }

    @Test
    public void deletingTakesTheTickedOnesAndClearsTheTicks() {
        NoteList list = three().toggledSelection("20260713-1448").toggledSelection("20260712-2300");
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
        Note fresh = new Note("20260715-1000", "newest", "");
        assertEquals("20260715-1000", list.added(fresh).notes().get(0).stamp());
    }

    @Test
    public void editingANoteKeepsItsPlace() {
        NoteList list = three().sortedBy(NoteList.Sort.STAMP);
        NoteList edited = list.replaced(ALPHA.withBody("rewritten"));
        assertEquals(stamps(list), stamps(edited));
        assertEquals("rewritten", edited.byStamp("20260713-1448").body());
    }
}
