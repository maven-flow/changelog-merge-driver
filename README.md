# GIT Changelog Merge Driver

This is a custom GIT merge driver that you can use to avoid conflicts when merging changelogs.

The changelogs must adhere to the format specified by [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
On top of this format, some additional features are supported. See [Changelog Format Extensions](#changelog-format-extensions).

## Usage

NOTE: To run this merge driver, you need to have Java installed. The minimum version is 17.

- [Download](https://github.com/maven-flow/changelog-merge-driver/packages/2134483) the merge driver jar or clone this repository and build it by running `mvn package`.

- Configure the merge driver in GIT:

```
$ git config merge.changelog.driver "java -jar <path_to_driver_jar> %A %O %B"
$ git config merge.changelog.name "Merge driver for changelogs"
```

- Tell GIT to use the merge driver by adding a `.gitattributes` file into your repository with the following content:

```
**/CHANGELOG.md merge=changelog
```

There is also an [automatic merge GitHub action](https://github.com/marketplace/actions/maven-flow-merge) that utilizes this merge driver.

## How It Works

- Take `ours` changelog file and use it as a base.

- Take all versions from `theirs` that are missing in `ours` and put them into `ours` before (on top of) the already present versions.

### Unreleased Versions

The first version in the changelog may be marked as [Unreleased](https://keepachangelog.com/en/1.1.0/#effort). This version is treated in a special way:

- If `ours` contains an unreleased version and `theirs` contains new released versions, all changes from the released versions will be merged also into the unreleased version in chronological order (older versions first). See [Merging Versions](#merging-versions).

- If `ours` contains an unreleased version and `theirs` also contains an unreleased version, the unreleased version from `theirs` will be merged into the unreleased version in `ours` (see [Merging Versions](#merging-versions)).

- If `ours` does not contain an unreleased version but `theirs` does, the unreleased version from `theirs` is copied into `ours`.

### Merging Versions

NOTE: Merging is only applied to unreleased versions and not to released versions (see [Limitations](#limitations)).

Each version is separated into sections ("Added", "Changed", "Fixed", "Removed", etc.) and each sections contains items (lines). The versions are merged by sections, which means that `their` "Added" section is merged into `our` "Added" section, `their` "Changed" section is merged into `our` "Changed" section, etc.

When merging sections, items (lines) from `theirs` section are added into `ours` section after the items already present in `ours` section. Adding duplicates is avoided (if an item from `theirs` is already present in `ours`, it is not added again).

If a section from `theirs` is not present in `ours`, it is copied into `ours`.

### Example

Let's say that you have the following changelog in your `develop` branch:

```markdown
# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added

- Feature 1 from unreleased
- Feature 2 from unreleased

### Changed

- Change 1 from unreleased
- Change 2 from unreleased

## [1.0.0] - 2024-02-27

### Added

- Feature 1 from 1.0.0
- Feature 2 from 1.0.0

### Changed

- Change 1 from 1.0.0
- Change 2 from 1.0.0
```

Now let's say you have a changelog in your `main` branch, which contains 2 new released versions `1.1.0` and `1.2.0` and some new unreleased changes:

```markdown
# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added

- Feature 1 from unreleased
- Feature 2 from unreleased
- Feature 3 from unreleased (new)
- Feature 4 from unreleased (new)

### Changed

- Change 1 from unreleased
- Change 2 from unreleased
- Change 3 from unreleased (new)
- Change 4 from unreleased (new)

## [1.2.0] - 2024-04-27

### Added

- Feature 1 from 1.2.0
- Feature 2 from 1.2.0

### Fixed

- Fix 1 from 1.2.0
- Fix 2 from 1.2.0

## [1.1.0] - 2024-03-27

### Added

- Feature 1 from 1.1.0
- Feature 2 from 1.1.0

### Changed

- Change 1 from 1.1.0
- Change 2 from 1.1.0

## [1.0.0] - 2024-02-27

### Added

- Feature 1 from 1.0.0
- Feature 2 from 1.0.0

### Changed

- Change 1 from 1.0.0
- Change 2 from 1.0.0
```

If you merge `main` branch into `develop`, you will get the following result:

```markdown
# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added

- Feature 1 from unreleased
- Feature 2 from unreleased
- Feature 1 from 1.1.0
- Feature 2 from 1.1.0
- Feature 1 from 1.2.0
- Feature 2 from 1.2.0
- Feature 3 from unreleased (new)
- Feature 4 from unreleased (new)

### Changed

- Change 1 from unreleased
- Change 2 from unreleased
- Change 1 from 1.1.0
- Change 2 from 1.1.0
- Change 3 from unreleased (new)
- Change 4 from unreleased (new)

### Fixed

- Fix 1 from 1.2.0
- Fix 2 from 1.2.0

## [1.2.0] - 2024-04-27

### Added

- Feature 1 from 1.2.0
- Feature 2 from 1.2.0

### Fixed

- Fix 1 from 1.2.0
- Fix 2 from 1.2.0

## [1.1.0] - 2024-03-27

### Added

- Feature 1 from 1.1.0
- Feature 2 from 1.1.0

### Changed

- Change 1 from 1.1.0
- Change 2 from 1.1.0

## [1.0.0] - 2024-02-27

### Added

- Feature 1 from 1.0.0
- Feature 2 from 1.0.0

### Changed

- Change 1 from 1.0.0
- Change 2 from 1.0.0
```

### Changelog Format Extensions

On top of the standard format defined in [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), this merge driver supports additional changelog features:

#### Marking Unreleased Versions

The standard format specifies that unreleased versions should be defined with name `[Unreleased]` and without a specific number. This merge driver also supports specifying numbers for unreleased versions, and also using the word "SNAPSHOT", which is equivalent to "Unreleased". The check is not case-sensitive.

These are all valid forms of specifying an unreleased version:

- `[Unreleased]`
- `[UNRELEASED]`
- `Unreleased`
- `UNRELEASED`
- `[1.0.0] - Unreleased`
- `[1.0.0] - [Unreleased]`
- `[Snapshot]`
- `[SNAPSHOT]`
- `Snapshot`
- `SNAPSHOT`
- `[1.0.0] - SNAPSHOT`
- `[1.0.0] - [SNAPSHOT]`
- `[1.0.0-SNAPSHOT]`

#### Version Descriptions

Versions can have a generic block of text before the standard sections.

For example:

```markdown
## [1.0.0] - 2024-04-27

This is a generic version description.
More than one line is supported.

### Added

- Added feature
```

### Limitations

For the sake of simplicity and performance, the merge driver has the following limitations. This may be changed in the future, if needed.

- The changelog header (the top part of the file which contains the caption and description, up to the first version) is not merged.
  It is copied from `ours`. Any changes made to the header in `theirs` will be lost. If you want to preserve those changes, you have to perform a standard GIT merge.

-  The released versions already present in `ours` are not merged. They are simply kept without modification. If `theirs` contains any changes in these versions, those changes will be lost. If you want to preserve those changes, you have to perform a standard GIT merge.

- Newly added versions are not sorted in any way. They are always considered to be newer than the already present versions, and therefore are always added to the top. This approach generally works without issues, given that the changelogs are being merged regularly.
