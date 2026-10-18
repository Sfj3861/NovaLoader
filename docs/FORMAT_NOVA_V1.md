# Nova package format — version 1 (draft)

A .nova file is a ZIP archive. v0.1 accepts it for metadata validation and storage only; it does not execute its contents.

## Required layout

    example.nova
    ├── manifest.json
    ├── README.md              optional
    ├── resources/             optional
    ├── locales/               optional
    └── payload/               reserved; not executed by v0.1

The manifest.json file must be at the archive root and must be valid UTF-8 JSON.

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

## v1 validation rules in v0.1

- format must equal nova.
- formatVersion must equal 1.
- id must match the pattern [A-Za-z0-9][A-Za-z0-9._-]{1,63}.
- name and version must be non-empty strings.
- manifest.json must be no larger than 256 KiB.
- The archive must contain no more than 1,000 entries.
- Absolute paths, parent-directory path segments, backslashes and duplicate entry names are rejected.
- The source archive must be no larger than 100 MiB.

Other fields are reserved/metadata-only in this milestone. Dependency installation, permission enforcement, signatures, hashing, version constraints and runtime entrypoints are not implemented yet. The validator is not a malware scanner and must not be treated as proof that a package is trustworthy.

## Compatibility policy

A .nova v1 archive is not a .geode file and does not imply Geode API compatibility. Conversion or binary execution must not be claimed unless a dedicated adapter and tests demonstrate it.
