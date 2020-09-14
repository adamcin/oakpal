package net.adamcin.oakpal.maven.mojo;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.adamcin.oakpal.api.Fun;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.repository.RepositorySystem;
import org.jetbrains.annotations.NotNull;

/**
 * Mojo Interface providing default methods for resolution of artifacts and creation of maven classloaders.
 */
public interface MojoWithRepositoryParams extends MojoWithCommonParams {

    RepositorySystem getRepositorySystem();

    /**
     * There are basically two use cases for the oakpal-maven-plugin:
     * 1) Adhoc testing of a content-package project artifact, in which case all oakpal classpath resources will be
     * found in {@code test} scope, or
     * 2) Creation and testing of an oakpal module with checklists, and/or creation and testing of an opear archive,
     * in which case all oakpal classpath resources will be found in a non-{@code test} scope classpath. Mojos should
     * override this method to return {@code true}, to specify that a {@code test}-scope classloader is desired, as in
     * the first case.
     *
     * @return true if a {@code test}-scope classloader is desired, or false if a non-{@code test}-scope classloader is
     * desired.
     */
    default boolean isTestScopeContainer() {
        return false;
    }

    /**
     * Creates a classloader for execution of oakpal logic.
     *
     * @return a container classLoader
     * @throws MojoFailureException if an error occurs
     */
    default ClassLoader createContainerClassLoader() throws MojoFailureException {
        final List<File> dependencyJars = new ArrayList<>();
        final boolean useTestScope = isTestScopeContainer();
        getProject().ifPresent(project -> {
            dependencyJars.add(useTestScope ?
                    new File(project.getBuild().getTestOutputDirectory()) :
                    new File(project.getBuild().getOutputDirectory()));
        });

        List<Dependency> unresolvedDependencies = new ArrayList<>();

        final Predicate<Dependency> scopeFilter = useTestScope
                ? dependency -> "test".equals(dependency.getScope())
                : dependency -> !"test".equals(dependency.getScope());

        final Set<String> desiredDepTypes = Stream.of("jar", "pom").collect(Collectors.toSet());
        getProject().ifPresent(project ->
                unresolvedDependencies.addAll(project.getDependencies().stream()
                        .filter(dependency -> desiredDepTypes.contains(dependency.getType()))
                        .filter(scopeFilter)
                        .collect(Collectors.toList()))
        );

        dependencyJars.addAll(resolveDependencies(unresolvedDependencies, true));

        URL[] urls = dependencyJars.stream()
                .map(Fun.compose1(File::toURI, Fun.uncheck1(URI::toURL)))
                .toArray(URL[]::new);

        return new URLClassLoader(urls, getClass().getClassLoader());
    }

    default Set<Artifact> depToArtifact(final @NotNull Dependency dependency,
                                        final @NotNull RepositoryRequest baseRequest,
                                        final boolean transitive) {
        Artifact artifact = getRepositorySystem().createDependencyArtifact(dependency);
        ArtifactResolutionRequest request = new ArtifactResolutionRequest(baseRequest);
        request.setArtifact(artifact);
        request.setResolveTransitively(transitive);
        ArtifactResolutionResult result = getRepositorySystem().resolve(request);
        return transitive ? result.getArtifacts() : Collections.singleton(artifact);
    }

    default List<Artifact> resolveArtifacts(final @NotNull List<Dependency> dependencies, final boolean transitive) {
        RepositoryRequest baseRequest = DefaultRepositoryRequest.getRepositoryRequest(getSession(),
                getProject().orElse(null));

        return dependencies.stream()
                .map(d -> depToArtifact(d, baseRequest, transitive))
                .flatMap(Set::stream)
                .collect(Collectors.toList());
    }

    default List<File> resolveDependencies(final @NotNull List<Dependency> dependencies, final boolean transitive)
            throws MojoFailureException {

        List<Artifact> preResolved = resolveArtifacts(dependencies, transitive);

        Optional<Artifact> unresolvedArtifact = preResolved.stream()
                .filter(a -> a.getFile() == null || !a.getFile().exists())
                .findFirst();

        if (unresolvedArtifact.isPresent()) {
            Artifact a = unresolvedArtifact.get();
            throw new MojoFailureException(String.format("Failed to resolve file for artifact: %s",
                    a.getId()));
        }

        return preResolved.stream()
                .map(Artifact::getFile)
                .filter(File::exists)
                .collect(Collectors.toList());
    }

}
