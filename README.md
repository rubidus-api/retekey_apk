# ReteKey IME

Android Hangul keyboard focused on standard IME behavior, hardware-key friendliness, and efficient Korean input.

## Status

P1A source-neutral input and P1B checked editor/session reliability are
complete. The T012-core instrumentation APK, separate editor/control host, and
state-restoring runner now assemble; real API/device execution remains the P1
gate before the stateful Hangul composer.

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

The workspace build target is arch-dev through ignored private configuration:

```sh
scripts/build-arch-dev.sh --check-config
scripts/setup-arch-dev-android-sdk.sh install
scripts/sync-arch-dev.sh sync
scripts/build-arch-dev.sh build
```

Do not put private remote hosts, paths, ports, user names, key names, or credentials in tracked files.
