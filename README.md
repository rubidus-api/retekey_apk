# ReteKey IME

Android Hangul keyboard focused on standard IME behavior, hardware-key friendliness, and efficient Korean input.

## Status

Initial scaffold.

## Stack

- Java/JDK 17 LTS
- Android SDK 36 (`minSdk 29`, `targetSdk 36`)
- Android Gradle Plugin 9.2.1
- Gradle Wrapper 9.4.1

The app uses Android's standard `InputMethodService` entry point. The Hangul composition and keymap logic are kept in plain Java so they can be tested without Android UI.

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

The workspace build target is arch-dev through ignored private configuration:

```sh
scripts/build-arch-dev.sh --check-config
scripts/setup-arch-dev-android-sdk.sh install
scripts/sync-arch-dev.sh sync
scripts/build-arch-dev.sh build
```

Do not put private remote hosts, paths, ports, user names, key names, or credentials in tracked files.
