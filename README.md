# NovaLoader

**NovaLoader** is an Android-first mod-loader project for Geometry Dash. Its current milestone focuses on a safe private package library, `.nova` validation and read-only inspection of Geode package metadata.

> **Current status — v0.1.x development:** NovaLoader imports, validates and lists `.nova` packages, and stores `.geode` archives and reads their root `mod.json` metadata, but does not execute them. It does not inject code into Geometry Dash or execute mods.

## What's included

- Kotlin Android app with private package storage.
- `.nova` ZIP validation with root `manifest.json`, schema, identifier and metadata checks.
- Archive safeguards for unsafe paths, duplicate names, entry-count limits, manifest size and expanded-size limits.
- Informational checks of declared Android platform, ABI and Geometry Dash version metadata.
- `.geode` ZIP safety checks and `mod.json` metadata inspection; packages remain non-executable.
- Export of stored packages through Android’s document picker.
- GitHub Actions workflow that builds a debug APK.
- Architecture documentation informed by static inspection of a Geode Android reference APK.

## Build

Open the project in Android Studio with JDK 17 and Android SDK Platform 35, or run:

    ./gradlew assembleDebug

On Windows, use:

    gradlew.bat assembleDebug

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Every push to `main` and every pull request runs the Android build workflow. The generated debug APK is a workflow artifact, not a signed release build.

## Creating a .nova package

A `.nova` file is a ZIP archive with a root-level `manifest.json`. The current format version is 1; see [docs/FORMAT_NOVA_V1.md](docs/FORMAT_NOVA_V1.md).

Geode imports are checked for safe ZIP paths and a readable root `mod.json`; NOVA packages use `formatVersion`, `game.versions`, `game.platforms` and `game.architectures`. NovaLoader checks any declared Android platform and ABI against the device. Declared game versions are displayed as metadata; they are not proof that the installed game's exact binary build matches. Package payloads are not extracted or executed.

Only import packages from sources you trust. Validation is not a malware scan.

## Geode research

[Geode Android APK analysis](docs/GEODE_ANDROID_APK_ANALYSIS.md) documents the static evidence from the supplied reference APK and the limits of the conclusions. The findings suggest separate launcher/profile, native bootstrap, in-game runtime and mod-package layers. NovaLoader does not copy Geode binaries or claim to use Geode's exact loading mechanism.

## Roadmap

- [x] v0.1 foundation: Android app, private package library, .nova validation and CI build.
- [x] v0.1.x: additional ZIP safety checks, declared ABI/platform metadata checks and Geode `mod.json` inspection.
- [ ] v0.2: package details, export/share and persistent package states.
- [ ] v0.3: public SDK and developer tooling for .nova.
- [ ] v0.4: research and prototype a version-specific Geometry Dash integration.
- [ ] v0.5: catalogue, updates and dependency resolution.
- [ ] v0.6–v0.9: translations, permission UX, compatibility matrix, security review and device testing.
- [ ] v1.0: stable release only after runtime compatibility and device testing.

## Compatibility note

Recognising or storing a `.geode` file is not the same as running a Geode mod. NOVA's `.nova` format is separate. In-game integration is a distinct native engineering task and must be verified against exact Geometry Dash builds and CPU architectures before being advertised as working.

## Contributing

Open an issue for bugs, supported Geometry Dash versions, device details and reproducible steps. Do not post proprietary game binaries, copyrighted assets or private signing keys.

## License

See [LICENSE](LICENSE).
