# Change Log

All notable changes to this project will be documented in this file. 

The format is based on [Keep a Changelog](http://keepachangelog.com)

## [Unreleased]

## [2.2.1] - 2020-09-11

### Added

- Duplicated OakMachineCaliperTest as an IT to re-run end-to-end test against shaded jar

### Fixed

- Added missing shade include for felix.cm.file.ConfigurationHandler.

## [2.2.0] - 2020-09-09

### Added

- Added Sling simulation behavior with specific ProgressCheck handling for Embedded Packages, 
RepositoryInitializer (repoinit) scripts, and OSGi Configurations in sling:OsgiConfig nodes, plus .cfg.json, 
.config, and .properties/.cfg file formats.
- CLI and maven-plugin execution support specifying runModes to simulate effects on JCR Installer 
behavior.
- Added caliper content-package module structure to provide a common basis for oakpal self-test 
functionality in the future. The intention is for it eventually to drive at least one example 
of every event expressible to a ProgressCheck by the OakMachine.
- Added subpackage and embedded package sorted installation similar to BEST_EFFORT DependencyHandling behavior
to give some expectation and control of predictable install order.

## [2.1.0] - 2020-05-28

### Added

- Added repoinit support for init stages adapted from plans and checklists.
- OakMachine now skips subpackage extraction based on SubPackageHandling.

## [2.0.0] - 2020-04-27

### Added

- API extracted to net.adamcin.oakpal.api package in new oakpal-api module for tighter dependency control, requiring a major version bump.
- @ProviderType and @ConsumerType annotations added to interfaces in exported packages
- Added setResourceBundle() method to ViolationReporter interface and default implementations to support future i18n enhancements.
- Added SimpleViolation.builder() with easy support for formatting violation description with MessageFormat.format(), using withArguments() builder method.
- Migrated CompositeStoreAlignment check from ACS AEM Commons because of unavoidable tight-coupling to oak-core-spi classes.
- Added --plan-file, --plan-file-base, and --extend-classpath CLI parameters

### Changed

- Added overload with default implementation for ProgressCheck.importedPath() that accepts new PathAction enumerator type. Other signature now deprecated.
- Moved Violation.Severity enumerator to top-level type because 1) I wanted to and 2) the API extraction made this a convenient time to do it.
- JSON Config keys are now managed in child interface types with @ProviderType annotations to avoid major version bumps when adding config constants.

## [1.5.2] - 2020-04-15

### Added
- #38 modified pom to upload cli dist binaries to releases.
- #39 add --no-hooks cli option

### Fixed
- #50 Provide CLI dist with Windows binary launcher
- ExpectAces ACE criteria now correctly trims around parameter names
- #35 copied InstallHookPolicy details to scan goal doc
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
