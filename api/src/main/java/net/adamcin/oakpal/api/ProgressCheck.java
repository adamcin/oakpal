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

package net.adamcin.oakpal.api;

import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.osgi.annotation.versioning.ConsumerType;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.jar.Manifest;

/**
 * Primary point of customization for the OakPAL framework. Receives events, ultimately, from a
 * {@link org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener}.
 * <p>
 * Some constraints for behavior:
 * <ol>
 * <li>Once constructed, instances of this class should be immutable from the perspective of any other public API.</li>
 * <li>The {@code OakMachine} will call these methods synchronously and in a single-threaded fashion.</li>
 * <li>Mutation of the repository state exposed by {@link Session} is not allowed.</li>
 * <li>Implementations of this which are referenced directly by a checklist must expose a zero-argument default
 * constructor.</li>
 * <li>If an implementation accepts parameters at construction, it must be constructed from a checklist-referenced
 * {@link ProgressCheckFactory} in order to be loaded successfully.</li>
 * </ol>
 */
@ConsumerType
public interface ProgressCheck extends ScanListener, ViolationReporter {

    /**
     * Return an optional label for displaying reports made by this reporter.
     *
     * @return an optional display label for this reporter.
     */
    default String getCheckName() {
        return getClass().getSimpleName();
    }


    /**
     * Called after the package is uploaded to the package manager at the beginning of the scan. Track subsequent
     * events using the package ID provided to this method. This method will only be called once for each package
     * provided to {@code OakMachine.scanPackage(File...)}.
     *
     * @param packageId the package ID of the newly opened package
     * @param file      the package file that will be opened
     */
    default void identifyPackage(PackageId packageId, File file) {

    }

    /**
     * Called after each subpackage is opened. Track subsequent events using the package ID provided to this method.
     *
     * @param packageId the package ID of the newly opened subpackage
     * @param parentId  the package ID of the parent package.
     */
    default void identifySubpackage(PackageId packageId, PackageId parentId) {

    }


    /**
     * If the package provides a {@link Manifest}, it will be provided to the check using this method, prior to calling
     * {@link #beforeExtract(PackageId, Session, PackageProperties, MetaInf, List)}.
     *
     * @param packageId the package ID of the newly opened package
     * @param manifest  the parsed manifest
     * @since 1.3.0
     */
    default void readManifest(PackageId packageId, Manifest manifest) {

    }

    /**
     * Called for each package before it is extracted.
     *
     * @param packageId         the package ID of the newly opened package
     * @param inspectSession    session providing access to repository state
     * @param packageProperties the package properties
     * @param metaInf           the package meta information
     * @param subpackages       extracted subpackages
     * @throws RepositoryException because of access to a {@link Session}
     */
    default void beforeExtract(PackageId packageId, Session inspectSession, PackageProperties packageProperties,
                               MetaInf metaInf, List<PackageId> subpackages) throws RepositoryException {

    }

    /**
     * Notified when package importer adds, modifies, or leaves a node untouched. This method is not called if
     * {@link #importedPath(PackageId, String, Node, PathAction)} is overridden.
     *
     * @param packageId the current package
     * @param path      the imported path
     * @param node      the imported node
     * @throws RepositoryException because of access to a {@link Node}
     * @deprecated 2.0.0 implement {@link #importedPath(PackageId, String, Node, PathAction)} instead
     */
    @Deprecated
    default void importedPath(PackageId packageId, String path, Node node) throws RepositoryException {

    }

    /**
     * Notified when package importer adds, modifies, or leaves a node untouched.
     *
     * @param packageId the current package
     * @param path      the imported path
     * @param node      the imported node
     * @param action    the reported path action type
     * @throws RepositoryException because of access to a {@link Node}
     */
    default void importedPath(PackageId packageId, String path, Node node, PathAction action) throws RepositoryException {
        importedPath(packageId, path, node);
    }

    /**
     * Notified when package importer deletes an existing node.
     *
     * @param packageId      the current package
     * @param path           deleted path
     * @param inspectSession session providing access to repository state
     * @throws RepositoryException because of access to a {@link Session}
     */
    default void deletedPath(PackageId packageId, String path, Session inspectSession) throws RepositoryException {

    }

    /**
     * Provides an opportunity to inspect repository state between packages.
     *
     * @param packageId      the current package
     * @param inspectSession session providing access to repository state
     * @throws RepositoryException because of access to a {@link Session}
     */
    default void afterExtract(PackageId packageId, Session inspectSession) throws RepositoryException {

    }

    /**
     * Override this method to accept an {@link SlingSimulator} to request installation of JCR resources like
     * FileVault packages and RepositoryInitializer factory configs, as if running within a Sling repository instance.
     * <p>
     * Also provied are the set of simulated Sling Run Modes. These are intended to drive construction of JCR path
     * patterns to select embedded resources for installation upon receiving a matching
     * {@link ProgressCheck#importedPath(PackageId, String, Node, PathAction)} event, but these run modes may also be
     * used as a global configuration hint for progress checks that support modal behavior outside of their explicit
     * JSON config format. NOTE: this set will always be empty by default, and must be populated in the plan or
     * overridden at runtime in the execution layer.
     *
     * @param slingSimulator the sling simulator
     * @param runModes       the simulated sling run modes
     * @since 2.1.0
     */
    default void simulateSling(SlingSimulator slingSimulator, Set<String> runModes) {

    }

    /**
     * Called after each embedded package is opened, if it has been submitted to the {@link SlingSimulator}. Track
     * subsequent events using the package ID provided to this method. Conceptually, at least for the purposes of
     * enforcing acceptance criteria against packaged JCR content, this is analogous to
     * {@link #identifySubpackage(PackageId, PackageId)}.
     *
     * @param packageId the package ID of the newly opened embeddedPackage
     * @param parentId  the package ID of the parent package.
     * @param jcrPath   the JCR path of this embedded package within the repository
     * @since 2.1.0
     */
    default void identifyEmbeddedPackage(PackageId packageId, PackageId parentId, String jcrPath) {

    }

    /**
     * Provides an opportunity to inspect repository state before installing a resource submitted to the
     * {@link SlingSimulator}.
     *
     * @param lastPackage      the last preinstall or scan package
     * @param slingInstallable the sling installable
     * @param inspectSession   session providing access to repository state
     * @throws RepositoryException because of access to a {@link Session}
     * @since 2.1.0
     */
    default void beforeSlingInstall(PackageId lastPackage, SlingInstallable<?> slingInstallable, Session inspectSession)
            throws RepositoryException {

    }

    /**
     * Provides an opportunity to inspect repository state after installing a resource submitted to the
     * {@link SlingSimulator}.
     *
     * @param lastPackage      the last preinstall or scan package
     * @param slingInstallable the sling installable
     * @param inspectSession   session providing access to repository state
     * @throws RepositoryException because of access to a {@link Session}
     * @since 2.1.0
     */
    default void appliedRepoInitScripts(PackageId lastPackage, SlingInstallable<?> slingInstallable, Session inspectSession)
            throws RepositoryException {

    }

    /**
     * Provides an opportunity to inspect repository state after complete installation (including its content,
     * subpackages, and Sling installable paths) of a package explicitly listed for scanning. This method is NOT called
     * for any of its subpackages or embedded packages.
     *
     * @param packageId      the scanned package id
     * @param inspectSession session providing access to repository state
     * @throws RepositoryException because of access to a {@link Session}
     * @since 2.1.0
     */
    default void afterScanPackage(PackageId packageId, Session inspectSession) throws RepositoryException {

    }

}
