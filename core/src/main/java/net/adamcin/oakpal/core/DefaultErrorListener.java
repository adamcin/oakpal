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
import java.util.Collection;
import javax.jcr.PathNotFoundException;

import org.apache.jackrabbit.vault.packaging.PackageId;

/**
 * Default implementation which reports all exceptions as violations.
 */
public class DefaultErrorListener implements ErrorListener {

    private final ReportCollector collector = new ReportCollector();

    protected void reportViolation(final Violation violation) {
        this.collector.reportViolation(violation);
    }

    @Override
    public Collection<Violation> getReportedViolations() {
        return collector.getReportedViolations();
    }

    @Override
    public void onNodeTypeRegistrationError(final Throwable e, final URL resource) {
        if (e.getCause() != null) {
            onNodeTypeRegistrationError(e.getCause(), resource);
        } else {
            reportViolation(
                    new SimpleViolation(Violation.Severity.MAJOR,
                            String.format("NodeType registration error (%s): %s \"%s\"",
                                    resource.toString(), e.getClass().getName(), e.getMessage())));
        }
    }

    @Override
    public void onJcrNamespaceRegistrationError(final Throwable e, final String prefix, final String uri) {
        if (e.getCause() != null) {
            onJcrNamespaceRegistrationError(e.getCause(), prefix, uri);
        } else {
            reportViolation(
                    new SimpleViolation(Violation.Severity.MAJOR,
                            String.format("JCR namespace registration error (%s=%s): %s \"%s\"",
                                    prefix, uri, e.getClass().getName(), e.getMessage())));
        }
    }

    @Override
    public void onJcrPrivilegeRegistrationError(final Throwable e, final String jcrPrivilege) {
        if (e.getCause() != null) {
            onJcrPrivilegeRegistrationError(e.getCause(), jcrPrivilege);
        } else {
            reportViolation(
                    new SimpleViolation(Violation.Severity.MAJOR,
                            String.format("JCR privilege registration error (%s): %s \"%s\"",
                                    jcrPrivilege, e.getClass().getName(), e.getMessage())));
        }
    }

    @Override
    public void onForcedRootCreationError(final Throwable e, final ForcedRoot forcedRoot) {
        if (e.getCause() != null) {
            onForcedRootCreationError(e.getCause(), forcedRoot);
        } else {
            reportViolation(
                    new SimpleViolation(Violation.Severity.MAJOR,
                            String.format("Forced root creation error (%s): %s \"%s\"",
                                    forcedRoot.getPath(), e.getClass().getName(), e.getMessage())));
        }
    }

    @Override
    public void onListenerException(Exception e, ProgressCheck listener, PackageId packageId) {
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
    public void onListenerPathException(Exception e, ProgressCheck handler, PackageId packageId, String path) {
        reportViolation(
                new SimpleViolation(Violation.Severity.MAJOR,
                        String.format("%s - Listener error: %s \"%s\"", path, e.getClass().getName(), e.getMessage()),
                        packageId));
    }

}
