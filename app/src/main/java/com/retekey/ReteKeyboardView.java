package com.retekey;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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

    private static final String KEY_LAST_LETTERS = "last_letter_layout";

    /** The Tab key's stable id, which the hold latch paints and addresses its events to. */
    private static final String TAB_KEY_ID = "touch.edit.tab";
    /** The space bar, which is drawn as a bar rather than labelled with a word. */
    private static final String SPACE_KEY_ID = "touch.text.space";
    /** The key that walks the letter layouts; it is captioned with the one it is showing. */
    private static final String LAYOUT_TOGGLE_KEY_ID = "touch.layout.toggle";

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final InputSink sink;
    private final KeyFeedback feedback;
    private final LatchState shiftLayer = new LatchState();
    /** Ctrl, Meta and Alt: tap to arm for one key, hold to lock. */
    private final ModifierLatches modifierLatches = new ModifierLatches();
    /**
     * Whether Tab is currently latched down. Not an armed modifier: the editor has been sent Tab's
     * down half and has not been sent the up, so as far as it knows a finger is still on the key.
     */
    private boolean tabHeld;
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
        // Which press this was, over the whole life of the view: settling types in press order,
        // and the map is keyed by pointer id, which reuse can hand out in any order.
        final int serial = nextTouchSerial++;
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
    private int nextTouchSerial;
    // Held-key auto-repeat (space, enter, backspace, arrows, letters …), configured in settings.
    private boolean repeatEnabled = KeyRepeatSettings.DEFAULT_ENABLED;
    private int repeatDelayMs = KeyRepeatSettings.DEFAULT_DELAY_MS;
    private int repeatIntervalMs = KeyRepeatSettings.DEFAULT_INTERVAL_MS;
    // Held strongly so the weak listener registration in the preferences survives; it applies
    // settings changes (feedback strengths, height) to a keyboard that is already on screen.
    private final SharedPreferences.OnSharedPreferenceChangeListener prefsListener =
        (changed, key) -> reloadPreferences();
    private enum Page { LETTERS, SPECIAL_CHARS, SPECIAL_KEYS, MENU }

    /** What the 12-key pages' own cells show: Hangul, digits, or the cursor cluster. */
    private PhoneOverlay phoneOverlay = PhoneOverlay.NONE;
    /** Whether a code point is being typed, in which case the keys are the hex pad. */
    private boolean unicodeEntry;
    /** What the pad's top strip reads: the digits so far and the character they name. */
    private String unicodePreview = "U+";


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
    private Runnable onUnicodeInput;
    private Runnable onNotepad;
    private Runnable onFloatingToggle;
    private Fn.Consumer<KeyboardLayoutId> onLayoutChanged;
    private final CheonjiinInterpreter cheonjiin = new CheonjiinInterpreter();
    private final NaratgeulInterpreter naratgeul = new NaratgeulInterpreter();
    /** Ends the 12-key multi-tap grouping after a pause, without ending the syllable. */
    private final Runnable endMultiTap = () -> {
        cheonjiin.endMultiTap();
        endCycleRun();
    };
    /** The cycling key a run of taps is currently inside, and how far through it that run is. */
    private String cycleKeyId;
    private int cycleIndex = -1;
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
    /**
     * The height multiplier for the orientation being drawn. NaN until it is first needed: the
     * default depends on the screen and on how many rows the layout has, neither of which the
     * constructor can ask for safely.
     */
    /** The height in percent of the screen; 0 until the first read resolves it. */
    private int heightPercent;
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
        return LetterLayouts.parse(
            OrientedPrefs.getString(prefs(), LetterLayouts.KEY_ORDER, orientation(), null));
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

    /** The height of the screen as it is being held now — what the percentage is a percentage of. */
    private int screenHeightPx() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return KeyboardHeightPrefs.screenHeightPx(orientation(), metrics);
    }

    /** The height in use, resolving it from preferences the first time anything asks. */
    private int heightPercent() {
        if (heightPercent <= 0) {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            heightPercent = KeyboardHeightPrefs.percent(
                prefs(),
                orientation(),
                screenHeightPx(),
                Math.max(metrics.widthPixels, metrics.heightPixels),
                metrics.density);
        }
        return heightPercent;
    }

    /** The current height, as a percentage of the screen. */
    public int keyboardHeightPercent() {
        return heightPercent();
    }

    /** Sets the height in percent of the screen, clamps it, optionally persists it, re-lays out. */
    public void setKeyboardHeightPercent(int percent, boolean persist) {
        int clamped = KeyboardHeightPercent.clamp(percent);
        if (clamped == heightPercent() && !persist) {
            return;
        }
        heightPercent = clamped;
        if (persist) {
            KeyboardHeightPrefs.setPercent(prefs(), orientation(), clamped);
        }
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int desired = KeyboardHeightPercent.heightPx(heightPercent(), screenHeightPx());
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
        if (unicodeEntry) {
            // While a code point is being typed the keys are the digits it is made of; anything
            // else would be a key that cannot be pressed for the thing on screen.
            return KeyboardLayouts.unicodeEntry();
        }
        switch (page) {
            case SPECIAL_CHARS:
                return KeyboardLayouts.specialChars();
            case SPECIAL_KEYS:
                return KeyboardLayouts.specialKeys(numpadMode);
            case MENU:
                return KeyboardLayouts.menu();
            default:
                if (phoneOverlay != PhoneOverlay.NONE
                    && (letterLayoutId == KeyboardLayoutId.KO_CHEONJIIN
                        || letterLayoutId == KeyboardLayoutId.KO_NARATGEUL)) {
                    return KeyboardLayouts.phone(letterLayoutId, phoneOverlay);
                }
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
        // Read the key this orientation actually writes. Reading the un-suffixed one meant every
        // preference change — including the height's own write — put the height back to default,
        // which is why neither the size keys nor the settings slider appeared to do anything.
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int stored = KeyboardHeightPrefs.percent(
            prefs(),
            orientation(),
            screenHeightPx(),
            Math.max(metrics.widthPixels, metrics.heightPixels),
            metrics.density);
        if (stored != heightPercent()) {
            heightPercent = stored;
            requestLayout();
        }
        invalidate();
    }

    /** Clears transient one-shot and pointer state when the editor session changes. */
    public void resetLayerState() {
        shiftLayer.clear();
        modifierLatches.clear();
        releaseTabHoldIfLatched();
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

    /** Which way the device is held; the height and the layouts on offer depend on it. */
    private ScreenOrientation orientation() {
        return OrientedPrefs.current(getContext());
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
            // Redraw the whole key in its pressed shade: the face lifts toward white and a little
            // toward the accent, and the label is painted again on top of it, so the key reads as
            // brighter rather than as covered over.
            drawKey(canvas, key, left, top, right, bottom,
                KeyPressTint.pressed(keyFillColor(key), palette.pressTint));
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
        return page + "|" + letterLayoutId + "|" + nextLayoutCaption() + "|" + numpadMode
            + "|" + phoneOverlay + "|" + unicodeEntry + "|" + unicodePreview
            + "|" + shiftLayer.isActive()
            + "|" + shiftLayer.isLocked() + "|" + modifierLatches.signature() + "|" + tabHeld + "|"
            + KeyboardPalette.isNight(getContext());
    }

    /**
     * What the layout-walking key says: an arrow and the layout it would land you on. The layout
     * in use is already legible on the keys themselves; what the key has to answer is "and if I
     * press you?".
     *
     * <p>The answer is not the same on every page. From the letters the key walks to the next
     * layout; from the symbols, keypad or menu pages the same key is the way back, and it returns
     * the layout you left rather than advancing past it. Naming the next one there was a caption
     * for a press that never happens — the key was right and the label was wrong.
     */
    private String nextLayoutCaption() {
        KeyboardLayoutId destination = page == Page.LETTERS
            ? LetterLayouts.next(letterOrder(), letterLayoutId)
            : letterLayoutId;
        return ">" + LetterLayouts.keyCapName(destination);
    }

    /** The text to paint for a key: its label, or a word when the device has no glyph for it. */
    private String labelOf(SoftwareKeySpec key) {
        if (KeyboardLayouts.UNICODE_DISPLAY_ID.equals(key.stableKeyId())) {
            return unicodePreview;
        }
        if (LAYOUT_TOGGLE_KEY_ID.equals(key.stableKeyId())) {
            return nextLayoutCaption();
        }
        return LegacyGlyphs.label(key.label(), android.os.Build.VERSION.SDK_INT);
    }

    /** Draws one raised, rounded key with its label and long-press hint into the cache canvas. */
    private void drawKey(Canvas canvas, SoftwareKeySpec key,
            int left, int top, int right, int bottom) {
        drawKey(canvas, key, left, top, right, bottom, keyFillColor(key));
    }

    /**
     * One key, in the given face colour. The colour is a parameter so a pressed key can be redrawn
     * whole — face, label and corner mark — in its brighter shade, rather than washed over with a
     * translucent sheet that takes the label down with it.
     */
    private void drawKey(Canvas canvas, SoftwareKeySpec key,
            int left, int top, int right, int bottom, int fill) {
        float l = left + keyGapPx;
        float t = top + keyGapPx;
        float r = right - keyGapPx;
        float b = bottom - keyGapPx;
        // A darker lip just below the face makes the key look raised.
        paint.setColor(palette.keyShadow);
        Compat.drawRoundRect(canvas, l, t + keyShadowPx, r, b + keyShadowPx, keyRadiusPx, paint);
        paint.setColor(fill);
        Compat.drawRoundRect(canvas, l, t, r, b, keyRadiusPx, paint);
        // The ink follows the fill, not the theme: a held key is painted strongly enough that the
        // ordinary label colour would sink into it.
        boolean latched = canBeHeld(key) && isHeld(key);
        // The code-point strip is not a key you cannot press: it is the readout, and the one thing
        // on that pad worth reading. Muting it the way an unusable key is muted hid it.
        boolean readout = KeyboardLayouts.UNICODE_DISPLAY_ID.equals(key.stableKeyId());
        int ink = latched
            ? palette.keyLatchedInk()
            : key.enabled() || key.isControl() || readout
                ? palette.inkOn(fill)
                : palette.keyTextMuted;
        paint.setColor(ink);
        if (SPACE_KEY_ID.equals(key.stableKeyId())) {
            // The space bar says what it is by its shape, the way a space bar always has. A word
            // there is both the longest label on the keyboard and the least necessary one.
            drawSpaceMark(canvas, l, t, r, b);
        } else {
            String label = labelOf(key);
            fitLabel(label, right - left, bottom - top);
            float x = (left + right) * 0.5f;
            float y = top + (bottom - top) * 0.62f;
            if (latched) {
                // An outline around the label, in the face's own strong ink: a held key is the one
                // state that has to carry across a glance, and the weight says so before the
                // colour does.
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(1.5f, (bottom - top) * 0.018f));
                paint.setColor(palette.inkOn(fill));
                canvas.drawText(label, x, y, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(ink);
            }
            canvas.drawText(label, x, y, paint);
        }
        if (key.longPressTexts().size() == 1 || key.hasLongPressHint()) {
            // What a long press reaches, in small text in the top-right of the key's own face:
            // the alternate character for a key that types one, or a letter naming the page for a
            // key that opens one. Inset from the face, not from the cell — drawn against the cell
            // it sat on the very edge and read as if it had slipped out of the key.
            String corner = key.hasLongPressHint()
                ? key.longPressHint()
                : key.longPressTexts().get(0);
            float hint = (bottom - top) * 0.20f;
            float inset = hint * 0.45f;
            paint.setColor(palette.hintOn(fill));
            paint.setTextSize(hint);
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(corner, r - inset, t + inset + hint * 0.85f, paint);
            paint.setTextAlign(Paint.Align.CENTER);
        } else if (canBeHeld(key)) {
            drawLatchMark(canvas, r, t, isHeld(key), palette.hintOn(fill));
        } else if (key.hasLongPress() || key.hasLongPressControl()) {
            paint.setColor(palette.hintOn(fill));
            canvas.drawCircle(r - keyRadiusPx, t + keyRadiusPx, 3.0f, paint);
        }
    }

    /**
     * The space key's mark: the open box that has meant "one space" on printed listings and in
     * type samples for as long as there have been either — a box with its top side missing. Drawn
     * rather than typed, because the character for it (U+2423) is missing from the fonts on the
     * older devices this keyboard is built to run on, and a key that draws nothing is worse than
     * one that draws a word.
     */
    private void drawSpaceMark(Canvas canvas, float l, float t, float r, float b) {
        float width = Math.min((r - l) * 0.30f, (b - t) * 0.62f);
        float height = width * 0.52f;
        float cx = (l + r) * 0.5f;
        float cy = (t + b) * 0.5f;
        float left = cx - width * 0.5f;
        float right = cx + width * 0.5f;
        float top = cy - height * 0.5f;
        float bottom = cy + height * 0.5f;
        float stroke = Math.max(2.0f, height * 0.16f);
        Paint.Style style = paint.getStyle();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        Path box = new Path();
        box.moveTo(left, top);
        box.lineTo(left, bottom);
        box.lineTo(right, bottom);
        box.lineTo(right, top);
        canvas.drawPath(box, paint);
        paint.setStyle(style);
    }

    /**
     * The corner mark on a key that can be held without a finger on it: an open ring while it is
     * not, a filled disc while it is. The two read as one shape in two states, which is what the
     * key is — the background colour says the same thing, and a mark says it again for anyone the
     * colour does not reach.
     */
    private void drawLatchMark(Canvas canvas, float right, float top, boolean locked, int ink) {
        float cx = right - 11.0f;
        float cy = top + 11.0f;
        paint.setColor(ink);
        if (locked) {
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy, 4.0f, paint);
            return;
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        canvas.drawCircle(cx, cy, 3.5f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    /** Whether holding this key keeps it down: Shift, the three modifiers, and Tab. */
    private static boolean canBeHeld(SoftwareKeySpec key) {
        if (key.isControl()) {
            return key.control() == ControlKey.SHIFT || ModifierLatches.handles(key.control());
        }
        return TAB_KEY_ID.equals(key.stableKeyId());
    }

    /** Whether it is being held right now — locked, not merely armed for the next key. */
    private boolean isHeld(SoftwareKeySpec key) {
        if (!key.isControl()) {
            return tabHeld;
        }
        if (key.control() == ControlKey.SHIFT) {
            return shiftLayer.isLocked();
        }
        return modifierLatches.isLocked(key.control());
    }

    /** Sizes {@link #paint} so {@code label} fits a cell of the given size, tracking cell size. */
    private void fitLabel(String label, int cellWidth, int cellHeight) {
        float cap = cellHeight * KeyLabelFit.HEIGHT_RATIO;
        float minSize = 8.5f * getResources().getDisplayMetrics().density;
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
        // A new finger settles every letter still riding on an earlier one. Keys type on release
        // so a fingertip can slide to its neighbour, but in a fast roll the fingers can lift out
        // of press order — and the 12-key automata read 획추가-before-ㄱ as a stroke with nothing
        // to act on, so the stroke is lost and the consonant lands plain. Typing the earlier key
        // now fixes the order at the last moment it is still known. Control keys stay held: a
        // modifier chord is two fingers down at once, and must remain one.
        settlePendingTaps();
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
            || (key.isControl()
                && (key.control() == ControlKey.SHIFT
                    || ModifierLatches.handles(key.control())))) {
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
        if (isCycleKey(key)) {
            CheonjiinInterpreter.Flick sideways =
                FlickDirection.of(x - touch.downX, y - touch.downY, flickDistancePx);
            if (sideways != CheonjiinInterpreter.Flick.LEFT
                && sideways != CheonjiinInterpreter.Flick.RIGHT) {
                // Up and down mean nothing on these keys, so the press stays a tap.
                return false;
            }
            removeCallbacks(touch.onHold);
            removeCallbacks(touch.onRepeat);
            touch.holdConsumed = true;
            typeCyclePick(key, sideways == CheonjiinInterpreter.Flick.RIGHT);
            feedback.playKeyDown();
            flashKeyboard(key, null);
            return true;
        }
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
        typeFlick(key, phoneKey, direction);
        // Show the same guide with the chosen way lit, so a drag says what it did and which way
        // it went — the letter alone leaves the gesture unexplained. An empty cell still consumes
        // the press: a drag that points at nothing types nothing, rather than the key's own letter.
        touch.guideOpen = true;
        touch.guideDirection = direction;
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

    /**
     * Types what dragging {@code direction} off this 천지인 key means, and says whether it meant
     * anything. A direction with no letter behind it — above a consonant, or below one whose group
     * has no tense letter — types nothing, which is why the guide leaves that cell empty.
     */
    private boolean typeFlick(SoftwareKeySpec key, CheonjiinInterpreter.Key phoneKey,
            CheonjiinInterpreter.Flick direction) {
        String label = CheonjiinInterpreter.flickLabel(phoneKey, direction);
        if (label == null) {
            return false;
        }
        emit(key, cheonjiin.flick(phoneKey, direction));
        restartMultiTapTimeout();
        feedback.playKeyDown();
        flashKeyboard(key, label);
        performClick();
        return true;
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
        return page + "|" + letterLayoutId + "|" + numpadMode + "|" + phoneOverlay
            + "|" + unicodeEntry;
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
                typeFlick(held, phoneKeyOf(held), aimed);
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
            typeTapped(held);
        }
    }

    /** Types an enabled, non-control key: the armed chord, the 12-key run, or the plain press. */
    private void typeTapped(SoftwareKeySpec held) {
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

    /**
     * Types every finger still waiting to type on release, in the order it went down, and spends
     * its press so the release types nothing more. Fingers whose press already acted — a hold, a
     * repeat, an open flick guide — and fingers on control keys are left exactly as they are.
     */
    private void settlePendingTaps() {
        String grid = gridSignature();
        java.util.List<Touch> pending = new java.util.ArrayList<>(touches.size());
        for (int i = 0; i < touches.size(); i++) {
            pending.add(touches.valueAt(i));
        }
        java.util.Collections.sort(pending, (a, b) -> Integer.compare(a.serial, b.serial));
        for (Touch touch : pending) {
            if (touch.holdConsumed || touch.repeatFired || touch.guideOpen
                || !touch.grid.equals(grid)) {
                continue;
            }
            SoftwareKeySpec held = layout().rows().get(touch.row).get(touch.key);
            if (held.isControl() || !held.enabled()) {
                continue;
            }
            removeCallbacks(touch.onHold);
            removeCallbacks(touch.onRepeat);
            touch.holdConsumed = true;
            typeTapped(held);
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
        Set<KeyModifier> mods = modifierLatches.active();
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
        consumeOneShotModifiers();
        consumeOneShotShift();
        invalidate();
        return true;
    }

    /**
     * Presses Tab and leaves it pressed, or lets it up again. A tap on Tab types one and is over;
     * this is the other thing a finger can do to a key, and a keyboard drawn on glass has no way
     * to keep one down, so the hold toggles it. No modifier is folded in: an armed Ctrl stays
     * armed for whatever key comes next rather than being spent on the latch.
     */
    private void toggleTabHold() {
        tabHeld = !tabHeld;
        sink.accept(ProjectKeyEvent.softwareDown(
            TAB_KEY_ID,
            SemanticInput.rawKey(
                RawKey.TAB,
                EnumSet.noneOf(KeyModifier.class),
                tabHeld ? RawKeyPhase.HOLD : RawKeyPhase.RELEASE)));
    }

    /** Lets a latched Tab up, so a held key cannot outlive the editor it was held in. */
    private void releaseTabHoldIfLatched() {
        if (tabHeld) {
            toggleTabHold();
        }
    }

    /** Folds the armed Ctrl/Meta/Alt into a raw key so it forms a chord; other keys are unchanged. */
    private ProjectKeyEvent pressEventWithModifiers(SoftwareKeySpec key) {
        SemanticInput input = key.semanticInput();
        Set<KeyModifier> mods = modifierLatches.active();
        if (input.kind() != SemanticInput.Kind.RAW_KEY || mods.isEmpty()) {
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
        if (key.isControl() && ModifierLatches.handles(key.control())) {
            // Holding a modifier keeps it down until it is held again, the way a finger would.
            modifierLatches.hold(key.control());
            touch.holdConsumed = true;
            feedback.playKeyDown();
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
    /** Whether this key holds several characters and cycles through them as it is tapped. */
    private static boolean isCycleKey(SoftwareKeySpec key) {
        return key.stableKeyId().startsWith("touch.phone.cycle.");
    }

    /** Forgets which cycling key was mid-run, so the next tap starts its label again. */
    private void endCycleRun() {
        cycleKeyId = null;
        cycleIndex = -1;
    }

    /**
     * Types a cycling key. A tap that lands while this same key's run is still open takes back the
     * character it typed and puts the next one in its place; anything else starts the label again.
     */
    private void typeCycle(SoftwareKeySpec key) {
        List<String> characters = MultiTapCycle.charactersOf(key.label());
        boolean runIsOpen = key.stableKeyId().equals(cycleKeyId) && cycleIndex >= 0;
        MultiTapCycle.Step step = MultiTapCycle.press(characters, cycleIndex, runIsOpen);
        emitCycleStep(key, step);
        cycleKeyId = key.stableKeyId();
        cycleIndex = step.index;
        restartMultiTapTimeout();
    }

    /** Types the character a drag picked, and ends the run: a drag chooses rather than cycles. */
    private void typeCyclePick(SoftwareKeySpec key, boolean rightwards) {
        emitCycleStep(key, MultiTapCycle.pick(MultiTapCycle.charactersOf(key.label()), rightwards));
        endCycleRun();
        removeCallbacks(endMultiTap);
    }

    private void emitCycleStep(SoftwareKeySpec key, MultiTapCycle.Step step) {
        List<SemanticInput> inputs = new java.util.ArrayList<>(2);
        if (step.replacesPrevious) {
            inputs.add(SemanticInput.deleteForCorrection());
        }
        inputs.add(SemanticInput.text(step.character));
        emit(key, inputs);
    }

    private boolean emitPhoneKey(SoftwareKeySpec key) {
        if (isCycleKey(key)) {
            // The Hangul run ends — a period is not part of the syllable being spelled — but this
            // key's own run carries on, which is what lets a second tap turn . into ,
            cheonjiin.reset();
            naratgeul.reset();
            typeCycle(key);
            return true;
        }
        String id = key.stableKeyId();
        if (id.startsWith("touch.cheonjiin.")) {
            emit(key, cheonjiin.press(CheonjiinInterpreter.Key.valueOf(
                id.substring("touch.cheonjiin.".length()).toUpperCase(java.util.Locale.ROOT))));
            restartMultiTapTimeout();
            return true;
        }
        if (id.startsWith("touch.naratgeul.")) {
            NaratgeulInterpreter.Key phoneKey = NaratgeulInterpreter.Key.valueOf(
                id.substring("touch.naratgeul.".length()).toUpperCase(java.util.Locale.ROOT));
            java.util.List<SemanticInput> edits = naratgeul.press(phoneKey);
            if (edits.isEmpty() && (phoneKey == NaratgeulInterpreter.Key.STROKE
                || phoneKey == NaratgeulInterpreter.Key.TWIN)) {
                // The run is broken — a restart, a hardware key, a layer switch — but the letter
                // is still on screen. The processor resolves the transform against what is
                // actually there: the composing syllable, or the character before the cursor.
                edits = java.util.Collections.singletonList(SemanticInput.transform(
                    phoneKey == NaratgeulInterpreter.Key.STROKE
                        ? SemanticInput.Transform.STROKE
                        : SemanticInput.Transform.TWIN));
            }
            emit(key, edits);
            return true;
        }
        // Anything else — space, the commit key, a layer key — ends the run, so the next tap on a
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
        endCycleRun();
    }

    /** The toggle's own key turns its overlay on and off; the other key switches straight over. */
    private void togglePhoneOverlay(PhoneOverlay overlay) {
        phoneOverlay = phoneOverlay == overlay ? PhoneOverlay.NONE : overlay;
        // The Hangul run cannot continue across the pad changing meaning under the fingers.
        resetPhoneInterpreters();
        invalidate();
    }

    private void consumeOneShotModifiers() {
        if (modifierLatches.consumeOneShots()) {
            invalidate();
        }
    }

    private void consumeOneShotShift() {
        if (shiftLayer.consumeOneShot()) {
            invalidate();
        }
    }

    /**
     * Puts the view into one named state so a picture can be taken of it. Used only by the debug
     * build's screenshot screen: the README's images have to come from the drawing code itself
     * rather than from a mock-up, and the emulator this project is developed on never gives an IME
     * window a drawing surface, so photographing the real keyboard means drawing it into an
     * ordinary window instead.
     *
     * <p>The spec is a colon-separated name — {@code letters:KO_DUBEOLSIK}, {@code chars},
     * {@code keys:ARROWS}, {@code menu}, {@code unicode}, {@code phone:KO_CHEONJIIN:DIGITS} — so
     * the debug screen can name every page without this class exposing its private state.
     */
    void showPreview(String spec) {
        String[] parts = spec.split(":");
        unicodeEntry = false;
        phoneOverlay = PhoneOverlay.NONE;
        switch (parts[0]) {
            case "chars":
                page = Page.SPECIAL_CHARS;
                break;
            case "keys":
                page = Page.SPECIAL_KEYS;
                numpadMode = parts.length > 1 ? NumpadMode.valueOf(parts[1]) : NumpadMode.NUMBERS;
                break;
            case "menu":
                page = Page.MENU;
                break;
            case "unicode":
                unicodeEntry = true;
                // A picture of an empty pad says nothing about what it is for, so it is shown
                // mid-entry, on a character no keyboard hands you.
                unicodePreview = parts.length > 1 ? parts[1] : "U+2318   \u2318";
                break;
            case "phone":
                page = Page.LETTERS;
                letterLayoutId = KeyboardLayoutId.valueOf(parts[1]);
                phoneOverlay = parts.length > 2
                    ? PhoneOverlay.valueOf(parts[2]) : PhoneOverlay.NONE;
                break;
            default:
                page = Page.LETTERS;
                if (parts.length > 1) {
                    letterLayoutId = KeyboardLayoutId.valueOf(parts[1]);
                }
                break;
        }
        requestLayout();
        invalidate();
    }

    /** What the hex pad's strip shows: the code typed so far, and the character it names. */
    public void setUnicodePreview(String text) {
        if (unicodePreview.equals(text)) {
            return;
        }
        unicodePreview = text;
        invalidate();
        requestLayout();
    }

    /** Shows or hides the hex pad the U+ entry types on. */
    public void setUnicodeEntry(boolean active) {
        if (unicodeEntry == active) {
            return;
        }
        unicodeEntry = active;
        unicodePreview = "U+";
        cancelAllTouches();
        requestLayout();
        invalidate();
    }

    /** Opens the notepad panel, which the service owns. */
    public void setOnNotepad(Runnable listener) {
        this.onNotepad = listener;
    }

    /** Opens the U+ code-point entry, which the service owns. */
    public void setOnUnicodeInput(Runnable listener) {
        this.onUnicodeInput = listener;
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
                    phoneOverlay = PhoneOverlay.NONE;
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
            case NOTEPAD:
                if (onNotepad != null) {
                    onNotepad.run();
                }
                break;
            case UNICODE_INPUT:
                if (onUnicodeInput != null) {
                    onUnicodeInput.run();
                }
                break;
            case PHONE_DIGITS:
                togglePhoneOverlay(PhoneOverlay.DIGITS);
                break;
            case PHONE_NAV:
                togglePhoneOverlay(PhoneOverlay.NAV);
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
                setKeyboardHeightPercent(
                    heightPercent() + KeyboardHeightPercent.STEP_PERCENT, true);
                break;
            case HEIGHT_DOWN:
                setKeyboardHeightPercent(
                    heightPercent() - KeyboardHeightPercent.STEP_PERCENT, true);
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
                // A tap arms it for one key; a hold locks it. Both are view-local: what reaches
                // the editor is the chord the next key makes.
                modifierLatches.tap(control);
                break;
            case TAB_HOLD:
                toggleTabHold();
                break;
            default:
                break;
        }
        invalidate();
    }

    /**
     * A key's face. A key that is <em>held</em> — locked down until it is pressed again, rather
     * than armed for the next keystroke — is drawn inverted: the face takes the ink colour and the
     * label takes the face's, which is the strongest thing a two-colour key can say and reads at a
     * glance from across the keyboard. Armed-for-one-key keeps the softer accent, so the two
     * states cannot be mistaken for each other.
     */
    private int keyFillColor(SoftwareKeySpec key) {
        if (key.isControl()) {
            ControlKey control = key.control();
            if (control == ControlKey.SHIFT) {
                if (shiftLayer.isLocked()) {
                    return palette.keyLatchedFace();
                }
                if (shiftLayer.isActive()) {
                    return palette.keyAccent;
                }
            }
            if (control == ControlKey.NUMLOCK && numpadMode == NumpadMode.NUMBERS) {
                // Num lock on means digits, as on any keyboard: the pad starts locked, and it is
                // turning it off that reaches the arrows.
                return palette.keyAccent;
            }
            if (control == ControlKey.FUNCTION_LOCK && numpadMode == NumpadMode.FUNCTIONS) {
                return palette.keyAccent;
            }
            if (modifierLatches.isLocked(control)) {
                return palette.keyLatchedFace();
            }
            if (modifierLatches.isActive(control)) {
                return palette.keyAccent;
            }
        }
        if (tabHeld && TAB_KEY_ID.equals(key.stableKeyId())) {
            // Tab latched down is held, not armed, so it inverts like the modifiers do.
            return palette.keyLatchedFace();
        }
        if (!key.enabled() && !key.isControl()) {
            return palette.keyDisabled;
        }
        return palette.keyFace;
    }
}
