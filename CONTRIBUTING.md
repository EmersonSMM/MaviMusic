# Contributing to MaviMusic

MaviMusic targets macOS, Windows and Linux. The repository also contains the web player, browser extension and Listen Together server.

## Development setup

- JDK 21 for the desktop application.
- Xcode command line tools on macOS; Clang builds the native Automix analyser.
- CMake and a C++17 compiler on Linux/Windows. Windows media integration requires the matching native toolchain.
- Node 22+ and pnpm for `web/`.
- The Go version declared in `backend/go.mod` for the optional party server.

Service credentials belong in the ignored `local.properties` file. Native desktop builds do not require a mobile SDK, NDK or signing keystore.

## Desktop commands

Run from the repository root:

```sh
./gradlew :desktopApp:run
./gradlew :shared:jvmTest :desktopUi:jvmTest :desktopApp:test
./gradlew :desktopApp:packageDmg
```

On Windows use `gradlew.bat`; distribution tasks are documented in [DESKTOP.md](DESKTOP.md).

## Architecture

- `desktopApp/`: application state, playback, downloads and OS integration.
- `desktopUi/`: Compose player, lyrics and page components, with desktop-owned resources.
- `shared/`: models, queue policy, network clients and analysis logic for the JVM runtime.
- `native/analyzer/`: platform-neutral DSP and JNI bridges.
- `desktopApp/src/main/resources/models/`: the bundled Automix models.
- `web/`: browser player and companion extension.
- `backend/`: synchronized-listening server.

## Changes and validation

Keep changes focused and describe the final behavior and validation. Preserve the existing desktop features when moving or deleting code. Run the relevant tests and update documentation when paths, configuration or user behavior changes.

Audio telemetry does not establish physical hardware behavior unless the actual device has been measured. Distinguish source metadata, decoder output, DSP format and device output.

Tests use an in-memory preferences factory. Normal tests must not query the macOS Keychain or launch the application against real account preferences.

## Licensing

Keep the existing copyright and license notices for inherited components. New dependencies should have a clear purpose and compatible distribution terms. Reviews should include the dependency and validation changes relevant to the implementation.
