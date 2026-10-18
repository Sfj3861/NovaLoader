# NovaLoader architecture and implementation plan

## Product decision

NovaLoader will use a **hybrid architecture**:

- Use the public Geode Android launcher and runtime as architectural references, and investigate supported integration points and licensing before any reuse.
- Keep NovaLoader's Android launcher, package manager, compatibility checks, recovery UX and NOVA package format as first-party components.
- Treat Geode packages as a separate format. A `.geode` archive must not be described as runnable merely because its metadata can be read.
- Do not copy proprietary game files or compiled Geode binaries into this repository.

## Target architecture

```text
NovaLoader Android UI
  ├── Game detector and compatibility report
  ├── Package manager
  │     ├── .geode metadata / safe archive inspection
  │     └── .nova manifest / NOVA package validation
  ├── Launch coordinator
  │     ├── normal game launch
  │     └── modded launch (only when a verified backend exists)
  └── Recovery / safe-mode UX

Native integration backend (future; version-specific)
  ├── exact game build + ABI validation
  ├── native bootstrap / loader
  ├── mod discovery and dependency resolution
  └── runtime API for supported package types
```

## Compatibility policy

- Never assume that a package ID or Android package name proves compatibility.
- Record the installed Geometry Dash version, package identifier and device ABI where Android permits reliable detection.
- The modded launch action must remain disabled unless a compatible backend is installed and its compatibility checks pass.
- Keep normal launch distinct from modded launch and label both honestly.
- Provide a recovery path that starts without third-party mods after a failed launch.
- Never execute package payloads during import or metadata inspection.

## Package formats

### Geode (.geode)

Treat this as a separate ecosystem. Inspect archive metadata and show the ID, version and declared dependencies. Actual installation/execution requires a compatible Geode runtime and a supported game build. Do not translate or relabel a Geode archive as a NOVA package.

### NOVA (.nova)

Version 1 is a ZIP archive with a root `manifest.json`. The manifest must identify its format/version, package ID, name and version. Future executable payloads must specify a documented ABI, permissions/capabilities and runtime API version. The format validator is not a runtime and is not a malware scanner.

## Implementation milestones

1. **Launcher foundation** — game detection, explicit normal launch, truthful UI states and compatibility report.
2. **Package manager** — metadata, install/uninstall/enable state, dependency display, integrity checks and storage management.
3. **Native feasibility prototype** — identify exact supported game builds and ABIs; investigate lawful, maintainable integration and build a minimal non-destructive bootstrap.
4. **First verified mod** — load a deliberately small test mod in a controlled build; verify logs, safe mode and recovery on a real device.
5. **Format/runtime contract** — document NOVA API and package ABI; only then enable execution for NOVA packages.
6. **Compatibility and release** — test clean install, upgrade, failed launch, missing dependencies, invalid archives and recovery on each supported build/ABI.

## Current implementation status

The Android app currently has package storage/inspection and a button that attempts to launch the installed game normally. It does **not** inject a native runtime, load Geode or NOVA mods, resolve dependencies, or provide a verified modded launch. A successful APK build proves compilation only, not in-game compatibility.

## Reference projects

- Geode Android launcher: https://github.com/geode-sdk/android-launcher
- Geode framework: https://github.com/geode-sdk/geode

These are references for research, not a claim that their binaries or code are embedded in NovaLoader. Check the relevant licenses and upstream documentation before reusing code.
