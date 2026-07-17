package dev.hellgates.retekeyime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
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
    private final KeyFeedback feedback;
    private final ShiftLayerState shiftLayer = new ShiftLayerState();
    private final Set<ControlKey> armedModifiers = EnumSet.noneOf(ControlKey.class);
    private final Runnable onHoldElapsed = this::handleLongPress;
    private final Runnable onRepeatElapsed = this::handleRepeat;
    // Held-key auto-repeat (space, enter, backspace, arrows, letters …), configured in settings.
    private boolean repeatEnabled = KeyRepeatSettings.DEFAULT_ENABLED;
    private int repeatDelayMs = KeyRepeatSettings.DEFAULT_DELAY_MS;
    private int repeatIntervalMs = KeyRepeatSettings.DEFAULT_INTERVAL_MS;
    private boolean repeatFired;
    // Held strongly so the weak listener registration in the preferences survives; it applies
    // settings changes (feedback strengths, height) to a keyboard that is already on screen.
    private final SharedPreferences.OnSharedPreferenceChangeListener prefsListener =
        (changed, key) -> reloadPreferences();
    private enum Page { LETTERS, SPECIAL_CHARS, SPECIAL_KEYS, MENU }

    /** One height step applied by the menu's 높이 −/＋ tiles. */
    private static final float HEIGHT_STEP = 0.1f;
    /** Gap (in dp) drawn around each key; also the touch dead zone, so the space between keys
     * registers no press and a near-boundary tap can't land on the wrong neighbour. */
    private static final float KEY_GAP_DP = 4.0f;
    private static final float KEY_RADIUS_DP = 5.0f;
    private static final float KEY_SHADOW_DP = 2.0f;
    /** The raised-key bottom lip colour, darker than the keyboard background. */
    private static final int KEY_SHADOW = Color.rgb(12, 14, 18);

    /** {@link #KEY_GAP_DP} resolved to pixels for this display; set in the constructor. */
    private final int keyGapPx;
    private final int keyRadiusPx;
    private final int keyShadowPx;

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
    /** Invoked when the 키보드관리 tile is tapped; the host opens the enable-keyboards screen. */
    private Runnable onManageIme;
    /** Invoked when the 한자 key is tapped; the host converts the reading to Hanja. */
    private Runnable onHanja;
    /** User-adjustable multiplier on the base keyboard height, persisted across sessions. */
    private float heightScale = KeyboardHeightScale.DEFAULT_SCALE;
    // The unpressed keyboard is rendered once into this bitmap and reused until the layout changes.
    private Bitmap baseBitmap;
    private String baseSignature;

    public ReteKeyboardView(Context context, InputSink sink) {
        super(context);
        this.sink = Objects.requireNonNull(sink, "sink");
        float density = context.getResources().getDisplayMetrics().density;
        this.keyGapPx = Math.round(KEY_GAP_DP * density);
        this.keyRadiusPx = Math.round(KEY_RADIUS_DP * density);
        this.keyShadowPx = Math.round(KEY_SHADOW_DP * density);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);
        heightScale = KeyboardHeightScale.clamp(
            prefs().getFloat(KEY_HEIGHT_SCALE, KeyboardHeightScale.DEFAULT_SCALE));
        feedback = new KeyFeedback(context);
        feedback.reload(prefs());
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

    /** Sets the handler the 키보드관리 tile runs to open the enable-keyboards settings screen. */
    public void setOnManageIme(Runnable handler) {
        this.onManageIme = handler;
    }

    /** Sets the handler the 한자 key runs to convert the reading before the cursor to Hanja. */
    public void setOnHanja(Runnable handler) {
        this.onHanja = handler;
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

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        prefs().registerOnSharedPreferenceChangeListener(prefsListener);
        reloadPreferences();
    }

    @Override
    protected void onDetachedFromWindow() {
        prefs().unregisterOnSharedPreferenceChangeListener(prefsListener);
        if (baseBitmap != null) {
            baseBitmap.recycle();
            baseBitmap = null;
        }
        super.onDetachedFromWindow();
    }

    /** Applies persisted settings (feedback strengths and height) to the on-screen keyboard. */
    private void reloadPreferences() {
        feedback.reload(prefs());
        repeatEnabled = prefs().getBoolean(
            KeyRepeatSettings.KEY_ENABLED, KeyRepeatSettings.DEFAULT_ENABLED);
        repeatDelayMs = KeyRepeatSettings.clampDelay(prefs().getInt(
            KeyRepeatSettings.KEY_DELAY_MS, KeyRepeatSettings.DEFAULT_DELAY_MS));
        repeatIntervalMs = KeyRepeatSettings.clampInterval(prefs().getInt(
            KeyRepeatSettings.KEY_INTERVAL_MS, KeyRepeatSettings.DEFAULT_INTERVAL_MS));
        float storedScale = KeyboardHeightScale.clamp(
            prefs().getFloat(KEY_HEIGHT_SCALE, KeyboardHeightScale.DEFAULT_SCALE));
        if (storedScale != heightScale) {
            heightScale = storedScale;
            requestLayout();
        }
        invalidate();
    }

    /** Clears transient one-shot and pointer state when the editor session changes. */
    public void resetLayerState() {
        shiftLayer.clear();
        armedModifiers.clear();
        cancelHold();
        feedback.reload(prefs());
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        // The unpressed keyboard (raised keys and labels) is cached to a bitmap and only rebuilt
        // when the layout, highlight state, or size changes; a key press just tints one key.
        ensureBaseBitmap(width, height);
        canvas.drawBitmap(baseBitmap, 0.0f, 0.0f, null);
        drawPressFeedback(canvas, width, height);
        drawPopup(canvas);
    }

    /** Tints the held key for a colour-change press feedback (respecting the intensity setting). */
    private void drawPressFeedback(Canvas canvas, int width, int height) {
        if (heldRow < 0 || heldKey < 0 || feedback.visualIntensity() <= 0.0f) {
            return;
        }
        KeyboardLayout layout = layout();
        if (heldRow >= layout.rows().size()) {
            return;
        }
        List<SoftwareKeySpec> row = layout.rows().get(heldRow);
        if (heldKey >= row.size()) {
            return;
        }
        SoftwareKeySpec key = row.get(heldKey);
        int top = layout.rowEdge(heldRow, height);
        int bottom = layout.rowEdge(heldRow + 1, height);
        int startColumn = layout.startColumn(heldRow, heldKey);
        int left = layout.columnEdge(startColumn, width);
        int right = layout.columnEdge(startColumn + key.columnSpan(), width);
        paint.setColor(Color.argb(
            Math.round(feedback.visualIntensity() * 150.0f), 120, 170, 235));
        canvas.drawRoundRect(left + keyGapPx, top + keyGapPx, right - keyGapPx, bottom - keyGapPx,
            keyRadiusPx, keyRadiusPx, paint);
    }

    /** Rebuilds the cached keyboard bitmap when the layout, highlight state, or size changes. */
    private void ensureBaseBitmap(int width, int height) {
        String signature = layoutSignature();
        if (baseBitmap != null && signature.equals(baseSignature)
            && baseBitmap.getWidth() == width && baseBitmap.getHeight() == height) {
            return;
        }
        if (baseBitmap != null) {
            baseBitmap.recycle();
        }
        baseBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas cache = new Canvas(baseBitmap);
        cache.drawColor(Color.rgb(28, 30, 36));
        paint.setTextAlign(Paint.Align.CENTER);
        KeyboardLayout layout = layout();
        List<List<SoftwareKeySpec>> rows = layout.rows();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<SoftwareKeySpec> keys = rows.get(rowIndex);
            int top = layout.rowEdge(rowIndex, height);
            int bottom = layout.rowEdge(rowIndex + 1, height);
            for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
                SoftwareKeySpec key = keys.get(keyIndex);
                int startColumn = layout.startColumn(rowIndex, keyIndex);
                int left = layout.columnEdge(startColumn, width);
                int right = layout.columnEdge(startColumn + key.columnSpan(), width);
                drawKey(cache, key, left, top, right, bottom);
            }
        }
        baseSignature = signature;
    }

    /** Identifies what the cached bitmap depends on, so it is reused until one of these changes. */
    private String layoutSignature() {
        return page + "|" + letterLayoutId + "|" + numpadMode + "|" + shiftLayer.isActive()
            + "|" + shiftLayer.isLocked() + "|" + armedModifiers;
    }

    /** Draws one raised, rounded key with its label and long-press hint into the cache canvas. */
    private void drawKey(Canvas canvas, SoftwareKeySpec key,
            int left, int top, int right, int bottom) {
        float l = left + keyGapPx;
        float t = top + keyGapPx;
        float r = right - keyGapPx;
        float b = bottom - keyGapPx;
        // A darker lip just below the face makes the key look raised.
        paint.setColor(KEY_SHADOW);
        canvas.drawRoundRect(l, t + keyShadowPx, r, b + keyShadowPx, keyRadiusPx, keyRadiusPx, paint);
        paint.setColor(keyFillColor(key));
        canvas.drawRoundRect(l, t, r, b, keyRadiusPx, keyRadiusPx, paint);
        paint.setColor(key.enabled() || key.isControl()
            ? Color.rgb(22, 27, 34)
            : Color.rgb(139, 148, 158));
        fitLabel(key.label(), right - left, bottom - top);
        canvas.drawText(key.label(), (left + right) * 0.5f, top + (bottom - top) * 0.62f, paint);
        if (key.longPressTexts().size() == 1) {
            // A single long-press character is hinted in small text at the top-right corner.
            paint.setColor(Color.rgb(120, 130, 145));
            float hint = (bottom - top) * 0.22f;
            paint.setTextSize(hint);
            canvas.drawText(key.longPressTexts().get(0),
                right - hint * 0.75f, top + hint * 1.15f, paint);
        } else if (key.hasLongPress() || key.hasLongPressControl()) {
            paint.setColor(Color.rgb(139, 148, 158));
            canvas.drawCircle(right - 10.0f, top + 10.0f, 3.0f, paint);
        }
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
            case MotionEvent.ACTION_MOVE:
                trackHold(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
                releaseHold(event.getX(), event.getY());
                invalidate();
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
        if (!withinKeyFace(layout, rowIndex, keyIndex, x, y)) {
            // The touch landed in the gap between keys; ignore it so a near-boundary tap
            // cannot register as the wrong neighbour.
            return;
        }
        heldRow = rowIndex;
        heldKey = keyIndex;
        // Give immediate press feedback: a haptic tick, a click sound, and a visual highlight.
        feedback.playKeyDown();
        invalidate();
        SoftwareKeySpec key = layout.rows().get(rowIndex).get(keyIndex);
        // Shift, and any key with a long press, react to a hold.
        if (key.hasLongPress()
            || key.hasLongPressControl()
            || (key.isControl() && key.control() == ControlKey.SHIFT)) {
            postDelayed(onHoldElapsed, ViewConfiguration.getLongPressTimeout());
        } else if (repeatEnabled && repeatsOnHold(key)) {
            // Ordinary keys with no long press auto-repeat while held.
            postDelayed(onRepeatElapsed, repeatDelayMs);
        }
    }

    /** Keys that fire again while held: plain text/edit/raw keys, but not controls or layer keys. */
    private static boolean repeatsOnHold(SoftwareKeySpec key) {
        return key.enabled() && !key.isControl()
            && !key.hasLongPress() && !key.hasLongPressControl();
    }

    /** Fires the held key once and schedules the next repeat, until the finger lifts. */
    private void handleRepeat() {
        if (heldRow < 0 || heldKey < 0 || popup != null) {
            return;
        }
        SoftwareKeySpec key = layout().rows().get(heldRow).get(heldKey);
        if (!repeatsOnHold(key)) {
            return;
        }
        sink.accept(pressEventWithModifiers(key));
        repeatFired = true;
        feedback.playKeyDown();
        postDelayed(onRepeatElapsed, repeatIntervalMs);
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
        boolean repeated = repeatFired;
        removeCallbacks(onHoldElapsed);
        removeCallbacks(onRepeatElapsed);

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
        if (consumed || repeated) {
            // A hold already acted — shift lock, a layer switch, or auto-repeat — so the release
            // must not also fire the tap.
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
            if (tryArmedModifierChord(held)) {
                performClick();
                return;
            }
            sink.accept(pressEventWithModifiers(held));
            consumeOneShotShift();
            performClick();
        }
    }

    /**
     * With a soft Ctrl armed, a letter key runs the matching editor command (Ctrl+A/C/V/X/Z/Y)
     * instead of typing the letter, so those shortcuts work from the on-screen keyboard too.
     */
    /**
     * With a soft Ctrl/Alt/Meta armed, a letter key is sent as a real key chord (e.g. Ctrl+B)
     * instead of typed. Rich editors turn Ctrl+A/C/V/X/Z/Y into select-all/copy/paste/cut/undo/redo
     * via {@code onKeyShortcut}; terminals receive the control code (Ctrl+B → 0x02). The armed
     * modifiers are one-shot: consumed after the chord.
     */
    private boolean tryArmedModifierChord(SoftwareKeySpec key) {
        Set<KeyModifier> mods = EnumSet.noneOf(KeyModifier.class);
        if (armedModifiers.contains(ControlKey.CTRL)) {
            mods.add(KeyModifier.CTRL);
        }
        if (armedModifiers.contains(ControlKey.ALT)) {
            mods.add(KeyModifier.ALT);
        }
        if (armedModifiers.contains(ControlKey.META)) {
            mods.add(KeyModifier.META);
        }
        if (mods.isEmpty()) {
            return false;
        }
        SemanticInput input = key.semanticInput();
        if (input == null || input.kind() != SemanticInput.Kind.TEXT) {
            return false;
        }
        String text = input.text();
        if (text == null || text.length() != 1) {
            return false;
        }
        char letter = Character.toUpperCase(text.charAt(0));
        if (letter < 'A' || letter > 'Z') {
            return false;
        }
        RawKey rawKey;
        try {
            rawKey = RawKey.valueOf(String.valueOf(letter));
        } catch (IllegalArgumentException notALetterKey) {
            return false;
        }
        sink.accept(ProjectKeyEvent.softwareDown(
            key.stableKeyId(), SemanticInput.rawKey(rawKey, mods)));
        armedModifiers.remove(ControlKey.CTRL);
        armedModifiers.remove(ControlKey.ALT);
        armedModifiers.remove(ControlKey.META);
        consumeOneShotShift();
        invalidate();
        return true;
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
        removeCallbacks(onRepeatElapsed);
        repeatFired = false;
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

    /**
     * Whether {@code (x, y)} falls on the visible face of the key at
     * {@code (rowIndex, keyIndex)} — the cell inset by {@link #keyGapPx}. Touches in the
     * surrounding gap return {@code false} so they register no press.
     */
    private boolean withinKeyFace(
            KeyboardLayout layout, int rowIndex, int keyIndex, float x, float y) {
        int width = getWidth();
        int height = getHeight();
        int top = layout.rowEdge(rowIndex, height);
        int bottom = layout.rowEdge(rowIndex + 1, height);
        int startColumn = layout.startColumn(rowIndex, keyIndex);
        SoftwareKeySpec key = layout.rows().get(rowIndex).get(keyIndex);
        int left = layout.columnEdge(startColumn, width);
        int right = layout.columnEdge(startColumn + key.columnSpan(), width);
        return x >= left + keyGapPx && x <= right - keyGapPx
            && y >= top + keyGapPx && y <= bottom - keyGapPx;
    }

    private void applyControl(ControlKey control) {
        switch (control) {
            case SHIFT:
                shiftLayer.tap();
                break;
            case LAYOUT_TOGGLE:
                // Only flip EN<->KO when already on the letters page; from another page just return
                // to letters keeping the language that was last in use.
                if (page == Page.LETTERS) {
                    letterLayoutId = KeyboardLayouts.otherLetters(letterLayoutId);
                }
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
            case MANAGE_IME:
                if (onManageIme != null) {
                    onManageIme.run();
                }
                break;
            case HANJA:
                if (onHanja != null) {
                    onHanja.run();
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
