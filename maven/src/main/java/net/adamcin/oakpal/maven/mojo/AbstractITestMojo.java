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

package net.adamcin.oakpal.maven.mojo;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.Violation;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.jetbrains.annotations.NotNull;

/**
 * Base Mojo providing access to maven context.
 */
abstract class AbstractITestMojo extends AbstractCommonMojo {

    /**
     * Conventional switch to skip integration-test phase goals.
     */
    @Parameter(property = "skipITs")
    protected boolean skipITs;

    /**
     * Conventional switch to skip test and integration-test phase goals.
     */
    @Parameter(property = "skipTests")
    protected boolean skipTests;

    /**
     * The summary file to read integration test results from.
     */
    @Parameter(defaultValue = "${project.build.directory}/oakpal-reports/oakpal-summary.json", required = true)
    protected File summaryFile;

    /**
     * Specify the minimum violation severity level that will trigger plugin execution failure. Valid options are
     * {@link net.adamcin.oakpal.core.Violation.Severity#MINOR},
     * {@link net.adamcin.oakpal.core.Violation.Severity#MAJOR}, and
     * {@link net.adamcin.oakpal.core.Violation.Severity#SEVERE}.
     * <p>
     * FYI: FileVault Importer errors are reported as MAJOR by default.
     * </p>
     *
     * @since 0.1.0
     */
    @Parameter(defaultValue = "MAJOR")
    protected Violation.Severity failOnSeverity = Violation.Severity.MAJOR;

    protected abstract boolean isIndividuallySkipped();

    @Override
    public boolean isTestScopeContainer() {
        return true;
    }

    private ClassLoader containerClassLoader;

    protected ClassLoader getContainerClassLoader() throws MojoFailureException {
        if (containerClassLoader == null) {
            this.containerClassLoader = createContainerClassLoader();
        }

        return this.containerClassLoader;
    }

    protected void reactToReports(final @NotNull List<CheckReport> reports) throws MojoFailureException {
        String errorMessage = String.format("** Violations were reported at or above severity: %s **", failOnSeverity);

        List<CheckReport> nonEmptyReports = reports.stream()
                .filter(r -> !r.getViolations().isEmpty())
                .collect(Collectors.toList());
        boolean shouldFail = nonEmptyReports.stream().anyMatch(r -> !r.getViolations(failOnSeverity).isEmpty());

        if (!nonEmptyReports.isEmpty()) {
            getLog().info("OakPAL Check Reports");
        }
        for (CheckReport r : nonEmptyReports) {
            getLog().info(String.format("  %s", String.valueOf(r.getCheckName())));
            for (Violation v : r.getViolations()) {
                Set<String> packageIds = v.getPackages().stream()
                        .map(PackageId::getDownloadName)
                        .collect(Collectors.toSet());
                String violLog = !packageIds.isEmpty()
                        ? String.format("   +- <%s> %s %s", v.getSeverity(), v.getDescription(), packageIds)
                        : String.format("   +- <%s> %s", v.getSeverity(), v.getDescription());
                if (v.getSeverity().isLessSevereThan(failOnSeverity)) {
                    getLog().info(" " + violLog);
                } else {
                    getLog().error("" + violLog);
                }
            }
        }

        if (shouldFail) {
            getLog().error(errorMessage);
            throw new MojoFailureException(errorMessage);
        }
    }

    void executeGuardedIntegrationTest() throws MojoExecutionException, MojoFailureException {

    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        boolean skip = isIndividuallySkipped();
        if (skip || skipITs || skipTests) {
            getLog().info("skipping [skip=" + skip + "][skipITs=" + skipITs + "][skipTests=" + skipTests + "]");
            return;
        } else {
            if (summaryFile == null) {
                throw new MojoExecutionException("summaryFile parameter is required.");
            }
            File parentFile = summaryFile.getParentFile();
            if (!(parentFile.isDirectory() || parentFile.mkdirs())) {
                throw new MojoExecutionException("Failed to create report summary directory " + parentFile.getPath());
            }
            ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getContainerClassLoader());
                executeGuardedIntegrationTest();
            } finally {
                Thread.currentThread().setContextClassLoader(oldCl);
            }
        }
    }
}
