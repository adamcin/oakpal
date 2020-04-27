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

import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ConsumerType;

import java.util.Collection;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Simple implementation of a {@link ProgressCheck} with convenient methods for reporting and collecting violations.
 */
@ConsumerType
public class SimpleProgressCheck implements ProgressCheck {
    protected final ReportCollector collector = new ReportCollector();
    private ResourceBundle resourceBundle;

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
    @Nullable
    protected ResourceBundle getResourceBundle() throws MissingResourceException {
        if (this.resourceBundle == null) {
            if (getResourceBundleBaseName() != null) {
                this.resourceBundle = ResourceBundle.getBundle(getResourceBundleBaseName());
            }
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
        return Optional.ofNullable(getResourceBundle())
                .filter(bundle -> bundle.containsKey(key))
                .map(bundle -> bundle.getString(key))
                .orElse(key);
    }

    protected void reportViolation(final Violation violation) {
        collector.reportViolation(violation);
    }

    /**
     * Report a violation with a customizing consumer function.
     *
     * @param customizer the customizing consumer function
     */
    protected final void reporting(@NotNull final Consumer<SimpleViolation.Builder> customizer) {
        SimpleViolation.Builder builder = SimpleViolation.builder(getResourceBundle());
        customizer.accept(builder);
        this.reportViolation(builder.build());
    }

    protected final void reportViolation(final Severity severity,
                                         final String description,
                                         final PackageId... packages) {
        this.reporting(builder -> builder
                .withSeverity(severity)
                .withDescription(description)
                .withPackage(packages));
    }

    protected final void minorViolation(final String description, final PackageId... packages) {
        this.reporting(builder -> builder
                .withSeverity(Severity.MINOR)
                .withDescription(description)
                .withPackage(packages));
    }

    protected final void majorViolation(final String description, final PackageId... packages) {
        this.reporting(builder -> builder
                .withSeverity(Severity.MAJOR)
                .withDescription(description)
                .withPackage(packages));
    }

    protected final void severeViolation(final String description, final PackageId... packages) {
        this.reporting(builder -> builder
                .withSeverity(Severity.SEVERE)
                .withDescription(description)
                .withPackage(packages));
    }

    @Override
    public void startedScan() {
        collector.clearViolations();
    }

    @Override
    public Collection<Violation> getReportedViolations() {
        return collector.getReportedViolations();
    }
}
