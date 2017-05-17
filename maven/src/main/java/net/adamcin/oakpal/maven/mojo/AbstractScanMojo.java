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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.adamcin.oakpal.core.PackageListener;
import net.adamcin.oakpal.core.PackageScanner;
import net.adamcin.oakpal.core.ScriptPackageListener;
import net.adamcin.oakpal.core.Violation;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.repository.RepositorySystem;

/**
 * Base scan class defining scanner parameters.
 */
abstract class AbstractScanMojo extends AbstractMojo {

    public static class JcrNs {
        private String prefix;
        private String uri;

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }

    @Component
    private RepositorySystem repositorySystem;

    @Parameter(name = "preInstallArtifacts")
    protected List<Dependency> preInstallArtifacts = new ArrayList<>();

    @Parameter(name = "preInstallFiles")
    protected List<File> preInstallFiles = new ArrayList<>();

    @Parameter(name = "cndFiles")
    protected List<File> cndFiles = new ArrayList<>();

    @Parameter(name = "jcrNamespaces")
    protected List<JcrNs> jcrNamespaces = new ArrayList<>();

    @Parameter(name = "jcrPrivileges")
    protected List<String> jcrPrivileges = new ArrayList<>();

    @Parameter(name = "forcedRoots")
    protected List<PackageScanner.ForcedRoot> forcedRoots = new ArrayList<>();

    @Parameter(name = "scriptReporters")
    protected List<File> scriptReporters = new ArrayList<>();

    @Parameter(defaultValue = "MAJOR")
    protected Violation.Severity failOnSeverity = Violation.Severity.MAJOR;

    private Artifact depToArtifact(Dependency dependency, RepositoryRequest baseRequest) {
        Artifact artifact = repositorySystem.createDependencyArtifact(dependency);
        ArtifactResolutionRequest request = new ArtifactResolutionRequest(baseRequest);
        request.setArtifact(artifact);
        repositorySystem.resolve(request);
        return artifact;
    }

    protected PackageScanner.Builder getBuilder() {

        final List<PackageListener> listeners = new ArrayList<>();

        if (scriptReporters != null) {
            List<ScriptPackageListener> scriptListeners = scriptReporters.stream().map(s -> {
                try {
                    return Optional.of(ScriptPackageListener.createScriptListener("nashorn",
                            s.toURI().toURL()));
                } catch (Exception e) {
                    getLog().error("Failed to read scriptReporter " + s.getAbsolutePath(), e);
                    return Optional.<ScriptPackageListener>empty();
                }
            }).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
            listeners.addAll(scriptListeners);
        }

        List<File> preInstall = new ArrayList<>();

        if (preInstallArtifacts != null && !preInstallArtifacts.isEmpty()) {
            RepositoryRequest baseRequest = DefaultRepositoryRequest.getRepositoryRequest(session, project);

            List<File> preInstallResolved = preInstallArtifacts.stream()
                    .map(d -> depToArtifact(d, baseRequest))
                    .map(a -> Optional.ofNullable(a.getFile()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(File::exists).collect(Collectors.toList());
            preInstall.addAll(preInstallResolved);
        }

        if (preInstallFiles != null) {
            preInstall.addAll(preInstallFiles);
        }

        PackageScanner.Builder builder = new PackageScanner.Builder()
                .withPackageListeners(listeners)
                .withCndFiles(cndFiles)
                .withPreInstallPackages(preInstall);

        if (jcrNamespaces != null) {
            for (JcrNs ns : jcrNamespaces) {
                builder = builder.withNs(ns.getPrefix(), ns.getUri());
            }
        }

        if (jcrPrivileges != null) {
            for (String privilege : jcrPrivileges) {
                builder = builder.withPrivilege(privilege);
            }
        }

        if (forcedRoots != null) {
            for (PackageScanner.ForcedRoot forcedRoot : forcedRoots) {
                builder = builder.withForcedRoot(forcedRoot);
            }
        }

        return builder;
    }
}
