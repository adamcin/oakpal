# Change Log

All notable changes to this project will be documented in this file. 

The format is based on [Keep a Changelog](http://keepachangelog.com)

## [Unreleased]

### Added
- #38 modified pom to upload cli dist binaries to releases.
- #39 add --no-hooks cli option

### Fixed
- ExpectAces ACE criteria now correctly trims around parameter names
- #51 copied InstallHookPolicy details to scan goal doc
- #37 switched to jar checksums for opear cache folder names.

## [1.5.1] - 2019-10-03

### Added
- Added `severity` config parameter to ExpectPaths and ExpectAces checks
- Added `principals` config parameter to ExpectAces check as multi-valued alternative to single-value `principal` param.
- Added `principal=` key to allowed ACE criteria syntax in the ExpectAces check to allow per-ACE override of `principal` config parameter.

## [1.5.0] - 2019-10-02

### Changed 
- Upgraded oak dependencies to 1.18.0, Jackrabbit to 2.18.3, and FileVault to 3.4.0.

### Added
- #18 Added CI builds for windows and mac osx.
- Added ExpectPaths and ExpectAces checks to core.
- Added SubackageSilencer to OakMachine and silenceAllSubpackages param to oakpal:scan.

### Fixed
- #20 resolved windows test failures

## [1.4.2] - 2019-09-06

### Changed
- Added timer logic to basic/echo check to assist in performance testing

### Fixed
- CLI didn't correctly redirect System.out to System.err for scan execution. 

## [1.4.1] - 2019-09-05

### Added
- bnd-baseline-maven-plugin has been added to core and webster modules to enforce semantic versioning of the API.
- Added runtime options to cli (--store-blobs) and maven plugin (-DstoreBlobs) to trade higher memory usage for lower I/O overhead.

### Changed
- CLI now redirects System.out to System.err before loading progress checks.
- CLI and maven plugin no longer cache blobs by default.
- Module README.md files now used as source for about / index.html for maven-site-plugin.

### Fixed
- #16 relocated default target files under single oakpal-plugin folder
- #17 eliminated oakpal-core transitive dependencies and duplicates from embedded libraries

## [1.4.0] - 2019-09-03

### Added
- Added oakpal-cli and Dockerfile

### Changed
- Introduced blob caching for cli and maven execution by default

### Fixed
- 100% test coverage uncovered several bugs which have been fixed

### Removed
- Removed aem and interactive modules
