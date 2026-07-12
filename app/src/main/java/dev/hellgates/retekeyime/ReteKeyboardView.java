package dev.hellgates.retekeyime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

public final class ReteKeyboardView extends View {
    public interface ActionSink {
        void accept(KeyAction action);
    }

    private static final String[] ROWS = {
        "ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ",
        "ㅁㄴㅇㄹㅎㅗㅓㅏㅣ",
        "ㅋㅌㅊㅍㅠㅜㅡ⌫",
        "CTRL ALT 한/영 ← ↓ ↑ →"
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ReteInputEngine engine = new ReteInputEngine();
    private final ActionSink sink;

    public ReteKeyboardView(Context context, ActionSink sink) {
        super(context);
        this.sink = sink;
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int rowHeight = Math.max(1, height / ROWS.length);

        canvas.drawColor(Color.rgb(245, 246, 248));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Math.max(18.0f, rowHeight * 0.38f));

        for (int row = 0; row < ROWS.length; row++) {
            String[] labels = ROWS[row].split(" ");
            if (labels.length == 1) {
                labels = splitGlyphs(ROWS[row]);
            }
            int keyWidth = Math.max(1, width / labels.length);
            for (int col = 0; col < labels.length; col++) {
                int left = col * keyWidth;
                int top = row * rowHeight;
                int right = col == labels.length - 1 ? width : left + keyWidth;
                int bottom = top + rowHeight;
                paint.setColor(Color.rgb(221, 225, 231));
                canvas.drawRect(left + 2, top + 2, right - 2, bottom - 2, paint);
                paint.setColor(Color.rgb(22, 27, 34));
                canvas.drawText(labels[col], (left + right) * 0.5f, top + rowHeight * 0.62f, paint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) {
            return true;
        }

        String label = labelAt(event.getX(), event.getY());
        sink.accept(engine.onSoftKey(label));
        return true;
    }

    private String labelAt(float x, float y) {
        int row = Math.min(ROWS.length - 1, Math.max(0, (int) (y / Math.max(1, getHeight() / ROWS.length))));
        String[] labels = ROWS[row].split(" ");
        if (labels.length == 1) {
            labels = splitGlyphs(ROWS[row]);
        }
        int col = Math.min(labels.length - 1, Math.max(0, (int) (x / Math.max(1, getWidth() / labels.length))));
        return labels[col];
    }

    private static String[] splitGlyphs(String text) {
        String[] out = new String[text.length()];
        for (int i = 0; i < text.length(); i++) {
            out[i] = String.valueOf(text.charAt(i));
        }
        return out;
    }
}
