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

@SuppressLint("ViewConstructor")
public final class ReteKeyboardView extends View {
    public interface InputSink {
        void accept(ProjectKeyEvent event);
    }

    private static final String PREFS = "retekey_view";
    private static final String KEY_HEIGHT_SCALE = "height_scale";
    private static final String KEY_LAST_LETTERS = "last_letter_layout";

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final InputSink sink;
    private final KeyFeedback feedback;
    private final ShiftLayerState shiftLayer = new ShiftLayerState();
    private final Set<ControlKey> armedModifiers = EnumSet.noneOf(ControlKey.class);
    /**
     * One finger, and the key it is holding. Typing with two thumbs means two of these at once, so
     * each carries its own long-press and auto-repeat timers rather than sharing the view's.
     */
    private final class Touch {
        final int pointerId;
        final float downX;
        final float downY;
        // The grid this finger's indexes were taken from. Another finger can switch the page
        // mid-press, and row/key would then point into a different keyboard.
        final String grid;
        // Not final: a finger that slides a slop clear of its key takes the key it slid onto.
        int row;
        int key;
        boolean holdConsumed;
        boolean repeatFired;
        /** The 천지인 direction guide is showing for this finger. */
        boolean guideOpen;
        /** Which way the finger has gone since the guide opened; null means it is still still. */
        CheonjiinInterpreter.Flick guideDirection;
        final Runnable onHold = () -> handleLongPress(this);
        final Runnable onRepeat = () -> handleRepeat(this);

        Touch(int pointerId, int row, int key, String grid, float downX, float downY) {
            this.pointerId = pointerId;
            this.downX = downX;
            this.downY = downY;
            this.row = row;
            this.key = key;
            this.grid = grid;
        }
    }

    private final android.util.SparseArray<Touch> touches = new android.util.SparseArray<>();
    // Held-key auto-repeat (space, enter, backspace, arrows, letters …), configured in settings.
    private boolean repeatEnabled = KeyRepeatSettings.DEFAULT_ENABLED;
    private int repeatDelayMs = KeyRepeatSettings.DEFAULT_DELAY_MS;
    private int repeatIntervalMs = KeyRepeatSettings.DEFAULT_INTERVAL_MS;
    // Held strongly so the weak listener registration in the preferences survives; it applies
    // settings changes (feedback strengths, height) to a keyboard that is already on screen.
    private final SharedPreferences.OnSharedPreferenceChangeListener prefsListener =
        (changed, key) -> reloadPreferences();
    private enum Page { LETTERS, SPECIAL_CHARS, SPECIAL_KEYS, MENU }

    /** One height step applied by the menu's 높이 −/＋ tiles. */
    private static final float HEIGHT_STEP = 0.1f;
    /**
     * How far a finger must go for a drag to be a drag. Kept small so the letter arrives at once —
     * that is the whole reason to drag rather than tap twice — but above the touch slop so an
     * ordinary press cannot become one by accident.
     */
    private static final float FLICK_DP = 14.0f;
    /**
     * How long a 12-key run waits before the next press of the same key starts a new letter rather
     * than cycling. A phone does the same, and it is what lets 삶 be followed by ㅇ — the key that
     * typed the ㅁ before it.
     */
    private static final int MULTI_TAP_TIMEOUT_MS = 800;

    /** Gap (in dp) drawn around each key. Drawing only: the touch target is the whole cell, so
     * the space between keys still belongs to a key. See TouchTargeting. */
    private static final float KEY_GAP_DP = 4.0f;
    private static final float KEY_RADIUS_DP = 5.0f;
    private static final float KEY_SHADOW_DP = 2.0f;

    /** {@link #KEY_GAP_DP} resolved to pixels for this display; set in the constructor. */
    private final int keyGapPx;
    /** How far a finger may wander before it is judged to have left its key. */
    private final int touchSlopPx;
    private final int flickDistancePx;
    private final int keyRadiusPx;
    private final int keyShadowPx;

    private KeyboardLayoutId letterLayoutId = KeyboardLayoutId.KO_DUBEOLSIK;
    private Page page = Page.LETTERS;
    private NumpadMode numpadMode = NumpadMode.NUMBERS;

    /** Invoked when the 설정 tile is tapped; the host service opens the settings screen. */
    private Runnable onOpenSettings;
    /** Invoked with an editor context-menu id (copy/paste/undo) for the host to perform. */
    private Fn.IntConsumer onEditCommand;
    /** Invoked when the 날짜 tile is tapped; the host inserts the current date and time. */
    private Runnable onInsertDate;
    /** Invoked when the 키보드전환 tile is tapped; the host opens the input-method picker. */
    private Runnable onSwitchIme;
    /** Invoked when the 키보드관리 tile is tapped; the host opens the enable-keyboards screen. */
    private Runnable onManageIme;
    /** Invoked when the 한자 key is tapped; the host converts the reading to Hanja. */
    private Runnable onHanja;
    private Runnable onFloatingToggle;
    private Fn.Consumer<KeyboardLayoutId> onLayoutChanged;
    private final CheonjiinInterpreter cheonjiin = new CheonjiinInterpreter();
    private final NaratgeulInterpreter naratgeul = new NaratgeulInterpreter();
    /** Ends the 12-key multi-tap grouping after a pause, without ending the syllable. */
    private final Runnable endMultiTap = () -> cheonjiin.endMultiTap();
    /** User-adjustable multiplier on the base keyboard height, persisted across sessions. */
    private static final long FLASH_MS = 180;
    private static final float FLASH_MAX_ALPHA = 150.0f;
    private boolean flashing;
    private String flashLabel;
    private final Runnable onFlashElapsed = () -> {
        flashing = false;
        flashLabel = null;
        invalidate();
    };
    private boolean collapsed;
    private float heightScale = KeyboardHeightScale.DEFAULT_SCALE;
    // The unpressed keyboard is rendered once into this bitmap and reused until the layout changes.
    private Bitmap baseBitmap;
    private String baseSignature;
    // Colours resolved to the current light/dark (and Material You) theme; refreshed on rebuild.
    private KeyboardPalette palette;

    public ReteKeyboardView(Context context, InputSink sink) {
        super(context);
        this.sink = Objects.requireNonNull(sink, "sink");
        float density = context.getResources().getDisplayMetrics().density;
        this.keyGapPx = Math.round(KEY_GAP_DP * density);
        this.touchSlopPx = ViewConfiguration.get(context).getScaledTouchSlop();
        this.flickDistancePx = Math.max(touchSlopPx, Math.round(FLICK_DP * density));
        this.keyRadiusPx = Math.round(KEY_RADIUS_DP * density);
        this.keyShadowPx = Math.round(KEY_SHADOW_DP * density);
        this.palette = KeyboardPalette.resolve(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);
        heightScale = KeyboardHeightScale.clamp(
            prefs().getFloat(KEY_HEIGHT_SCALE, KeyboardHeightScale.DEFAULT_SCALE));
        feedback = new KeyFeedback(context);
        feedback.reload(prefs());
        letterLayoutId = restoreLetterLayout();
    }

    /** Sets the handler the 설정 tile runs to open settings; the service owns the launch. */
    public void setOnOpenSettings(Runnable handler) {
        this.onOpenSettings = handler;
    }

    /** Sets the handler for editor context-menu commands (copy/paste/undo) from menu tiles. */
    public void setOnEditCommand(Fn.IntConsumer handler) {
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

    public void setOnFloatingToggle(Runnable handler) {
        this.onFloatingToggle = handler;
    }

    /** Told which letter layout the globe key moved to, so the host can name it on screen. */
    public void setOnLayoutChanged(Fn.Consumer<KeyboardLayoutId> handler) {
        this.onLayoutChanged = handler;
    }

    /** The layouts the globe key walks, as the user ordered them in settings. */
    private java.util.List<KeyboardLayoutId> letterOrder() {
        return LetterLayouts.parse(prefs().getString(LetterLayouts.KEY_ORDER, null));
    }

    /** Re-reads the layout order and lands on a layout that is still enabled. */
    public void reloadLetterLayouts() {
        java.util.List<KeyboardLayoutId> order = letterOrder();
        if (!order.contains(letterLayoutId)) {
            letterLayoutId = LetterLayouts.firstOf(order);
        }
        requestLayout();
        invalidate();
    }

    /** The layout the globe key was last left on, when it is still one the user enabled. */
    private KeyboardLayoutId restoreLetterLayout() {
        java.util.List<KeyboardLayoutId> order = letterOrder();
        String stored = prefs().getString(KEY_LAST_LETTERS, null);
        if (stored != null) {
            for (KeyboardLayoutId id : order) {
                if (id.name().equals(stored)) {
                    return id;
                }
            }
        }
        return LetterLayouts.firstOf(order);
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

    /**
     * Collapses the keyboard to nothing without hiding the IME window, so the Hanja candidate strip
     * can be on screen on its own while a hardware keyboard is doing the typing.
     */
    public void setCollapsed(boolean collapsed) {
        if (this.collapsed == collapsed) {
            return;
        }
        this.collapsed = collapsed;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (collapsed) {
            // One pixel, not zero: the IME window has to exist for the Hanja candidate window to
            // have something to attach to, and a zero-height window is not laid out at all.
            setMeasuredDimension(width, 1);
            return;
        }
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
        cancelAllTouches();
        feedback.reload(prefs());
        invalidate();
    }

    /**
     * Flashes the whole keyboard for a moment so a keystroke is visible at the panel level, not
     * only on the one key under the finger. Follows the visual-feedback strength setting, so
     * turning that to zero turns the blink off with it.
     */
    private void flashKeyboard(SoftwareKeySpec key, String typed) {
        if (feedback.visualIntensity() <= 0.0f) {
            return;
        }
        removeCallbacks(onFlashElapsed);
        flashing = true;
        flashLabel = echoLabel(key, typed);
        invalidate();
        postDelayed(onFlashElapsed, FLASH_MS);
    }

    /**
     * What the echo box should show, or {@code null} for keys that type nothing to echo — the
     * layer switches, the modifiers, backspace, enter. Their own label would say nothing useful
     * blown up to the size of a syllable.
     */
    static String echoLabel(SoftwareKeySpec key, String typed) {
        if (typed != null) {
            return typed;
        }
        if (key == null || key.isControl() || !key.enabled()) {
            return null;
        }
        SemanticInput input = key.semanticInput();
        if (input == null) {
            return null;
        }
        switch (input.kind()) {
            case TEXT:
            case JAMO:
                return key.label();
            default:
                return null;
        }
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
        drawFlickGuides(canvas, width, height);
        drawFlash(canvas, width, height);
        drawEchoBox(canvas, width, height);
    }

    /**
     * The keystroke blink: the gaps between the keys light up, and the keys themselves do not, so
     * the grid flashes as a lattice around what you are reading rather than washing over it.
     */
    private void drawFlash(Canvas canvas, int width, int height) {
        if (!flashing) {
            return;
        }
        int tint = palette.keyAccent;
        paint.setColor(Color.argb(
            Math.round(feedback.visualIntensity() * FLASH_MAX_ALPHA),
            Color.red(tint), Color.green(tint), Color.blue(tint)));
        KeyboardLayout layout = layout();
        List<List<SoftwareKeySpec>> rows = layout.rows();
        int saved = canvas.save();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<SoftwareKeySpec> keys = rows.get(rowIndex);
            int top = layout.rowEdge(rowIndex, height);
            int bottom = layout.rowEdge(rowIndex + 1, height);
            for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
                SoftwareKeySpec key = keys.get(keyIndex);
                int startColumn = layout.startColumn(rowIndex, keyIndex);
                int left = layout.columnEdge(startColumn, width);
                int right = layout.columnEdge(startColumn + key.columnSpan(), width);
                // Cut each key face out of the wash; what is left is exactly the gaps.
                Compat.clipOut(canvas,
                    left + keyGapPx, top + keyGapPx, right - keyGapPx, bottom - keyGapPx);
            }
        }
        canvas.drawRect(0.0f, 0.0f, width, height, paint);
        canvas.restoreToCount(saved);
    }

    /**
     * Shows the character that was just typed, large and in a box over the keyboard, for as long as
     * the blink lasts. A finger covers the key it pressed; this puts the result somewhere it can
     * actually be read.
     */
    private void drawEchoBox(Canvas canvas, int width, int height) {
        if (!flashing || flashLabel == null) {
            return;
        }
        float boxHeight = Math.min(height * 0.38f, dp(72));
        float textSize = boxHeight * 0.62f;
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);
        float boxWidth = Math.max(boxHeight, paint.measureText(flashLabel) + dp(28));
        float centerX = width * 0.5f;
        // Along the top edge, away from the rows the hand is usually over.
        float top = dp(6);
        float left = centerX - boxWidth * 0.5f;
        float radius = dp(10);

        paint.setColor(palette.keyAccent);
        Compat.drawRoundRect(
            canvas, left, top, left + boxWidth, top + boxHeight, radius, paint);
        paint.setColor(palette.background);
        canvas.drawText(
            flashLabel, centerX, top + boxHeight * 0.5f - (paint.descent() + paint.ascent()) / 2.0f,
            paint);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** Tints each held key for a colour-change press feedback, one per finger on the keyboard. */
    private void drawPressFeedback(Canvas canvas, int width, int height) {
        if (touches.size() == 0 || feedback.visualIntensity() <= 0.0f) {
            return;
        }
        KeyboardLayout layout = layout();
        for (int i = 0; i < touches.size(); i++) {
            Touch touch = touches.valueAt(i);
            if (touch.row >= layout.rows().size()) {
                continue;
            }
            List<SoftwareKeySpec> row = layout.rows().get(touch.row);
            if (touch.key >= row.size()) {
                continue;
            }
            SoftwareKeySpec key = row.get(touch.key);
            int top = layout.rowEdge(touch.row, height);
            int bottom = layout.rowEdge(touch.row + 1, height);
            int startColumn = layout.startColumn(touch.row, touch.key);
            int left = layout.columnEdge(startColumn, width);
            int right = layout.columnEdge(startColumn + key.columnSpan(), width);
            int tint = palette.pressTint;
            paint.setColor(Color.argb(Math.round(feedback.visualIntensity() * 150.0f),
                Color.red(tint), Color.green(tint), Color.blue(tint)));
            Compat.drawRoundRect(canvas, left + keyGapPx, top + keyGapPx, right - keyGapPx,
                bottom - keyGapPx, keyRadiusPx, paint);
        }
    }

    /**
     * The guide a held 천지인 key raises: the four letters around it and, in the middle, the digit
     * it holds. The way the finger has gone is lit, so what will be typed on release is the one
     * under it — and a quick drag shows the same thing with its choice already made.
     */
    private void drawFlickGuides(Canvas canvas, int width, int height) {
        KeyboardLayout layout = layout();
        for (int i = 0; i < touches.size(); i++) {
            Touch touch = touches.valueAt(i);
            if (!touch.guideOpen || touch.row >= layout.rows().size()) {
                continue;
            }
            List<SoftwareKeySpec> row = layout.rows().get(touch.row);
            if (touch.key >= row.size()) {
                continue;
            }
            SoftwareKeySpec key = row.get(touch.key);
            CheonjiinInterpreter.Key phoneKey = phoneKeyOf(key);
            if (phoneKey == null) {
                continue;
            }
            int startColumn = layout.startColumn(touch.row, touch.key);
            float cellLeft = layout.columnEdge(startColumn, width);
            float cellRight = layout.columnEdge(startColumn + key.columnSpan(), width);
            float cellTop = layout.rowEdge(touch.row, height);
            float cellBottom = layout.rowEdge(touch.row + 1, height);
            float box = Math.min(cellRight - cellLeft, cellBottom - cellTop) * 0.78f;
            float step = box * 1.04f;
            // Centred on the key, then nudged back inside the keyboard when it would hang off.
            float centreX = Math.min(Math.max((cellLeft + cellRight) * 0.5f, step + box * 0.5f),
                width - step - box * 0.5f);
            float centreY = Math.min(Math.max((cellTop + cellBottom) * 0.5f, step + box * 0.5f),
                height - step - box * 0.5f);

            drawGuideCell(canvas, centreX, centreY, box,
                key.hasLongPress() ? key.longPressTexts().get(0) : null,
                touch.guideDirection == null);
            drawGuideCell(canvas, centreX - step, centreY, box,
                CheonjiinInterpreter.flickLabel(phoneKey, CheonjiinInterpreter.Flick.LEFT),
                touch.guideDirection == CheonjiinInterpreter.Flick.LEFT);
            drawGuideCell(canvas, centreX + step, centreY, box,
                CheonjiinInterpreter.flickLabel(phoneKey, CheonjiinInterpreter.Flick.RIGHT),
                touch.guideDirection == CheonjiinInterpreter.Flick.RIGHT);
            drawGuideCell(canvas, centreX, centreY - step, box,
                CheonjiinInterpreter.flickLabel(phoneKey, CheonjiinInterpreter.Flick.UP),
                touch.guideDirection == CheonjiinInterpreter.Flick.UP);
            drawGuideCell(canvas, centreX, centreY + step, box,
                CheonjiinInterpreter.flickLabel(phoneKey, CheonjiinInterpreter.Flick.DOWN),
                touch.guideDirection == CheonjiinInterpreter.Flick.DOWN);
        }
    }

    private void drawGuideCell(
            Canvas canvas, float centreX, float centreY, float box, String label, boolean aimed) {
        if (label == null) {
            return;
        }
        float half = box * 0.5f;
        float radius = box * 0.18f;
        paint.setColor(palette.keyShadow);
        Compat.drawRoundRect(canvas, centreX - half, centreY - half + keyShadowPx,
            centreX + half, centreY + half + keyShadowPx, radius, paint);
        paint.setColor(aimed ? palette.keyAccent : palette.keyFace);
        Compat.drawRoundRect(
            canvas, centreX - half, centreY - half, centreX + half, centreY + half, radius, paint);
        paint.setColor(aimed ? palette.background : palette.keyText);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(box * 0.5f);
        canvas.drawText(label, centreX, centreY - (paint.descent() + paint.ascent()) / 2.0f, paint);
    }

    private void ensureBaseBitmap(int width, int height) {
        String signature = layoutSignature();
        if (baseBitmap != null && signature.equals(baseSignature)
            && baseBitmap.getWidth() == width && baseBitmap.getHeight() == height) {
            return;
        }
        if (baseBitmap != null) {
            baseBitmap.recycle();
        }
        // The cached keyboard is opaque — it starts with a solid background fill — so it needs no
        // alpha channel, and RGB_565 halves what the cache costs. On a tablet that is megabytes.
        baseBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        palette = KeyboardPalette.resolve(getContext());
        Canvas cache = new Canvas(baseBitmap);
        cache.drawColor(palette.background);
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
            + "|" + shiftLayer.isLocked() + "|" + armedModifiers + "|"
            + KeyboardPalette.isNight(getContext());
    }

    /** The text to paint for a key: its label, or a word when the device has no glyph for it. */
    private String labelOf(SoftwareKeySpec key) {
        return LegacyGlyphs.label(key.label(), android.os.Build.VERSION.SDK_INT);
    }

    /** Draws one raised, rounded key with its label and long-press hint into the cache canvas. */
    private void drawKey(Canvas canvas, SoftwareKeySpec key,
            int left, int top, int right, int bottom) {
        float l = left + keyGapPx;
        float t = top + keyGapPx;
        float r = right - keyGapPx;
        float b = bottom - keyGapPx;
        // A darker lip just below the face makes the key look raised.
        paint.setColor(palette.keyShadow);
        Compat.drawRoundRect(canvas, l, t + keyShadowPx, r, b + keyShadowPx, keyRadiusPx, paint);
        paint.setColor(keyFillColor(key));
        Compat.drawRoundRect(canvas, l, t, r, b, keyRadiusPx, paint);
        paint.setColor(key.enabled() || key.isControl() ? palette.keyText : palette.keyTextMuted);
        String label = labelOf(key);
        fitLabel(label, right - left, bottom - top);
        canvas.drawText(label, (left + right) * 0.5f, top + (bottom - top) * 0.62f, paint);
        if (key.longPressTexts().size() == 1) {
            // A single long-press character is hinted in small text at the top-right corner.
            paint.setColor(palette.hint);
            float hint = (bottom - top) * 0.22f;
            paint.setTextSize(hint);
            canvas.drawText(key.longPressTexts().get(0),
                right - hint * 0.75f, top + hint * 1.15f, paint);
        } else if (key.hasLongPress() || key.hasLongPressControl()) {
            paint.setColor(palette.hint);
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int index = event.getActionIndex();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                // Every finger gets its own key. Typing fast rolls — the next finger lands before
                // the last one lifts — and a keyboard that only hears the first and last pointer
                // loses everything pressed in between.
                beginTouch(event.getPointerId(index), event.getX(index), event.getY(index));
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                endTouch(event.getPointerId(index), event.getX(index), event.getY(index));
                return true;
            case MotionEvent.ACTION_MOVE:
                moveTouches(event);
                return true;
            case MotionEvent.ACTION_CANCEL:
                cancelAllTouches();
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

    /** Starts this finger on the key under it, with its own hold and repeat timers. */
    private void beginTouch(int pointerId, float x, float y) {
        KeyboardLayout layout = layout();
        int rowIndex = rowAt(layout, y);
        int keyIndex = keyIndexAt(layout, rowIndex, x);
        if (rowIndex < 0 || keyIndex < 0) {
            return;
        }
        // Every pixel of the keyboard belongs to a key. The gap drawn around each face is a gap in
        // the picture only: a touch target with dead space between the keys throws away roughly a
        // third of the area, and every tap that lands there is a keystroke the user has to repeat.
        Touch touch = new Touch(pointerId, rowIndex, keyIndex, gridSignature(), x, y);
        touches.put(pointerId, touch);
        // Give immediate press feedback: a haptic tick, a click sound, and a visual highlight.
        feedback.playKeyDown();
        invalidate();
        armTimers(touch, layout.rows().get(rowIndex).get(keyIndex));
    }

    /** A finger's hold and repeat timers, armed for whichever key it is on now. */
    private void armTimers(Touch touch, SoftwareKeySpec key) {
        // Shift, and any key with a long press, react to a hold.
        if (key.hasLongPress()
            || key.hasLongPressControl()
            || (key.isControl() && key.control() == ControlKey.SHIFT)) {
            postDelayed(touch.onHold, ViewConfiguration.getLongPressTimeout());
        } else if (repeatEnabled && repeatsOnHold(key)) {
            // Ordinary keys with no long press auto-repeat while held.
            postDelayed(touch.onRepeat, repeatDelayMs);
        }
    }

    /**
     * A finger has moved. It keeps the key it started on until it is a touch slop clear of that
     * key's cell, and then takes the key it moved onto. A press therefore survives the roll of a
     * fingertip — which is most of what "the key didn't register" turns out to be — while a finger
     * that genuinely slides to the next key types that one.
     */
    private void moveTouches(MotionEvent event) {
        KeyboardLayout layout = layout();
        String grid = gridSignature();
        for (int pointer = 0; pointer < event.getPointerCount(); pointer++) {
            Touch touch = touches.get(event.getPointerId(pointer));
            if (touch == null || touch.holdConsumed || touch.repeatFired
                || !touch.grid.equals(grid)) {
                // Nothing to retarget, or a hold has already acted, or the page changed under it.
                continue;
            }
            float x = event.getX(pointer);
            float y = event.getY(pointer);
            if (touch.guideOpen) {
                // The guide is up: the finger is choosing, not typing. It types when it lifts.
                CheonjiinInterpreter.Flick aimed =
                    FlickDirection.of(x - touch.downX, y - touch.downY, flickDistancePx);
                if (aimed != touch.guideDirection) {
                    touch.guideDirection = aimed;
                    invalidate();
                }
                continue;
            }
            if (tryFlick(layout, touch, x, y)) {
                continue;
            }
            if (!escapedKey(layout, touch, x, y)) {
                continue;
            }
            int rowIndex = rowAt(layout, y);
            int keyIndex = keyIndexAt(layout, rowIndex, x);
            if (rowIndex < 0 || keyIndex < 0
                || (rowIndex == touch.row && keyIndex == touch.key)) {
                continue;
            }
            removeCallbacks(touch.onHold);
            removeCallbacks(touch.onRepeat);
            touch.row = rowIndex;
            touch.key = keyIndex;
            invalidate();
            armTimers(touch, layout.rows().get(rowIndex).get(keyIndex));
        }
    }

    /**
     * A drag off a 12-key key types that key's dragged letter at once. Such a key never hands its
     * finger to the neighbour, because on those pages leaving the key is itself the input.
     */
    private boolean tryFlick(KeyboardLayout layout, Touch touch, float x, float y) {
        SoftwareKeySpec key = layout.rows().get(touch.row).get(touch.key);
        if (!key.stableKeyId().startsWith("touch.cheonjiin.")) {
            return false;
        }
        CheonjiinInterpreter.Flick direction =
            FlickDirection.of(x - touch.downX, y - touch.downY, flickDistancePx);
        if (direction == null) {
            return false;
        }
        removeCallbacks(touch.onHold);
        removeCallbacks(touch.onRepeat);
        // The press is spent: the release must not type the key's tap letter as well.
        touch.holdConsumed = true;
        CheonjiinInterpreter.Key phoneKey = CheonjiinInterpreter.Key.valueOf(
            key.stableKeyId().substring("touch.cheonjiin.".length())
                .toUpperCase(java.util.Locale.ROOT));
        emit(key, cheonjiin.flick(phoneKey, direction));
        restartMultiTapTimeout();
        feedback.playKeyDown();
        // Show the same guide with the chosen way lit, so a drag says what it did and which way
        // it went — the letter alone leaves the gesture unexplained.
        touch.guideOpen = true;
        touch.guideDirection = direction;
        flashKeyboard(key, CheonjiinInterpreter.flickLabel(phoneKey, direction));
        invalidate();
        return true;
    }

    /** The 천지인 key this spec drives, or null when it is not one of them. */
    private static CheonjiinInterpreter.Key phoneKeyOf(SoftwareKeySpec key) {
        String id = key.stableKeyId();
        if (!id.startsWith("touch.cheonjiin.")) {
            return null;
        }
        return CheonjiinInterpreter.Key.valueOf(
            id.substring("touch.cheonjiin.".length()).toUpperCase(java.util.Locale.ROOT));
    }

    /** Types a key's held alternate — the digit on a 천지인 key, whatever it is elsewhere. */
    private void typeLongPress(SoftwareKeySpec key) {
        if (!key.hasLongPress()) {
            return;
        }
        sink.accept(key.longPressEvent(0));
        resetPhoneInterpreters();
        consumeOneShotShift();
        feedback.playKeyDown();
        flashKeyboard(key, key.longPressTexts().get(0));
        performClick();
    }

    /** Whether a finger has left its key's cell by more than a touch slop. */
    private boolean escapedKey(KeyboardLayout layout, Touch touch, float x, float y) {
        SoftwareKeySpec key = layout.rows().get(touch.row).get(touch.key);
        int startColumn = layout.startColumn(touch.row, touch.key);
        return TouchTargeting.escaped(x, y,
            layout.columnEdge(startColumn, getWidth()),
            layout.rowEdge(touch.row, getHeight()),
            layout.columnEdge(startColumn + key.columnSpan(), getWidth()),
            layout.rowEdge(touch.row + 1, getHeight()),
            touchSlopPx);
    }

    /** What the grid of cells depends on: the same keys in the same places means the same value. */
    private String gridSignature() {
        return page + "|" + letterLayoutId + "|" + numpadMode;
    }

    /** Keys that fire again while held: plain text/edit/raw keys, but not controls or layer keys. */
    private static boolean repeatsOnHold(SoftwareKeySpec key) {
        return key.enabled() && !key.isControl()
            && !key.hasLongPress() && !key.hasLongPressControl();
    }

    /** Fires the held key once and schedules the next repeat, until the finger lifts. */
    private void handleRepeat(Touch touch) {
        if (touches.get(touch.pointerId) != touch) {
            return;
        }
        SoftwareKeySpec key = layout().rows().get(touch.row).get(touch.key);
        if (!repeatsOnHold(key)) {
            return;
        }
        if (!emitPhoneKey(key)) {
            sink.accept(pressEventWithModifiers(key));
        }
        touch.repeatFired = true;
        feedback.playKeyDown();
        flashKeyboard(key, null);
        postDelayed(touch.onRepeat, repeatIntervalMs);
    }

    /** Ends this finger's key: it types unless a hold already acted for it. */
    private void endTouch(int pointerId, float x, float y) {
        Touch touch = touches.get(pointerId);
        if (touch == null) {
            return;
        }
        forget(touch);
        invalidate();
        if (touch.holdConsumed || touch.repeatFired) {
            // A hold already acted — shift lock, a layer switch, an alternate, or auto-repeat — so
            // the release must not also fire the tap.
            return;
        }
        if (!touch.grid.equals(gridSignature())) {
            // Another finger switched the page while this one was down; its key is gone.
            return;
        }
        if (touch.guideOpen) {
            // Held, then lifted: whatever the guide was showing under the finger is what types.
            SoftwareKeySpec held = layout().rows().get(touch.row).get(touch.key);
            CheonjiinInterpreter.Flick aimed =
                FlickDirection.of(x - touch.downX, y - touch.downY, flickDistancePx);
            if (aimed == null) {
                typeLongPress(held);
            } else {
                emit(held, cheonjiin.flick(phoneKeyOf(held), aimed));
                restartMultiTapTimeout();
                feedback.playKeyDown();
                flashKeyboard(held, CheonjiinInterpreter.flickLabel(phoneKeyOf(held), aimed));
                performClick();
            }
            return;
        }
        if (tryFlick(layout(), touch, x, y)) {
            // A drag too quick to have reported a move on the way is still a drag.
            return;
        }
        // The finger types the key it is on, which moves are what decide. Where it happens to lift
        // is not a second chance to disagree: a release re-tested against the layout drops the
        // keystroke whenever the fingertip drifted a pixel, and the drift is what people notice.
        SoftwareKeySpec held = layout().rows().get(touch.row).get(touch.key);
        if (held.isControl()) {
            applyControl(held.control());
            flashKeyboard(held, null);
            performClick();
            return;
        }
        if (held.enabled()) {
            if (tryArmedModifierChord(held)) {
                flashKeyboard(held, null);
                performClick();
                return;
            }
            if (!emitPhoneKey(held)) {
                sink.accept(pressEventWithModifiers(held));
            }
            consumeOneShotShift();
            flashKeyboard(held, null);
            performClick();
        }
    }

    /** Drops a finger's timers and its claim on a key. */
    private void forget(Touch touch) {
        removeCallbacks(touch.onHold);
        removeCallbacks(touch.onRepeat);
        touches.remove(touch.pointerId);
    }

    /** The gesture was cancelled: no finger types. */
    private void cancelAllTouches() {
        for (int i = touches.size() - 1; i >= 0; i--) {
            Touch touch = touches.valueAt(i);
            removeCallbacks(touch.onHold);
            removeCallbacks(touch.onRepeat);
        }
        touches.clear();
        invalidate();
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

    private void handleLongPress(Touch touch) {
        if (touches.get(touch.pointerId) != touch) {
            return;
        }
        SoftwareKeySpec key = layout().rows().get(touch.row).get(touch.key);
        if (key.isControl() && key.control() == ControlKey.SHIFT) {
            shiftLayer.toggleLock();
            touch.holdConsumed = true;
            invalidate();
            return;
        }
        if (key.hasLongPressControl()) {
            applyControl(key.longPressControl());
            touch.holdConsumed = true;
            return;
        }
        if (phoneKeyOf(key) != null) {
            // A 천지인 key has four letters around it and a digit under it, so holding one shows
            // what is where and waits. Lift without moving and the digit is what you meant; drag
            // to one of the four and lift, and that is.
            touch.guideOpen = true;
            feedback.playKeyDown();
            invalidate();
            return;
        }
        if (key.hasLongPress()) {
            // Holding a key types its alternate straight away. There is no popup to aim at and
            // nothing to drag to: the finger is already where it needs to be.
            sink.accept(key.longPressEvent(0));
            // The alternate is not part of a 12-key run, so it ends one.
            resetPhoneInterpreters();
            consumeOneShotShift();
            feedback.playKeyDown();
            flashKeyboard(key, key.longPressTexts().get(0));
            touch.holdConsumed = true;
            performClick();
        }
    }

    /**
     * Sends a 12-key press through its interpreter, which answers with the edits it means — a jamo,
     * or a backspace and the jamo that replaces it. Returns false for every other key, which the
     * caller then emits itself.
     */
    private boolean emitPhoneKey(SoftwareKeySpec key) {
        String id = key.stableKeyId();
        if (id.startsWith("touch.cheonjiin.")) {
            emit(key, cheonjiin.press(CheonjiinInterpreter.Key.valueOf(
                id.substring("touch.cheonjiin.".length()).toUpperCase(java.util.Locale.ROOT))));
            restartMultiTapTimeout();
            return true;
        }
        if (id.startsWith("touch.naratgeul.")) {
            emit(key, naratgeul.press(NaratgeulInterpreter.Key.valueOf(
                id.substring("touch.naratgeul.".length()).toUpperCase(java.util.Locale.ROOT))));
            return true;
        }
        // Anything else — space, the period, the commit key — ends the run, so the next tap on a
        // consonant key types its first letter instead of continuing the one before.
        resetPhoneInterpreters();
        return false;
    }

    private void emit(SoftwareKeySpec key, java.util.List<SemanticInput> inputs) {
        for (SemanticInput input : inputs) {
            sink.accept(ProjectKeyEvent.softwareDown(key.stableKeyId(), input));
        }
    }

    private void restartMultiTapTimeout() {
        removeCallbacks(endMultiTap);
        postDelayed(endMultiTap, MULTI_TAP_TIMEOUT_MS);
    }

    /** A 12-key run ends when the layout or page changes, or the editor does. */
    public void resetPhoneInterpreters() {
        removeCallbacks(endMultiTap);
        cheonjiin.reset();
        naratgeul.reset();
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
                // The globe walks the layouts the user enabled, in their order. From another page
                // it just returns to letters, keeping the layout that was last in use.
                if (page == Page.LETTERS) {
                    letterLayoutId = LetterLayouts.next(letterOrder(), letterLayoutId);
                    resetPhoneInterpreters();
                    prefs().edit().putString(KEY_LAST_LETTERS, letterLayoutId.name()).apply();
                    if (onLayoutChanged != null) {
                        onLayoutChanged.accept(letterLayoutId);
                    }
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
                runEditCommand(EditMenuIds.UNDO);
                break;
            case REDO:
                runEditCommand(EditMenuIds.REDO);
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
            case FLOATING_TOGGLE:
                if (onFloatingToggle != null) {
                    onFloatingToggle.run();
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
                    return palette.keyAccent;
                }
                if (shiftLayer.isActive()) {
                    return palette.keyAccentSoft;
                }
            }
            if (control == ControlKey.NUMLOCK && numpadMode == NumpadMode.ARROWS) {
                return palette.keyAccent;
            }
            if (control == ControlKey.FUNCTION_LOCK && numpadMode == NumpadMode.FUNCTIONS) {
                return palette.keyAccent;
            }
            if (armedModifiers.contains(control)) {
                return palette.keyAccent;
            }
        }
        if (!key.enabled() && !key.isControl()) {
            return palette.keyDisabled;
        }
        return palette.keyFace;
    }
}
