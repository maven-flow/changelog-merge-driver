# Changelog Merge Example (Normal Mode)

Let's say that you have the following changelog in your `develop` branch:

```markdown
# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added

- Feature 1 in unreleased
- Feature 2 in unreleased

### Changed

- Change 1 in unreleased
- Change 2 in unreleased

## [1.0.0] - 2024-02-27

### Added

- Feature 1 in 1.0.0
- Feature 2 in 1.0.0

### Changed

- Change 1 in 1.0.0
- Change 2 in 1.0.0
```

Now let's say you have a changelog in your `main` branch, which contains 2 new released versions `1.1.0` and `1.2.0` and some new unreleased changes:

```markdown
# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added

- Feature 1 in unreleased
- Feature 2 in unreleased
- Feature 3 in unreleased (new)
- Feature 4 in unreleased (new)

### Changed

- Change 1 in unreleased
- Change 2 in unreleased
- Change 3 in unreleased (new)
- Change 4 in unreleased (new)

## [1.2.0] - 2024-04-27

### Added

- Feature 1 in 1.2.0
- Feature 2 in 1.2.0

### Fixed

- Fix 1 in 1.2.0
- Fix 2 in 1.2.0

## [1.1.0] - 2024-03-27

### Added

- Feature 1 in 1.1.0
- Feature 2 in 1.1.0

### Changed

- Change 1 in 1.1.0
- Change 2 in 1.1.0

## [1.0.0] - 2024-02-27

### Added

- Feature 1 in 1.0.0
- Feature 2 in 1.0.0

### Changed

- Change 1 in 1.0.0
- Change 2 in 1.0.0
```

If you merge `main` branch into `develop`, you will get the following result:

```markdown
# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added

- Feature 1 in unreleased
- Feature 2 in unreleased
- [from `1.1.0`] Feature 1 in 1.1.0
- [from `1.1.0`] Feature 2 in 1.1.0
- [from `1.2.0`] Feature 1 in 1.2.0
- [from `1.2.0`] Feature 2 in 1.2.0
- Feature 3 in unreleased (new)
- Feature 4 in unreleased (new)

### Changed

- Change 1 in unreleased
- Change 2 in unreleased
- [from `1.1.0`] Change 1 in 1.1.0
- [from `1.1.0`] Change 2 in 1.1.0
- Change 3 in unreleased (new)
- Change 4 in unreleased (new)

### Fixed

- [from `1.2.0`] Fix 1 in 1.2.0
- [from `1.2.0`] Fix 2 in 1.2.0

## [1.2.0] - 2024-04-27

### Added

- Feature 1 in 1.2.0
- Feature 2 in 1.2.0

### Fixed

- Fix 1 in 1.2.0
- Fix 2 in 1.2.0

## [1.1.0] - 2024-03-27

### Added

- Feature 1 in 1.1.0
- Feature 2 in 1.1.0

### Changed

- Change 1 in 1.1.0
- Change 2 in 1.1.0

## [1.0.0] - 2024-02-27

### Added

- Feature 1 in 1.0.0
- Feature 2 in 1.0.0

### Changed

- Change 1 in 1.0.0
- Change 2 in 1.0.0
```
