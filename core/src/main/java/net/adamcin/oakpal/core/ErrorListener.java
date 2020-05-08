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

package net.adamcin.oakpal.core;

import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.ScanListener;
import net.adamcin.oakpal.api.ViolationReporter;
import net.adamcin.oakpal.api.EmbeddedPackageInstallable;
import net.adamcin.oakpal.api.RepoInitScriptsInstallable;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.osgi.annotation.versioning.ConsumerType;

import java.net.URL;
import java.util.List;

/**
 * A single error handler is used during an OakPAL scan.
 */
@ConsumerType
public interface ErrorListener extends ScanListener, ViolationReporter {

    /**
     * Called for each unresolved error thrown during node type definition auto-installation.
     *
     * @param error    the error.
     * @param resource the classpath resource of the failed Sling-Nodetypes entry.
     */
    default void onNodeTypeRegistrationError(final Throwable error, final URL resource) {
    }

    /**
     * Called for each unresolved error thrown during JCR namespace prefix registration.
     *
     * @param error  the error.
     * @param prefix the prefix being registered.
     * @param uri    the uri being registered.
     */
    default void onJcrNamespaceRegistrationError(final Throwable error, final String prefix, final String uri) {
    }

    /**
     * Called for each unresolved error thrown during JCR privilege registration.
     *
     * @param error        the error.
     * @param jcrPrivilege the jcrPrivilege being registered.
     */
    default void onJcrPrivilegeRegistrationError(final Throwable error, final String jcrPrivilege) {
    }

    /**
     * Called for each error thrown during creation of a forced JCR root.
     *
     * @param error      the error.
     * @param forcedRoot the root path being created.
     */
    default void onForcedRootCreationError(final Throwable error, final ForcedRoot forcedRoot) {
    }

    /**
     * Called when a {@link ProgressCheck} throws an exception.
     *
     * @param error     the error
     * @param listener  the listener
     * @param packageId the current package id
     */
    default void onListenerException(final Exception error, final ProgressCheck listener, final PackageId packageId) {

    }

    /**
     * Called when a {@link ProgressCheck} throws an exception when handling an imported path.
     *
     * @param error     the error
     * @param handler   the handler
     * @param packageId the current package id
     * @param path      the current path
     */
    default void onListenerPathException(final Exception error, final ProgressCheck handler, final PackageId packageId, final String path) {

    }

    /**
     * Called when the package FileVault importer encounters an error such as an XML syntax exception.
     *
     * @param error     the caught exception
     * @param packageId the current package ID
     * @param path      the related repository path, if applicable
     */
    default void onImporterException(final Exception error, final PackageId packageId, final String path) {

    }

    /**
     * Called when an exception was thrown when attempting to open or extract a package.
     *
     * @param error     the Exception that was thrown
     * @param packageId the offending package id
     */
    default void onSubpackageException(final Exception error, final PackageId packageId) {

    }

    /**
     * Called when an exception is thrown when attempting to register install hooks for a particular package.
     *
     * @param error     the error thrown
     * @param packageId the package attempting to register install hooks
     */
    default void onInstallHookError(final Throwable error, final PackageId packageId) {

    }

    /**
     * Called after any install hooks have been registered for a particular package during a scan
     * which has specified {@link OakpalPlan#getInstallHookPolicy()} value of {@link InstallHookPolicy#PROHIBIT}.
     *
     * @param packageId the package which registered one or more install hooks
     */
    default void onProhibitedInstallHookRegistration(final PackageId packageId) {

    }

    /**
     * Called for an IOException or RepoInitParsingException when parsing a repoinit url during
     * {@code InitStage.initSession()}.
     *
     * @param error       the error thrown
     * @param repoinitUrl the repoinit url
     */
    default void onRepoInitUrlError(final Throwable error, final URL repoinitUrl) {

    }

    /**
     * Called for an IOException or RepoInitParsingException when parsing a list of repoinit scripts during
     * {@code InitStage.initSession()}.
     *
     * @param error     the error thrown
     * @param repoinits the repoinit scripts
     */
    default void onRepoInitInlineError(final Throwable error, final List<String> repoinits) {

    }

    /**
     * Called for an IOException or RepoInitParsingException when parsing an installable repoinit script submitted
     * to a {@link net.adamcin.oakpal.api.SlingSimulator}.
     *
     * @param error        the error thrown
     * @param failedScript the script that failed
     * @param installable  the repoinit scripts installable
     */
    default void onSlingRepoInitScriptsError(final Throwable error,
                                             final String failedScript,
                                             final RepoInitScriptsInstallable installable) {
    }

    /**
     * Called for an IOException, PackageException, or RepositoryException when installing an embedded package submitted
     * to a {@link net.adamcin.oakpal.api.SlingSimulator}.
     *
     * @param error       the error thrown
     * @param installable the subpackage installable
     */
    default void onSlingEmbeddedPackageError(final Throwable error,
                                             final EmbeddedPackageInstallable installable) {

    }
}
