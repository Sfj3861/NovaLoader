# Native runtime feasibility — research and prototype (v0.4.0)

## Research findings

The public Geode Android Launcher project describes its goal as launching an unmodified Geometry Dash installation with the Geode loader added. Its launcher repository is public under the BSL-1.0 license; the separate Geode framework is also public, but reuse must be evaluated against each repository's current license and notices.

- Android launcher: https://github.com/geode-sdk/android-launcher
- Geode framework: https://github.com/geode-sdk/geode
- Geode Android update notes: https://github.com/geode-sdk/geode/wiki/GD-Update-Procedure

The framework source currently declares Geometry Dash Android 2.2081 as its configured game version. Its update procedure describes architecture-specific native symbol extraction and patch-address work. This is evidence that a compatible loader is not just a Kotlin package manager: it needs a native backend tied to the exact game binary and ABI.

Sources checked on 2026-10-18:
- https://github.com/geode-sdk/android-launcher
- https://github.com/geode-sdk/geode/blob/main/CMakeLists.txt
- https://github.com/geode-sdk/geode/wiki/GD-Update-Procedure

## Decision

Do not invent memory offsets or assume that a package name proves compatibility. NovaLoader should first prove its own JNI build and device ABI reporting, then establish a supported backend for one exact Geometry Dash build.

The initial integration strategy is to study the public launcher and framework as upstream references, then decide whether a licensed, maintainable integration or a clean-room compatible backend is feasible. Do not copy prebuilt Geode binaries or game binaries into NovaLoader.

## What this prototype implements

Version 0.3.0 adds a small C++ shared library built by Android CMake and called through JNI. The in-app diagnostic reports whether NovaLoader's own native library can be called, the ABI for which it was compiled, and native pointer width.

This is deliberately a native build/integration probe, not a mod loader. It does not open or modify Geometry Dash, inject into another process, patch memory, load Geode packages, or execute NOVA payloads.

## Next milestones

1. Run CI for the JNI/CMake build and fix compile issues.
2. Test the diagnostic on a physical Android device and record its ABI.
3. Pick one exact Geometry Dash build and inspect compatibility evidence from a lawful installation.
4. Build a minimal, isolated backend proof of concept only after the exact build, licensing and integration model are established.
5. Add safe-mode/recovery behavior before any experimental game integration.
6. Load and verify one harmless test mod before claiming runtime support.

## Exit criteria for a real runtime

Do not mark mod support implemented until a device test demonstrates all of the following:
- Exact game version and ABI identified.
- Compatible native backend present and loaded.
- A test mod is loaded inside the game and its expected behavior is observed.
- Failure path returns to vanilla launch or safe mode.
- Logs distinguish package validation, backend initialization and mod activation.
