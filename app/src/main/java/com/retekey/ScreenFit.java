package com.retekey;

import android.os.Build;
import android.view.View;

/**
 * Keeps an activity's content out from under the system's furniture and its own title bar.
 *
 * <p>Android 15 lays every activity of an app targeting SDK 35 and later out edge to edge, and
 * Android 16 takes away the opt-out. On some ROMs — One UI 8.5 was the one reported, issue #2 —
 * that puts the first rows of the settings screens under the status bar and the action bar, where
 * no amount of scrolling reaches them; on others the framework still places the content below the
 * bar and everything is fine. The two cannot be told apart by reading insets, any more than the
 * keyboard's bottom band could (issue #1): the answer is to <em>measure</em> where the screen's
 * frame landed against where the furniture reaches, and pad by the overlap. Where there is no
 * overlap the padding is zero, so a phone that already fits the content gets nothing added.
 *
 * <p>The rule is Android-free so it can be tested with the numbers off a screenshot; the
 * measuring is done by {@link WindowInsetsWatcher}, the one class allowed to touch insets.
 */
final class ScreenFit {
    private ScreenFit() {
    }

    /**
     * Fits {@code content} inside {@code frame}: {@code frame} is the view that fills the window's
     * content area (a scroller, or the content itself) and is what gets measured; {@code content} is
     * what receives the padding, on top of whatever padding it already has. Call once, after both
     * are built and before the activity shows. Does nothing below API 20, where there is no
     * edge-to-edge layout and no insets to ask.
     */
    static void apply(View frame, View content) {
        if (Build.VERSION.SDK_INT >= SystemBarInsets.ANY_INSETS_SDK) {
            WindowInsetsWatcher.fitScreen(frame, content);
        }
    }

    /**
     * How far furniture coming down from the top of the screen — the status bar, and the action
     * bar below it — reaches into a frame whose top edge is at {@code frameTopOnScreen}.
     *
     * @param furnitureBottomOnScreen where the lowest piece of top furniture ends, in screen pixels
     * @param frameTopOnScreen where the frame begins, in screen pixels
     */
    static int topOverlap(int furnitureBottomOnScreen, int frameTopOnScreen) {
        return Math.max(0, furnitureBottomOnScreen - frameTopOnScreen);
    }

    /**
     * How far furniture coming up from the bottom of the screen — the navigation bar, or the
     * keyboard — reaches into a frame whose bottom edge sits {@code frameBottomOnScreen} down a
     * screen {@code screenHeight} tall. This is the keyboard's own lift rule from issue #1: the
     * furniture's height, less how far the frame already sits above the bottom.
     */
    static int bottomOverlap(int furnitureHeight, int screenHeight, int frameBottomOnScreen) {
        int lift = Math.max(0, screenHeight - frameBottomOnScreen);
        return Math.max(0, furnitureHeight - lift);
    }

    /** The same for a side: how far furniture {@code furnitureWidth} wide from the left reaches in. */
    static int leftOverlap(int furnitureWidth, int frameLeftOnScreen) {
        return Math.max(0, furnitureWidth - Math.max(0, frameLeftOnScreen));
    }

    /** And from the right, on a screen {@code screenWidth} wide. */
    static int rightOverlap(int furnitureWidth, int screenWidth, int frameRightOnScreen) {
        return Math.max(0, furnitureWidth - Math.max(0, screenWidth - frameRightOnScreen));
    }
}
