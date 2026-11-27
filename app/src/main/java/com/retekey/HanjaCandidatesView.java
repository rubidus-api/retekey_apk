package com.retekey;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * The Hanja candidate panel. It pages through candidates in a grid, shows each candidate's 훈음
 * (gloss) when known, and serves both directions: 한글 → 한자 (value = Hanja, gloss = meaning) and
 * 한자 → 한글 (value = reading, no gloss). It rides in a {@link FloatingKeyboardFrame} of its own —
 * a panel over the document that can be moved and resized — so it appears the same way whether the
 * typing comes from the on-screen keyboard, an external keyboard, or the floating keyboard.
 */
public final class HanjaCandidatesView extends LinearLayout {
    /** Notified with the chosen text (a Hanja, or a Hangul reading) when a candidate is tapped. */
    public interface OnPick {
        void pick(String value);
    }

    /** One candidate: the text to commit, and an optional gloss shown beneath it. */
    public static final class Item {
        final String value;
        final String gloss;

        public Item(String value, String gloss) {
            this.value = value;
            this.gloss = gloss;
        }
    }

    private static final int COLUMNS = 3;
    private static final int ROWS = 3;
    private static final int PAGE_SIZE = COLUMNS * ROWS;

    private OnPick listener;
    /** Run when the user closes the panel without converting anything. */
    private Runnable onDismiss;
    private String reading = "";
    private List<Item> items = new ArrayList<>();
    private int page;
    private KeyboardPalette palette;

    public HanjaCandidatesView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        palette = KeyboardPalette.resolve(context);
        setBackgroundColor(palette.background);
    }

    public void setOnPick(OnPick listener) {
        this.listener = listener;
    }

    public void setOnDismiss(Runnable onDismiss) {
        this.onDismiss = onDismiss;
    }

    /** Shows candidates for {@code reading}, starting at the first page. */
    public void show(String reading, List<Item> items) {
        this.reading = reading == null ? "" : reading;
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
        this.page = 0;
        palette = KeyboardPalette.resolve(getContext());
        setBackgroundColor(palette.background);
        render();
    }

    /** Number of candidates shown per page, so callers can label number keys (1..N). */
    public static int pageSize() {
        return PAGE_SIZE;
    }

    /** Picks the candidate at 1-based {@code number} on the current page; false if out of range. */
    public boolean selectByNumber(int number) {
        if (number < 1 || number > PAGE_SIZE || listener == null) {
            return false;
        }
        int index = page * PAGE_SIZE + (number - 1);
        if (index >= items.size()) {
            return false;
        }
        listener.pick(items.get(index).value);
        return true;
    }

    /** Advances to the next page; false if already on the last page. */
    public boolean nextPage() {
        if (page >= pageCount() - 1) {
            return false;
        }
        page++;
        render();
        return true;
    }

    /** Goes back a page; false if already on the first page. */
    public boolean prevPage() {
        if (page <= 0) {
            return false;
        }
        page--;
        render();
        return true;
    }

    private int pageCount() {
        return Math.max(1, (items.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void render() {
        removeAllViews();
        addView(buildHeader());

        LinearLayout grid = new LinearLayout(getContext());
        grid.setOrientation(VERTICAL);
        int start = page * PAGE_SIZE;
        for (int r = 0; r < ROWS; r++) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(HORIZONTAL);
            boolean rowHasContent = false;
            for (int c = 0; c < COLUMNS; c++) {
                int index = start + r * COLUMNS + c;
                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                cellParams.setMargins(dp(3), dp(3), dp(3), dp(3));
                if (index < items.size()) {
                    row.addView(buildCell(items.get(index), r * COLUMNS + c + 1), cellParams);
                    rowHasContent = true;
                } else {
                    View filler = new View(getContext());
                    row.addView(filler, cellParams);
                }
            }
            if (rowHasContent) {
                grid.addView(row);
            }
        }
        ScrollView scroller = new ScrollView(getContext());
        if (ScreenTheme.still(getContext())) {
            scroller.setOverScrollMode(OVER_SCROLL_NEVER);
        }
        scroller.addView(grid, new LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(scroller, new LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getChildCount() != 2 || MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        int maxH = MeasureSpec.getSize(heightMeasureSpec);
        int padH = getPaddingLeft() + getPaddingRight();
        int padV = getPaddingTop() + getPaddingBottom();
        int childWSpec = MeasureSpec.makeMeasureSpec(
            Math.max(0, MeasureSpec.getSize(widthMeasureSpec) - padH),
            MeasureSpec.getMode(widthMeasureSpec));
        View header = getChildAt(0);
        View scroller = getChildAt(1);
        header.measure(childWSpec, MeasureSpec.makeMeasureSpec(maxH, MeasureSpec.AT_MOST));
        int remain = Math.max(0, maxH - header.getMeasuredHeight() - padV);
        scroller.measure(childWSpec, MeasureSpec.makeMeasureSpec(remain, MeasureSpec.AT_MOST));
        int naturalH = header.getMeasuredHeight() + scroller.getMeasuredHeight() + padV;
        int measuredH = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY
            ? maxH
            : Math.min(maxH, naturalH);
        int measuredW = resolveSize(
            Math.max(header.getMeasuredWidth(), scroller.getMeasuredWidth()) + padH,
            widthMeasureSpec);
        setMeasuredDimension(measuredW, measuredH);
    }

    private LinearLayout buildHeader() {
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(getContext());
        label.setText(reading + " ▸");
        label.setTextColor(palette.keyTextMuted);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        label.setPadding(dp(12), dp(6), dp(8), dp(6));
        header.addView(label, new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (pageCount() > 1) {
            View prev = navButton("‹", page > 0, v -> {
                if (page > 0) {
                    page--;
                    render();
                }
            });
            header.addView(prev);

            TextView pageLabel = new TextView(getContext());
            pageLabel.setText((page + 1) + " / " + pageCount());
            pageLabel.setTextColor(palette.keyTextMuted);
            pageLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            pageLabel.setGravity(Gravity.CENTER);
            pageLabel.setMinWidth(dp(40));
            pageLabel.setPadding(dp(2), 0, dp(2), 0);
            header.addView(pageLabel);

            header.addView(navButton("›", page < pageCount() - 1, v -> {
                if (page < pageCount() - 1) {
                    page++;
                    render();
                }
            }));
        }

        // Leaving without converting. The reading stays as it was typed — this is a way out, not
        // an undo — and it is the only control here that is always present, so it sits at the end
        // where the eye finishes rather than moving about with the page buttons.
        header.addView(navButton("✕", true, v -> {
            if (onDismiss != null) {
                onDismiss.run();
            }
        }));
        return header;
    }

    /**
     * A round-cornered control for the panel's header. Disabled ones are drawn faintly rather than
     * removed, so the header keeps its shape as the pages turn and nothing shifts under a thumb
     * that is already on its way down.
     */
    private View navButton(String glyph, boolean enabled, OnClickListener onClick) {
        TextView button = new TextView(getContext());
        button.setText(glyph);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        button.setTextColor(enabled ? palette.keyText : palette.keyTextMuted);
        Compat.setBackground(button, roundedKeyFace(enabled));
        button.setMinWidth(dp(40));
        button.setMinHeight(dp(34));
        if (enabled) {
            button.setOnClickListener(onClick);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(3), dp(5), dp(3), dp(5));
        button.setLayoutParams(params);
        return button;
    }

    private android.graphics.drawable.Drawable roundedKeyFace(boolean enabled) {
        android.graphics.drawable.GradientDrawable face =
            new android.graphics.drawable.GradientDrawable();
        face.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        face.setCornerRadius(dp(6));
        face.setColor(enabled ? palette.keyFace : palette.keyDisabled);
        return face;
    }

    private LinearLayout buildCell(Item item, int number) {
        LinearLayout cell = new LinearLayout(getContext());
        cell.setOrientation(HORIZONTAL);
        cell.setGravity(Gravity.CENTER_VERTICAL);
        cell.setBackgroundColor(palette.keyFace);
        cell.setPadding(dp(6), dp(5), dp(6), dp(5));

        TextView index = new TextView(getContext());
        index.setText(String.valueOf(number));
        index.setTextColor(palette.keyAccent);
        index.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        index.setPadding(0, 0, dp(4), 0);
        cell.addView(index);

        boolean hasGloss = item.gloss != null && !item.gloss.isEmpty();
        boolean longValue = item.value != null
            && item.value.codePointCount(0, item.value.length()) > 2;
        TextView value = new TextView(getContext());
        value.setText(item.value);
        value.setTextColor(palette.keyText);
        value.setTextSize(TypedValue.COMPLEX_UNIT_SP, longValue ? 13 : 22);
        value.setMaxLines(2);
        value.setEllipsize(TextUtils.TruncateAt.END);
        if (!hasGloss || longValue) {
            cell.addView(value, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        } else {
            cell.addView(value);
        }

        if (hasGloss) {
            // The gloss reads alongside the character, not under it, and takes whatever width is
            // left; when that is not enough it wraps onto a second line rather than being cut.
            TextView gloss = new TextView(getContext());
            gloss.setText(item.gloss);
            gloss.setTextColor(palette.keyTextMuted);
            gloss.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            gloss.setMaxLines(2);
            gloss.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams glossParams = longValue
                ? new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                : new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            glossParams.leftMargin = dp(4);
            cell.addView(gloss, glossParams);
        }

        final String chosen = item.value;
        cell.setOnClickListener(v -> {
            if (listener != null) {
                listener.pick(chosen);
            }
        });
        return cell;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
