# Changelog

## Unreleased

### Added

### Changed

### Removed

### Fixed

## 2.3.0 - 2024-07-25

### Fixed

- Fix compatibility with IntelliJ 2024.2
- Fix error "TreeUI should be accessed only from EDT" during processes reload
- Fix "container is already disposed" error during table selection

## 2.2.0 - 2023-09-12

### Added

- Improve double click handling in the process overview table

### Changed

- Improve tool window icon for IntelliJ's new UI

## 2.1.0 2023-02-09

### Added

- Offer open settings in the "No processes collected yet" panel

### Changed

- Improve the UI of the process details panel
- Improve compatibility with IntelliJ's new experimental UI

### Fixed

- Fix incapability of icon scaling
- Fix JVM processes are collected during the opening of every tab

## 2.0.1 - 2022-11-30

### Fixed

- Fix IntelliJ 2022.3 compatibility

## 2.0.0 - 2022-10-26

### Added

- Add action to attach Java agents
- Add information about attached Java agents
- Add more IntelliJ/JetBrains processes to the process type mapping

### Changed

- Update OSHI core to 6.3.0

### Fixed

- Show only the JAR filename for the JVM entry point
- Show open file handles count for macOS

## 1.1.0 - 2022-08-10

### Added

- Add debug agent information
- Add list open files function
- Add list open ports function
- Add more process name to icons mappings

### Changed

- Collect processes during everytime the tool window gets opened

## 1.0.1 - 2022-07-07

### Fixed

- Prevent a NPE if the user, group or working directory of a process is unknown.
- Don't hide stripe button by default.

## 1.0.0 - 2022-07-02

### Added

- Initial Release
