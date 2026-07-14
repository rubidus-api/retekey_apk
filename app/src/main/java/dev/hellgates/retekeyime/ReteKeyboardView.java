package dev.hellgates.retekeyime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
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
    private KeyboardLayoutId layoutId = KeyboardLayoutId.KO_DUBEOLSIK;

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

    /** Clears one-shot and sticky view state when the editor session changes. */
    public void resetLayerState() {
        shiftLayer.clear();
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
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) {
            return true;
        }

        SoftwareKeySpec key = layout().keyAt(
            event.getX(),
            event.getY(),
            getWidth(),
            getHeight()
        );
        if (key == null) {
            return true;
        }
        if (key.isControl()) {
            applyControl(key.control());
            performClick();
            return true;
        }
        if (key.enabled()) {
            sink.accept(key.pressEvent());
            if (shiftLayer.consumeOneShot()) {
                invalidate();
            }
            performClick();
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
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
