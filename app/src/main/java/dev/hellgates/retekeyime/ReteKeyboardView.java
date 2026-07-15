package dev.hellgates.retekeyime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@SuppressLint("ViewConstructor")
public final class ReteKeyboardView extends View {
    public interface InputSink {
        void accept(ProjectKeyEvent event);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final InputSink sink;
    private final ShiftLayerState shiftLayer = new ShiftLayerState();
    private final Set<ControlKey> armedModifiers = EnumSet.noneOf(ControlKey.class);
    private final Runnable onHoldElapsed = this::handleLongPress;
    private enum Page { LETTERS, SPECIAL_CHARS, SPECIAL_KEYS }

    private KeyboardLayoutId letterLayoutId = KeyboardLayoutId.KO_DUBEOLSIK;
    private Page page = Page.LETTERS;
    private NumpadMode numpadMode = NumpadMode.NUMBERS;
    private int heldRow = -1;
    private int heldKey = -1;
    private LongPressPopup popup;
    private int popupIndex = -1;
    private boolean holdConsumed;

    public ReteKeyboardView(Context context, InputSink sink) {
        super(context);
        this.sink = Objects.requireNonNull(sink, "sink");
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);
    }

    /** The layout currently drawn and hit-tested, including layer, shift, and keypad mode. */
    public KeyboardLayout layout() {
        switch (page) {
            case SPECIAL_CHARS:
                return KeyboardLayouts.specialChars();
            case SPECIAL_KEYS:
                return KeyboardLayouts.specialKeys(numpadMode);
            default:
                return KeyboardLayouts.of(letterLayoutId, shiftLayer.isActive());
        }
    }

    /** Clears transient one-shot and pointer state when the editor session changes. */
    public void resetLayerState() {
        shiftLayer.clear();
        armedModifiers.clear();
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
        paint.setTextSize(Math.max(18.0f, rowHeight * 0.34f));

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
                if (key.hasLongPress() || key.hasLongPressControl()) {
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
        SoftwareKeySpec key = layout.rows().get(rowIndex).get(keyIndex);
        // Shift, and any key with a long press, react to a hold.
        if (key.hasLongPress()
            || key.hasLongPressControl()
            || (key.isControl() && key.control() == ControlKey.SHIFT)) {
            postDelayed(onHoldElapsed, ViewConfiguration.getLongPressTimeout());
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
        boolean consumed = holdConsumed;
        removeCallbacks(onHoldElapsed);

        if (openedPopup != null) {
            int index = openedPopup.indexAt(x, y);
            // Releasing outside the popup cancels the choice rather than committing the base key.
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
        if (consumed) {
            // A hold (shift lock, or a layer switch) already acted; the tap must not also fire.
            return;
        }
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
            sink.accept(pressEventWithModifiers(held));
            consumeOneShotShift();
            performClick();
        }
    }

    /** Folds the armed Ctrl/Meta/Alt into a raw key so it forms a chord; other keys are unchanged. */
    private ProjectKeyEvent pressEventWithModifiers(SoftwareKeySpec key) {
        SemanticInput input = key.semanticInput();
        if (input.kind() != SemanticInput.Kind.RAW_KEY || armedModifiers.isEmpty()) {
            return key.pressEvent();
        }
        Set<KeyModifier> mods = EnumSet.noneOf(KeyModifier.class);
        for (ControlKey armed : armedModifiers) {
            switch (armed) {
                case CTRL:
                    mods.add(KeyModifier.CTRL);
                    break;
                case ALT:
                    mods.add(KeyModifier.ALT);
                    break;
                case META:
                    mods.add(KeyModifier.META);
                    break;
                default:
                    break;
            }
        }
        if (mods.isEmpty()) {
            return key.pressEvent();
        }
        return ProjectKeyEvent.softwareDown(key.stableKeyId(), input.withModifiers(mods));
    }

    private void handleLongPress() {
        if (heldRow < 0 || heldKey < 0) {
            return;
        }
        SoftwareKeySpec key = layout().rows().get(heldRow).get(heldKey);
        if (key.isControl() && key.control() == ControlKey.SHIFT) {
            shiftLayer.toggleLock();
            holdConsumed = true;
            invalidate();
            return;
        }
        if (key.hasLongPressControl()) {
            applyControl(key.longPressControl());
            holdConsumed = true;
            return;
        }
        popup = LongPressPopup.open(layout(), heldRow, heldKey, getWidth(), getHeight());
        popupIndex = -1;
        invalidate();
    }

    private void cancelHold() {
        removeCallbacks(onHoldElapsed);
        heldRow = -1;
        heldKey = -1;
        popup = null;
        popupIndex = -1;
        holdConsumed = false;
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
        switch (control) {
            case SHIFT:
                shiftLayer.tap();
                break;
            case LAYOUT_TOGGLE:
                letterLayoutId = KeyboardLayouts.otherLetters(letterLayoutId);
                page = Page.LETTERS;
                shiftLayer.clear();
                break;
            case SPECIAL_CHARS_LAYER:
                page = Page.SPECIAL_CHARS;
                shiftLayer.clear();
                break;
            case SPECIAL_KEYS_LAYER:
                page = Page.SPECIAL_KEYS;
                numpadMode = NumpadMode.NUMBERS;
                shiftLayer.clear();
                break;
            case PREVIOUS_LAYER:
                page = Page.LETTERS;
                shiftLayer.clear();
                break;
            case NUMLOCK:
                numpadMode = numpadMode == NumpadMode.ARROWS
                    ? NumpadMode.NUMBERS
                    : NumpadMode.ARROWS;
                break;
            case FUNCTION_LOCK:
                numpadMode = numpadMode == NumpadMode.FUNCTIONS
                    ? NumpadMode.NUMBERS
                    : NumpadMode.FUNCTIONS;
                break;
            case CTRL:
            case META:
            case ALT:
            case TAB:
                // Latch the modifier. Its armed state is view-local until the raw-key action lands.
                if (!armedModifiers.remove(control)) {
                    armedModifiers.add(control);
                }
                break;
            default:
                break;
        }
        invalidate();
    }

    private int keyFillColor(SoftwareKeySpec key) {
        if (key.isControl()) {
            ControlKey control = key.control();
            if (control == ControlKey.SHIFT) {
                if (shiftLayer.isLocked()) {
                    return Color.rgb(159, 190, 233);
                }
                if (shiftLayer.isActive()) {
                    return Color.rgb(196, 214, 240);
                }
            }
            if (control == ControlKey.NUMLOCK && numpadMode == NumpadMode.ARROWS) {
                return Color.rgb(159, 190, 233);
            }
            if (control == ControlKey.FUNCTION_LOCK && numpadMode == NumpadMode.FUNCTIONS) {
                return Color.rgb(159, 190, 233);
            }
            if (armedModifiers.contains(control)) {
                return Color.rgb(159, 190, 233);
            }
        }
        if (!key.enabled() && !key.isControl()) {
            return Color.rgb(233, 236, 240);
        }
        return Color.rgb(221, 225, 231);
    }
}
