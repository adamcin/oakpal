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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.adamcin.oakpal.core.PackageListener;
import net.adamcin.oakpal.core.PackageScanner;
import net.adamcin.oakpal.core.ScriptPackageListener;
import net.adamcin.oakpal.core.Violation;
import net.adamcin.oakpal.core.ViolationReport;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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

    /**
     * Specify a list of content-package artifacts to download and pre-install before the scanned packages.
     *
     * For example:
     *
     * <pre>
     * &lt;preInstallArtifacts&gt;
     *   &lt;preInstallArtifact&gt;
     *     &lt;groupId&gt;com.adobe.acs&lt;/groupId&gt;
     *     &lt;artifactId&gt;acs-aem-commons-content&lt;/artifactId&gt;
     *     &lt;version&gt;3.9.0&lt;/version&gt;
     *     &lt;type&gt;zip&lt;/type&gt;
     *   &lt;/preInstallArtifact&gt;
     * &lt;/preInstallArtifacts&gt;
     * </pre>
     *
     * @since 0.2.0
     */
    @Parameter(name = "preInstallArtifacts")
    protected List<Dependency> preInstallArtifacts = new ArrayList<>();

    /**
     * Specify a list of content package files by path to pre-install, which have already been built or downloaded in a
     * previous phase.
     * @since 0.2.0
     */
    @Parameter(name = "preInstallFiles")
    protected List<File> preInstallFiles = new ArrayList<>();

    /**
     * Specify a list of Compact NodeType Definition (CND) files to import before any packages are installed during the
     * scan. This is usually necessary for proprietary CRX applications like AEM. Use "Tools - Export Node Type" in
     * CRX/de lite to export all nodetypes and save it to a file in your project code base.
     *
     * @since 0.1.0
     */
    @Parameter(name = "cndFiles")
    protected List<File> cndFiles = new ArrayList<>();

    /**
     * Specify a list of additional JCR namespaces to register before installing any packages for the scan.
     *
     * For example:
     *
     * <pre>
     * &lt;jcrNamespaces&gt;
     *   &lt;jcrNamespace&gt;
     *     &lt;prefix&gt;crx&lt;/prefix&gt;
     *     &lt;uri&gt;http://www.day.com/crx/1.0&lt;/uri&gt;
     *   &lt;/jcrNamespace&gt;
     * &lt;/jcrNamespaces&gt;
     * </pre>
     *
     * @since 0.2.0
     */
    @Parameter(name = "jcrNamespaces")
    protected List<JcrNs> jcrNamespaces = new ArrayList<>();

    /**
     * Specify a list of additional JCR privileges to register before installing any packages for the scan.
     *
     * For example:
     *
     * <pre>
     * &lt;jcrPrivileges&gt;
     *   &lt;jcrPrivilege&gt;crx:replicate&lt;/jcrPrivilege&gt;
     * &lt;/jcrPrivileges&gt;
     * </pre>
     *
     * @since 0.2.0
     */
    @Parameter(name = "jcrPrivileges")
    protected List<String> jcrPrivileges = new ArrayList<>();

    /**
     * Specify a list of paths with associated primaryType and mixinTypes values to create in the repository before
     * installing any packages for the scan. This should only be necessary for packages that you are pre-installing
     * and do not have the ability to adjust to ensure that they contain the necessary DocView XML files to ensure
     * content structure nodetype dependencies are self-contained in the package that depends on them.
     *
     * For example, to ensure that /home/users/system is created as a rep:AuthorizableFolder, you would add a
     * forcedRoot element with a path of "/home/users/system" and a primaryType of "rep:AuthorizableFolder".
     *
     * <pre>
     * &lt;forcedRoots&gt;
     *   &lt;forcedRoot&gt;
     *     &lt;path&gt;/home/users/system&lt;/path&gt;
     *     &lt;primaryType&gt;rep:AuthorizableFolder&lt;/primaryType&gt;
     *     &lt;mixinTypes&gt;
     *       &lt;mixinType&gt;rep:AccessControllable&lt;/mixinType&gt;
     *     &lt;/mixinTypes&gt;
     *   &lt;/forcedRoot&gt;
     * &lt;/forcedRoots&gt;
     * </pre>
     *
     * @since 0.2.0
     */
    @Parameter(name = "forcedRoots")
    protected List<PackageScanner.ForcedRoot> forcedRoots = new ArrayList<>();

    /**
     * Specify a list of javascript files implementing the {@link PackageListener} functions that will receive events
     * for each scanned package.
     *
     * @since 0.1.0
     */
    @Parameter(name = "scriptReporters")
    protected List<File> scriptReporters = new ArrayList<>();

    /**
     * Specify the minimum violation severity level that will trigger plugin execution failure. Valid options are
     * {@link net.adamcin.oakpal.core.Violation.Severity#MINOR},
     * {@link net.adamcin.oakpal.core.Violation.Severity#MAJOR}, and
     * {@link net.adamcin.oakpal.core.Violation.Severity#SEVERE}.
     *
     * FYI: FileVault Importer errors are reported as MAJOR by default.
     *
     * @since 0.1.0
     */
    @Parameter(defaultValue = "MAJOR")
    protected Violation.Severity failOnSeverity = Violation.Severity.MAJOR;

    protected Artifact depToArtifact(Dependency dependency, RepositoryRequest baseRequest) {
        Artifact artifact = repositorySystem.createDependencyArtifact(dependency);
        ArtifactResolutionRequest request = new ArtifactResolutionRequest(baseRequest);
        request.setArtifact(artifact);
        repositorySystem.resolve(request);
        return artifact;
    }

    protected void reactToReports(List<ViolationReport> reports, boolean logPackageId) throws MojoFailureException {
        String errorMessage = String.format("** Violations were reported at or above severity: %s **", failOnSeverity);

        List<ViolationReport> nonEmptyReports = reports.stream()
                .filter(r -> !r.getViolations().isEmpty())
                .collect(Collectors.toList());
        boolean shouldFail = nonEmptyReports.stream().anyMatch(r -> !r.getViolations(failOnSeverity).isEmpty());

        nonEmptyReports.forEach(r -> {
            getLog().info("");
            getLog().info(String.format(" OakPAL Reporter: %s", String.valueOf(r.getReporterUrl())));
            r.getViolations().forEach(v -> {
                Set<PackageId> packageIds = new LinkedHashSet<>(v.getPackages());
                String violLog = logPackageId && !packageIds.isEmpty()
                        ? String.format("  +- <%s> %s %s", v.getSeverity(), v.getDescription(), packageIds)
                        : String.format("  +- <%s> %s", v.getSeverity(), v.getDescription());
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
    }

    protected PackageScanner.Builder getBuilder() throws MojoExecutionException {

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

            List<Artifact> preResolved = preInstallArtifacts.stream()
                    .map(d -> depToArtifact(d, baseRequest)).collect(Collectors.toList());

            Optional<Artifact> unresolvedArtifact = preResolved.stream()
                    .filter(a -> a.getFile() == null || !a.getFile().exists())
                    .findFirst();

            if (unresolvedArtifact.isPresent()) {
                Artifact a = unresolvedArtifact.get();
                throw new MojoExecutionException(String.format("Failed to resolve file for artifact: %s:%s:%s",
                        a.getGroupId(), a.getArtifactId(), a.getVersion()));
            }

            List<File> preInstallResolved = preResolved.stream()
                    .map(Artifact::getFile)
                    .filter(File::exists)
                    .collect(Collectors.toList());

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
