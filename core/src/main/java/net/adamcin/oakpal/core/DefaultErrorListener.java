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
import java.util.Optional;
import javax.jcr.PathNotFoundException;

import org.apache.jackrabbit.vault.packaging.PackageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation which reports all exceptions as violations.
 */
public class DefaultErrorListener implements ErrorListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultErrorListener.class);

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
            final String message = String.format("NodeType registration error (%s): %s \"%s\"",
                                    String.valueOf(resource), e.getClass().getName(), e.getMessage());
            LOGGER.trace("[onNodeTypeRegistrationError] stack trace for: " + message, e);
            reportViolation(new SimpleViolation(Violation.Severity.MAJOR, message));
        }
    }

    @Override
    public void onJcrNamespaceRegistrationError(final Throwable e, final String prefix, final String uri) {
        if (e.getCause() != null) {
            onJcrNamespaceRegistrationError(e.getCause(), prefix, uri);
        } else {
            final String message = String.format("JCR namespace registration error (%s=%s): %s \"%s\"",
                                    prefix, uri, e.getClass().getName(), e.getMessage());
            LOGGER.trace("[onJcrNamespaceRegistrationError] stack trace for: " + message, e);
            reportViolation(new SimpleViolation(Violation.Severity.MAJOR, message));
        }
    }

    @Override
    public void onJcrPrivilegeRegistrationError(final Throwable e, final String jcrPrivilege) {
        if (e.getCause() != null) {
            onJcrPrivilegeRegistrationError(e.getCause(), jcrPrivilege);
        } else {
            final String message = String.format("JCR privilege registration error (%s): %s \"%s\"",
                                    jcrPrivilege, e.getClass().getName(), e.getMessage());
            LOGGER.trace("[onJcrPrivilegeRegistrationError] stack trace for: " + message, e);
            reportViolation(new SimpleViolation(Violation.Severity.MAJOR, message));
        }
    }

    @Override
    public void onForcedRootCreationError(final Throwable e, final ForcedRoot forcedRoot) {
        if (e.getCause() != null) {
            onForcedRootCreationError(e.getCause(), forcedRoot);
        } else {
            final String message = String.format("Forced root creation error (%s): %s \"%s\"",
                                    forcedRoot.getPath(), e.getClass().getName(), e.getMessage());
            LOGGER.trace("[onForcedRootCreationError] stack trace for: " + message, e);
            reportViolation(new SimpleViolation(Violation.Severity.MAJOR, message));
        }
    }

    @Override
    public void onListenerException(final Exception e, final ProgressCheck listener, final PackageId packageId) {
        final String message = String.format("Listener error (%s): %s \"%s\"",
                listener.getClass().getName(), e.getClass().getName(), e.getMessage());
        LOGGER.trace("[onListenerException] stack trace for: " + message, e);
        reportViolation(new SimpleViolation(Violation.Severity.MAJOR, message, packageId));
    }

    @Override
    public void onSubpackageException(final Exception e, final PackageId packageId) {
        final String message = String.format("Package error: %s \"%s\"", e.getClass().getName(), e.getMessage());
        LOGGER.trace("[onSubpackageException] stack trace for: " + message, e);
        reportViolation(new SimpleViolation(Violation.Severity.MAJOR, message, packageId));
    }

    @Override
    public void onImporterException(final Exception e, final PackageId packageId, final String path) {
        // Ignore PathNotFoundException, as it is thrown A LOT
        if (!(e instanceof PathNotFoundException)) {
            final String message = String.format("%s - Importer error: %s \"%s\"", path, e.getClass().getName(), e.getMessage());
            LOGGER.trace("[onImporterException] stack trace for: " + message, e);
            reportViolation(new SimpleViolation(Violation.Severity.MAJOR, message, packageId));
        }
    }

    @Override
    public void onListenerPathException(final Exception e, final ProgressCheck handler,
                                        final PackageId packageId, final String path) {
        final String message = String.format("%s - Listener error: %s \"%s\"", path, e.getClass().getName(), e.getMessage());
        LOGGER.trace("[onListenerPathException] stack trace for: " + message, e);
        reportViolation(new SimpleViolation(Violation.Severity.MAJOR, message, packageId));
    }

    @Override
    public void onInstallHookError(final Throwable e, final PackageId packageId) {
        final String message = String.format("InstallHook error: %s \"%s\"",
                Optional.ofNullable(e.getCause()).orElse(e).getClass().getName(), e.getMessage());
        LOGGER.trace("[onInstallHookError] stack trace for: " + message, e);
        reportViolation(new SimpleViolation(Violation.Severity.MAJOR, message, packageId));
    }

    @Override
    public void onProhibitedInstallHookRegistration(final PackageId packageId) {
        reportViolation(
                new SimpleViolation(Violation.Severity.MAJOR,
                        "Policy prohibits the use of InstallHooks in packages", packageId));
    }
}
