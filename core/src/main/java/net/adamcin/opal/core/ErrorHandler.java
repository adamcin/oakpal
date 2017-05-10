/*
 * Copyright 2017 Mark Adamcin
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

package net.adamcin.opal.core;

import java.io.File;

import org.apache.jackrabbit.vault.packaging.PackageId;

/**
 * A single error handler is used during an Opal scan.
 */
public interface ErrorHandler extends OpalViolationReporter {

    /**
     * Called when a {@link OpalHandler} throws an exception.
     *
     * @param e         the error
     * @param handler   the handler
     * @param packageId the current package id
     */
    void onHandlerException(Exception e, OpalHandler handler, PackageId packageId);

    /**
     * Called when a {@link OpalHandler} throws an exception when handling an imported path.
     *
     * @param e         the error
     * @param handler   the handler
     * @param packageId the current package id
     * @param path      the current path
     */
    void onHandlerPathException(Exception e, OpalHandler handler, PackageId packageId, String path);

    /**
     * Called when the package FileVault importer encounters an error such as an XML syntax exception.
     *
     * @param e         the caught exception
     * @param packageId the current package ID
     * @param path      the related repository path, if applicable
     */
    void onImporterException(Exception e, PackageId packageId, String path);

    /**
     * Called when an exception was thrown when attempting to open or extract a package.
     *
     * @param e    the Exception that was thrown
     * @param file the offending file
     */
    void onPackageException(Exception e, File file);

    /**
     * Called when an exception was thrown when attempting to open or extract a package.
     *
     * @param e         the Exception that was thrown
     * @param packageId the offending package id
     */
    void onPackageException(Exception e, PackageId packageId);

    /**
     * Called when an exception was thrown and not caught during normal processing. Runtime
     * exceptions will be rethrown after this notification returns.
     *
     * @param e the uncaught throwable
     */
    void onFatalError(Throwable e);

}
