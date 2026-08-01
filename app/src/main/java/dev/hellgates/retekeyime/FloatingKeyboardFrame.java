package dev.hellgates.retekeyime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * The floating half-screen keyboard: a translucent panel that the user drags around one half of the
 * screen, resizes, sends to the other half, or closes.
 *
 * <p>The frame itself fills the whole IME window and draws nothing outside the panel, so the
 * service can hand the window manager a touchable region that is exactly the panel — everything
 * else keeps reaching the app underneath. Geometry rules (half-screen width cap, staying inside
 * one half, the mirror move) live in {@link FloatingKeyboardBounds}; this class only turns touches
 * into calls on it.
 *
 * <p>The title bar carries, left to right: the move handle, the cross-over key that reads
 * {@code >} on the left half and {@code <} on the right, then at the far end the close key and the
 * resize handle.
 */
public final class FloatingKeyboardFrame extends ViewGroup {
    /** Notified when the user closes the floating keyboard with the bar's ✕ key. */
    public interface OnClose {
        void close();
    }

    /** Notified whenever the panel moves or resizes, so the host can persist and re-inset. */
    public interface OnBoundsChanged {
        void changed(FloatingKeyboardBounds bounds);
    }

    private static final int BAR_HEIGHT_DP = 34;
    private static final int CELL_WIDTH_DP = 44;
    private static final int CORNER_DP = 10;
    /** How opaque the panel is; low enough to see the app underneath, high enough to read keys. */
    private int panelAlpha =
        FloatingKeyboardSettings.alphaOf(FloatingKeyboardSettings.DEFAULT_OPACITY_PERCENT);

    private final ReteKeyboardView keyboardView;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF panelRect = new RectF();
    private final Rect moveCell = new Rect();
    private final Rect crossCell = new Rect();
    private final Rect closeCell = new Rect();
    private final Rect resizeCell = new Rect();

    private KeyboardPalette palette;
    private FloatingKeyboardBounds bounds;
    private OnClose onClose;
    private OnBoundsChanged onBoundsChanged;

    private int dragMode = DRAG_NONE;
    private static final int DRAG_NONE = 0;
    private static final int DRAG_MOVE = 1;
    private static final int DRAG_RESIZE = 2;
    private float dragAnchorX;
    private float dragAnchorY;

    public FloatingKeyboardFrame(Context context, ReteKeyboardView keyboardView) {
        super(context);
        this.keyboardView = keyboardView;
        this.palette = KeyboardPalette.resolve(context);
        setWillNotDraw(false);
        // The keyboard paints its own opaque background, so the translucency has to be applied to
        // the whole child rather than to the panel behind it.
        keyboardView.setAlpha(panelAlpha / 255.0f);
        addView(keyboardView);
    }

    public void setOnClose(OnClose listener) {
        this.onClose = listener;
    }

    public void setOnBoundsChanged(OnBoundsChanged listener) {
        this.onBoundsChanged = listener;
    }

    /** Places the panel. The frame re-lays out and reports the corrected bounds back. */
    public void setBounds(FloatingKeyboardBounds bounds) {
        if (bounds == null) {
            throw new IllegalArgumentException("bounds must not be null");
        }
        this.bounds = bounds;
        requestLayout();
        invalidate();
    }

    public FloatingKeyboardBounds bounds() {
        return bounds;
    }

    /** The panel's rectangle in this frame's coordinates: the only part that takes touches. */
    public Rect panelBounds() {
        if (bounds == null) {
            return new Rect();
        }
        return new Rect(bounds.left(), bounds.top(), bounds.right(), bounds.bottom());
    }

    public int barHeightPx() {
        return dp(BAR_HEIGHT_DP);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED
            ? getResources().getDisplayMetrics().heightPixels
            : MeasureSpec.getSize(heightMeasureSpec);
        if (height <= 0) {
            height = getResources().getDisplayMetrics().heightPixels;
        }
        ensureBounds(width, height);
        int keyboardHeight = Math.max(0, bounds.height() - barHeightPx());
        keyboardView.measure(
            MeasureSpec.makeMeasureSpec(bounds.width(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(keyboardHeight, MeasureSpec.EXACTLY)
        );
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        ensureBounds(r - l, b - t);
        int top = bounds.top() + barHeightPx();
        keyboardView.layout(bounds.left(), top, bounds.right(), bounds.bottom());
        layoutBarCells();
    }

    /** Creates or rescales the panel when the frame's own size is first known or changes. */
    private void ensureBounds(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (bounds == null) {
            bounds = FloatingKeyboardBounds.initial(
                width, height, FloatingKeyboardBounds.Side.RIGHT);
            notifyBounds();
        } else if (bounds.screenWidth() != width || bounds.screenHeight() != height) {
            bounds = bounds.onScreen(width, height);
            notifyBounds();
        }
    }

    private void layoutBarCells() {
        // Four cells share the bar. On a narrow panel they must shrink rather than overlap, or the
        // cross-over key and the close key answer the same touch.
        int cell = Math.max(1, Math.min(dp(CELL_WIDTH_DP), bounds.width() / 4));
        int barBottom = bounds.top() + barHeightPx();
        moveCell.set(bounds.left(), bounds.top(), bounds.left() + cell, barBottom);
        crossCell.set(moveCell.right, bounds.top(), moveCell.right + cell, barBottom);
        resizeCell.set(bounds.right() - cell, bounds.top(), bounds.right(), barBottom);
        closeCell.set(resizeCell.left - cell, bounds.top(), resizeCell.left, barBottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bounds == null) {
            return;
        }
        float corner = dp(CORNER_DP);
        panelRect.set(bounds.left(), bounds.top(), bounds.right(), bounds.bottom());
        fill.setColor(withAlpha(palette.background, panelAlpha));
        canvas.drawRoundRect(panelRect, corner, corner, fill);

        // The bar is a shade brighter than the panel so the handles read as controls, not as keys.
        fill.setColor(withAlpha(palette.keyFace, panelAlpha));
        canvas.drawRoundRect(
            new RectF(bounds.left(), bounds.top(), bounds.right(), bounds.top() + barHeightPx()),
            corner,
            corner,
            fill
        );

        text.setColor(palette.keyText);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(dp(15));
        drawGlyph(canvas, moveCell, "☰");
        drawGlyph(canvas, crossCell, bounds.isLeft() ? "›" : "‹");
        drawGlyph(canvas, closeCell, "✕");
        drawGlyph(canvas, resizeCell, "⇲");
    }

    private void drawGlyph(Canvas canvas, Rect cell, String glyph) {
        float baseline = cell.centerY() - (text.descent() + text.ascent()) / 2.0f;
        canvas.drawText(glyph, cell.centerX(), baseline, text);
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // Everything in the title bar belongs to the frame; the keyboard below keeps its own
        // touches, including a drag that started on a key.
        return bounds != null
            && event.getActionMasked() == MotionEvent.ACTION_DOWN
            && isInBar(event.getX(), event.getY());
    }

    private boolean isInBar(float x, float y) {
        return x >= bounds.left() && x < bounds.right()
            && y >= bounds.top() && y < bounds.top() + barHeightPx();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (bounds == null) {
            return false;
        }
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!isInBar(x, y)) {
                    return false;
                }
                if (contains(closeCell, x, y)) {
                    if (onClose != null) {
                        onClose.close();
                    }
                    return true;
                }
                if (contains(crossCell, x, y)) {
                    setBounds(bounds.mirrored());
                    notifyBounds();
                    return true;
                }
                dragMode = contains(resizeCell, x, y) ? DRAG_RESIZE
                    : contains(moveCell, x, y) ? DRAG_MOVE
                    : DRAG_NONE;
                dragAnchorX = x;
                dragAnchorY = y;
                return dragMode != DRAG_NONE;
            case MotionEvent.ACTION_MOVE:
                if (dragMode == DRAG_NONE) {
                    return false;
                }
                applyDrag(Math.round(x - dragAnchorX), Math.round(y - dragAnchorY));
                dragAnchorX = x;
                dragAnchorY = y;
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragMode == DRAG_NONE) {
                    return false;
                }
                dragMode = DRAG_NONE;
                notifyBounds();
                return true;
            default:
                return false;
        }
    }

    private void applyDrag(int dx, int dy) {
        FloatingKeyboardBounds updated = dragMode == DRAG_RESIZE
            ? bounds.resizedBy(dx, dy)
            : bounds.movedBy(dx, dy);
        if (updated.left() == bounds.left() && updated.top() == bounds.top()
            && updated.width() == bounds.width() && updated.height() == bounds.height()) {
            return;
        }
        setBounds(updated);
    }

    private void notifyBounds() {
        if (onBoundsChanged != null && bounds != null) {
            onBoundsChanged.changed(bounds);
        }
    }

    private static boolean contains(Rect cell, float x, float y) {
        return x >= cell.left && x < cell.right && y >= cell.top && y < cell.bottom;
    }

    /** Sets how solid the panel is, from the user's opacity setting. */
    public void setOpacityPercent(int percent) {
        int alpha = FloatingKeyboardSettings.alphaOf(percent);
        if (alpha == panelAlpha) {
            return;
        }
        panelAlpha = alpha;
        keyboardView.setAlpha(panelAlpha / 255.0f);
        invalidate();
    }

    /** Re-reads the theme so the panel follows a light/dark switch like the keyboard does. */
    public void refreshTheme() {
        palette = KeyboardPalette.resolve(getContext());
        invalidate();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    /** The keyboard this frame carries, so the host can keep wiring its callbacks as before. */
    public View keyboard() {
        return keyboardView;
    }
}
