# ReteKey IME

MIT-licensed Android Hangul keyboard focused on standard IME behavior, hardware-key friendliness, and efficient Korean input.

## Status

P1A source-neutral input and P1B checked editor/session reliability are complete.
T012-core is verified on a real Android lane (API 33 emulator): start, view,
restart without finish, and teardown behave as specified against the actual
framework callback stream. The stateful Hangul composer (P2) is next; the full
API matrix and the Galaxy Note20 gate remain open.

The touch layout is one orthogonal ten-column grid shared by English QWERTY and
Korean 2-beolsik: equal keys, a three-column space bar, and no staggered rows.

## Layout

Korean 2-beolsik, on the shared ten-column grid:

![Korean 2-beolsik layout](docs/images/keyboard-korean.png)

The symbol layer (the `!#1` key, or holding the period): a right-hand digit pad,
the four arithmetic operators, and the everyday punctuation; the Num and Fn keys
turn the pad into the arrow cluster or the function keys.

![Symbol and number layer](docs/images/keyboard-symbol.png)

The Num toggle turns the keypad into arrows and navigation, which send real key
events to the editor:

![Arrow / navigation mode](docs/images/keyboard-arrows.png)

Its Shift page carries the special keys. Esc, PrtSc, ScrLk, Pause, and Menu send
key events; the media keys, 한자/Lang, and the right-hand modifiers stay muted
until their systems land (RFC-0005/0006):

![Special-key page](docs/images/keyboard-special.png)

> These images are rendered from the actual layout data. A live screenshot from
> the emulator lane is not shown because that host is headless with no KVM and no
> window, so `screencap` returns a blank framebuffer; capture on a real device or
> a GUI emulator for device screenshots. `PreviewActivity` (the app's launcher
> screen) provides a text field for trying the keyboard on such a device.

## Stack

- Java/JDK 17 LTS
- Android SDK 36 (`minSdk 29`, `targetSdk 36`)
- Android Gradle Plugin 9.2.1
- Gradle Wrapper 9.4.1

The app uses Android's standard `InputMethodService` entry point. Event
normalization, semantic jamo, 2-beolsik hardware mapping, dispatch disposition,
matched key-up tracking, immutable transition plans, checked editor execution,
Unicode-safe deletion, and bounded selection reconciliation are plain Java and
JVM-tested. The current stateless compatibility-jamo fallback keeps scaffold
input visible; the stateful Hangul composer remains planned.

## Documentation

- `SPEC.md`: current design and architecture
- `REQUIREMENTS.md`: accepted requirements
- `docs/rfc/rfc-0001-implementation-plan.md`: first implementation plan
- `docs/rfc/rfc-0003-jamotong-automata-lineage.md`: accepted Hangul-core
  lineage and touch/input separation
- `docs/rfc/rfc-0004-android-ime-reliability.md`: accepted lifecycle, editor
  failure, verification, and release contract
- `docs/tests/test-index.md`: T000-T013 verification catalog, including editor
  fault injection, real IME lifecycle, and exhaustive/model robustness
- `docs/resources/sources-and-licenses.md`: pinned build tools, Jamotong,
  Android, AOSP, and Unicode evidence/provenance boundaries
- `docs/manual/emulator-lane.md`: the device-free T012 emulator lane
- `docs/manual/`: durable manuals

## Build

Local builds require JDK 17 and Android SDK platform 36/Build Tools 36.0.0.
Use the checked-in wrapper rather than a system Gradle:

```sh
./gradlew testDebugUnitTest assembleDebug
```

Compile the Android lifecycle harness without changing device state:

```sh
./gradlew :testhost:assembleDebug :app:assembleInstrumentationAndroidTest
```

Run it only on an authorized, unlocked test device. The runner preserves and
restores the previously selected/enabled IME state and never prints its id.
The host also needs `adb`, `sha256sum`, `base64`, and util-linux `flock`:

```sh
scripts/run-ime-instrumentation.sh connected
scripts/run-ime-instrumentation.sh matrix
```

On a host with no device and no KVM, the emulator lane boots its own guest under
plain TCG emulation and runs the same case on it:

```sh
scripts/emulator-lane.sh run
```

See `docs/manual/emulator-lane.md` for why it pins an API 33 AOSP ATD x86_64
image and what the lane does and does not prove.

The private matrix file is declarative, not a shell script: use exactly one
plain `KEY=value` line for each key named by `--help`; blank lines and full-line
`#` comments are allowed. Unknown, duplicate, executable, or malformed content
is rejected without printing its values.

The workspace build target is arch-dev through ignored private configuration:

```sh
scripts/build-arch-dev.sh --check-config
scripts/setup-arch-dev-android-sdk.sh install
scripts/sync-arch-dev.sh sync
scripts/build-arch-dev.sh build
```

Do not put private remote hosts, paths, ports, user names, key names, or credentials in tracked files.
