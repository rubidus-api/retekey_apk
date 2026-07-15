package dev.hellgates.retekeyime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntConsumer;

@SuppressLint("ViewConstructor")
public final class ReteKeyboardView extends View {
    public interface InputSink {
        void accept(ProjectKeyEvent event);
    }

    private static final String PREFS = "retekey_view";
    private static final String KEY_HEIGHT_SCALE = "height_scale";

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final InputSink sink;
    private final ShiftLayerState shiftLayer = new ShiftLayerState();
    private final Set<ControlKey> armedModifiers = EnumSet.noneOf(ControlKey.class);
    private final Runnable onHoldElapsed = this::handleLongPress;
    private enum Page { LETTERS, SPECIAL_CHARS, SPECIAL_KEYS, MENU }

    /** One height step applied by the menu's 높이 −/＋ tiles. */
    private static final float HEIGHT_STEP = 0.1f;

    private KeyboardLayoutId letterLayoutId = KeyboardLayoutId.KO_DUBEOLSIK;
    private Page page = Page.LETTERS;
    private NumpadMode numpadMode = NumpadMode.NUMBERS;
    private int heldRow = -1;
    private int heldKey = -1;
    private LongPressPopup popup;
    private int popupIndex = -1;
    private boolean holdConsumed;

    /** Invoked when the 설정 tile is tapped; the host service opens the settings screen. */
    private Runnable onOpenSettings;
    /** Invoked with an editor context-menu id (copy/paste/undo) for the host to perform. */
    private IntConsumer onEditCommand;
    /** Invoked when the 날짜 tile is tapped; the host inserts the current date and time. */
    private Runnable onInsertDate;
    /** Invoked when the 키보드전환 tile is tapped; the host opens the input-method picker. */
    private Runnable onSwitchIme;
    /** User-adjustable multiplier on the base keyboard height, persisted across sessions. */
    private float heightScale = KeyboardHeightScale.DEFAULT_SCALE;
    // Two-finger vertical drag resizes the keyboard; these track the gesture in progress.
    private boolean resizing;
    private float resizeStartMidY;
    private int resizeStartHeight;
    private int resizeBaseHeight;

    public ReteKeyboardView(Context context, InputSink sink) {
        super(context);
        this.sink = Objects.requireNonNull(sink, "sink");
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);
        heightScale = KeyboardHeightScale.clamp(
            prefs().getFloat(KEY_HEIGHT_SCALE, KeyboardHeightScale.DEFAULT_SCALE));
    }

    /** Sets the handler the 설정 tile runs to open settings; the service owns the launch. */
    public void setOnOpenSettings(Runnable handler) {
        this.onOpenSettings = handler;
    }

    /** Sets the handler for editor context-menu commands (copy/paste/undo) from menu tiles. */
    public void setOnEditCommand(IntConsumer handler) {
        this.onEditCommand = handler;
    }

    /** Sets the handler the 날짜 tile runs to insert the current date and time. */
    public void setOnInsertDate(Runnable handler) {
        this.onInsertDate = handler;
    }

    /** Sets the handler the 키보드전환 tile runs to open the input-method picker. */
    public void setOnSwitchIme(Runnable handler) {
        this.onSwitchIme = handler;
    }

    private SharedPreferences prefs() {
        return getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** The base (scale-1.0) keyboard height in pixels for the current rows and display density. */
    private int baseHeightPx() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return KeyboardHeightScale.baseHeightPx(layout().rows().size(), metrics.density);
    }

    /** The current height multiplier; 1.0 is the default. Exposed for a future settings screen. */
    public float keyboardHeightScale() {
        return heightScale;
    }

    /** Sets the height multiplier, clamps it, optionally persists it, and re-lays out. */
    public void setKeyboardHeightScale(float scale, boolean persist) {
        float clamped = KeyboardHeightScale.clamp(scale);
        if (clamped == heightScale && !persist) {
            return;
        }
        heightScale = clamped;
        if (persist) {
            prefs().edit().putFloat(KEY_HEIGHT_SCALE, heightScale).apply();
        }
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int desired = KeyboardHeightScale.heightForScale(heightScale, baseHeightPx());
        int height;
        switch (MeasureSpec.getMode(heightMeasureSpec)) {
            case MeasureSpec.EXACTLY:
                height = MeasureSpec.getSize(heightMeasureSpec);
                break;
            case MeasureSpec.AT_MOST:
                height = Math.min(desired, MeasureSpec.getSize(heightMeasureSpec));
                break;
            default:
                height = desired;
                break;
        }
        setMeasuredDimension(width, height);
    }

    /** The layout currently drawn and hit-tested, including layer, shift, and keypad mode. */
    public KeyboardLayout layout() {
        switch (page) {
            case SPECIAL_CHARS:
                return KeyboardLayouts.specialChars();
            case SPECIAL_KEYS:
                return KeyboardLayouts.specialKeys(numpadMode);
            case MENU:
                return KeyboardLayouts.menu();
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

        canvas.drawColor(Color.rgb(245, 246, 248));
        paint.setTextAlign(Paint.Align.CENTER);

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
                fitLabel(key.label(), right - left, bottom - top);
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

    /** Sizes {@link #paint} so {@code label} fits a cell of the given size, tracking cell size. */
    private void fitLabel(String label, int cellWidth, int cellHeight) {
        float cap = cellHeight * KeyLabelFit.HEIGHT_RATIO;
        float minSize = 10.0f * getResources().getDisplayMetrics().density;
        paint.setTextSize(cap);
        float measured = paint.measureText(label);
        float size = KeyLabelFit.fitSize(
            measured, cap, cellWidth * KeyLabelFit.WIDTH_RATIO, minSize);
        paint.setTextSize(size);
    }

    private void drawPopup(Canvas canvas) {
        if (popup == null) {
            return;
        }
        int cellHeight = popup.bottom() - popup.top();
        for (int index = 0; index < popup.candidateCount(); index++) {
            int left = popup.cellLeft(index);
            int right = left + popup.cellWidth();
            paint.setColor(index == popupIndex
                ? Color.rgb(159, 190, 233)
                : Color.rgb(255, 255, 255));
            canvas.drawRect(left + 2, popup.top() + 2, right - 2, popup.bottom() - 2, paint);
            paint.setColor(Color.rgb(22, 27, 34));
            fitLabel(popup.candidate(index), popup.cellWidth(), cellHeight);
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
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() >= 2) {
                    beginResize(event);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (resizing) {
                    trackResize(event);
                } else {
                    trackHold(event.getX(), event.getY());
                }
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                // Dropping back below two fingers ends the resize; commit the new height.
                if (resizing && event.getPointerCount() <= 2) {
                    endResize();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (resizing) {
                    endResize();
                } else {
                    releaseHold(event.getX(), event.getY());
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (resizing) {
                    endResize();
                } else {
                    cancelHold();
                    invalidate();
                }
                return true;
            default:
                return true;
        }
    }

    private void beginResize(MotionEvent event) {
        // A second finger means the user is resizing, not typing: drop any pending key press.
        cancelHold();
        resizing = true;
        resizeStartMidY = midY(event);
        resizeStartHeight = getHeight();
        resizeBaseHeight = baseHeightPx();
    }

    private void trackResize(MotionEvent event) {
        if (event.getPointerCount() < 2) {
            return;
        }
        // Dragging the two fingers up (mid-point Y decreasing) grows the bottom-anchored keyboard.
        float delta = resizeStartMidY - midY(event);
        int target = Math.round(resizeStartHeight + delta);
        setKeyboardHeightScale(
            KeyboardHeightScale.scaleForHeight(target, resizeBaseHeight), false);
    }

    private void endResize() {
        resizing = false;
        // Persist whatever scale the drag settled on.
        setKeyboardHeightScale(heightScale, true);
    }

    private static float midY(MotionEvent event) {
        return (event.getY(0) + event.getY(1)) * 0.5f;
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

    private void runEditCommand(int contextMenuId) {
        if (onEditCommand != null) {
            onEditCommand.accept(contextMenuId);
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
            case MENU_LAYER:
                page = Page.MENU;
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
            case OPEN_SETTINGS:
                if (onOpenSettings != null) {
                    onOpenSettings.run();
                }
                break;
            case HEIGHT_UP:
                setKeyboardHeightScale(heightScale + HEIGHT_STEP, true);
                break;
            case HEIGHT_DOWN:
                setKeyboardHeightScale(heightScale - HEIGHT_STEP, true);
                break;
            case COPY:
                runEditCommand(android.R.id.copy);
                break;
            case CUT:
                runEditCommand(android.R.id.cut);
                break;
            case PASTE:
                runEditCommand(android.R.id.paste);
                break;
            case UNDO:
                runEditCommand(android.R.id.undo);
                break;
            case REDO:
                runEditCommand(android.R.id.redo);
                break;
            case SELECT_ALL:
                runEditCommand(android.R.id.selectAll);
                break;
            case INSERT_DATE:
                if (onInsertDate != null) {
                    onInsertDate.run();
                }
                break;
            case SWITCH_IME:
                if (onSwitchIme != null) {
                    onSwitchIme.run();
                }
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
