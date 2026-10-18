# NovaLoader

**NovaLoader** is an Android-first mod-loader project for Geometry Dash.

> **Current status — v0.3.0:** NovaLoader imports, inspects, stores, exports and deletes .nova and .geode packages. It now builds a small JNI/C++ diagnostic library and reports its compiled ABI. This is a native integration probe only; **it does not load mods into Geometry Dash**.

## What's included

- Kotlin Android app with a private package library.
- .nova ZIP validation with root manifest, format/version and metadata checks.
- Archive safety limits for unsafe paths, duplicate names, entry count, manifest size, archive size and expanded size.
- Read-only .geode archive inspection through mod.json.
- Package details, export and delete actions.
- Geometry Dash installation report and explicit vanilla-game launch confirmation.
- C++ shared library built with Android CMake and called through JNI.
- In-app native diagnostic showing whether JNI responds, the compiled ABI and native pointer width.
- GitHub Actions build workflow for the debug APK.

## Important limitations

- No native runtime or integration into Geometry Dash's process.
- No execution of .geode or .nova packages.
- No dependency resolution, mod activation, signature verification or verified game compatibility matrix.
- JNI diagnostic proves only that NovaLoader's own native library can be called. It does not prove compatibility with Geometry Dash.
- Archive validation is not a malware scan. Import packages only from sources you trust.

## Build

Open the project in Android Studio with JDK 17, Android SDK Platform 35, Android NDK and CMake 3.22.1, or run:

    ./gradlew assembleDebug

On Windows:

    gradlew.bat assembleDebug

The APK is written to app/build/outputs/apk/debug/app-debug.apk. CI publishes a debug APK artifact, not a signed release build.

## NOVA package format

A .nova file is a ZIP archive with a root-level manifest.json. Format v1 is documented in [docs/FORMAT_NOVA_V1.md](docs/FORMAT_NOVA_V1.md). Compatibility metadata is informational; it does not make a package executable.

Geode imports are read-only inspections of root mod.json. The .geode and .nova formats are distinct and are not interchangeable.

## Native runtime research

- [Native runtime feasibility report](docs/NATIVE_RUNTIME_FEASIBILITY.md)
- [Architecture and roadmap](docs/ARCHITECTURE_AND_ROADMAP.md)
- [Geode Android APK analysis](docs/GEODE_ANDROID_APK_ANALYSIS.md)

The public Geode Android launcher/framework show that a real Android loader requires a native backend and exact-build compatibility work. NovaLoader studies those public projects as references; it does not bundle their binaries or Geometry Dash assets.

## Roadmap

- [x] Private package library and NOVA format validation.
- [x] Geode archive safety checks and metadata inspection.
- [x] Export/delete and basic game installation report.
- [x] JNI/CMake native integration probe.
- [ ] Test native diagnostic on a physical device.
- [ ] Establish compatibility for one exact Android Geometry Dash build.
- [ ] Implement a supported backend and verify a harmless test mod on-device.
- [ ] Safe mode/recovery and dependency handling before stable v1.0.

## Contributing

Report bugs with the device model, Android version, ABI and reproducible steps. Do not upload proprietary game binaries, copyrighted assets or private signing keys.

See [LICENSE](LICENSE) for licensing terms.
