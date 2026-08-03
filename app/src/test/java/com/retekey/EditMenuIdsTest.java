package com.retekey;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * The undo/redo ids are copied constants, and a copied constant that drifts is a silent bug. These
 * are the values the platform froze; if a future SDK ever disagrees, this is where it shows.
 */
public final class EditMenuIdsTest {
    @Test
    public void theyAreThePlatformIds() {
        assertEquals(0x0102001a, EditMenuIds.UNDO);
        assertEquals(0x0102001b, EditMenuIds.REDO);
    }
}
