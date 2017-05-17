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

package net.adamcin.oakpal.core;

import javax.jcr.PathNotFoundException;

import org.apache.jackrabbit.vault.packaging.PackageId;

/**
 * Default implementation which reports all exceptions as violations.
 */
public class DefaultErrorListener extends AbstractViolationReporter implements ErrorListener {

    @Override
    public void onListenerException(Exception e, PackageListener listener, PackageId packageId) {
        reportViolation(
                new SimpleViolation(Violation.Severity.MAJOR,
                        String.format("Listener error (%s): %s \"%s\"",
                                listener.getClass().getName(), e.getClass().getName(), e.getMessage()), packageId));
    }

    @Override
    public void onSubpackageException(Exception e, PackageId packageId) {
        reportViolation(
                new SimpleViolation(Violation.Severity.MAJOR,
                        String.format("Package error: %s \"%s\"",
                                e.getClass().getName(), e.getMessage()), packageId));
    }

    @Override
    public void onImporterException(Exception e, PackageId packageId, String path) {
        // Ignore PathNotFoundException, as it is thrown A LOT
        if (!(e instanceof PathNotFoundException)) {
            reportViolation(
                    new SimpleViolation(Violation.Severity.MAJOR,
                            String.format("%s - Importer error: %s \"%s\"", path, e.getClass().getName(), e.getMessage()),
                            packageId));
        }
    }

    @Override
    public void onListenerPathException(Exception e, PackageListener handler, PackageId packageId, String path) {
        reportViolation(
                new SimpleViolation(Violation.Severity.MAJOR,
                        String.format("%s - Listener error: %s \"%s\"", path, e.getClass().getName(), e.getMessage()),
                        packageId));
    }

}
