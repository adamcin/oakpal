/*
 * Copyright 2018 Mark Adamcin
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
 * import javax.jcr.Node;
 * import javax.jcr.Session;
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
 */
function importedPath(packageId /* PackageId */, path /* String */, node /* Node */) {

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
 * Called once at the beginning of the scan.
 */
function startedScan() {

}

/**
 * Called once at the end of the scan.
 */
function finishedScan() {

}
