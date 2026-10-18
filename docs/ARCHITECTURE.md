# NovaLoader architecture

## Current implementation

The app is a package-library prototype. It does not patch, inject into or launch Geometry Dash and must not claim that any mod is running.

- **Android app:** Kotlin UI, document picker, private archive storage and package list.
- **NOVA format:** versioned ZIP container with root `manifest.json`.
- **Validation:** metadata checks, ZIP entry/path checks, size limits and declared platform/ABI checks.
- **Geode compatibility:** `.geode` files can be stored only. Binary compatibility and execution are not implemented.
- **Native runtime:** intentionally absent until supported game builds, ABIs, loading points and recovery behaviour can be tested on real devices.
- **Repository service:** future work; the app does not download third-party code.

## Planned module boundaries

1. `app`: screens, file picker, lifecycle and user-facing errors.
2. `nova-format`: manifest models, archive validation and format migrations.
3. `nova-core`: package records, dependency graph, permissions and installed-state database.
4. `nova-sdk`: public API and sample mod tooling.
5. `geode-compat`: metadata detection and compatibility tests; execution compatibility is a separate milestone.
6. `nova-runtime`: version/ABI-specific native integration, only after feasibility is proven.

These are architectural targets, not modules that already exist.

## Geode Android reference findings

The supplied APK contains a launcher layer, a `liblauncherfix.so` helper for Android ABIs, JNI bridge symbols and references to `libcocos2dcpp.so`, profile paths and safe mode. The APK does not contain the Geometry Dash native engine itself. See [GEODE_ANDROID_APK_ANALYSIS.md](GEODE_ANDROID_APK_ANALYSIS.md).

This static evidence informs the separation of concerns; it does not establish the exact runtime patch sequence or prove that the same mechanism can be reused by NovaLoader. NovaLoader must implement its own code and validate each supported build.

## Security principles

- Never execute a package as part of importing it.
- Keep imported archives in app-private storage and do not extract package paths.
- Bound the imported archive size, expanded content and number of ZIP entries.
- Reject absolute paths, parent-directory path segments, duplicate ZIP entry names and invalid manifests.
- Treat repository metadata, manifests and package descriptions as untrusted data.
- Do not embed signing keys or access tokens in the app or repository.
- Future mod permissions must be explicit, reviewable and default to least privilege.

## Runtime feasibility gate

Before advertising in-game loading, document and test at least: exact Geometry Dash build, Android version, arm64-v8a/armeabi-v7a ABI, native library loading strategy, clean uninstall/recovery, crash behaviour and separation between trusted and untrusted mods. Importing a file extension does not imply compatibility.
