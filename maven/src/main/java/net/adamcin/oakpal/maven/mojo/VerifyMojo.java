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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.ReportMapper;
import net.adamcin.oakpal.maven.component.OakpalComponentConfigurator;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Deferred reporting and reacting to oakpal check reports during the verify phase.
 *
 * @since 1.1.0
 */
@Mojo(name = "verify", defaultPhase = LifecyclePhase.VERIFY, configurator = OakpalComponentConfigurator.HINT)
public class VerifyMojo extends AbstractITestMojo {

    /**
     * Specifically skip this plugin's execution.
     *
     * @since 1.1.0
     */
    @Parameter(property = "oakpal.verify.skip")
    public boolean skip;

    /**
     * Specify additional summary files to verify. Non-existent files are ignored, but any failure to read an existing
     * file will throw a {@link MojoExecutionException}.
     *
     * @since 1.1.0
     */
    @Parameter
    private List<File> summaryFiles = new ArrayList<>();

    @Override
    protected boolean isIndividuallySkipped() {
        return skip;
    }

    @Override
    void executeGuardedIntegrationTest() throws MojoExecutionException, MojoFailureException {
        List<CheckReport> reports;
        try {
            reports = new ArrayList<>(readReportsFromFile(summaryFile));
            if (summaryFiles != null) {
                for (File file : summaryFiles) {
                    reports.addAll(readReportsFromFile(file));
                }
            }
        } catch (final Exception e) {
            throw new MojoExecutionException("Failed to read check report summary file.", e);
        }

        reactToReports(reports);
    }

    private List<CheckReport> readReportsFromFile(final File summaryFile) throws Exception {
        if (summaryFile != null && summaryFile.exists()) {
            return ReportMapper.readReportsFromFile(summaryFile);
        } else {
            return Collections.emptyList();
        }
    }
}
