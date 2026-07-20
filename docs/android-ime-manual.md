# Android IME Implementation Manual

**English** · [한국어](android-ime-manual.ko.md)

Practical guidance for building an Android input method, written from the mistakes made while
building ReteKey. It covers only IME implementation. Every section that describes a rule also
records the failure that produced it, because the failure is the part that is hard to re-derive.

> **This is a living document.** It is updated whenever the IME implementation changes — a new
> callback, a changed editor interaction, a new pitfall found on a device. If behaviour here no
> longer matches the code, the document is the thing that is wrong.

## Table of contents

- [1. The service lifecycle](#1-the-service-lifecycle)
- [2. The InputConnection contract](#2-the-inputconnection-contract)
- [3. The editor is authoritative](#3-the-editor-is-authoritative)
- [4. Editor kinds and unknown selections](#4-editor-kinds-and-unknown-selections)
- [5. Composing text](#5-composing-text)
- [6. Hardware keyboards](#6-hardware-keyboards)
- [7. The candidates view](#7-the-candidates-view)
- [8. Drawing a custom keyboard](#8-drawing-a-custom-keyboard)
- [9. Theming](#9-theming)
- [10. Testing and verification](#10-testing-and-verification)
- [11. Anti-patterns, with the failures that taught them](#11-anti-patterns-with-the-failures-that-taught-them)
- [12. Pre-release checklist](#12-pre-release-checklist)

## 1. The service lifecycle

An IME extends `InputMethodService`. The callbacks you will actually implement:

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
  `onCurrentInputMethodSubtypeChanged` (see [§5](#5-composing-text)).

## 2. The InputConnection contract

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

## 3. The editor is authoritative

This is the single most important design rule, and the most expensive one to learn.

**The editor owns the text and the cursor. The IME's idea of them is a hint that may be wrong at any
moment.** Selection updates arrive late, out of order, coalesced, or not at all. Some editors report
`-1`. Some apply your edit differently from how you predicted.

Therefore:

- Keep a **passive cursor cache**: update it optimistically after your own edit, and let
  `onUpdateSelection` overwrite it unconditionally. This is the AOSP LatinIME model.
- **Never** enter a state that refuses input because the cache disagrees with the editor. There must
  be no "desynchronised" state; if you cannot reconcile, adopt what the editor reported and carry on.
- Tolerate unknown: `-1` selections are normal, not an error.

```java
// Right: reconcile by adopting what the editor says.
void onUpdateSelection(int oldStart, int oldEnd, int newStart, int newEnd, int csStart, int csEnd) {
    if (newStart < 0 || newEnd < 0) {
        cursor = EditorBounds.unknown();   // unknown is a normal state
        return;
    }
    cursor = EditorBounds.of(newStart, newEnd, csStart, csEnd);
}
```

See [§11.1](#111-a-strict-expectation-ledger-that-can-latch) for what happens when you do the
opposite.

## 4. Editor kinds and unknown selections

Classify the editor once, in `onStartInput`, from `EditorInfo`:

- `inputType == InputType.TYPE_NULL` — the editor wants **raw key events**, not text edits. Terminal
  emulators and some game/console views do this. Send keys with `sendKeyEvent`.
- Otherwise it is a rich text editor: `commitText` / `setComposingText` are appropriate.

Terminals additionally tend to report `initialSelStart == -1` and never send meaningful
`onUpdateSelection`. **A keyboard that needs to know the selection cannot type in a terminal.**

Rules:

- Never gate insertion or backspace on having a known selection.
- Do not assume `performContextMenuAction` works; terminals ignore it.
- Do not assume the editor has an "action"; if it has none and is single-line, a real
  `KEYCODE_ENTER` is the correct fallback (see [§11.3](#113-refusing-a-raw-enter)).

## 5. Composing text

A stateful composer (Hangul, pinyin, kana) shows an in-progress syllable with `setComposingText` and
commits it with `finishComposingText` or by committing the next syllable.

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

## 6. Hardware keyboards

Hardware keys arrive at `onKeyDown` / `onKeyUp` / `onKeyMultiple` **before** the application sees
them. Whatever you return `true` for, the app never receives.

**Pass modifier chords through.** Modifier keys themselves, and any chord holding Ctrl / Alt / Meta,
are application shortcuts. If the IME swallows them, Ctrl+A / Ctrl+C / Ctrl+V stop working
everywhere:

```java
static boolean passThroughToApp(boolean isModifierKey, boolean ctrl, boolean alt, boolean meta) {
    return isModifierKey || ctrl || alt || meta;   // Shift is excluded: Shift+letter is text
}
```

To *send* a chord yourself (a soft Ctrl key, a remapped key), build a real `KeyEvent` with the meta
state and `sendKeyEvent` it. This works in both worlds: a rich editor turns Ctrl+A into select-all
via `onKeyShortcut`, a terminal receives the control code.

```java
int meta = KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
ic.sendKeyEvent(new KeyEvent(down, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_B, 0, meta));
ic.sendKeyEvent(new KeyEvent(down, now, KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_B, 0, meta));
```

If you let the user assign physical keys to IME functions (a language toggle, a conversion key),
intercept them **before** the pass-through check, or a chord like Right-Ctrl will be delegated away
before you ever see it. Note that left and right modifiers are distinct key codes
(`KEYCODE_CTRL_LEFT` = 113, `KEYCODE_CTRL_RIGHT` = 114), which is what makes "Right Ctrl toggles the
language, Left Ctrl still does Ctrl+C" possible.

For a lone-modifier binding, capture it on **key up**: on key down you cannot yet tell whether the
user is pressing Ctrl alone or starting Ctrl+Space.

## 7. The candidates view

`onCreateCandidatesView()` plus `setCandidatesViewShown(true/false)` gives you a strip that is
independent of the keyboard view. This matters because **the candidates view still shows when the
input view is hidden** — which is exactly the case when a hardware keyboard is attached. Conversion
UI belongs here, not in the keyboard view.

The view is created lazily, so populate it in the right order:

```java
private void showCandidates(String reading, List<Item> items) {
    pending = items;                    // remember for onCreateCandidatesView
    setCandidatesViewShown(true);       // may call onCreateCandidatesView now
    if (candidatesView != null) {
        candidatesView.show(reading, items);
    }
}
```

Hide the strip when the user types anything else, and on `onStartInput` / `onFinishInputView`, or a
stale strip will outlive the text it referred to.

## 8. Drawing a custom keyboard

A canvas-drawn keyboard redraws on every press. The cost that matters is
*redraw frequency × per-draw complexity*, so keep the per-frame work proportional to what changed.

**Cache the static keyboard.** Render the unpressed keyboard — key shapes, bevels, labels — once
into a `Bitmap`, and on each frame blit the bitmap and draw only the pressed key's overlay:

```java
protected void onDraw(Canvas canvas) {
    ensureBaseBitmap(getWidth(), getHeight());   // rebuilt only when the signature changes
    canvas.drawBitmap(baseBitmap, 0, 0, null);
    drawPressFeedback(canvas);                   // one rounded rect
}
```

The cache key must include everything the static image depends on: page, layout, shift state, armed
modifiers, keypad mode, size, and theme. It must **not** include the pressed key, or you rebuild the
whole bitmap on every keystroke.

Other rules:

- Allocate nothing in `onDraw` — no `Paint`, no `Shader`, no boxing. Reuse fields.
- Prefer a 1–2 px offset rounded rect for a raised look over `setShadowLayer`, which blurs and is
  expensive.
- Recycle the cached bitmap in `onDetachedFromWindow`.

Touch handling has its own rule: **a tap counts for the key it started on.** Resolve the key on
`ACTION_DOWN`, and on `ACTION_UP` only commit if the release is still on that key. If you want a
dead zone between keys so near-boundary taps do not hit the neighbour, test the touch against the
key's *visible* face rather than its whole grid cell.

## 9. Theming

Resolve colours from the device theme; do not hardcode them.

- Honour light/dark: `Configuration.uiMode & UI_MODE_NIGHT_MASK`.
- Assign colours by Material role — background as surface, keys as an elevated surface, labels as
  on-surface, active keys as primary, press as a primary state layer — rather than picking hues.
- On Android 12+ use the user's dynamic palette (`android.R.color.system_neutral1_*`,
  `system_accent1_*`); fall back to a tuned light/dark palette on older versions and if resolution
  fails.
- Include the theme in your draw-cache key so a light/dark switch rebuilds the keyboard.
- Use one palette for every surface you draw: keys, long-press popup, candidate strip.

## 10. Testing and verification

**Keep the input core Android-free.** Event normalisation, layout geometry, the composer automaton,
dictionaries, and settings clamping can all be plain Java, and then they are unit-testable on the
JVM with no device. This is where nearly all real logic bugs are caught.

Also test the **shipped data**, not just fixtures: parse the real dictionary file in a unit test and
assert a few known conversions, so a bad regeneration fails the build.

**Do not trust a headless emulator as an oracle for IME behaviour.** Two failure modes were measured
repeatedly on a headless, no-KVM emulator:

- The IME window reports `Requested w=0 h=0`, `mViewVisibility=GONE`, `mHasSurface=false`, and
  `screencap` returns a blank framebuffer — *even for a build known to work on a real device.* You
  cannot conclude "the keyboard does not show" from this.
- Injected key events (`adb shell input keyevent`) may not reach the IME's `onKeyDown` at all, and
  the result is not even stable between runs.

What a headless emulator *can* prove: the APK installs, the IME registers and can be selected,
activities inflate, and nothing crashes (`logcat` for `FATAL` / exceptions). Anything visual or
input-interactive must be verified on a real device.

A useful technique when a regression appears: build a **known-good tag** and install it on the same
emulator. If the known-good build produces identical symptoms, the symptom is the environment, not
your change.

## 11. Anti-patterns, with the failures that taught them

### 11.1 A strict expectation ledger that can latch

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

### 11.2 Gating insertion on a known selection

**What was built.** `commitText` and `setComposingText` were rejected with `INVALID_SELECTION` when
the cached bounds were unknown.

**What happened.** Terminals report `-1`, so every keystroke was dropped and a failure toast fired
on each one — the keyboard looked completely broken in that app.

**The fix.** Insertion is cursor-relative and needs no bounds. The same applies to
`deleteSurroundingText`: backspace was also gated on a known selection and also failed in terminals
until the gate was removed entirely.

**Rule.** Only operations that address *absolute* positions may require a known selection — and in
practice you should not have any.

### 11.3 Refusing a raw Enter

**What was built.** Enter resolved to an editor action; for a single-line editor with no action it
produced an empty result and did nothing. Raw Enter was additionally refused for "rich" editors.

**What happened.** Enter did nothing at all in terminals — no newline, no submit.

**The fix.** Fall through to a real `KEYCODE_ENTER` via `sendKeyEvent`. Every editor understands a
real Enter key: a terminal submits the line, a plain field does its default.

**Rule.** For any key with a "semantic" mapping, keep a raw-key fallback. Doing nothing is never the
right answer for a key the user pressed.

### 11.4 Re-anchoring the composer on any prediction mismatch

**What was built.** On `onUpdateSelection`, if the reported cursor differed from the predicted one,
the composer called `finishComposingText()` and reset — intended to re-anchor after the user tapped
elsewhere.

**What happened.** Real editors do not match the prediction exactly. The mismatch fired on the
IME's *own* edits, so every keystroke triggered finish+reset, plausibly re-entering
`onUpdateSelection`, and the IME died: **the keyboard stopped appearing at all.** This shipped and
had to be reverted.

**The fix.** Revert to the passive model. A re-anchor must be triggered by a clear signal — the
cursor moving outside the composing region — never by "the prediction was not exact".

**Rule.** Never drive destructive state changes from a prediction mismatch. Predictions are hints;
only unambiguous evidence should reset user-visible state.

### 11.5 Letting an exception escape

**What happened.** An exception thrown while handling a key or a selection update killed the IME
process, and the keyboard vanished mid-typing.

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

### 11.6 Swallowing application shortcuts

**What happened.** The IME consumed modifier chords, so Ctrl+A / Ctrl+C / Ctrl+V did nothing in any
app while the keyboard was active.

**The fix.** Delegate modifier keys and Ctrl/Alt/Meta chords to `super.onKeyDown` — except the
specific chords the user has bound to IME functions, which must be checked first.

### 11.7 Mapping a soft modifier to editor commands only

**What was built.** A soft Ctrl combined with a letter was mapped to context-menu actions, but only
for `a/c/v/x/z/y`. Every other letter fell through and was typed as text.

**What happened.** In a terminal, Ctrl+B inserted the literal character `b` instead of sending
`0x02`.

**The fix.** Send a real key chord for *any* letter. Rich editors handle Ctrl+A/C/V/X/Z/Y through
`onKeyShortcut`, and terminals get their control codes — one mechanism instead of a special-case
table.

**Rule.** Prefer emitting the real input event over simulating its effect. The event works in more
places than your table of effects.

### 11.8 Rebuilding the whole keyboard image per keystroke

**What it costs.** Gradients, shadows, and text for 30–40 keys recomputed on every press, on the UI
thread, for a change that affects one key.

**The fix.** [§8](#8-drawing-a-custom-keyboard) — cache the static image, redraw one key. Keep the
pressed key out of the cache key.

### 11.9 Hardcoding colours

**What happened.** A fixed palette looked wrong the moment the system switched to dark mode, and
ignored the user's theme entirely.

**The fix.** [§9](#9-theming) — resolve from the theme, include the theme in the cache key.

### 11.10 Trusting the emulator

**What happened.** A headless emulator showed a zero-sized, never-rendered IME window and dropped
injected key events, which was mistaken for a code regression. Building a known-good tag and
installing it on the same emulator reproduced the identical symptoms, proving the environment was at
fault.

**Rule.** Before concluding "the keyboard is broken", reproduce with a build that is known to work.
See [§10](#10-testing-and-verification).

## 12. Pre-release checklist

- [ ] No code path refuses input because of internal cursor bookkeeping.
- [ ] Typing, backspace, and Enter all work in a **terminal** app (`TYPE_NULL`, selection `-1`).
- [ ] Composing is finished in `onFinishInput`, `onFinishInputView`, and on subtype change.
- [ ] Ctrl/Alt/Meta chords still reach the app; bound IME shortcuts are intercepted before that.
- [ ] Key handlers cannot throw out of the service.
- [ ] The draw cache key covers layout, shift, modifiers, size, and theme — but not the pressed key.
- [ ] Light and dark themes both render correctly, and switching rebuilds the keyboard.
- [ ] Unit tests cover the Android-free core **and** parse the shipped data files.
- [ ] Anything visual or input-interactive was verified on a real device, not an emulator.
- [ ] This manual was updated for whatever changed.
