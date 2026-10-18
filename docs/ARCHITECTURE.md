# NovaLoader architecture (draft)

## v0.1 boundaries

The first app is a package-library prototype. It does not patch, inject into or launch Geometry Dash and must not claim that any mod is running.

- Android app: Kotlin UI, document picker, private archive storage and package list.
- Nova format: versioned ZIP container with a root manifest.
- Geode compatibility: currently only accepts a .geode file into private storage. No binary compatibility or execution is implemented.
- Native runtime: intentionally absent until supported game versions, ABIs, loading points and recovery behaviour can be tested on real devices.
- Repository service: future work; v0.1 does not download third-party code.

## Planned module boundaries

1. app: screens, file picker, lifecycle and user-facing errors.
2. nova-format: manifest models, archive validation and format migrations.
3. nova-core: package records, dependency graph, permissions and installed-state database.
4. nova-sdk: public API and sample mod tooling.
5. geode-compat: metadata detection and compatibility tests; execution compatibility is a separate milestone.
6. nova-runtime: version/ABI-specific native integration, only after feasibility is proven.

These modules are architectural targets, not modules that already exist in v0.1.

## Security principles

- Never execute a package as part of importing it.
- Keep imported archives in app-private storage and do not extract package paths.
- Bound the imported archive size and the number of ZIP entries.
- Reject absolute paths, parent-directory path segments, duplicate ZIP entry names and invalid manifests.
- Treat repository metadata, manifests and package descriptions as untrusted data.
- Do not embed signing keys or access tokens in the app or repository.
- Future mod permissions must be explicit, reviewable and default to least privilege.

## Runtime feasibility gate

Before advertising in-game loading, document and test at least: exact Geometry Dash version, Android version, arm64-v8a/armeabi-v7a ABI, native library loading strategy, clean uninstall/recovery, crash behaviour and separation between trusted and untrusted mods. No compatibility is implied by importing a file extension.
