# Changelog Merge Driver Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.0] - [SNAPSHOT]



## [0.2.0] - 2024-05-26

### Added

- Console log to inform that the merge driver is running.
- "From labels": if a change has been merged from another released version, the change is prefixed with the word "from" and the released version name.
  For example: ``- [from `1.1.0`] Feature introduced in version 1.1.0``

### Fixed

- Duplicated unreleased items: If an item in the unreleased section has been merged from `theirs` into `ours` before, then released in `ours` (but not in `theirs`) and now is being merged again, it is not added to the unreleased section in `ours` again.

## [0.1.0] - 2024-04-29

First released version.
