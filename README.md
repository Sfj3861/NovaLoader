# NovaLoader

**NovaLoader** is an Android-first mod-loader project for Geometry Dash. Its first milestone focuses on a safe package library and a defined .nova package format.

> **Current status — v0.1.0 development build:** The Android package manager can import, inspect and remove .nova packages, and can store .geode files for later compatibility work. This release does not inject code into Geometry Dash and does not execute mods.

## What's included

- Native Android app written in Kotlin.
- Private package storage; archives are not extracted to shared storage.
- .nova ZIP validation, manifest checks and basic archive-path validation.
- .geode file storage, clearly marked as not executable yet.
- GitHub Actions workflow to build and publish a debug APK as an artifact.
- Initial architecture and format documentation.

## Build

Open the project in Android Studio with JDK 17 and Android SDK Platform 35, or run:

    ./gradlew assembleDebug

On Windows, use:

    gradlew.bat assembleDebug

The lightweight Gradle launcher downloads Gradle 8.9 from the official Gradle distribution service when it is not already cached. The APK is written to app/build/outputs/apk/debug/app-debug.apk.

Every push to main and every pull request runs the Android build workflow. The generated debug APK is uploaded as a workflow artifact; it is not a signed release build.

## Creating a .nova package

A .nova file is a ZIP archive with a root-level manifest.json. The current format version is 1. See docs/FORMAT_NOVA_V1.md for the first draft and an example manifest.

NovaLoader validates package metadata and stores the archive. Fields such as entrypoint are reserved for future runtime work and are not executed by v0.1.0. Only install packages from sources you trust.

## Roadmap

- [x] v0.1 foundation: Android project, package library, .nova manifest validation and CI build.
- [ ] v0.2: package details, improved error reporting, export/share and persistent package states.
- [ ] v0.3: public SDK and developer tooling for .nova.
- [ ] v0.4: research and prototype a version-specific Geometry Dash integration.
- [ ] v0.5: repository/catalogue, updates and dependency resolution.
- [ ] v0.6–v0.9: translations, permission UX, compatibility matrix, security review and testing.
- [ ] v1.0: stable release only after runtime compatibility and device testing.

## Compatibility note

Recognising or storing a .geode file is not the same as running Geode mods. The in-game integration is a separate native engineering task and must be verified against the exact Geometry Dash version and CPU architecture before it is advertised as working.

## Contributing

Please open an issue for bugs, supported Geometry Dash versions, device details and reproducible steps. Avoid posting proprietary game binaries, copyrighted assets or private signing keys.

## License

See LICENSE.
