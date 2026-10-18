# Geode Android APK: architectural observations

## Sample and scope

The supplied reference was named `Geode_skybound.apk`. Static inspection of its archive and DEX/native strings showed:

- Android DEX files and a launcher namespace containing `MainActivity` and `GeometryDashActivity`.
- Native helpers `lib/arm64-v8a/liblauncherfix.so` and `lib/armeabi-v7a/liblauncherfix.so`.
- JNI names including `setDataPath`, `setOriginalDataPath`, `enableExceptionsRenaming` and `performPatches`.
- Strings referring to `libcocos2dcpp.so`, relocation patching, memory protection, profile/mod paths and `--geode:safe-mode`.
- No `libcocos2dcpp.so` or bundled Geometry Dash APK in the inspected archive.

These observations indicate that the launcher APK is not the game engine itself and that the Android implementation separates launcher/profile management from native game integration and mod loading.

## High-level architecture inferred

1. Android launcher selects or prepares a game profile.
2. The launcher configures paths and launch state.
3. The game activity is started with Geode-specific options.
4. A native bridge prepares runtime patches against the game's native library.
5. A loader/runtime discovers and initializes compatible mods.

This is a high-level inference from static contents, not a traced execution sequence. It does not establish the exact patching algorithm or guarantee compatibility with any particular GDPS build.

## What this means for NovaLoader

NovaLoader must not treat a floating overlay or a package extension as a mod runtime. The project currently implements private package storage and validation only. The next engineering stages are:

- Reliable identification of the installed game build, package ID and ABI.
- A version-specific feasibility study for a native bootstrap.
- A safe-mode/recovery path before runtime code is introduced.
- A documented NOVA runtime API and package ABI.
- Tests for each supported game build and Android ABI.

The current app can store `.geode` archives but does not execute them or promise binary/API compatibility with Geode mods. NOVA's `.nova` format is separate from Geode's `.geode` format.

## Security and licensing boundaries

- Do not bundle Geometry Dash's proprietary APK/assets.
- Do not copy Geode's proprietary compiled binaries or code into NovaLoader.
- Treat imported archives as untrusted; validate them and keep them in app-private storage.
- Do not execute packages during import.
- Do not claim in-game loading until it has been demonstrated on real devices with the exact build and ABI.
