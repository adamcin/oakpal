/*
 * Copyright 2019 Mark Adamcin
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
import java.nio.file.Files;
import javax.json.JsonObject;

import net.adamcin.oakpal.maven.component.OakpalComponentConfigurator;
import net.adamcin.oakpal.webster.CliArgParser;
import net.adamcin.oakpal.webster.JcrFactory;
import net.adamcin.oakpal.webster.WebsterPlan;
import net.adamcin.oakpal.webster.targets.JsonTargetFactory;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Execute Webster targets that read from an external Oak JCR repository to update nodetypes, privileges, and checklist
 * files managed in a maven project.
 */
@Mojo(name = "webster", requiresDirectInvocation = true, configurator = OakpalComponentConfigurator.HINT)
public class WebsterMojo extends org.apache.maven.plugin.AbstractMojo {
    public static final String PARAM_REPOSITORY_HOME = "webster.repositoryHome";
    public static final String PARAM_OAK_RUN_ARGS = "webster.oakRunArgs";
    public static final String PARAM_TMPDIR = "webster.tmpdir";

    /**
     * The path to a folder containing a {@code segmentstore} directory, that also contains a {@code journal.log} file
     * and the rest of an Oak SegmentTar NodeStore. If a {@code datastore} child folder also exists, it will be treated
     * as a FileBlobStore. Otherwise, inline blobs will be assumed.
     * <p>
     * If this folder does not exist, is null, or is the current directory, the mojo will gracefully opt out of definition
     * export.
     * <p>
     * This can be overridden using the webster.oakRunArgsString property.
     */
    @Parameter(property = PARAM_REPOSITORY_HOME)
    private File websterRepositoryHome;

    /**
     * If the Oak repository NodeStore requires more complex configurations.
     */
    @Parameter(property = PARAM_OAK_RUN_ARGS)
    private String websterOakRunArgs;

    /**
     * Specify a particular director for creation of temporary segment stores.
     */
    @Parameter(property = PARAM_TMPDIR, defaultValue = "${project.build.directory}/webster-tmpdir")
    private File websterTempDirectory;

    /**
     * Directory containing resources for a FileVault archive. This is required for "nodetypes" and "privileges" targets.
     * It is also possible to reference the built content-package artifact, as in
     * "${project.build.directory}/${project.build.finalName}.zip", if this goal is executed after completion of the
     * "package" phase.
     */
    @Parameter(defaultValue = "${basedir}/src/main/content")
    private File websterArchiveRoot;

    /**
     * Specify the list of targets to update.
     */
    @Parameter
    private JsonObject websterTargets;

    /**
     * The Oak repository spin up and query process can be quite verbose. By default, Webster sets
     * "org.apache.jackrabbit.oak" loggers to ERROR to avoid flooding the execution log with minutiae. Set this
     * parameter to true to avoid suppressing these logging statements.
     */
    @Parameter(property = "webster.revealOakLogging")
    private boolean revealOakLogging;

    @Parameter(defaultValue = "${project.basedir}")
    private File baseDir;

    static void suppressOakLogging() {
        final String SIMPLE_LOGGER_PREFIX = "org.slf4j.simpleLogger.log.";
        System.setProperty(SIMPLE_LOGGER_PREFIX + "org.apache.jackrabbit.oak", "ERROR");
    }

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        WebsterPlan.Builder builder = new WebsterPlan.Builder();
        builder.withArchiveRoot(websterArchiveRoot.getAbsoluteFile());
        if (!revealOakLogging) {
            suppressOakLogging();
        }

        if (websterTargets == null || websterTargets.isEmpty()) {
            getLog().info("No websterTargets configuration found in plugin configuration. skipping webster execution.");
            return;
        }

        try {
            builder.withTargets(JsonTargetFactory.fromJsonHintMap(baseDir, websterTargets));
        } catch (Exception e) {
            throw new MojoFailureException("Failed to parse websterTargets", e);
        }

        try {
            if (websterOakRunArgs != null && !websterOakRunArgs.trim().isEmpty()) {
                getLog().info("Using webster.oakRunArgsString to configure NodeStore: " +
                        websterOakRunArgs);
                builder.withFixtureProvider(() ->
                        JcrFactory.getNodeStoreFixture(true, CliArgParser.parse(websterOakRunArgs)));
            } else if (websterRepositoryHome != null
                    && websterRepositoryHome.isDirectory()) {
                getLog().info("Using webster.repositoryHome to configure NodeStore: " +
                        websterRepositoryHome.getAbsolutePath());
                final File segmentStore = new File(websterRepositoryHome, "segmentstore");
                if (!(new File(segmentStore, "journal.log")).isFile()) {
                    getLog().info("segmentstore/journal.log file not found in configured webster.repositoryHome.");
                    return;
                }

                builder.withFixtureProvider(() -> JcrFactory.getReadOnlyFixture(segmentStore));
            } else {
                getLog().info("No source Oak repository provided. skipping webster execution.");
                return;
            }

            websterTempDirectory.mkdirs();
            final File globalRepositoryHome = Files
                    .createTempDirectory(websterTempDirectory.toPath(), "webster_repository")
                    .toFile().getAbsoluteFile();

            try {
                WebsterPlan plan = builder
                        .withGlobalSegmentStore(new File(globalRepositoryHome, "segmentstore")).build();
                plan.perform();
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to execute Webster plan.", e);
            } finally {
                try {
                    FileUtils.deleteDirectory(globalRepositoryHome);
                } catch (IOException e) {
                    getLog().error("Failed to delete temp global segment store: "
                            + globalRepositoryHome.getAbsolutePath(), e);
                }
            }
        } catch (IOException e) {
            throw new MojoFailureException("Failed to create temp global segment store", e);
        }
    }
}
