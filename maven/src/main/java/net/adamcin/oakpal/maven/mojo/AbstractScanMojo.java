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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.ForcedRoot;
import net.adamcin.oakpal.core.InitStage;
import net.adamcin.oakpal.core.Locator;
import net.adamcin.oakpal.core.PackageCheck;
import net.adamcin.oakpal.core.PackageScanner;
import net.adamcin.oakpal.core.ScriptPackageCheck;
import net.adamcin.oakpal.core.SlingNodetypesScanner;
import net.adamcin.oakpal.core.Violation;
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

    /**
     * package private for tests.
     */
    @Component
    RepositorySystem repositorySystem;

    /**
     * Specify a list of content-package artifacts to download and pre-install before the scanned packages.
     * <p>
     * For example:
     * <p>
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
     *
     * @since 0.2.0
     */
    @Parameter(name = "preInstallFiles")
    protected List<File> preInstallFiles = new ArrayList<>();

    /**
     * Specify a list of Compact NodeType Definition (CND) resource names (to discover in the test-scope classpath)
     * to import before any packages are installed during the scan. This is usually necessary for proprietary CRX applications
     * like AEM.
     *
     * @since 0.4.0
     */
    @Parameter(name = "cndNames")
    protected List<String> cndNames = new ArrayList<>();

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
     * Disable automatic discovery and installation of CND files referenced in Sling-Nodetypes Manifest headers on the
     * class path.
     *
     * @since 0.4.0
     */
    @Parameter(name = "slingNodeTypes")
    protected boolean slingNodeTypes;

    /**
     * Specify a list of additional JCR namespaces to register before installing any packages for the scan.
     * <p>
     * For example:
     * <p>
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
     * <p>
     * For example:
     * <p>
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
     * <p>
     * For example, to ensure that /home/users/system is created as a rep:AuthorizableFolder, you would add a
     * forcedRoot element with a path of "/home/users/system" and a primaryType of "rep:AuthorizableFolder".
     * <p>
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
    protected List<ForcedRoot> forcedRoots = new ArrayList<>();

    /**
     * Specify a list of javascript files implementing the {@link PackageCheck} functions that will receive events
     * for each scanned package.
     *
     * @since 0.1.0
     * @deprecated 0.4.0 use scriptPaths
     */
    @Deprecated
    @Parameter(name = "scriptReporters")
    protected List<File> scriptReporters = new ArrayList<>();

    /**
     * Specify a list of paths to script files implementing the {@link ScriptPackageCheck} functions that will receive events
     * for each scanned package.
     *
     * @since 0.4.0
     */
    @Parameter(name = "scriptPaths")
    protected List<File> scriptPaths = new ArrayList<>();

    /**
     * Specify a list of classPath resource names to locate and load as {@code PackageCheck}s.
     *
     * @since 0.4.0
     */
    @Parameter(name = "classPathChecks")
    protected List<String> classPathChecks = new ArrayList<>();

    /**
     * Specify the minimum violation severity level that will trigger plugin execution failure. Valid options are
     * {@link net.adamcin.oakpal.core.Violation.Severity#MINOR},
     * {@link net.adamcin.oakpal.core.Violation.Severity#MAJOR}, and
     * {@link net.adamcin.oakpal.core.Violation.Severity#SEVERE}.
     * <p>
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

    protected void reactToReports(List<CheckReport> reports, boolean logPackageId) throws MojoFailureException {
        String errorMessage = String.format("** Violations were reported at or above severity: %s **", failOnSeverity);

        List<CheckReport> nonEmptyReports = reports.stream()
                .filter(r -> !r.getViolations().isEmpty())
                .collect(Collectors.toList());
        boolean shouldFail = nonEmptyReports.stream().anyMatch(r -> !r.getViolations(failOnSeverity).isEmpty());

        for (CheckReport r : nonEmptyReports) {
            getLog().info(String.format(" OakPAL Check: %s", String.valueOf(r.getCheckName())));
            for (Violation v : r.getViolations()) {
                Set<PackageId> packageIds = new LinkedHashSet<>(v.getPackages());
                String violLog = logPackageId && !packageIds.isEmpty()
                        ? String.format("  +- <%s> %s %s", v.getSeverity(), v.getDescription(), packageIds)
                        : String.format("  +- <%s> %s", v.getSeverity(), v.getDescription());
                if (v.getSeverity().isLessSevereThan(failOnSeverity)) {
                    getLog().info(" " + violLog);
                } else {
                    getLog().error("" + violLog);
                }
            }
        }

        if (shouldFail) {
            getLog().error(errorMessage);
            throw new MojoFailureException(errorMessage);
        }
    }

    protected List<File> resolveDependencies(final List<Dependency> dependencies) throws MojoExecutionException {
        RepositoryRequest baseRequest = DefaultRepositoryRequest.getRepositoryRequest(session, project);

        List<Artifact> preResolved = dependencies.stream()
                .map(d -> depToArtifact(d, baseRequest)).collect(Collectors.toList());

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

    protected PackageScanner.Builder getBuilder() throws MojoExecutionException {

        final List<PackageCheck> listeners = new ArrayList<>();

        if (classPathChecks != null) {
            for (String checkName : classPathChecks) {
                try {
                    PackageCheck packageCheck = Locator.loadPackageCheck(checkName);
                    listeners.add(packageCheck);
                } catch (final Exception e) {
                    throw new MojoExecutionException("Failed to load package check by name on classPath: " + checkName, e);
                }
            }
        }

        if (scriptPaths != null) {
            if (scriptReporters != null && !scriptReporters.isEmpty()) {
                getLog().info("the scriptReporters parameter is deprecated. please use scriptPaths instead.");
                scriptPaths.addAll(scriptReporters);
            }

            List<ScriptPackageCheck> scriptListeners = scriptPaths.stream().map(s -> {
                try {
                    return Optional.of(ScriptPackageCheck.createScriptListener(s.toURI().toURL()));
                } catch (Exception e) {
                    getLog().error("Failed to read check script " + s.getAbsolutePath(), e);
                    return Optional.<ScriptPackageCheck>empty();
                }
            }).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());

            listeners.addAll(scriptListeners);
        }

        List<File> preInstall = new ArrayList<>();

        if (preInstallArtifacts != null && !preInstallArtifacts.isEmpty()) {
            List<File> preInstallResolved = resolveDependencies(preInstallArtifacts);
            preInstall.addAll(preInstallResolved);
        }

        if (preInstallFiles != null) {
            preInstall.addAll(preInstallFiles);
        }

        InitStage.Builder builder = new InitStage.Builder();

        final Set<URL> unorderedCndUrls = new LinkedHashSet<>();

        List<File> dependencyJars = new ArrayList<>();

        getProject().ifPresent(project -> {
            dependencyJars.add(new File(project.getBuild().getTestOutputDirectory()));
        });

        dependencyJars.addAll(resolveDependencies(project.getDependencies().stream()
                .filter(dependency -> "jar".equals(dependency.getType()))
                .filter(dependency -> "test".equals(dependency.getScope()))
                .collect(Collectors.toList())));

        if (cndNames != null) {
            try {
                Map<String, URL> projectNtds = SlingNodetypesScanner.resolveNodeTypeDefinitions(dependencyJars, cndNames);
                Map<String, URL> pluginNtds = SlingNodetypesScanner.resolveNodeTypeDefinitions(getClass().getClassLoader(), cndNames);
                for (String cndName : cndNames) {
                    if (!projectNtds.containsKey(cndName) && !pluginNtds.containsKey(cndName)) {
                        throw new MojoExecutionException("Failed to find node type definition on classpath for cndName " + cndName);
                    }
                }
                unorderedCndUrls.addAll(projectNtds.values());
                unorderedCndUrls.addAll(pluginNtds.values());
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to resolve cndNames.", e);
            }
        }

        if (slingNodeTypes) {
            try {
                List<URL> projectNtds = SlingNodetypesScanner.findNodeTypeDefinitions(dependencyJars);
                for (URL ntd : projectNtds) {
                    if (!unorderedCndUrls.contains(ntd)) {
                        getLog().info(SlingNodetypesScanner.SLING_NODETYPES + ": Discovered node types: "
                                + ntd.toString());
                    }
                }
                unorderedCndUrls.addAll(projectNtds);
                List<URL> pluginNtds = SlingNodetypesScanner.findNodeTypeDefinitions(getClass().getClassLoader());
                for (URL ntd : pluginNtds) {
                    if (!unorderedCndUrls.contains(ntd)) {
                        getLog().info(SlingNodetypesScanner.SLING_NODETYPES + ": Discovered node types: "
                                + ntd.toString());
                    }
                }
                unorderedCndUrls.addAll(pluginNtds);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to resolve cndNames.", e);
            }
        }

        builder.withOrderedCndUrls(new ArrayList<>(unorderedCndUrls));

        if (cndFiles != null) {
            List<URL> cndUrls = cndFiles.stream()
                    .map(File::toURI)
                    .map(uri -> {
                        URL url = null;
                        try {
                            url = uri.toURL();
                        } catch (MalformedURLException ignored) {
                        }
                        return Optional.ofNullable(url);
                    }).filter(Optional::isPresent)
                    .map(Optional::get).collect(Collectors.toList());
            builder.withOrderedCndUrls(cndUrls);
        }

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
            for (ForcedRoot forcedRoot : forcedRoots) {
                builder = builder.withForcedRoot(forcedRoot);
            }
        }

        PackageScanner.Builder scannerBuilder = new PackageScanner.Builder()
                .withPackageListeners(listeners)
                .withInitStage(builder.build())
                .withPreInstallPackages(preInstall);

        return scannerBuilder;
    }
}
