package net.adamcin.oakpal.maven.mojo;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.repository.RepositorySystem;

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

        getProject().ifPresent(project ->
                unresolvedDependencies.addAll(project.getDependencies().stream()
                        .filter(dependency -> "jar".equals(dependency.getType()))
                        .filter(scopeFilter)
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
            throw new MojoFailureException("ClassLoader error: ", e);
        }
    }

    default Set<Artifact> depToArtifact(final Dependency dependency, final RepositoryRequest baseRequest,
                                        final boolean transitive) {
        Artifact artifact = getRepositorySystem().createDependencyArtifact(dependency);
        ArtifactResolutionRequest request = new ArtifactResolutionRequest(baseRequest);
        request.setArtifact(artifact);
        request.setResolveTransitively(transitive);
        ArtifactResolutionResult result = getRepositorySystem().resolve(request);
        return transitive ? result.getArtifacts() : Collections.singleton(artifact);
    }

    default List<Artifact> resolveArtifacts(final List<Dependency> dependencies, final boolean transitive)
            throws MojoFailureException {
        RepositoryRequest baseRequest = DefaultRepositoryRequest.getRepositoryRequest(getSession(),
                getProject().orElse(null));

        return dependencies.stream()
                .map(d -> depToArtifact(d, baseRequest, transitive))
                .flatMap(Set::stream)
                .collect(Collectors.toList());
    }

    default List<File> resolveDependencies(final List<Dependency> dependencies, final boolean transitive)
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
