package dev.hellgates.retekeyime;

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
    private static final int MAX_WIDTH_DP = 340;
    private static final int SIDE_MARGIN_DP = 8;
    private static final int GAP_DP = 6;
    private static final int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final Context context;
    private final PopupWindow popup;
    private final HanjaCandidatesView view;
    private int panelHeight;

    public HanjaCandidatesWindow(Context context, HanjaCandidatesView.OnPick onPick) {
        this.context = context;
        this.view = new HanjaCandidatesView(context);
        this.view.setOnPick(onPick);
        this.popup = new PopupWindow(view, width(context), WRAP_CONTENT);
        // Never focusable: the service must keep receiving key events while the panel is up.
        popup.setFocusable(false);
        popup.setTouchable(true);
        popup.setOutsideTouchable(false);
        popup.setClippingEnabled(true);
        popup.setBackgroundDrawable(new ColorDrawable(0x00000000));
        popup.setElevation(dp(context, 8));
    }

    /**
     * Shows the candidates above {@code keyboardTop} in screen coordinates, or near the bottom of
     * the screen when nothing is on screen to sit above.
     *
     * @param anchor any attached view; only its window token is used
     */
    public void show(View anchor, String reading, List<HanjaCandidatesView.Item> items,
                     int keyboardTop) {
        if (anchor == null || anchor.getWindowToken() == null) {
            return;
        }
        view.show(reading, items);
        int width = width(context);
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        int x = Math.max(dp(context, SIDE_MARGIN_DP), (screenWidth - width) / 2);
        int bottom = keyboardTop > 0 && keyboardTop <= screenHeight
            ? keyboardTop - dp(context, GAP_DP)
            : screenHeight - dp(context, 48);
        // Measure first so the panel can be placed by its bottom edge rather than its top.
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST)
        );
        // The window keeps the height its first page needed. A later page with fewer than nine
        // candidates then leaves empty space instead of resizing the window under the user's
        // finger — and cannot be re-measured into something the size of the screen.
        panelHeight = Math.min(view.getMeasuredHeight(), screenHeight);
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
            popup.update(width(context), panelHeight);
        }
    }

    private static int width(Context context) {
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        return Math.min(dp(context, MAX_WIDTH_DP), screenWidth - 2 * dp(context, SIDE_MARGIN_DP));
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
