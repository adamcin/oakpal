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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.jackrabbit.vault.packaging.PackageId;

/**
 * Default implementation.
 */
public class DefaultErrorHandler extends AbstractViolationReporter implements ErrorHandler {

    @Override
    public void onHandlerException(Exception e, OpalHandler handler, PackageId packageId) {
        e.printStackTrace(System.err);
    }

    @Override
    public void onPackageException(Exception e, File file) {

        e.printStackTrace(System.err);
    }

    @Override
    public void onPackageException(Exception e, PackageId packageId) {
        reportViolation(
                new SimpleViolation(OpalViolation.Severity.MAJOR,
                        String.format("Package error: %s \"%s\"", e.getClass().getName(), e.getMessage()),
                        packageId));
        e.printStackTrace(System.err);
    }

    @Override
    public void onImporterException(Exception e, PackageId packageId, String path) {
        reportViolation(
                new SimpleViolation(OpalViolation.Severity.MAJOR,
                        String.format("%s - Importer error: %s \"%s\"", path, e.getClass().getName(), e.getMessage()),
                        packageId));
        e.printStackTrace(System.err);
    }

    @Override
    public void onHandlerPathException(Exception e, OpalHandler handler, PackageId packageId, String path) {
        e.printStackTrace(System.err);
    }

    @Override
    public void onFatalError(Throwable e) {
        e.printStackTrace(System.err);
    }

    @Override
    protected void beforeComplete() { }
}
