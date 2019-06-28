package net.adamcin.oakpal.maven.mojo;

import java.io.File;
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
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.repository.RepositorySystem;

public interface MojoWithRepositoryParams extends MojoWithCommonParams {

    RepositorySystem getRepositorySystem();

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
            throws MojoExecutionException {
        RepositoryRequest baseRequest = DefaultRepositoryRequest.getRepositoryRequest(getSession(),
                getProject().orElse(null));

        return dependencies.stream()
                .map(d -> depToArtifact(d, baseRequest, transitive))
                .flatMap(Set::stream)
                .collect(Collectors.toList());
    }

    default List<File> resolveDependencies(final List<Dependency> dependencies, final boolean transitive)
            throws MojoExecutionException {

        List<Artifact> preResolved = resolveArtifacts(dependencies, transitive);

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

}
