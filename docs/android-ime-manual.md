# Android IME Implementation Manual

**English** · [한국어](android-ime-manual.ko.md)

A manual for building an Android input method: how an IME is put together, a minimal working one you
can copy, the reference implementations worth reading, and then the parts that are hard — the
editor contract, hardware keys, drawing, theming — each with the failures that shaped them.

It covers only IME implementation, and it is written against real, shipping code: every rule here
was paid for in this project.

> **This is a living document.** It is updated whenever the IME implementation changes — a new
> callback, a changed editor interaction, a new pitfall found on a device. If behaviour here no
> longer matches the code, the document is the thing that is wrong.

## Table of contents

- [1. How an Android IME is put together](#1-how-an-android-ime-is-put-together)
  - [1.1 The moving parts](#11-the-moving-parts)
  - [1.2 How the system finds, enables, and selects an IME](#12-how-the-system-finds-enables-and-selects-an-ime)
  - [1.3 The data flow](#13-the-data-flow)
  - [1.4 A worked component map](#14-a-worked-component-map)
- [2. A minimal working IME](#2-a-minimal-working-ime)
- [3. Reference implementations and where to get them](#3-reference-implementations-and-where-to-get-them)
- [4. The service lifecycle](#4-the-service-lifecycle)
- [5. The InputConnection contract](#5-the-inputconnection-contract)
- [6. The editor is authoritative](#6-the-editor-is-authoritative)
- [7. Editor kinds and unknown selections](#7-editor-kinds-and-unknown-selections)
- [8. Composing text](#8-composing-text)
- [9. Hardware keyboards](#9-hardware-keyboards)
- [10. The candidates view](#10-the-candidates-view)
- [11. Drawing a custom keyboard](#11-drawing-a-custom-keyboard)
- [12. Theming](#12-theming)
- [13. Settings and persistence](#13-settings-and-persistence)
- [14. Testing and verification](#14-testing-and-verification)
- [15. Anti-patterns, with the failures that taught them](#15-anti-patterns-with-the-failures-that-taught-them)
- [16. Pre-release checklist](#16-pre-release-checklist)

## 1. How an Android IME is put together

### 1.1 The moving parts

An IME is a **service**, not an app screen. Everything else hangs off it.

| Part | What it is | Required? |
|---|---|---|
| `InputMethodService` subclass | the IME itself: lifecycle, editor access, key handling | yes |
| Manifest `<service>` entry | declares the service with `BIND_INPUT_METHOD` and the `android.view.InputMethod` action | yes |
| `res/xml/method.xml` | IME metadata: label, settings activity, subtypes (languages) | yes |
| Input view | the on-screen keyboard, from `onCreateInputView()` | no — a hardware-only IME can omit it |
| Candidates view | a strip above the keyboard, from `onCreateCandidatesView()` | no |
| Settings activity | ordinary `Activity`, linked from `method.xml` | no, but expected |
| Launcher activity | to help the user enable/select the IME | no, but expected |

The system side you talk to:

- **`InputConnection`** — your only handle on the app's text. Obtained per-callback with
  `getCurrentInputConnection()`.
- **`EditorInfo`** — what the focused field says about itself (input type, IME action, initial
  selection). Delivered in `onStartInput`.
- **`InputMethodManager`** — the system service, for things like showing the IME picker.

Two things surprise people coming from app development:

1. **You never touch the app's `View`s.** You cannot read the text field directly; everything goes
   through `InputConnection`, which is asynchronous and may fail.
2. **The IME process is separate and long-lived.** It is bound to whichever app has focus, and it
   survives across apps. A crash takes the keyboard away from the *whole system*, which is why
   [§15.5](#155-letting-an-exception-escape) matters so much.

### 1.2 How the system finds, enables, and selects an IME

Because an IME can observe everything the user types, Android gates it behind two explicit user
steps, and **your app cannot perform either of them programmatically**:

1. **Enable** — Settings → *On-screen keyboard* / *Manage keyboards*. Until this happens, the IME
   does not exist as far as the system is concerned.
2. **Select** — make it the current input method (the keyboard picker).

What you *can* do is send the user to the right screen:

```java
// "Manage installed keyboards": the enable step.
startActivity(new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

// "Choose keyboard": the select step.
InputMethodManager imm = getSystemService(InputMethodManager.class);
if (imm != null) {
    imm.showInputMethodPicker();
}
```

Put both on your launcher screen. A first-run user who cannot find these two switches will conclude
the keyboard is broken.

For development, the same steps from a shell:

```sh
adb shell ime list -a              # what the system knows about
adb shell ime enable  com.example.ime/.MyImeService
adb shell ime set     com.example.ime/.MyImeService
```

### 1.3 The data flow

There are two independent input sources, and they arrive at different places:

```
 ┌──────────────┐  touch      ┌───────────────┐
 │  input view  │────────────▶│               │
 └──────────────┘             │               │   commitText / setComposingText
                              │  your IME     │   deleteSurroundingText / sendKeyEvent
 ┌──────────────┐  onKeyDown  │   service     │──────────────────────────────▶ ┌────────┐
 │ hardware key │────────────▶│               │                                │ editor │
 └──────────────┘             │               │◀────────────────────────────── └────────┘
                              └───────────────┘   onStartInput(EditorInfo)
                                                  onUpdateSelection(...)
```

- **Touch** hits your own view; you decide what it means and call `InputConnection`.
- **Hardware keys** arrive at `onKeyDown`/`onKeyUp` **before the app sees them**. Returning `true`
  consumes the key; returning `super.onKeyDown(...)` lets the app have it ([§9](#9-hardware-keyboards)).
- **The editor talks back** only through `onStartInput` (what kind of field this is) and
  `onUpdateSelection` (the cursor moved). Everything else you must ask for, and may not get.

A single soft key press in a real IME therefore travels:

```
touch → key hit-test → semantic event ("jamo ㄱ", "backspace", "raw F5")
      → composer / dispatcher (may hold state)
      → a list of editor actions
      → InputConnection calls
      → (later, maybe) onUpdateSelection
```

Keeping those stages separate is what makes an IME testable, because everything before
"InputConnection calls" can be plain Java ([§14](#14-testing-and-verification)).

### 1.4 A worked component map

ReteKey's structure, as a concrete example of the split above. Names are illustrative; the shape is
the point.

| Layer | Responsibility | Android-free? |
|---|---|---|
| `ReteKeyImeService` | lifecycle, `InputConnection` calls, hardware keys, candidates | no |
| `ReteKeyboardView` | draw the keyboard, hit-test touches, emit semantic events | no |
| `KeyboardPalette` | resolve colours from the system/Material You theme | no |
| `KeyboardLayouts` / `SoftwareKeySpec` | which key sits where, what it means | **yes** |
| `HangulComposer` / `HangulInputProcessor` | the 2-beolsik automaton and its editor actions | **yes** |
| `InputDispatcher` / `TransitionPlan` / `KeyAction` | turn events into an ordered list of edits | **yes** |
| `CheckedEditorExecutor` | run the edits, report what happened | **yes** |
| `InputConnectionEditorBridge` | the only place that touches `InputConnection` | no |
| `InputSessionController` | the passive cursor cache ([§6](#6-the-editor-is-authoritative)) | **yes** |
| `HanjaTable` / `HunumTable` / `HanjaDictionary` | conversion data and lookup | table logic **yes** |
| `SettingsActivity` / `PreviewActivity` | settings and the enable/select helper screen | no |

The "Android-free?" column is the useful one: **roughly 70% of an IME can be pure Java**, and that
is the part where the logic bugs live.

## 2. A minimal working IME

This is a complete, working IME: it shows a one-button keyboard that types "A". Start here and grow.

**`AndroidManifest.xml`**

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:label="@string/app_name">
        <service
            android:name=".MyImeService"
            android:label="@string/ime_name"
            android:permission="android.permission.BIND_INPUT_METHOD"
            android:exported="true">
            <intent-filter>
                <action android:name="android.view.InputMethod" />
            </intent-filter>
            <meta-data
                android:name="android.view.im"
                android:resource="@xml/method" />
        </service>
    </application>
</manifest>
```

`android:permission="android.permission.BIND_INPUT_METHOD"` is what tells the system only it may
bind this service. Without it — or without the exact `android.view.InputMethod` action — your IME
never appears in the keyboard list, with no error message.

**`res/xml/method.xml`**

```xml
<input-method xmlns:android="http://schemas.android.com/apk/res/android"
    android:settingsActivity="com.example.ime.SettingsActivity"
    android:supportsSwitchingToNextInputMethod="true">
    <subtype
        android:label="@string/subtype_korean"
        android:imeSubtypeLocale="ko_KR"
        android:imeSubtypeMode="keyboard"
        android:isAsciiCapable="false" />
    <subtype
        android:label="@string/subtype_english"
        android:imeSubtypeLocale="en_US"
        android:imeSubtypeMode="keyboard"
        android:isAsciiCapable="true" />
</input-method>
```

Subtypes are how the system knows which languages you offer; they drive the globe key and
`onCurrentInputMethodSubtypeChanged`. Mark at least one subtype `isAsciiCapable` or some password
fields will refuse your IME.

**The service**

```java
public class MyImeService extends InputMethodService {

    @Override
    public View onCreateInputView() {
        Button key = new Button(this);
        key.setText("A");
        key.setOnClickListener(v -> commit("A"));
        return key;                       // any View can be the keyboard
    }

    private void commit(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {                 // always possible
            return;
        }
        ic.commitText(text, 1);
    }
}
```

That is a functioning input method. Everything after this point is about doing it *well*: composing
text, hardware keys, not breaking terminals, and not crashing.

**Growing it.** The next three steps, in the order that hurts least:

1. Replace the `Button` with a custom `View` that hit-tests a key grid and emits *semantic* events
   (`"jamo ㄱ"`, `"backspace"`) rather than calling `InputConnection` directly.
2. Put a plain-Java layer between those events and the edits, so it can be unit-tested.
3. Only then add a composer, hardware keys, and candidates.

## 3. Reference implementations and where to get them

Read these before inventing anything. The first two are the ones this project actually leaned on.

**AOSP LatinIME** — the Google Keyboard lineage, the most complete open IME for Android, and the
source of the cursor model in [§6](#6-the-editor-is-authoritative).

```sh
git clone https://android.googlesource.com/platform/packages/inputmethods/LatinIME
```

Worth reading in it: `LatinIME.java` (the service), and `RichInputConnection.java` — its
`mExpectedSelStart` / `mExpectedSelEnd` pair is the passive cursor cache, and its comments are
candid about how often the editor disagrees.

**AOSP `SoftKeyboard` sample** — the small, official "here is the skeleton" IME. Much easier to read
end to end than LatinIME, and the right template for §2.

```sh
git clone https://android.googlesource.com/platform/development
# samples/SoftKeyboard/
```

**Official documentation** (titles given too, because these paths move):

- *Create an input method* — the platform guide:
  <https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method>
- `InputMethodService`:
  <https://developer.android.com/reference/android/inputmethodservice/InputMethodService>
- `InputConnection`:
  <https://developer.android.com/reference/android/view/inputmethod/InputConnection>
- `EditorInfo`:
  <https://developer.android.com/reference/android/view/inputmethod/EditorInfo>
- `InputMethodManager`:
  <https://developer.android.com/reference/android/view/inputmethod/InputMethodManager>

**Reading order that works:** the platform guide for vocabulary → `SoftKeyboard` for the skeleton →
`InputConnection`'s reference page in full (it is short and every paragraph matters) → LatinIME for
the hard parts.

**This project.** ReteKey itself is MIT-licensed and readable end to end; the Hangul automaton and
the Hanja tables are ported from a sibling project and credited in `THIRD_PARTY_NOTICES.md`, which
is also the place to look for the licences of any data you might want to reuse.

## 4. The service lifecycle

The callbacks you will actually implement:

| Callback | When | Use it for |
|---|---|---|
| `onCreate` | service created | process-wide setup |
| `onCreateInputView` | the keyboard view is first needed | build the view, wire callbacks |
| `onCreateCandidatesView` | the candidates strip is first shown | build the candidate UI |
| `onStartInput(info, restarting)` | a new editor is attached | classify the editor, reset state |
| `onStartInputView(info, restarting)` | the keyboard becomes visible | reset transient UI state |
| `onUpdateSelection(...)` | the editor's cursor moved | update your cursor cache |
| `onFinishInputView(finishing)` | the keyboard is hidden | finish composing |
| `onFinishInput` | the editor is detached | finish composing, drop session |
| `onUnbindInput` | the input binding is dropped | drop session |
| `onDestroy` | service torn down | release resources |

Also useful:

- `onEvaluateInputViewShown()` — return `false` to hide the on-screen keyboard (for example when a
  hardware keyboard is attached). Call `super` and combine, do not ignore it.
- `onEvaluateFullscreenMode()` — return `false` unless you really want the landscape full-screen
  extract mode.
- `onCurrentInputMethodSubtypeChanged(subtype)` — the language switched; finish composing
  ([§8](#8-composing-text)).

**Do not assume the order is the tidy one in the documentation.** Measured on API 33, switching to
another IME produces:

```
onUnbindInput → (onDestroy begins) → onFinishInputView → onFinishInput → (onDestroy ends)
```

That is: **`onUnbindInput` arrives before `onFinishInput`**, and the finish callbacks are *nested
inside* `onDestroy`. Code that clears the session marker in `onUnbindInput` will then fail to
attribute its own teardown callbacks. Treat teardown as idempotent: any of these may run first, more
than once, or with the input connection already gone.

Rules:

- Every teardown path must be safe to run twice.
- `getCurrentInputConnection()` may return `null` at any time, including inside teardown. Always
  null-check; never cache the connection across callbacks.
- Finish composing on **all** of `onFinishInput`, `onFinishInputView`, and
  `onCurrentInputMethodSubtypeChanged`.

## 5. The InputConnection contract

Everything you do to the editor goes through `getCurrentInputConnection()`.

| Call | Meaning | Notes |
|---|---|---|
| `commitText(text, 1)` | insert text at the cursor | replaces the selection if one exists |
| `setComposingText(text, 1)` | show an underlined preedit | replaces the previous composing region |
| `finishComposingText()` | make the preedit permanent | no-op when nothing is composing |
| `deleteSurroundingText(before, after)` | delete relative to the cursor | **does not need to know the cursor position** |
| `deleteSurroundingTextInCodePoints(b, a)` | same, Unicode-safe | prefer this when deleting user-visible characters |
| `sendKeyEvent(event)` | deliver a real `KeyEvent` | the only way to send chords and raw keys |
| `performEditorAction(id)` | run Go/Search/Send | from `EditorInfo.imeOptions` |
| `performContextMenuAction(id)` | run select-all/copy/paste/undo | works in rich editors, not terminals |
| `getTextBeforeCursor(n, 0)` | read back text | may return `null`; may be truncated |
| `getSelectedText(0)` | read the selection | may return `null` or throw in some editors |
| `beginBatchEdit()` / `endBatchEdit()` | group edits | use for delete-then-commit so the editor sees one change |

Two properties matter more than the rest:

1. **Insertion and relative deletion are cursor-relative.** `commitText`, `setComposingText`, and
   `deleteSurroundingText` all act at *the editor's* cursor. You never need to know the absolute
   selection to type or to backspace.
2. **Every call can fail or be ignored.** They return `boolean` and may throw. Treat a `false` as
   "this editor did not do it", not as "my state is corrupt".

Replacing a reading with a conversion is a batch:

```java
ic.beginBatchEdit();
try {
    if (deleteLength > 0) {
        ic.deleteSurroundingText(deleteLength, 0);
    }
    ic.commitText(replacement, 1);   // with a live selection this replaces it
} finally {
    ic.endBatchEdit();
}
```

Sending a real key (the fallback for anything semantic that has no better mapping):

```java
private void sendRawKey(int keyCode, int metaState) {
    InputConnection ic = getCurrentInputConnection();
    if (ic == null) {
        return;
    }
    long now = SystemClock.uptimeMillis();
    ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, metaState,
        KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_SOFT_KEYBOARD));
    ic.sendKeyEvent(new KeyEvent(now, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0,
        metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_SOFT_KEYBOARD));
}
```

Always send both `ACTION_DOWN` and `ACTION_UP`, and reuse one `downTime` across the pair, or
repeat-detection in the target app misbehaves.

## 6. The editor is authoritative

This is the single most important design rule, and the most expensive one to learn.

**The editor owns the text and the cursor. The IME's idea of them is a hint that may be wrong at any
moment.** Selection updates arrive late, out of order, coalesced, or not at all. Some editors report
`-1`. Some apply your edit differently from how you predicted.

Therefore:

- Keep a **passive cursor cache**: update it optimistically after your own edit, and let
  `onUpdateSelection` overwrite it unconditionally. This is the AOSP LatinIME model
  (`RichInputConnection.mExpectedSelStart` / `mExpectedSelEnd`).
- **Never** enter a state that refuses input because the cache disagrees with the editor. There must
  be no "desynchronised" state; if you cannot reconcile, adopt what the editor reported and carry on.
- Tolerate unknown: `-1` selections are normal, not an error.

```java
// The whole model. There is no confirmation, no reservation, and no failure state.
@Override
public void onUpdateSelection(int oldSelStart, int oldSelEnd,
                              int newSelStart, int newSelEnd,
                              int candidatesStart, int candidatesEnd) {
    super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
        candidatesStart, candidatesEnd);
    if (newSelStart < 0 || newSelEnd < 0) {
        cursor = EditorBounds.unknown();          // unknown is a normal state
        return;
    }
    cursor = EditorBounds.of(newSelStart, newSelEnd, candidatesStart, candidatesEnd);
}

// After our own edit we may guess, but the guess has no authority.
private void afterOwnEdit(EditorBounds predicted) {
    cursor = predicted;    // overwritten by the next onUpdateSelection, whatever it says
}
```

See [§15.1](#151-a-strict-expectation-ledger-that-can-latch) for what happens when you do the
opposite.

## 7. Editor kinds and unknown selections

Classify the editor once, in `onStartInput`, from `EditorInfo`:

```java
@Override
public void onStartInput(EditorInfo info, boolean restarting) {
    super.onStartInput(info, restarting);
    boolean rawKeyEditor = info != null
        && (info.inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_NULL;
    profile = rawKeyEditor ? EditorProfile.rawKeys() : EditorProfile.richText();
    cursor = (info != null && info.initialSelStart >= 0)
        ? EditorBounds.of(info.initialSelStart, info.initialSelEnd, -1, -1)
        : EditorBounds.unknown();     // terminals land here
}
```

- `TYPE_NULL` — the editor wants **raw key events**, not text edits. Terminal emulators and some
  game/console views do this. Send keys with `sendKeyEvent`.
- Otherwise it is a rich text editor: `commitText` / `setComposingText` are appropriate.

Terminals additionally tend to report `initialSelStart == -1` and never send meaningful
`onUpdateSelection`. **A keyboard that needs to know the selection cannot type in a terminal.**

Rules:

- Never gate insertion or backspace on having a known selection.
- Do not assume `performContextMenuAction` works; terminals ignore it.
- Do not assume the editor has an action; if it has none and is single-line, a real `KEYCODE_ENTER`
  is the correct fallback ([§15.3](#153-refusing-a-raw-enter)).

Also read `EditorInfo.imeOptions` for the Enter key's meaning:

```java
int action = info.imeOptions & EditorInfo.IME_MASK_ACTION;
boolean noEnterAction = (info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;
boolean multiLine = (info.inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0;

if (!noEnterAction && action != EditorInfo.IME_ACTION_NONE) {
    ic.performEditorAction(action);          // Go / Search / Send / Done
} else if (multiLine) {
    ic.commitText("\n", 1);
} else {
    sendRawKey(KeyEvent.KEYCODE_ENTER, 0);   // never do nothing
}
```

## 8. Composing text

A stateful composer (Hangul, pinyin, kana) shows an in-progress syllable with `setComposingText` and
commits it with `finishComposingText` or by committing the next syllable.

A composer step therefore produces an *ordered list* of edits, not one call — this is worth
modelling explicitly, because it is exactly the part that is unit-testable:

```java
// "ㄱ" then "ㅏ" then "ㄴ" then "ㄷ":  가 → 간 → commit("가") + compose("ㄴㄷ"→ 낟) …
List<KeyAction> actions = composer.accept(jamo);
for (KeyAction action : actions) {
    switch (action.kind()) {
        case COMMIT_TEXT:        ic.commitText(action.text(), 1);        break;
        case SET_COMPOSING_TEXT: ic.setComposingText(action.text(), 1);  break;
        case DELETE_BACKWARD:    ic.deleteSurroundingTextInCodePoints(1, 0); break;
        case RAW_KEY:            sendRawKey(action.keyCode(), action.meta()); break;
    }
}
```

**Finish composing at every session boundary.** If the user leaves the field mid-syllable, an
unfinished preedit is stranded — visually underlined text that belongs to no session, which can
later reappear or be deleted unexpectedly.

```java
@Override public void onFinishInput()      { finishComposingInEditor(); /* … */ }
@Override public void onFinishInputView(boolean finishing) { finishComposingInEditor(); reset(); }
@Override public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype s) {
    finishComposingInEditor();   // a language switch must not carry a half-formed jamo across
    reset();
}

private void finishComposingInEditor() {
    InputConnection ic = getCurrentInputConnection();
    if (ic != null) {
        ic.finishComposingText();
    }
}
```

When you need the composing text as ordinary text — for example to convert it — the simplest correct
move is to `finishComposingText()` first, then read it back with `getTextBeforeCursor` and replace
it. That avoids having to model the composing region yourself.

## 9. Hardware keyboards

Hardware keys arrive at `onKeyDown` / `onKeyUp` / `onKeyMultiple` **before** the application sees
them. Whatever you return `true` for, the app never receives.

**Pass modifier chords through.** Modifier keys themselves, and any chord holding Ctrl / Alt / Meta,
are application shortcuts. If the IME swallows them, Ctrl+A / Ctrl+C / Ctrl+V stop working
everywhere:

```java
@Override
public boolean onKeyDown(int keyCode, KeyEvent event) {
    // 1. Keys the user bound to IME functions must be checked FIRST, before the pass-through,
    //    or a binding like Right-Ctrl is delegated away before we ever see it.
    if (event.getRepeatCount() == 0 && handleBoundFunctionKey(keyCode, event)) {
        return true;
    }
    // 2. Modifier keys and Ctrl/Alt/Meta chords belong to the app.
    if (KeyEvent.isModifierKey(keyCode) || event.isCtrlPressed()
            || event.isAltPressed() || event.isMetaPressed()) {
        return super.onKeyDown(keyCode, event);
    }
    // 3. Our own handling (jamo mapping, raw keys, …).
    return handle(keyCode, event) || super.onKeyDown(keyCode, event);
}
```

Shift is deliberately *not* in the pass-through test: Shift+letter is ordinary text that the IME
still composes, and the framework reports the shift meta state on the letter event anyway.

To *send* a chord yourself (a soft Ctrl key, a remapped key), build a real `KeyEvent` with the meta
state and `sendKeyEvent` it. This works in both worlds: a rich editor turns Ctrl+A into select-all
via `onKeyShortcut`, a terminal receives the control code.

```java
sendRawKey(KeyEvent.KEYCODE_B, KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON);
```

Left and right modifiers are distinct key codes (`KEYCODE_CTRL_LEFT` = 113,
`KEYCODE_CTRL_RIGHT` = 114), which is what makes "Right Ctrl toggles the language, Left Ctrl still
does Ctrl+C" possible.

For a lone-modifier binding, capture it on **key up**: on key down you cannot yet tell whether the
user is pressing Ctrl alone or starting Ctrl+Space.

```java
// Capturing a user-assigned shortcut in a settings Activity.
@Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (!capturing) return super.onKeyDown(keyCode, event);
    if (KeyEvent.isModifierKey(keyCode)) return true;         // wait: chord, or lone modifier?
    save(new Binding(modifiersOf(event), keyCode));           // e.g. Shift+Space
    capturing = false;
    return true;
}
@Override public boolean onKeyUp(int keyCode, KeyEvent event) {
    if (capturing && KeyEvent.isModifierKey(keyCode)) {
        save(new Binding(0, keyCode));                        // e.g. Right Ctrl alone
        capturing = false;
        return true;
    }
    return super.onKeyUp(keyCode, event);
}
```

Finally, decide whether the on-screen keyboard should hide while a hardware keyboard is attached:

```java
@Override
public boolean onEvaluateInputViewShown() {
    super.onEvaluateInputViewShown();
    Configuration config = getResources().getConfiguration();
    boolean hardware = config.keyboard != Configuration.KEYBOARD_NOKEYS
        && config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
    return !hardware;
}
```

## 10. The candidates view

`onCreateCandidatesView()` plus `setCandidatesViewShown(true/false)` gives you a strip that is
independent of the keyboard view. This matters because **the candidates view still shows when the
input view is hidden** — which is exactly the case when a hardware keyboard is attached. Conversion
UI belongs here, not in the keyboard view.

The view is created lazily, so populate it in the right order:

```java
@Override
public View onCreateCandidatesView() {
    candidatesView = new CandidatesView(this);
    candidatesView.setOnPick(this::commitCandidate);
    if (pendingItems != null) {                 // shown before the view existed
        candidatesView.show(pendingReading, pendingItems);
    }
    return candidatesView;
}

private void showCandidates(String reading, List<Item> items) {
    pendingReading = reading;
    pendingItems = items;
    setCandidatesViewShown(true);               // may call onCreateCandidatesView now
    if (candidatesView != null) {
        candidatesView.show(reading, items);
    }
    candidatesShown = true;
}
```

Hide the strip when the user types anything else, and on `onStartInput` / `onFinishInputView`, or a
stale strip will outlive the text it referred to. If you support number-key selection, intercept
those keys *before* the "any other key hides the strip" rule.

## 11. Drawing a custom keyboard

A canvas-drawn keyboard redraws on every press. The cost that matters is
*redraw frequency × per-draw complexity*, so keep the per-frame work proportional to what changed.

**Cache the static keyboard.** Render the unpressed keyboard — key shapes, bevels, labels — once
into a `Bitmap`, and on each frame blit the bitmap and draw only the pressed key's overlay:

```java
@Override
protected void onDraw(Canvas canvas) {
    int width = getWidth();
    int height = getHeight();
    if (width <= 0 || height <= 0) {
        return;
    }
    ensureBaseBitmap(width, height);              // rebuilt only when the signature changes
    canvas.drawBitmap(baseBitmap, 0f, 0f, null);
    drawPressFeedback(canvas, width, height);     // one rounded rect
}

private void ensureBaseBitmap(int width, int height) {
    String signature = layoutSignature();
    if (baseBitmap != null && signature.equals(baseSignature)
        && baseBitmap.getWidth() == width && baseBitmap.getHeight() == height) {
        return;                                   // the common case: reuse
    }
    if (baseBitmap != null) {
        baseBitmap.recycle();
    }
    baseBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas cache = new Canvas(baseBitmap);
    cache.drawColor(palette.background);
    for (Key key : layout.keys()) {
        drawKey(cache, key);                      // rounded face + shadow lip + label
    }
    baseSignature = signature;
}

/** Everything the static image depends on — and deliberately NOT the pressed key. */
private String layoutSignature() {
    return page + "|" + layoutId + "|" + numpadMode + "|" + shift.isActive()
        + "|" + shift.isLocked() + "|" + armedModifiers + "|" + isNightMode();
}
```

The raised look itself is two rounded rects, which is cheap enough to bake into the cache:

```java
paint.setColor(palette.keyShadow);                                    // a lip below the face
canvas.drawRoundRect(l, t + shadowPx, r, b + shadowPx, radius, radius, paint);
paint.setColor(keyFillColor(key));                                    // the face on top
canvas.drawRoundRect(l, t, r, b, radius, radius, paint);
```

Other rules:

- Allocate nothing in `onDraw` — no `Paint`, no `Shader`, no boxing. Reuse fields.
- Prefer a 1–2 px offset rounded rect for a raised look over `setShadowLayer`, which blurs and is
  expensive.
- Recycle the cached bitmap in `onDetachedFromWindow`.

Touch handling has its own rule: **a tap counts for the key it started on.** Resolve the key on
`ACTION_DOWN`, and on `ACTION_UP` only commit if the release is still on that key:

```java
case MotionEvent.ACTION_DOWN:  beginHold(event.getX(), event.getY()); return true;
case MotionEvent.ACTION_UP:    releaseHold(event.getX(), event.getY()); return true;
```

If you want a dead zone between keys so near-boundary taps do not hit the neighbour, test the touch
against the key's *visible* face rather than its whole grid cell.

Held-key auto-repeat is a `postDelayed` loop that re-fires the held key and re-posts itself; cancel
it in every path that ends the press, and skip the release-tap if a repeat already fired.

## 12. Theming

Resolve colours from the device theme; do not hardcode them.

```java
static KeyboardPalette resolve(Context context) {
    boolean night = (context.getResources().getConfiguration().uiMode
        & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {                                    // Material You: match the user's own palette
            return night
                ? new KeyboardPalette(
                    color(context, android.R.color.system_neutral1_900),   // background
                    color(context, android.R.color.system_neutral1_700),   // key face
                    color(context, android.R.color.system_accent1_300),    // active key
                    color(context, android.R.color.system_neutral1_50))    // label
                : new KeyboardPalette(
                    color(context, android.R.color.system_neutral1_100),
                    color(context, android.R.color.system_neutral1_50),
                    color(context, android.R.color.system_accent1_600),
                    color(context, android.R.color.system_neutral1_900));
        } catch (RuntimeException useStatic) {
            // fall through
        }
    }
    return night ? DARK : LIGHT;                 // tuned fallback for older versions
}
```

Rules:

- Honour light/dark via `UI_MODE_NIGHT_MASK`.
- Assign colours by Material role — background as surface, keys as an elevated surface, labels as
  on-surface, active keys as primary, press as a primary state layer — rather than picking hues.
- Include the theme in your draw-cache key ([§11](#11-drawing-a-custom-keyboard)) so a light/dark
  switch rebuilds the keyboard.
- Use one palette for every surface you draw: keys, long-press popup, candidate strip.

## 13. Settings and persistence

The settings screen is an ordinary `Activity` named in `method.xml`. The only structural point is
that **the service and the activity are different processes' worth of state in the same process**,
and the keyboard must notice changes:

```java
// In the keyboard view: read on attach, and follow later edits.
private final SharedPreferences.OnSharedPreferenceChangeListener prefsListener =
    (prefs, key) -> reloadPreferences();

@Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    prefs().registerOnSharedPreferenceChangeListener(prefsListener);
    reloadPreferences();
}

@Override protected void onDetachedFromWindow() {
    prefs().unregisterOnSharedPreferenceChangeListener(prefsListener);
    super.onDetachedFromWindow();
}
```

Without the listener, a setting changed while the keyboard is alive only takes effect the next time
the view is created — which users read as "the slider does nothing".

Keep the clamping of stored values in plain Java so it is unit-testable, and clamp on **read** as
well as write; preference files outlive your validation rules.

## 14. Testing and verification

**Keep the input core Android-free.** Event normalisation, layout geometry, the composer automaton,
dictionaries, and settings clamping can all be plain Java, and then they are unit-testable on the
JVM with no device. This is where nearly all real logic bugs are caught.

Also test the **shipped data**, not just fixtures: parse the real dictionary file in a unit test and
assert a few known conversions, so a bad regeneration fails the build.

```java
@Test public void shippedDataConvertsCommonReadingsAndWords() {
    HanjaTable table = HanjaTable.parse(Files.readAllLines(
        Paths.get("src/main/assets/hanja.txt"), StandardCharsets.UTF_8));
    assertTrue(table.candidates("가").contains("家"));
    assertTrue(table.candidates("학교").contains("學校"));   // word entry, longest match
}
```

**Do not trust a headless emulator as an oracle for IME behaviour.** Two failure modes were measured
repeatedly on a headless, no-KVM emulator:

- The IME window reports `Requested w=0 h=0`, `mViewVisibility=GONE`, `mHasSurface=false`, and
  `screencap` returns a blank framebuffer — *even for a build known to work on a real device.* You
  cannot conclude "the keyboard does not show" from this.
- Injected key events (`adb shell input keyevent`) may not reach the IME's `onKeyDown` at all, and
  the result is not even stable between runs.

What a headless emulator *can* prove: the APK installs, the IME registers and can be selected,
activities inflate, and nothing crashes. Useful commands:

```sh
adb shell ime list -a
adb shell settings get secure default_input_method
adb shell dumpsys input_method | grep -E "mInputShown|mServedView|mCurMethodId"
adb logcat -d | grep -E "FATAL|AndroidRuntime"
```

Anything visual or input-interactive must be verified on a real device. When a regression appears,
build a **known-good tag** and install it on the same emulator: if the known-good build produces
identical symptoms, the symptom is the environment, not your change.

## 15. Anti-patterns, with the failures that taught them

### 15.1 A strict expectation ledger that can latch

**What was built.** Every edit reserved an "expectation" in a ledger; `onUpdateSelection` had to
confirm it. A contradiction called `desynchronize()`, which moved the session to a
`DESYNCHRONIZED` state — and every subsequent key was refused from that state.

**What happened.** In a terminal, `onUpdateSelection` reports unknown bounds, which counts as a
contradiction. So: type a few characters, hit the first unknown update, latch, and the keyboard is
dead for the rest of the session. The bug was reported three times as *"it types a few characters
then stops"* and patched three times by closing one door at a time.

**The fix.** Delete the layer. A passive cursor cache with no confirmation, no reservation, and no
failure state. Input can never be refused because of internal bookkeeping.

```java
// Wrong: bookkeeping can refuse input.
if (state == DESYNCHRONIZED) {
    return ExecutionResult.notDispatched(Reason.SESSION_DESYNCHRONIZED);
}

// Right: there is no such state; unknown just means unknown.
cursor = reported.isKnown() ? reported : EditorBounds.unknown();
```

**Rule.** If a code path can answer "I refuse to type because my model is unsure", that path is a
bug. Delete the state that makes the answer possible.

### 15.2 Gating insertion on a known selection

**What was built.** `commitText` and `setComposingText` were rejected with `INVALID_SELECTION` when
the cached bounds were unknown.

**What happened.** Terminals report `-1`, so every keystroke was dropped and a failure toast fired
on each one — the keyboard looked completely broken in that app.

**The fix.** Insertion is cursor-relative and needs no bounds. The same applies to
`deleteSurroundingText`: backspace was also gated on a known selection and also failed in terminals
until the gate was removed entirely.

```java
// Wrong.
if (!bounds.hasSelection()) {
    return refuse(Reason.INVALID_SELECTION);
}
ic.commitText(text, 1);

// Right: the editor knows where its own cursor is.
ic.commitText(text, 1);
```

**Rule.** Only operations that address *absolute* positions may require a known selection — and in
practice you should not have any.

### 15.3 Refusing a raw Enter

**What was built.** Enter resolved to an editor action; for a single-line editor with no action it
produced an empty result and did nothing. Raw Enter was additionally refused for "rich" editors.

**What happened.** Enter did nothing at all in terminals — no newline, no submit.

**The fix.** Fall through to a real `KEYCODE_ENTER` via `sendKeyEvent`. Every editor understands a
real Enter key: a terminal submits the line, a plain field does its default. See the Enter cascade in
[§7](#7-editor-kinds-and-unknown-selections).

**Rule.** For any key with a "semantic" mapping, keep a raw-key fallback. Doing nothing is never the
right answer for a key the user pressed.

### 15.4 Re-anchoring the composer on any prediction mismatch

**What was built.** On `onUpdateSelection`, if the reported cursor differed from the predicted one,
the composer called `finishComposingText()` and reset — intended to re-anchor after the user tapped
elsewhere.

**What happened.** Real editors do not match the prediction exactly. The mismatch fired on the
IME's *own* edits, so every keystroke triggered finish+reset, plausibly re-entering
`onUpdateSelection`, and the IME died: **the keyboard stopped appearing at all.** This shipped and
had to be reverted.

```java
// Wrong: a prediction mismatch is not evidence of anything.
if (!reported.equals(predicted)) {
    ic.finishComposingText();
    composer.reset();
}

// Right: only an unambiguous signal re-anchors — the cursor left the composing region.
if (composer.isComposing() && !composingRegion.contains(reported.start())) {
    ic.finishComposingText();
    composer.reset();
}
```

**Rule.** Never drive destructive state changes from a prediction mismatch. Predictions are hints;
only unambiguous evidence should reset user-visible state.

### 15.5 Letting an exception escape

**What happened.** An exception thrown while handling a key or a selection update killed the IME
process, and the keyboard vanished mid-typing — for every app, not just the one that misbehaved.

**The fix.** Wrap the editor-facing entry points and degrade instead of propagating:

```java
try {
    execute(dispatcher.dispatch(event));
} catch (RuntimeException crash) {
    dispatcher.reset();
    inputProcessor.reset();   // lose the half-formed syllable, keep the keyboard alive
}
```

**Rule.** A misbehaving editor must never be able to take the keyboard down. Losing a syllable is
acceptable; losing the keyboard is not.

### 15.6 Swallowing application shortcuts

**What happened.** The IME consumed modifier chords, so Ctrl+A / Ctrl+C / Ctrl+V did nothing in any
app while the keyboard was active.

**The fix.** Delegate modifier keys and Ctrl/Alt/Meta chords to `super.onKeyDown` — except the
specific chords the user has bound to IME functions, which must be checked first
([§9](#9-hardware-keyboards)).

### 15.7 Mapping a soft modifier to editor commands only

**What was built.** A soft Ctrl combined with a letter was mapped to context-menu actions, but only
for `a/c/v/x/z/y`. Every other letter fell through and was typed as text.

**What happened.** In a terminal, Ctrl+B inserted the literal character `b` instead of sending
`0x02`.

**The fix.** Send a real key chord for *any* letter. Rich editors handle Ctrl+A/C/V/X/Z/Y through
`onKeyShortcut`, and terminals get their control codes — one mechanism instead of a special-case
table.

```java
// Wrong: a table of effects that only covers what you thought of.
int id = contextMenuIdFor(letter);      // a→selectAll, c→copy, … else 0
if (id != 0) ic.performContextMenuAction(id); else ic.commitText(letter, 1);

// Right: emit the real event and let each editor interpret it.
sendRawKey(keyCodeForLetter(letter), KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON);
```

**Rule.** Prefer emitting the real input event over simulating its effect. The event works in more
places than your table of effects.

### 15.8 Rebuilding the whole keyboard image per keystroke

**What it costs.** Gradients, shadows, and text for 30–40 keys recomputed on every press, on the UI
thread, for a change that affects one key.

**The fix.** [§11](#11-drawing-a-custom-keyboard) — cache the static image, redraw one key. Keep the
pressed key out of the cache key, or you have cached nothing.

### 15.9 Hardcoding colours

**What happened.** A fixed palette looked wrong the moment the system switched to dark mode, and
ignored the user's theme entirely.

**The fix.** [§12](#12-theming) — resolve from the theme, and include the theme in the cache key.

### 15.10 Trusting the emulator

**What happened.** A headless emulator showed a zero-sized, never-rendered IME window and dropped
injected key events, which was mistaken for a code regression. Building a known-good tag and
installing it on the same emulator reproduced the identical symptoms, proving the environment was at
fault.

**Rule.** Before concluding "the keyboard is broken", reproduce with a build that is known to work.
See [§14](#14-testing-and-verification).

## 16. Pre-release checklist

- [ ] The IME appears in the keyboard list (manifest permission, action, and `method.xml` correct).
- [ ] No code path refuses input because of internal cursor bookkeeping.
- [ ] Typing, backspace, and Enter all work in a **terminal** app (`TYPE_NULL`, selection `-1`).
- [ ] Composing is finished in `onFinishInput`, `onFinishInputView`, and on subtype change.
- [ ] Ctrl/Alt/Meta chords still reach the app; bound IME shortcuts are intercepted before that.
- [ ] Key handlers cannot throw out of the service.
- [ ] The draw cache key covers layout, shift, modifiers, size, and theme — but not the pressed key.
- [ ] Light and dark themes both render correctly, and switching rebuilds the keyboard.
- [ ] Settings changes take effect on the live keyboard, not just after a restart.
- [ ] Unit tests cover the Android-free core **and** parse the shipped data files.
- [ ] Anything visual or input-interactive was verified on a real device, not an emulator.
- [ ] This manual was updated for whatever changed.
