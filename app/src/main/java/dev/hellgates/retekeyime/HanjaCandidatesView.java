package dev.hellgates.retekeyime;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * The Hanja candidate panel shown in the IME candidates area. It pages through candidates in a
 * grid, shows each candidate's 훈음 (gloss) when known, and serves both directions: 한글 → 한자
 * (value = Hanja, gloss = meaning) and 한자 → 한글 (value = reading, no gloss). It renders in the
 * candidates area, so it appears with the soft keyboard and with a hardware keyboard alike.
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
    private String reading = "";
    private List<Item> items = new ArrayList<>();
    private int page;

    public HanjaCandidatesView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setBackgroundColor(Color.rgb(28, 30, 36));
    }

    public void setOnPick(OnPick listener) {
        this.listener = listener;
    }

    /** Shows candidates for {@code reading}, starting at the first page. */
    public void show(String reading, List<Item> items) {
        this.reading = reading == null ? "" : reading;
        this.items = items == null ? new ArrayList<>() : items;
        this.page = 0;
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
                addView(row);
            }
        }
    }

    private LinearLayout buildHeader() {
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(getContext());
        label.setText(reading + " ▸");
        label.setTextColor(Color.rgb(150, 160, 172));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        label.setPadding(dp(12), dp(6), dp(8), dp(6));
        header.addView(label, new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (pageCount() > 1) {
            Button prev = navButton("◀", page > 0);
            prev.setOnClickListener(v -> {
                if (page > 0) {
                    page--;
                    render();
                }
            });
            header.addView(prev);

            TextView pageLabel = new TextView(getContext());
            pageLabel.setText((page + 1) + "/" + pageCount());
            pageLabel.setTextColor(Color.rgb(180, 188, 198));
            pageLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            pageLabel.setPadding(dp(6), 0, dp(6), 0);
            header.addView(pageLabel);

            Button next = navButton("▶", page < pageCount() - 1);
            next.setOnClickListener(v -> {
                if (page < pageCount() - 1) {
                    page++;
                    render();
                }
            });
            header.addView(next);
        }
        return header;
    }

    private Button navButton(String glyph, boolean enabled) {
        Button button = new Button(getContext());
        button.setText(glyph);
        button.setAllCaps(false);
        button.setTextColor(enabled ? Color.rgb(233, 237, 243) : Color.rgb(90, 96, 104));
        button.setBackgroundColor(Color.rgb(40, 44, 52));
        button.setMinWidth(dp(44));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(2), dp(4), dp(2), dp(4));
        button.setLayoutParams(params);
        return button;
    }

    private LinearLayout buildCell(Item item, int number) {
        LinearLayout cell = new LinearLayout(getContext());
        cell.setOrientation(VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setBackgroundColor(Color.rgb(40, 44, 52));
        cell.setPadding(dp(6), dp(6), dp(6), dp(6));

        LinearLayout top = new LinearLayout(getContext());
        top.setOrientation(HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView index = new TextView(getContext());
        index.setText(String.valueOf(number));
        index.setTextColor(Color.rgb(120, 170, 235));
        index.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        index.setPadding(0, 0, dp(5), 0);
        top.addView(index);
        TextView value = new TextView(getContext());
        value.setText(item.value);
        value.setTextColor(Color.rgb(233, 237, 243));
        value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        value.setGravity(Gravity.CENTER);
        top.addView(value);
        cell.addView(top);

        if (item.gloss != null && !item.gloss.isEmpty()) {
            TextView gloss = new TextView(getContext());
            gloss.setText(item.gloss);
            gloss.setTextColor(Color.rgb(150, 160, 172));
            gloss.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            gloss.setGravity(Gravity.CENTER);
            gloss.setMaxLines(1);
            gloss.setEllipsize(TextUtils.TruncateAt.END);
            cell.addView(gloss);
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
