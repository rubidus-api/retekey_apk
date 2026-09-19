package com.retekey;

/**
 * What a share handed to ReteKey is: a piece of text to keep, or a layout to install (issues #10
 * and #11).
 *
 * <p>The first cut decided by reading the text — anything starting with the layout header was a
 * layout — which the reporter of #11 rightly called a patch: a note that happens to begin that way
 * would be installed instead of kept, and a layout that lost its first line would be kept instead
 * of installed. The door is split in two instead. Sharing to **ReteKey** keeps text, always; a
 * second entry of its own, and opening a {@code .rkl} file, installs a layout, always. Nothing is
 * guessed from the contents, so neither use can be mistaken for the other.
 *
 * <p>Pure, so the routing is tested without an Android runtime.
 */
final class ShareRoute {

    enum Route {
        /** Keep the text inside ReteKey, to be typed later. */
        KEEP_TEXT,
        /** Read it as a layout and install it. */
        INSTALL_LAYOUT,
        /** Nothing usable arrived. */
        NOTHING
    }

    private ShareRoute() {
    }

    /**
     * Where an arriving intent goes.
     *
     * @param throughLayoutDoor whether it came through the layout entry, or an opened file
     * @param text              the text it carries, if any
     */
    static Route of(boolean throughLayoutDoor, String text) {
        boolean empty = text == null || text.trim().isEmpty();
        if (throughLayoutDoor) {
            // A file or an entry chosen on purpose: an empty one is still a layout that failed to
            // read, and saying so is better than silently keeping nothing.
            return Route.INSTALL_LAYOUT;
        }
        return empty ? Route.NOTHING : Route.KEEP_TEXT;
    }
}
