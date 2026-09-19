package com.retekey;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Issue #11: the two uses of a share are told apart by the door, never by the text. */
public class ShareRouteTest {

    @Test
    public void ordinaryTextIsKept() {
        assertEquals(ShareRoute.Route.KEEP_TEXT, ShareRoute.of(false, "a password"));
    }

    @Test
    public void textThatMerelyLooksLikeALayoutIsStillKept() {
        // The whole point of the split: a note beginning with the header is a note.
        assertEquals(ShareRoute.Route.KEEP_TEXT,
            ShareRoute.of(false, UserLayout.HEADER + "\nname=not really"));
    }

    @Test
    public void theLayoutDoorAlwaysInstalls() {
        assertEquals(ShareRoute.Route.INSTALL_LAYOUT,
            ShareRoute.of(true, UserLayout.HEADER + "\nname=Greek"));
        // Even text that is not a layout: it came through the layout door, so the answer the user
        // needs is "that was not a layout", not a note kept somewhere they did not ask for.
        assertEquals(ShareRoute.Route.INSTALL_LAYOUT, ShareRoute.of(true, "hello"));
        assertEquals(ShareRoute.Route.INSTALL_LAYOUT, ShareRoute.of(true, null));
    }

    @Test
    public void nothingUsableIsNothing() {
        assertEquals(ShareRoute.Route.NOTHING, ShareRoute.of(false, null));
        assertEquals(ShareRoute.Route.NOTHING, ShareRoute.of(false, "   "));
    }
}
