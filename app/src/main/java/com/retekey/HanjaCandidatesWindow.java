package com.retekey;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import java.util.List;

/**
 * The Hanja candidate panel as a window of its own, floating over whatever the keyboard happens to
 * be doing.
 *
 * <p>It used to live in the platform's candidates area, which ties it to the IME window: with an
 * external keyboard that window is suppressed and the strip had nowhere to appear, and with the
 * floating keyboard it would have been stapled to a panel the user moves around. A popup is
 * independent of both — the same panel shows for the on-screen keyboard, an external keyboard, and
 * the floating keyboard.
 *
 * <p>The popup never takes focus, so key events keep reaching the service and the number keys still
 * pick a candidate.
 */
public final class HanjaCandidatesWindow {
    private static final int GAP_DP = 6;
    /** No matter where the anchor points, leave at least this much room for the panel itself. */
    private static final int MIN_PANEL_DP = 120;
    private static final int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final Context context;
    private final PopupWindow popup;
    private final HanjaCandidatesView view;
    private int panelHeight;
    private int panelWidth;

    public HanjaCandidatesWindow(Context context, HanjaCandidatesView.OnPick onPick,
                                 Runnable onDismiss) {
        this.context = context;
        this.view = new HanjaCandidatesView(context);
        this.view.setOnPick(onPick);
        this.view.setOnDismiss(onDismiss);
        this.popup = new PopupWindow(
            view, context.getResources().getDisplayMetrics().widthPixels, WRAP_CONTENT);
        // Never focusable: the service must keep receiving key events while the panel is up.
        popup.setFocusable(false);
        popup.setTouchable(true);
        popup.setOutsideTouchable(false);
        popup.setClippingEnabled(true);
        // The popup is a child of the IME window, and a child laid out "in decor" is clipped to
        // its parent's frame — an IME window is only as tall as the keyboard, so a panel taller
        // than that lost its bottom rows to the clip. Detaching it from the decor lets it be
        // placed against the screen instead, which is the space its coordinates are computed in.
        Compat.setAttachedInDecor(popup, false);
        popup.setBackgroundDrawable(new ColorDrawable(0x00000000));
        Compat.setElevation(popup, dp(context, 8));
    }

    /**
     * Shows the candidates above {@code keyboardTop} in screen coordinates, or near the bottom of
     * the screen when nothing is on screen to sit above.
     *
     * @param anchor any attached view; only its window token is used
     */
    public void show(View anchor, String reading, List<HanjaCandidatesView.Item> items,
                     int keyboardLeft, int keyboardWidth, int keyboardTop) {
        if (anchor == null || anchor.getWindowToken() == null) {
            return;
        }
        view.show(reading, items);
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        // The area the system bars leave, asked of the window rather than of the display: a
        // service's display metrics do not have the navigation bar taken out of them, and placing
        // the panel against the raw display height puts its last row under the system buttons.
        android.graphics.Rect visible = new android.graphics.Rect();
        anchor.getWindowVisibleDisplayFrame(visible);
        int screenHeight = visible.bottom > 0
            ? visible.bottom
            : context.getResources().getDisplayMetrics().heightPixels;
        // The panel spans the keyboard it belongs to, edge to edge, so the glosses have room.
        int width = keyboardWidth > 0 ? Math.min(keyboardWidth, screenWidth) : screenWidth;
        int x = Math.max(0, Math.min(keyboardLeft, screenWidth - width));
        // Where the panel's bottom edge goes. The anchor can point below the usable screen — a
        // collapsed keyboard reports a top near the bottom of the IME window, which is past the
        // navigation bar — so it is clamped. Without this the panel is placed by a bottom that
        // does not exist and its last row is drawn underneath the navigation bar.
        int bottom = keyboardTop > 0
            ? Math.min(keyboardTop - dp(context, GAP_DP), screenHeight)
            : screenHeight - dp(context, 48);
        bottom = Math.max(dp(context, MIN_PANEL_DP), bottom);
        // Measure first so the panel can be placed by its bottom edge rather than its top.
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(bottom, View.MeasureSpec.AT_MOST)
        );
        // The window keeps the height its first page needed. A later page with fewer than nine
        // candidates then leaves empty space instead of resizing the window under the user's
        // finger — and cannot be re-measured into something the size of the screen. It can never
        // be taller than the room above `bottom`, which is what keeps every row on screen.
        panelHeight = Math.min(view.getMeasuredHeight(), bottom);
        panelWidth = width;
        int y = Math.max(0, bottom - panelHeight);
        if (popup.isShowing()) {
            popup.update(x, y, width, panelHeight);
            return;
        }
        popup.setWidth(width);
        popup.setHeight(panelHeight);
        try {
            popup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);
        } catch (RuntimeException ignored) {
            // A bad window token must never take the keyboard down with it.
        }
    }

    public void hide() {
        try {
            popup.dismiss();
        } catch (RuntimeException ignored) {
            // Dismissing an already-gone window is not an error worth crashing over.
        }
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    public boolean selectByNumber(int number) {
        return view.selectByNumber(number);
    }

    public boolean nextPage() {
        boolean turned = view.nextPage();
        keepSize();
        return turned;
    }

    public boolean prevPage() {
        boolean turned = view.prevPage();
        keepSize();
        return turned;
    }

    /** Holds the window to the size it was shown at, whatever the new page's content needs. */
    private void keepSize() {
        if (popup.isShowing() && panelHeight > 0) {
            popup.update(panelWidth, panelHeight);
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
