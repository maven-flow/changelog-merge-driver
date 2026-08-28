# Changelog Merge Driver Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.5.0] - [SNAPSHOT]

### Added

- The driver can be run with [jbang](https://www.jbang.dev/), which removes the need to download a jar and point Git at its path: `jbang changelog-merge-driver@maven-flow/changelog-merge-driver %A %O %B`. The jar keeps working unchanged.

### Changed

- When the version cannot be read from the jar manifest (which jbang runs have none of), the startup line now says "(from source)" instead of "null".
- Released versions present in both changelogs are now merged line-based three-way against the Git merge base (the `%O` file, previously ignored), instead of being kept from `ours` unchanged. A fix made in `theirs` to an already-released version is no longer lost; where both sides changed the same lines differently, `theirs` wins.
- The changelog header (the caption and the description above the first version) is now merged the same way, instead of being copied as a whole from `ours` (merge) or `theirs` (rebase).

### Fixed

- Merging or rebasing a changelog without an unreleased version in `ours` crashed with a `NullPointerException`.

### Fixed

- The unbracketed unreleased markers listed under [Marking Unreleased Versions](README.md#marking-unreleased-versions) (`## Unreleased`, `## SNAPSHOT`, ...) crashed the driver with a `StringIndexOutOfBoundsException`. They are now parsed, and written back in the canonical `## [Unreleased]` form.
- Other level 2 headings without a bracketed version name (for example `## Older versions`) crashed the same way. The parser now keeps them as content of the enclosing version or section. (A heading that lands in a version description — before the first `### ` section heading — is still subject to the pre-existing limitation that version descriptions are not carried through a merge.)
- A version heading without a release date (for example `## [Unreleased]`) got the heading itself assigned as its release date, and was printed back as `## [Unreleased] -  [Unreleased]`.
- A release date separated from the version name by something other than `" - "` (for example an en dash) got the whole heading assigned as its release date, the same way. The separator is now skipped whatever it is, and the canonical `" - "` is written back; trailing whitespace on the heading line is dropped.

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

First released version.
