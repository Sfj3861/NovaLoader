# NovaLoader

**NovaLoader** is an Android-first mod-loader project for Geometry Dash. Version 0.2.1 improves the installation report and keeps the package library separate from game execution.

> **Current status — v0.2.1:** NovaLoader can import and inspect .nova and .geode archives, store them privately, export them, and report basic Android installation information for Geometry Dash. It does **not** load mods into the game yet.

## What's included

- Kotlin Android app with private package storage.
- .nova ZIP validation with a root manifest.json, format/version and metadata checks.
- Archive safeguards for unsafe paths, duplicate names, entry-count limits, manifest size and expanded-size limits.
- Informational checks of declared Android platform, ABI and Geometry Dash version metadata.
- .geode ZIP safety checks and read-only inspection of root mod.json.
- Export and delete actions for stored packages.
- Geometry Dash installation report: Android package name, declared version, version code and device ABIs.
- Confirmation before opening the installed unmodified game.
- GitHub Actions workflow that builds a debug APK.

## What is not implemented yet

- No native runtime or injection into Geometry Dash.
- No execution of .geode or .nova packages.
- No dependency resolution, mod activation, or verified compatibility matrix.
- The installation report only reads Android package metadata. A version string and ABI do not prove that a future native backend supports that exact game build.

Importing a package only validates and stores it. Validation is not a malware scan; only import files from sources you trust.

## Build

Open the project in Android Studio with JDK 17 and Android SDK Platform 35, or run:

    ./gradlew assembleDebug

On Windows, use:

    gradlew.bat assembleDebug

The APK is written to app/build/outputs/apk/debug/app-debug.apk. The workflow artifact is a debug APK, not a signed release build.

## Creating a .nova package

A .nova file is a ZIP archive with a root-level manifest.json. The current format version is 1; see [docs/FORMAT_NOVA_V1.md](docs/FORMAT_NOVA_V1.md).

NOVA packages use format, formatVersion, id, name, version and optional game.versions, game.platforms and game.architectures metadata. These compatibility fields are informational; they do not make a package executable.

Geode imports are read-only inspections of the root mod.json. The .geode and .nova formats are distinct.

## Architecture and research

- [Architecture and roadmap](docs/ARCHITECTURE_AND_ROADMAP.md)
- [Geode Android APK analysis](docs/GEODE_ANDROID_APK_ANALYSIS.md)

The research suggests that real in-game loading requires a separate native bootstrap/runtime and exact-build compatibility work. NovaLoader does not copy Geode binaries or claim to use Geode's exact loading mechanism.

## Roadmap

- [x] Private package library and NOVA format validation.
- [x] Geode archive safety checks and metadata inspection.
- [x] Package export/delete and basic Geometry Dash installation report.
- [ ] Native feasibility prototype for a specific Android Geometry Dash build.
- [ ] First mod loaded and verified on-device.
- [ ] NOVA runtime contract, dependencies, recovery mode and compatibility matrix.
- [ ] Stable v1.0 only after runtime and device testing.

## Contributing

Open an issue for bugs, supported Geometry Dash versions, device details and reproducible steps. Do not post proprietary game binaries, copyrighted assets or private signing keys.

## License

See [LICENSE](LICENSE).
