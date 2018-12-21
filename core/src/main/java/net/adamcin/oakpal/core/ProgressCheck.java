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

package net.adamcin.oakpal.core;

import java.io.File;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import aQute.bnd.annotation.ProviderType;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;

/**
 * Primary point of customization for the OakPAL framework. Receives events, ultimately, from a
 * {@link org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener}.
 * <p>
 * Some constraints for behavior:
 * <ol>
 *     <li>Once constructed, instances of this class should be immutable from the perspective of any other public API.</li>
 *     <li>The {@link OakMachine} will call these methods synchronously and in a single-threaded fashion.</li>
 *     <li>Mutation of the repository state exposed by {@link Session} is not allowed.</li>
 *     <li>Implementations of this which are referenced directly by a checklist must expose a zero-argument default
 *     constructor.</li>
 *     <li>If an implementation accepts parameters at construction, it must be constructed from a checklist-referenced
 *     {@link ProgressCheckFactory} in order to be loaded successfully.</li>
 * </ol>
 */
@ProviderType
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
     * provided to {@link OakMachine#scanPackage(File...)}.
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
     * Notified when package importer adds, modifies, or leaves a node untouched.
     *
     * @param packageId the current package
     * @param path      the imported path
     * @param node      the imported node
     * @throws RepositoryException because of access to a {@link Node}
     */
    default void importedPath(PackageId packageId, String path, Node node) throws RepositoryException {

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
}
