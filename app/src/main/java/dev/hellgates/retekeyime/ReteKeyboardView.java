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

    private static final List<List<SoftwareKeySpec>> ROWS = ScaffoldKeyboardLayout.rows();

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final InputSink sink;

    public ReteKeyboardView(Context context, InputSink sink) {
        super(context);
        this.sink = Objects.requireNonNull(sink, "sink");
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int rowCount = ROWS.size();
        int rowHeight = Math.max(1, height / rowCount);

        canvas.drawColor(Color.rgb(245, 246, 248));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Math.max(18.0f, rowHeight * 0.38f));

        for (int row = 0; row < rowCount; row++) {
            List<SoftwareKeySpec> keys = ROWS.get(row);
            int top = row * height / rowCount;
            int bottom = (row + 1) * height / rowCount;
            for (int col = 0; col < keys.size(); col++) {
                int left = col * width / keys.size();
                int right = (col + 1) * width / keys.size();
                paint.setColor(Color.rgb(221, 225, 231));
                canvas.drawRect(left + 2, top + 2, right - 2, bottom - 2, paint);
                paint.setColor(Color.rgb(22, 27, 34));
                canvas.drawText(
                    keys.get(col).label(),
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

        SoftwareKeySpec key = keyAt(event.getX(), event.getY());
        if (key != null && key.enabled()) {
            sink.accept(key.pressEvent());
            performClick();
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private SoftwareKeySpec keyAt(float x, float y) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0 || x < 0.0f || y < 0.0f || x >= width || y >= height) {
            return null;
        }
        int rowIndex = Math.min(ROWS.size() - 1, (int) (y * ROWS.size() / height));
        List<SoftwareKeySpec> row = ROWS.get(rowIndex);
        int colIndex = Math.min(row.size() - 1, (int) (x * row.size() / width));
        return row.get(colIndex);
    }
}
