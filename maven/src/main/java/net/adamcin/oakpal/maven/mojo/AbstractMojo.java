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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
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

    /**
     * package private for tests.
     */
    @Component
    RepositorySystem repositorySystem;

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

    private ClassLoader containerClassLoader;

    protected Set<Artifact> depToArtifact(final Dependency dependency, final RepositoryRequest baseRequest,
                                          final boolean transitive) {
        Artifact artifact = repositorySystem.createDependencyArtifact(dependency);
        ArtifactResolutionRequest request = new ArtifactResolutionRequest(baseRequest);
        request.setArtifact(artifact);
        request.setResolveTransitively(transitive);
        ArtifactResolutionResult result = repositorySystem.resolve(request);
        return transitive ? result.getArtifacts() : Collections.singleton(artifact);
    }

    protected List<File> resolveDependencies(final List<Dependency> dependencies, final boolean transitive)
            throws MojoExecutionException {
        RepositoryRequest baseRequest = DefaultRepositoryRequest.getRepositoryRequest(session, project);

        Set<Artifact> preResolved = dependencies.stream()
                .map(d -> depToArtifact(d, baseRequest, transitive))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        Optional<Artifact> unresolvedArtifact = preResolved.stream()
                .filter(a -> a.getFile() == null || !a.getFile().exists())
                .findFirst();

        if (unresolvedArtifact.isPresent()) {
            Artifact a = unresolvedArtifact.get();
            throw new MojoExecutionException(String.format("Failed to resolve file for artifact: %s:%s:%s",
                    a.getGroupId(), a.getArtifactId(), a.getVersion()));
        }

        return preResolved.stream()
                .map(Artifact::getFile)
                .filter(File::exists)
                .collect(Collectors.toList());
    }

    protected ClassLoader getContainerClassLoader() throws MojoExecutionException {
        if (containerClassLoader == null) {
            this.containerClassLoader = createContainerClassLoader();
        }

        return this.containerClassLoader;
    }

    private ClassLoader createContainerClassLoader() throws MojoExecutionException {
        final List<File> dependencyJars = new ArrayList<>();
        getProject().ifPresent(project -> {
            dependencyJars.add(new File(project.getBuild().getTestOutputDirectory()));
        });

        List<Dependency> unresolvedDependencies = new ArrayList<>();

        getProject().ifPresent(project ->
                unresolvedDependencies.addAll(project.getDependencies().stream()
                        .filter(dependency -> "jar".equals(dependency.getType()))
                        .filter(dependency -> "test".equals(dependency.getScope()))
                        .collect(Collectors.toList()))
        );

        dependencyJars.addAll(resolveDependencies(unresolvedDependencies, true));

        try {
            List<URL> urls = new ArrayList<>(dependencyJars.size());
            for (File file : dependencyJars) {
                urls.add(file.toURI().toURL());
            }
            return new URLClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
        } catch (Exception e) {
            throw new MojoExecutionException("ClassLoader error: ", e);
        }
    }

    void executeGuardedIntegrationTest() throws MojoExecutionException, MojoFailureException {

    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (IT_PHASES.contains(execution.getLifecyclePhase())) {
            boolean skip = isIndividuallySkipped();
            if (skip || skipITs || skipTests) {
                getLog().info("skipping [skip=" + skip + "][skipITs=" + skipITs + "][skipTests=" + skipTests + "]");
                return;
            } else {
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
}
