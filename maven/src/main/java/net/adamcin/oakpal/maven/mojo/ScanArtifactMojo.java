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
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import net.adamcin.oakpal.core.AbortedScanException;
import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.ReportMapper;
import net.adamcin.oakpal.maven.component.OakpalComponentConfigurator;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Scans the main project artifact by simulating package installation and listening for violations reported by the
 * configured {@code checks}.
 *
 * @since 0.1.0
 */
@Mojo(name = "scan", requiresDependencyResolution = ResolutionScope.TEST, configurator = OakpalComponentConfigurator.HINT,
        defaultPhase = LifecyclePhase.INTEGRATION_TEST)
public class ScanArtifactMojo extends AbstractScanMojo {

    /**
     * Specifically skip this plugin's execution.
     */
    @Parameter(property = "oakpal.scan.skip")
    public boolean skip;

    @Override
    protected boolean isIndividuallySkipped() {
        return skip;
    }

    @Override
    protected void executeGuardedIntegrationTest() throws MojoExecutionException, MojoFailureException {
        Optional<File> packageArtifact = getProject()
                .flatMap(p -> Optional.ofNullable(p.getArtifact()))
                .flatMap(a -> Optional.ofNullable(a.getFile()));
        if (packageArtifact.isPresent() && packageArtifact.get().exists()) {
            List<CheckReport> reports;
            try {
                reports = getBuilder().build().scanPackage(packageArtifact.get());
            } catch (AbortedScanException e) {
                String currentFilePath = e.getCurrentPackageFile()
                        .map(f -> "Failed package: " + f.getAbsolutePath()).orElse("");
                throw new MojoExecutionException("Failed to execute package scan. " + currentFilePath, e);
            }

            try {
                ReportMapper.writeReportsToFile(reports, summaryFile);
                getLog().info("Check report summary written to " + summaryFile.getPath());
            } catch (final IOException e) {
                throw new MojoExecutionException("Failed to write summary reports.", e);
            }

            if (deferBuildFailure) {
                getLog().info("Evaluation of check reports has been deferred by 'deferBuildFailure=true'.");
            } else {
                reactToReports(reports);
            }
        } else {
            throw new MojoExecutionException("Failed to resolve file for project artifact.");
        }
    }
}
