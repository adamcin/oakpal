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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

/**
 * Base Mojo providing access to maven context.
 */
abstract class AbstractMojo extends org.apache.maven.plugin.AbstractMojo {
    private static final List<String> IT_PHASES = Arrays.asList(
            "pre-integration-test",
            "integration-test",
            "post-integration-test",
            "verify");

    @Parameter(defaultValue = "${mojoExecution}", readonly = true)
    protected MojoExecution execution;

    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;

    @Parameter(defaultValue = "${settings}", readonly = true)
    protected Settings settings;

    @Parameter(defaultValue = "${project}", readonly = true, required = false)
    protected MavenProject project;

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

    protected Optional<MavenProject> getProject() {
        return Optional.ofNullable(project);
    }

    protected abstract boolean isIndividuallySkipped();

    protected void executeGuardedIntegrationTest() throws MojoExecutionException, MojoFailureException {

    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (IT_PHASES.contains(execution.getLifecyclePhase())) {
            boolean skip = isIndividuallySkipped();
            if (skip || skipITs || skipTests) {
                getLog().info("skipping [skip=" + skip + "][skipITs=" + skipITs + "][skipTests=" + skipTests + "]");
                return;
            } else {
                executeGuardedIntegrationTest();
            }
        }
    }
}
