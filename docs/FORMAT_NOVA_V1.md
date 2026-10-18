# NOVA package format — version 1 (draft)

A `.nova` file is a ZIP archive. NovaLoader v0.1 accepts it for metadata validation and private storage only; it does not execute its contents.

## Required layout

    example.nova
    ├── manifest.json
    ├── README.md              optional
    ├── resources/             optional
    ├── locales/               optional
    └── payload/               reserved; not executed by current versions

The `manifest.json` file must be at the archive root and contain valid UTF-8 JSON.

## Example manifest

    {
      "format": "nova",
      "formatVersion": 1,
      "id": "example.sample-mod",
      "name": "Sample Mod",
      "version": "0.1.0",
      "author": "Example Author",
      "description": "An example package for testing the format.",
      "game": {
        "versions": ["2.2081"],
        "platforms": ["android"],
        "architectures": ["arm64-v8a"]
      },
      "permissions": [],
      "dependencies": [],
      "entrypoint": null
    }

## Validation rules in current app

- `format` must equal `nova`.
- `formatVersion` must equal 1.
- `id` must match `[A-Za-z0-9][A-Za-z0-9._-]{1,63}`.
- `name` and `version` must be non-empty strings.
- `manifest.json` must be no larger than 256 KiB.
- The archive may contain no more than 1,000 entries.
- Absolute paths, drive-prefixed paths, parent/current-directory path segments, backslashes, NUL characters and duplicate entry names are rejected.
- The compressed archive must be no larger than 100 MiB.
- Known expanded entry sizes are limited to 64 MiB in total.
- If `game.platforms` is declared, it must include Android.
- If `game.architectures` is declared, at least one ABI must match the device.
- Declared Geometry Dash versions are shown as metadata only; the current app does not establish the installed game's exact binary build.

## Not implemented

Dependency installation, permission enforcement, cryptographic signatures/hashes, version constraints, payload ABI inspection and runtime entrypoints are not implemented. The validator is not a malware scanner and must not be treated as proof that a package is trustworthy.

## Compatibility policy

A NOVA v1 archive is not a `.geode` file and does not imply Geode API compatibility. Conversion or binary execution must not be claimed unless a dedicated adapter and tests demonstrate it.
