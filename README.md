# ReteKey IME

**English** · [한국어](README.ko.md)

An MIT-licensed Android Hangul keyboard focused on standard IME behaviour, hardware-keyboard
friendliness, and efficient Korean input. Written in plain Java with no third-party runtime
dependencies — the release APK is about 230 KB.

> This English README is the canonical version. The Korean translation follows it.

## Table of contents

- [Download](#download)
- [Android version support](#android-version-support)
- [Features](#features)
- [Layout](#layout)
- [Floating keyboard](#floating-keyboard)
- [Hanja conversion](#hanja-conversion)
- [Physical keyboards](#physical-keyboards)
- [Settings](#settings)
- [Theming](#theming)
- [Architecture](#architecture)
- [Build](#build)
- [Documentation](#documentation)
- [License](#license)

## Download

**[⬇ Download the latest APK](https://github.com/rubidus-api/retekey_apk/releases/latest/download/retekey.apk)**
&nbsp;·&nbsp; [all releases](https://github.com/rubidus-api/retekey_apk/releases)

Current release: **v0.1.40** —
[retekey-0.1.40.apk](https://github.com/rubidus-api/retekey_apk/releases/download/v0.1.40/retekey-0.1.40.apk)

After installing, enable ReteKey in *Settings → Keyboards* and select it as the default input
method. The app's launcher screen has shortcuts for both steps and a field for trying the keyboard.

## Android version support

**Android 9 (API 28) through Android 16 (API 36).**

| | Version | Note |
|---|---|---|
| Minimum | **Android 9** — API 28 | `minSdk 28`. Android 8.1 and older cannot install the APK. |
| Target | **Android 16** — API 36 | `targetSdk 36`, `compileSdk 36`. |
| Verified by an automated lane | Android 13 — API 33 | AOSP x86_64 emulator, IME lifecycle test. |
| Verified by hand | Android 13 — API 33 | Galaxy Note20, the project's primary device. |

The whole 9–16 range is supported and compiled against one API surface: every platform call the
app makes exists on API 28, and the few that do not are guarded by an explicit version check.
The rows marked *verified* are the versions an actual test run has covered; the others are
supported by construction and have not yet been exercised on a device.

What changes with the OS version:

- **Android 12 (API 31) and newer** — the keyboard takes its colours from the user's **Material You**
  palette, and press vibration goes through `VibratorManager`. Android 10 and 11 use a hand-tuned
  light/dark palette and the legacy vibrator instead. Everything else is identical.
- **Android 9–12 (API 28–32)** — deletion falls back to UTF-16 code units, or to a raw key event,
  because code-point deletion is not dependable on those releases. A syllable or an emoji still
  disappears in one press; only the mechanism differs.

No feature is withheld on any supported version. Hanja conversion, the 2-beolsik composer, physical
keyboard mapping, auto-repeat, and the settings screen behave the same across the whole range.

## Features

- **Five layouts** — 2-beolsik, QWERTY, Dvorak, and the 12-key 천지인 and 나랏글 phone modes. A 🌐
  key walks the ones you enabled, in the order you set; holding it converts Hanja.
- **Stateful 2-beolsik Hangul composer** with compound vowels and final consonants, consonant
  migration, and reversible backspace (닭 → 달 → 다).
- **Hanja conversion** in both directions, with 훈음 glosses, paging, and number-key selection, in
  a window of its own that appears whichever keyboard you are using.
- **Floating keyboard** for a tablet in landscape: a translucent panel confined to one half of the
  screen, draggable, resizable, and mirrored to the other half with one key.
- **Physical keyboard support**: user-assignable KO/EN and Hanja keys, modifier chords, and
  2-beolsik mapping for Bluetooth or wired keyboards.
- **Held-key auto-repeat** for space, enter, backspace and the arrows, with a configurable start
  delay and interval.
- **Raised, rounded keys** with press feedback — the pressed key tints, the gaps between the keys
  blink, and the character you just typed appears large in a box above them, plus haptics and
  sound, each independently adjustable.
- **Hold a key to type its alternate** straight away: no popup, no drag. The letter pages hold
  `1234567890` on the top row, `!@#$%^&*;` on the middle one, and `_-:='"?` on the bottom one.
- **Follows the system theme** — light/dark, and the Material You palette on Android 12+.

## Layout

The touch layout is one orthogonal ten-column grid with equal keys and no staggered rows. The
bottom row is the same on every page of the full-size layouts:
`Ctrl · Meta · Alt · Tab · space · ☰ · pad · !# · 🌐`. The two 12-key pages keep the same frame in a
different shape: the modifiers own the leftmost column, ☰ and pad ride the second one, every Hangul
key is two columns wide, and the right-hand column carries backspace, space, then the period and
Enter, with `!#` and 🌐 closing the bottom row.

Five letter layouts share that grid:

| Layout | Shape |
|---|---|
| **두벌식** | the standard Korean full keyboard |
| **QWERTY** | English |
| **Dvorak** | English, in its own 7/10/9 shape; the three cells the top row does not need for letters carry Enter, backspace and the period, on the left |
| **천지인** | 12-key: the elements ㅣ ㆍ ㅡ build the vowels, each consonant key cycles its group (ㄱ → ㅋ → ㄲ), and ▷ ends the syllable so the next letter starts its own |
| **나랏글** | 12-key: a consonant block with 획 adding a stroke (ㄴ → ㄷ → ㅌ) and 쌍 doubling (ㅅ → ㅆ) |

The 🌐 key walks the layouts you ticked in settings, in the order you put them there, naming each
one as it arrives. Holding it runs the Hanja conversion.

Holding a letter key types a character instead of repeating it. The alternates come in three groups
— `1234567890`, `!@#$%^&*;` and `_-:='"?` — one to a row. Their sizes are 10/9/7, which is QWERTY's
row shape and Dvorak's 7/10/9 read in a different order, so Dvorak carries the same three groups
rotated to fit: rows 1-2-3 become 3-1-2, and no group is split or padded. Space, enter, backspace
and the arrows have no alternate and keep repeating when held.

A physical keyboard is not affected by any of this: it keeps its own layout, and the on-screen
choice only changes what the on-screen keys do.

Korean 2-beolsik:

![Korean 2-beolsik layout](assets/keyboard-korean.png)

The `!#` key opens the special-characters page, where every key commits its symbol. Holding a key
types its alternate immediately — there is no popup to aim at and nothing to drag to. Holding the
period types a comma, and holding `_` types `-`, the pair a physical keyboard puts on one key:

![Special characters page](assets/keyboard-chars.png)

The `pad` key opens the special-keys page: a right-hand keypad plus special keys. The digits and
`+ - = .` commit text; `Esc`, `PrtSc`, `ScrLk`, `Pause`, and `Menu` send key events. `Num` turns the
keypad into arrows and navigation, where the top-right key becomes forward-delete:

![Special keys page](assets/keyboard-keys.png)

`Fn` swaps the whole page to the function and media keys. F1–F12 send key events; F13–F15 (which
have no Android key code), the media keys, and Back stay muted:

![Special keys, Fn page](assets/keyboard-keys-fn.png)

The `☰` key opens a menu page with editing commands (copy, cut, paste, undo, redo, select all),
cursor navigation, date insertion, keyboard height, and shortcuts to the system keyboard settings.

> These images are rendered from the actual layout data. A live screenshot is not shown because the
> build host is headless with no KVM and no window, so `screencap` returns a blank framebuffer;
> capture on a real device or a GUI emulator instead.

## Floating keyboard

The ☰ menu's **Floating** tile turns the keyboard into a translucent panel that floats over the
app instead of docking to the bottom edge — the shape a tablet in landscape wants, where a
full-width keyboard is both too wide to type on and too tall to see past.

The panel is confined to one half of the screen and can never be wider than that half, so the other
half stays yours. Its title bar carries, left to right:

| Key | Does |
|---|---|
| ☰ | drag to move the panel, anywhere within its half |
| ‹ / › | send the panel to the other half, mirrored about the centre line |
| ✕ | leave floating mode and go back to the ordinary keyboard |
| ⇲ | drag to resize the panel |

Dragging stops at the centre line: crossing over is the arrow key's job, and it reflects the panel
rather than dropping it somewhere new, so a panel hugging the left edge comes back hugging the
right one. Everything outside the panel still belongs to the app — the keyboard takes no touches
there and the app is not resized to make room for it. The mode and the panel's placement are
remembered, and a rotation rescales the panel instead of stranding it off-screen. How solid the
panel is, is yours to set — the opacity slider in settings runs from barely-there to fully solid.

## Hanja conversion

Pressing the 한자 key — the one on the special-keys page, or a physical key you assigned — converts
Korean to Hanja:

- With a **selection**, the whole selection is converted.
- With **no selection**, the reading immediately before the cursor is converted, preferring the
  longest match, so `학교` becomes `學校` rather than converting `교` alone.
- Pressing it on **Hanja** converts back to its reading, including whole Hanja words.

Candidates appear in a window of their own, spanning the keyboard's full width, in a paged grid of
nine. Each 훈음 gloss sits beside its character (家 집 가) and wraps onto a second line when the
column is narrow. The window is not part of the keyboard, so it appears the same way for the
on-screen keyboard, an external keyboard with no keyboard on screen at all, and the floating panel;
it keeps one size while it is up, so a short last page does not resize it. Tap a candidate, or
press its number key **1**–**9**; `←`/`→` and `PageUp`/`PageDown` turn the page and `Esc` dismisses.

The dictionary is bundled (about 1,100 readings plus common words). See
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for its provenance and licences.

## Physical keyboards

With a Bluetooth or wired keyboard, letter keys are mapped to 2-beolsik jamo while Korean mode is
on. In settings you can assign **several physical keys per function**:

- **KO/EN toggle** — for example `Shift+Space` *and* `Right Ctrl`.
- **Hanja** — for example `F9` *and* `Right Alt`.

A binding may be a lone key or a modifier chord; a modifier pressed on its own registers as itself.
Modifier chords the IME does not claim are passed through, so application shortcuts keep working,
and a soft `Ctrl` plus a letter sends a real chord — terminals receive the control code, editors
run select-all/copy/paste/undo.

## Settings

The settings screen uses stock controls only and follows the system theme:

- **Keyboard height**, shown as the percentage of the screen the keyboard occupies.
- **Floating keyboard opacity**, from 25% to fully solid.
- **Key-press feedback** — visual, vibration, and sound strengths, each 0–100%.
- **Key auto-repeat** — on/off, start delay, and repeat interval.
- **Physical keyboard shortcuts** — register and remove KO/EN and Hanja keys.

A **Back to main screen** button at the top returns to the app's main screen at any time.

## Theming

The keyboard resolves its colours from the device theme rather than hardcoding them:

- The system light/dark mode is honoured (`Configuration.UI_MODE_NIGHT_MASK`).
- Colours are assigned by Material role — background as surface, keys as an elevated surface,
  labels as on-surface, active keys as primary, and the press effect as a primary state layer.
- On Android 12+ the user's **Material You** palette is used, so the keyboard matches their theme;
  older versions fall back to a tuned light/dark palette.

The keys, the long-press popup, the Hanja candidate window, and the floating panel all share
one palette.

## Architecture

The app uses Android's standard `InputMethodService` entry point. The input core is deliberately
Android-free so it can be unit-tested on the JVM: event normalisation, semantic jamo, the 2-beolsik
hardware mapping, dispatch disposition, immutable transition plans, checked editor execution,
Unicode-safe deletion, the Hanja tables, and the key-repeat and shortcut settings.

Cursor state follows the AOSP LatinIME model: a passive cache that the editor's own
`onUpdateSelection` always overrides, tolerating unknown (`-1`) selections. The keyboard never
refuses input because its idea of the cursor disagrees with the editor's.

The keyboard is drawn on a canvas. The unpressed keyboard is rendered once into a cached bitmap and
reused until the layout, highlight state, size, or theme changes; a key press only tints one key, so
the raised styling costs nothing per frame. That cache is `RGB_565` — the keyboard is opaque, so it
needs no alpha channel, and half the bytes per pixel matters on a tablet.

- Java / JDK 17 LTS
- Android SDK 36 (`minSdk 28`, `targetSdk 36`)
- Android Gradle Plugin 9.2.1, Gradle wrapper 9.4.1
- R8 minification; no third-party runtime libraries

## Build

Local builds require JDK 17 and Android SDK platform 36 with Build Tools 36.0.0. Use the checked-in
wrapper rather than a system Gradle:

```sh
./gradlew testDebugUnitTest assembleDebug
```

A signed release build additionally needs a local `keystore.properties`; without it the release
variant is simply unsigned.

## Documentation

### Android IME implementation manual

**[English](docs/android-ime-manual.md)** · [한국어](docs/android-ime-manual.ko.md)

A manual for building an Android input method: how an IME is put together, a minimal working one
you can copy, the reference implementations worth reading, and then the parts that are hard — each
with the failure from this project that shaped it.

It covers:

- **how an IME is structured** — the service, `method.xml`, input and candidates views, how the
  system finds/enables/selects it, and the data flow from a touch to the editor;
- **a minimal working IME** — manifest, `method.xml`, and a service you can paste and run, plus the
  order in which to grow it;
- **reference implementations and where to get them** — AOSP LatinIME and the `SoftKeyboard`
  sample, with clone commands and what to read in each;
- the `InputMethodService` lifecycle, including the **real measured teardown order**, which is not
  the tidy one the documentation implies;
- the `InputConnection` contract, and why insertion and relative deletion never need to know the
  cursor position;
- **the editor is authoritative** — the passive cursor cache that follows from it, and why a
  keyboard must never refuse input over its own bookkeeping;
- editor kinds: terminals (`TYPE_NULL`, selection `-1`) and everything that breaks in them;
- composing text, hardware keyboards and modifier chords, and the candidates view;
- drawing a custom keyboard cheaply, theming it to the system and Material You palettes, and
  keeping settings live on a running keyboard;
- what a headless emulator can and cannot prove about an IME;
- **a detailed anti-pattern chapter** — ten real failures from this project, each with what was
  built, what went wrong, the fix, and the resulting rule, with wrong-versus-right code;
- a pre-release checklist.

The English version is canonical and the Korean version is its translation; the two link to each
other. It is a **living document**, updated whenever ReteKey's IME implementation changes, so it
describes working code rather than intentions.

Design RFCs, the verification catalog, decisions, and the changelog live in a private companion
repository and are not part of this public surface.

## License

MIT — see [LICENSE](LICENSE). Bundled third-party data and ported code are credited in
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
