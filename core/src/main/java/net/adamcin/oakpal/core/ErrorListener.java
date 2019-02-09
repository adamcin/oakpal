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

import java.net.URL;

import org.apache.jackrabbit.vault.packaging.PackageId;

/**
 * A single error handler is used during an OakPAL scan.
 */
public interface ErrorListener extends ScanListener, ViolationReporter {

    /**
     * Called for each unresolved error thrown during node type definition auto-installation.
     *
     * @param e        the error.
     * @param resource the classpath resource of the failed Sling-Nodetypes entry.
     */
    default void onNodeTypeRegistrationError(final Throwable e, final URL resource) {
    }

    /**
     * Called for each unresolved error thrown during JCR namespace prefix registration.
     *
     * @param e      the error.
     * @param prefix the prefix being registered.
     * @param uri    the uri being registered.
     */
    default void onJcrNamespaceRegistrationError(final Throwable e, final String prefix, final String uri) {
    }

    /**
     * Called for each unresolved error thrown during JCR privilege registration.
     *
     * @param e            the error.
     * @param jcrPrivilege the jcrPrivilege being registered.
     */
    default void onJcrPrivilegeRegistrationError(final Throwable e, final String jcrPrivilege) {
    }

    /**
     * Called for each error thrown during creation of a forced JCR root.
     *
     * @param e          the error.
     * @param forcedRoot the root path being created.
     */
    default void onForcedRootCreationError(final Throwable e, final ForcedRoot forcedRoot) {
    }

    /**
     * Called when a {@link ProgressCheck} throws an exception.
     *
     * @param e         the error
     * @param listener  the listener
     * @param packageId the current package id
     */
    default void onListenerException(final Exception e, final ProgressCheck listener, final PackageId packageId) {

    }

    /**
     * Called when a {@link ProgressCheck} throws an exception when handling an imported path.
     *
     * @param e         the error
     * @param handler   the handler
     * @param packageId the current package id
     * @param path      the current path
     */
    default void onListenerPathException(final Exception e, final ProgressCheck handler, final PackageId packageId, final String path) {

    }

    /**
     * Called when the package FileVault importer encounters an error such as an XML syntax exception.
     *
     * @param e         the caught exception
     * @param packageId the current package ID
     * @param path      the related repository path, if applicable
     */
    default void onImporterException(final Exception e, final PackageId packageId, final String path) {

    }

    /**
     * Called when an exception was thrown when attempting to open or extract a package.
     *
     * @param e         the Exception that was thrown
     * @param packageId the offending package id
     */
    default void onSubpackageException(final Exception e, final PackageId packageId) {

    }
}
