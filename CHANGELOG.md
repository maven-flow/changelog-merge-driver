# Changelog Merge Driver Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.5.0] - [SNAPSHOT]

### Added

- The driver can be run with [jbang](https://www.jbang.dev/), which removes the need to download a jar and point GIT at its path: `jbang changelog-merge-driver@maven-flow/changelog-merge-driver %A %O %B`. The jar keeps working unchanged.

### Changed

- When the version cannot be read from the jar manifest (which jbang runs have none of), the startup line now says "(from source)" instead of "null".
- Released versions present in both changelogs are now merged line-based three-way against the GIT merge base (the `%O` file, previously ignored), instead of being kept from `ours` unchanged. A fix made in `theirs` to an already-released version is no longer lost; where both sides changed the same lines differently, `theirs` wins.



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
