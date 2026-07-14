package dev.hellgates.retekeyime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import java.util.List;
import java.util.Objects;

@SuppressLint("ViewConstructor")
public final class ReteKeyboardView extends View {
    public interface InputSink {
        void accept(ProjectKeyEvent event);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final InputSink sink;
    private final ShiftLayerState shiftLayer = new ShiftLayerState();
    private final Runnable openPopup = this::openPopupForHeldKey;
    private KeyboardLayoutId layoutId = KeyboardLayoutId.KO_DUBEOLSIK;
    private int heldRow = -1;
    private int heldKey = -1;
    private LongPressPopup popup;
    private int popupIndex = -1;

    public ReteKeyboardView(Context context, InputSink sink) {
        super(context);
        this.sink = Objects.requireNonNull(sink, "sink");
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);
    }

    /** The layout currently drawn and hit-tested, including the active shift layer. */
    public KeyboardLayout layout() {
        return KeyboardLayouts.of(layoutId, shiftLayer.isActive());
    }

    /** Clears one-shot, popup, and pointer state when the editor session changes. */
    public void resetLayerState() {
        shiftLayer.clear();
        cancelHold();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        KeyboardLayout layout = layout();
        int width = getWidth();
        int height = getHeight();
        List<List<SoftwareKeySpec>> rows = layout.rows();
        int rowHeight = Math.max(1, height / rows.size());

        canvas.drawColor(Color.rgb(245, 246, 248));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Math.max(18.0f, rowHeight * 0.38f));

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<SoftwareKeySpec> keys = rows.get(rowIndex);
            int top = layout.rowEdge(rowIndex, height);
            int bottom = layout.rowEdge(rowIndex + 1, height);
            for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
                SoftwareKeySpec key = keys.get(keyIndex);
                int startColumn = layout.startColumn(rowIndex, keyIndex);
                int left = layout.columnEdge(startColumn, width);
                int right = layout.columnEdge(startColumn + key.columnSpan(), width);
                paint.setColor(keyFillColor(key));
                canvas.drawRect(left + 2, top + 2, right - 2, bottom - 2, paint);
                paint.setColor(key.enabled() || key.isControl()
                    ? Color.rgb(22, 27, 34)
                    : Color.rgb(139, 148, 158));
                canvas.drawText(
                    key.label(),
                    (left + right) * 0.5f,
                    top + (bottom - top) * 0.62f,
                    paint
                );
                if (key.hasLongPress()) {
                    // A dot marks the keys that hide alternates, so holding is discoverable.
                    paint.setColor(Color.rgb(139, 148, 158));
                    canvas.drawCircle(right - 10.0f, top + 10.0f, 3.0f, paint);
                }
            }
        }

        drawPopup(canvas);
    }

    private void drawPopup(Canvas canvas) {
        if (popup == null) {
            return;
        }
        for (int index = 0; index < popup.candidateCount(); index++) {
            int left = popup.cellLeft(index);
            int right = left + popup.cellWidth();
            paint.setColor(index == popupIndex
                ? Color.rgb(159, 190, 233)
                : Color.rgb(255, 255, 255));
            canvas.drawRect(left + 2, popup.top() + 2, right - 2, popup.bottom() - 2, paint);
            paint.setColor(Color.rgb(22, 27, 34));
            canvas.drawText(
                popup.candidate(index),
                (left + right) * 0.5f,
                popup.top() + (popup.bottom() - popup.top()) * 0.62f,
                paint
            );
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                beginHold(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_MOVE:
                trackHold(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
                releaseHold(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_CANCEL:
                cancelHold();
                invalidate();
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void beginHold(float x, float y) {
        cancelHold();
        KeyboardLayout layout = layout();
        int rowIndex = rowAt(layout, y);
        int keyIndex = keyIndexAt(layout, rowIndex, x);
        if (rowIndex < 0 || keyIndex < 0) {
            return;
        }
        heldRow = rowIndex;
        heldKey = keyIndex;
        if (layout.rows().get(rowIndex).get(keyIndex).hasLongPress()) {
            postDelayed(openPopup, ViewConfiguration.getLongPressTimeout());
        }
    }

    private void trackHold(float x, float y) {
        if (popup == null) {
            return;
        }
        int index = popup.indexAt(x, y);
        if (index != popupIndex) {
            popupIndex = index;
            invalidate();
        }
    }

    private void releaseHold(float x, float y) {
        LongPressPopup openedPopup = popup;
        int rowIndex = heldRow;
        int keyIndex = heldKey;
        removeCallbacks(openPopup);

        if (openedPopup != null) {
            int index = openedPopup.indexAt(x, y);
            // Releasing outside the popup cancels the choice rather than committing the base key:
            // the user already saw the alternates and moved away from them.
            if (index >= 0) {
                sink.accept(openedPopup.key().longPressEvent(index));
                consumeOneShotShift();
                performClick();
            }
            cancelHold();
            invalidate();
            return;
        }

        cancelHold();
        if (rowIndex < 0 || keyIndex < 0) {
            return;
        }
        KeyboardLayout layout = layout();
        // A tap counts for the key it started on, so a small slide inside one key still types it.
        SoftwareKeySpec key = layout.keyAt(x, y, getWidth(), getHeight());
        SoftwareKeySpec held = layout.rows().get(rowIndex).get(keyIndex);
        if (key != held) {
            return;
        }
        if (held.isControl()) {
            applyControl(held.control());
            performClick();
            return;
        }
        if (held.enabled()) {
            sink.accept(held.pressEvent());
            consumeOneShotShift();
            performClick();
        }
    }

    private void openPopupForHeldKey() {
        if (heldRow < 0 || heldKey < 0) {
            return;
        }
        popup = LongPressPopup.open(layout(), heldRow, heldKey, getWidth(), getHeight());
        popupIndex = -1;
        invalidate();
    }

    private void cancelHold() {
        removeCallbacks(openPopup);
        heldRow = -1;
        heldKey = -1;
        popup = null;
        popupIndex = -1;
    }

    private void consumeOneShotShift() {
        if (shiftLayer.consumeOneShot()) {
            invalidate();
        }
    }

    private int rowAt(KeyboardLayout layout, float y) {
        int height = getHeight();
        if (height <= 0 || y < 0.0f || y >= height) {
            return -1;
        }
        int rows = layout.rows().size();
        return Math.min(rows - 1, (int) (y * rows / height));
    }

    private int keyIndexAt(KeyboardLayout layout, int rowIndex, float x) {
        int width = getWidth();
        if (rowIndex < 0 || width <= 0 || x < 0.0f || x >= width) {
            return -1;
        }
        int column = Math.min(layout.columns() - 1, (int) (x * layout.columns() / width));
        List<SoftwareKeySpec> keys = layout.rows().get(rowIndex);
        int cursor = 0;
        for (int index = 0; index < keys.size(); index++) {
            cursor += keys.get(index).columnSpan();
            if (column < cursor) {
                return index;
            }
        }
        return keys.size() - 1;
    }

    private void applyControl(ControlKey control) {
        if (control == ControlKey.SHIFT) {
            shiftLayer.advance();
        } else if (control == ControlKey.LAYOUT_TOGGLE) {
            layoutId = KeyboardLayouts.other(layoutId);
            shiftLayer.clear();
        }
        invalidate();
    }

    private int keyFillColor(SoftwareKeySpec key) {
        if (key.isControl() && key.control() == ControlKey.SHIFT) {
            if (shiftLayer.isSticky()) {
                return Color.rgb(159, 190, 233);
            }
            if (shiftLayer.isActive()) {
                return Color.rgb(196, 214, 240);
            }
        }
        if (!key.enabled() && !key.isControl()) {
            return Color.rgb(233, 236, 240);
        }
        return Color.rgb(221, 225, 231);
    }
}
