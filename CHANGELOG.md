# Change Log

All notable changes to this project will be documented in this file. 

The format is based on [Keep a Changelog](http://keepachangelog.com)

## [Unreleased]

### Added
- bnd-baseline-maven-plugin has been added to core and webster modules to enforce semantic versioning of the API.
- Added runtime options to cli (--no-blobs) and maven plugin (-DnoBlobStore) to trade higher memory usage for lower I/O overhead.

### Changed
- CLI now redirects System.out to System.err before loading progress checks.

## [1.4.0] - 2019-09-03

### Added
- Added oakpal-cli and Dockerfile

### Changed
- Introduced blob caching for cli and maven execution by default

### Fixed
- 100% test coverage uncovered several bugs which have been fixed

### Removed
- Removed aem and interactive modules