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
import net.adamcin.oakpal.api.ReportCollector;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.api.SimpleViolation;
import net.adamcin.oakpal.api.Violation;
import net.adamcin.oakpal.api.ViolationReporter;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Default implementation which reports all exceptions as violations.
 */
public class DefaultErrorListener implements ErrorListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultErrorListener.class);

    private final ReportCollector collector = new ReportCollector();

    private ResourceBundle resourceBundle;

    protected void reportViolation(final Violation violation) {
        this.collector.reportViolation(violation);
    }

    @Override
    public void setResourceBundle(final ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    /**
     * Used by {@link #getString(String)} to retrieve localized messages.
     * NOTE: If this method is called before a non-null ResourceBundle has been injected via
     * {@link ViolationReporter#setResourceBundle(ResourceBundle)}, it will try to get a fallback ResourceBundle
     * by calling {@link ResourceBundle#getBundle(String)}, which invokes the default classloader and locale behavior,
     * using {@link ViolationReporter#getResourceBundleBaseName()} as the resource bundle base name.
     *
     * @return the resource bundle
     * @see ResourceBundle#getBundle(String)
     * @see ViolationReporter#getResourceBundleBaseName()
     */
    @NotNull
    protected ResourceBundle getResourceBundle() throws MissingResourceException {
        if (this.resourceBundle == null) {
            this.resourceBundle = ResourceBundle.getBundle(getResourceBundleBaseName());
        }
        return this.resourceBundle;
    }

    /**
     * Lookup a localized string from the resource bundle.
     *
     * @param key the i18n key
     * @return the localized string
     * @throws MissingResourceException if an attempt is made to load a missing ResourceBundle
     */
    @NotNull
    protected String getString(@NotNull final String key) {
        if (getResourceBundle().containsKey(key)) {
            return getResourceBundle().getString(key);
        } else {
            return key;
        }
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
            final String message = MessageFormat.format(getString("NodeType registration error ({0}): {1} \"{2}\""),
                    String.valueOf(resource), e.getClass().getName(), e.getMessage());
            LOGGER.trace("[onNodeTypeRegistrationError] stack trace for: " + message, e);
            reportViolation(new SimpleViolation(Severity.MAJOR, message));
        }
    }

    @Override
    public void onJcrNamespaceRegistrationError(final Throwable e, final String prefix, final String uri) {
        if (e.getCause() != null) {
            onJcrNamespaceRegistrationError(e.getCause(), prefix, uri);
        } else {
            final String message = MessageFormat.format(getString("JCR namespace registration error ({0}={1}): {2} \"{3}\""),
                    prefix, uri, e.getClass().getName(), e.getMessage());
            LOGGER.trace("[onJcrNamespaceRegistrationError] stack trace for: " + message, e);
            reportViolation(new SimpleViolation(Severity.MAJOR, message));
        }
    }

    @Override
    public void onJcrPrivilegeRegistrationError(final Throwable e, final String jcrPrivilege) {
        if (e.getCause() != null) {
            onJcrPrivilegeRegistrationError(e.getCause(), jcrPrivilege);
        } else {
            final String message = MessageFormat.format(getString("JCR privilege registration error ({0}): {1} \"{2}\""),
                    jcrPrivilege, e.getClass().getName(), e.getMessage());
            LOGGER.trace("[onJcrPrivilegeRegistrationError] stack trace for: " + message, e);
            reportViolation(new SimpleViolation(Severity.MAJOR, message));
        }
    }

    @Override
    public void onForcedRootCreationError(final Throwable e, final ForcedRoot forcedRoot) {
        if (e.getCause() != null) {
            onForcedRootCreationError(e.getCause(), forcedRoot);
        } else {
            final String message = MessageFormat.format(getString("Forced root creation error ({0}): {1} \"{2}\""),
                    forcedRoot, e.getClass().getName(), e.getMessage());
            LOGGER.trace("[onForcedRootCreationError] stack trace for: " + message, e);
            reportViolation(new SimpleViolation(Severity.MAJOR, message));
        }
    }

    @Override
    public void onListenerException(final Exception e, final ProgressCheck listener, final PackageId packageId) {
        final String message = MessageFormat.format(getString("Listener error ({0}): {1} \"{2}\""),
                Optional.ofNullable(listener).map(lstr -> lstr.getClass().getName()).orElse(null),
                e.getClass().getName(), e.getMessage());
        LOGGER.trace("[onListenerException] stack trace for: " + message, e);
        reportViolation(new SimpleViolation(Severity.MAJOR, message, packageId));
    }

    @Override
    public void onSubpackageException(final Exception e, final PackageId packageId) {
        final String message = MessageFormat.format(getString("Package error: {0} \"{1}\""),
                e.getClass().getName(), e.getMessage());
        LOGGER.trace("[onSubpackageException] stack trace for: " + message, e);
        reportViolation(new SimpleViolation(Severity.MAJOR, message, packageId));
    }

    @Override
    public void onImporterException(final Exception e, final PackageId packageId, final String path) {
        // Ignore PathNotFoundException, as it is thrown A LOT
        if (!(e instanceof PathNotFoundException)) {
            final String message = MessageFormat.format(getString("{0} - Importer error: {1} \"{2}\""),
                    path, e.getClass().getName(), e.getMessage());
            LOGGER.trace("[onImporterException] stack trace for: " + message, e);
            reportViolation(new SimpleViolation(Severity.MAJOR, message, packageId));
        }
    }

    @Override
    public void onListenerPathException(final Exception e, final ProgressCheck handler,
                                        final PackageId packageId, final String path) {
        final String message = MessageFormat.format(getString("{0} - Listener error: {1} \"{2}\""),
                path, e.getClass().getName(), e.getMessage());
        LOGGER.trace("[onListenerPathException] stack trace for: " + message, e);
        reportViolation(new SimpleViolation(Severity.MAJOR, message, packageId));
    }

    @Override
    public void onInstallHookError(final Throwable e, final PackageId packageId) {
        final String message = MessageFormat.format(getString("InstallHook error: {0} \"{1}\""),
                Optional.ofNullable(e.getCause()).orElse(e).getClass().getName(), e.getMessage());
        LOGGER.trace("[onInstallHookError] stack trace for: " + message, e);
        reportViolation(new SimpleViolation(Severity.MAJOR, message, packageId));
    }

    @Override
    public void onProhibitedInstallHookRegistration(final PackageId packageId) {
        reportViolation(
                new SimpleViolation(Severity.MAJOR,
                        getString("Policy prohibits the use of InstallHooks in packages"), packageId));
    }
}
