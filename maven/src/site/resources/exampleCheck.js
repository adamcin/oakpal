/*
 * Copyright 2020 Mark Adamcin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * 1. Implement the optional listener functions stubbed out below to perform your Oak package acceptance tests.
 *
 * 2. Use the oakpal global object to report violations
 *
 *   For MINOR violations:
 *     oakpal.minorViolation(description, packageId);
 *
 *   For MAJOR violations:
 *     oakpal.majorViolation(description, packageId);
 *
 *   For SEVERE violations:
 *     oakpal.severeViolation(description, packageId);
 *
 * 3. If configuration parameters are available, they can be accessed using the "config" global object.
 *
 * Java type imports for reference:
 *
 * import org.apache.jackrabbit.vault.packaging.PackageId;
 * import org.apache.jackrabbit.vault.packaging.PackageProperties;
 * import org.apache.jackrabbit.vault.fs.config.MetaInf;
 * import java.io.File;
 * import java.util.jar.Manifest;
 * import javax.jcr.Node;
 * import javax.jcr.Session;
 * import net.adamcin.oakpal.api.PathAction;
 * import net.adamcin.oakpal.api.SlingSimulator;
 * import net.adamcin.oakpal.api.SlingInstallable;
 * import net.adamcin.oakpal.api.EmbeddedPackageInstallable;
 */

/***
 * Optionally implement this function to return a label for your check.
 */

/*
function getCheckName() {
    return "ACME Example Check";
}
*/

/**
 * Override this method to accept a SlingSimulator to request installation of JCR resources like
 * FileVault packages and RepositoryInitializer factory configs, as if running within a Sling repository instance.
 * <p>
 * Also provided are the set of simulated Sling Run Modes. These are intended to drive construction of JCR path
 * patterns to select embedded resources for installation upon receiving a matching
 * importedPath(PackageId, String, Node, PathAction) event, but these run modes may also be
 * used as a global configuration hint for progress checks that support modal behavior outside of their explicit
 * JSON config format. NOTE: this set will always be empty by default, and must be populated in the plan or
 * overridden at runtime in the execution layer.
 *
 * @param slingSimulator    the sling simulator
 * @param runModes          the simulated sling run modes
 * @since 2.2.0
 */
function simulateSling(slingSimulator /* SlingSimulator */, runModes /* String[] */) {

}

/**
 * Called once at the beginning of the scan.
 */
function startedScan() {

}

/**
 * Called after the package is uploaded to the package manager at the beginning of the scan. Track subsequent
 * events using the package ID provided to this method.
 *
 * @param packageId         the package ID of the newly opened package
 * @param file              the package file that will be opened
 */
function identifyPackage(packageId /* PackageId */, file /* File */) {

}

/**
 * Called after each subpackage is opened. Track subsequent events using the package ID provided to this method.
 *
 * @param packageId         the package ID of the newly opened subpackage
 * @param parentId          the package ID of the parent package.
 */
function identifySubpackage(packageId /* PackageId */, parentId /* PackageId */) {

}

/**
 * Called when a package has a jar manifest that can be read for additional metadata, like Export-Package.
 *
 * @param packageId         the package ID of the newly opened package
 * @param manifest          the parsed manifest
 * @since 1.3.0
 */
function readManifest(packageId /* PackageId */, manifest /* Manifest */) {

}

/**
 * Called for each package before it is extracted.
 *
 * @param packageId         the package ID of the newly opened package
 * @param inspectSession    session providing access to repository state
 * @param packageProperties the package properties
 * @param metaInf           the package meta information
 * @param subpackages       extracted subpackages
 */
function beforeExtract(packageId /* PackageId */, inspectSession /* Session */,
                       packageProperties /* PackageProperties */, metaInf /* MetaInf */,
                       subpackages /* PackageId[] */) {

}

/**
 * Notified when package importer adds, modifies, or leaves a node untouched.
 *
 * @param packageId         the current package
 * @param path              the imported path
 * @param node              the imported JCR node
 * @param action            the import action (NOOP, ADDED, MODIFIED, or REPLACED)
 */
function importedPath(packageId /* PackageId */, path /* String */, node /* Node */, action /* PathAction */) {

}

/**
 * Notified when package importer deletes an existing node.
 *
 * @param packageId         the current package
 * @param path              deleted path
 * @param inspectSession    session providing access to repository state
 */
function deletedPath(packageId /* PackageId */, path /* String */, inspectSession /* Session */) {

}

/**
 * Provides an opportunity to inspect repository state between packages.
 *
 * @param packageId         the current package
 * @param inspectSession    session providing access to repository state
 */
function afterExtract(packageId /* PackageId */, inspectSession /* Session */) {

}

/**
 * Provides an opportunity to inspect repository state before installing a resource submitted to the
 * SlingSimulator.
 *
 * @param scanPackageId     the last preinstall or scan package
 * @param slingInstallable  the sling installable
 * @param inspectSession    session providing access to repository state
 * @since 2.2.0
 */
function beforeSlingInstall(scanPackageId /* PackageId */, slingInstallable /* SlingInstallable */,
                            inspectSession /* Session */) {

}

/**
 * Called after each embedded package is opened, if it has been submitted to the SlingSimulator. Track
 * subsequent events using the package ID provided to this method. Conceptually, at least for the purposes of
 * enforcing acceptance criteria against packaged JCR content, this is analogous to
 * identifySubpackage(packageId, parentId).
 *
 * @param packageId         the embedded package id
 * @param parentPackageId   the parent packageId
 * @param slingInstallable  the embedded package slingInstallable that was previously provided to the
 *                          beforeSlingInstall() event.
 * @since 2.2.0
 */
function identifyEmbeddedPackage(packageId /* PackageId */, parentPackageId /* PackageId */,
                                 slingInstallable /* EmbeddedPackageInstallable */) {

}

/**
 * Provides an opportunity to inspect repository state after installing a RepoInit scripts resource submitted to the
 * SlingSimulator.
 *
 * @param scanPackageId     the last preinstall or scan package
 * @param scripts           the repoinit scripts that were applied
 * @param slingInstallable  the associated SlingInstallable identifying the source JCR event that provided
 *                          the repo init scripts
 * @param inspectSession    session providing access to repository state
 * @since 2.2.0
 */
function appliedRepoInitScripts(scanPackageId /* PackageId */, scripts /* String[] */,
                                slingInstallable /* SlingInstallable */, inspectSession /* Session */) {

}

/**
 * Provides an opportunity to inspect repository state after complete installation (including its content,
 * subpackages, and Sling installable paths) of a package explicitly listed for scanning. This method is NOT called
 * for any of its subpackages or embedded packages.
 *
 * @param scanPackageId     the scanned package id
 * @param inspectSession    session providing access to repository state
 * @since 2.2.0
 */
function afterScanPackage(scanPackageId /* PackageId */, inspectSession /* Session */) {

}

/**
 * Called once at the end of the scan.
 */
function finishedScan() {

}
