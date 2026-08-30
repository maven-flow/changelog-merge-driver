# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- [#1](https://github.com/maven-flow/changelog-merge-driver/issues/1) Convert to jbang

### Changed

- When the version cannot be read from the jar manifest (which jbang runs have none of), the startup line now says "(from source)" instead of "null".
- Released versions present in both changelogs are now merged line-based three-way against the Git merge base (the `%O` file, previously ignored), instead of being kept from `ours` unchanged. Where both sides changed the same lines differently, `theirs` wins.
- The changelog header (the caption and the description above the first version) is now merged the same way, instead of being copied as a whole from one side.
- Upgraded all dependencies to latest versions.

### Fixed

- Merging or rebasing a changelog without an unreleased version in `ours` crashed with a `NullPointerException`.
- The unbracketed unreleased markers listed under [Marking Unreleased Versions](README.md#marking-unreleased-versions) (`## Unreleased`, `## SNAPSHOT`, ...) crashed the driver. They are now parsed, and written back in the canonical `## [Unreleased]` form.
- Other level 2 headings without a bracketed version name (for example `## Older versions`) crashed the same way. They are now kept as content of the enclosing version or section.
- A version heading without a release date (for example `## [Unreleased]`) got the heading itself assigned as its release date.
- A release date separated from the version name by something other than `" - "` (for example an en dash) was lost the same way. The separator is now skipped whatever it is, and the canonical `" - "` is written back.
- [#10](https://github.com/maven-flow/changelog-merge-driver/issues/10) Version heading links are dropped when the changelog is written back

## [0.4.0] - 2024-07-13

### Added

- Rebase mode (activated with argument `--rebase`).

## [0.3.0] - 2024-05-26

### Fixed

- Empty sections (Added, Fixed, ...) were added to the unreleased version during merge in some cases.

## [0.2.0] - 2024-05-26

### Added

- Console log to inform that the merge driver is running.
- "From labels": if a change has been merged from another released version, the change is prefixed with the word "from" and the released version name.
  For example: ``- [from `1.1.0`] Feature introduced in version 1.1.0``

### Fixed

- Duplicated unreleased items: If an item in the unreleased section has been merged from `theirs` into `ours` before, then released in `ours` (but not in `theirs`) and now is being merged again, it is not added to the unreleased section in `ours` again.

## [0.1.0] - 2024-04-29

### Added

- First released version.

[Unreleased]: https://github.com/maven-flow/changelog-merge-driver/compare/0.4.0...HEAD
[0.4.0]: https://github.com/maven-flow/changelog-merge-driver/compare/0.3.0...0.4.0
[0.3.0]: https://github.com/maven-flow/changelog-merge-driver/compare/0.2.0...0.3.0
[0.2.0]: https://github.com/maven-flow/changelog-merge-driver/compare/0.1.0...0.2.0
[0.1.0]: https://github.com/maven-flow/changelog-merge-driver/releases/tag/0.1.0
