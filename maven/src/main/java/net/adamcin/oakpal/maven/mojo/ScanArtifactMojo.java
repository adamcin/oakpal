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

package net.adamcin.oakpal.maven.mojo;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.adamcin.oakpal.core.AbortedScanException;
import net.adamcin.oakpal.core.ViolationReport;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
  * Scans the main project artifact by simulating package installation and listening for violations reported by the
  * configured {@code scriptReporters}.
  */
@Mojo(name = "scan",
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
        if (packageArtifact.isPresent()) {
            try {
                String errorMessage = String.format("** Violations were reported at or above severity: %s **", failOnSeverity);
                List<ViolationReport> reports = getBuilder().build().scanPackage(packageArtifact.get());
                List<ViolationReport> nonEmptyReports = reports.stream()
                        .filter(r -> !r.getViolations().isEmpty())
                        .collect(Collectors.toList());
                boolean shouldFail = nonEmptyReports.stream().anyMatch(r -> !r.getViolations(failOnSeverity).isEmpty());

                nonEmptyReports.forEach(r -> {
                    getLog().info("");
                    getLog().info(String.format(" OakPAL Reporter: %s", String.valueOf(r.getReporterUrl())));
                    r.getViolations().forEach(v -> {
                        String violLog = String.format("  +- <%s> %s", v.getSeverity(), v.getDescription());
                        if (v.getSeverity().isLessSevereThan(failOnSeverity)) {
                            getLog().info(" " + violLog);
                        } else {
                            getLog().error(violLog);
                        }
                    });
                });

                if (shouldFail) {
                    getLog().error("");
                    getLog().error(errorMessage);
                    throw new MojoFailureException(errorMessage);
                }

            } catch (AbortedScanException e) {
                String currentFilePath = e.getCurrentPackageFile()
                        .map(f -> "Failed package: " + f.getAbsolutePath()).orElse("");
                throw new MojoExecutionException("Failed to execute package scan. " + currentFilePath, e);
            }
        }
    }
}
