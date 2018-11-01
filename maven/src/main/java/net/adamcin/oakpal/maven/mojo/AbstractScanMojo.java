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
import net.adamcin.oakpal.core.CheckSpec;
import net.adamcin.oakpal.core.ChecklistPlanner;
import net.adamcin.oakpal.core.DefaultErrorListener;
import net.adamcin.oakpal.core.ErrorListener;
import net.adamcin.oakpal.core.ForcedRoot;
import net.adamcin.oakpal.core.InitStage;
import net.adamcin.oakpal.core.JcrNs;
import net.adamcin.oakpal.core.Locator;
import net.adamcin.oakpal.core.PackageCheck;
import net.adamcin.oakpal.core.PackageCheckFactory;
import net.adamcin.oakpal.core.PackageScanner;
import net.adamcin.oakpal.core.ScriptPackageCheck;
import net.adamcin.oakpal.core.SlingNodetypesScanner;
import net.adamcin.oakpal.core.Violation;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;
import org.json.JSONObject;

/**
 * Base scan class defining scanner parameters.
 */
abstract class AbstractScanMojo extends AbstractMojo {

    /**
     * Specify a list of content-package artifacts to download and pre-install before the scanned packages.
     * <p>
     * For example:
     * </p>
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
     * Enable automatic discovery and installation of CND files referenced in Sling-Nodetypes Manifest headers on the
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
     * </p>
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
     * </p>
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
     * </p>
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
     * Specify a config object that will be provided as bindings to checks loaded via {@link #scriptPaths}
     * and {@link #classPathChecks}. The Maven configuration format is normalized to an {@code org.json.JSONObject} structure.
     * <p>
     * For example, to provide a key of {@code foo} with a value of {@code bar}
     * </p>
     * <pre>
     * &lt;adhocCheckConfig&gt;
     *   &lt;foo&gt;bar&lt;/foo&gt;
     * &lt;/adhocCheckConfig&gt;
     * </pre>
     * <p>Your check script would then be able to use the value {@code config.foo} or {@code config["foo"]}</p>
     *
     * @since 0.5.0
     */
    @Parameter(name = "adhocCheckConfig")
    protected JSONObject adhocCheckConfig;

    /**
     * Specify a list of classPath resource names to locate and load as {@code PackageCheck}s.
     *
     * @since 0.4.0
     * @deprecated 0.5.0
     */
    @Deprecated
    @Parameter(name = "classPathChecks")
    protected List<String> classPathChecks = new ArrayList<>();

    /**
     * Specify a list of Checks to locate and load as {@code PackageCheck}s.
     *
     * @since 0.5.0
     */
    @Parameter(name = "checks")
    protected List<CheckSpec> checks = new ArrayList<>();

    /**
     * Specify a list of checklist ids {@code [ module/ ]name} to enforce.
     *
     * @since 0.6.0
     */
    @Parameter(name = "checklists")
    protected List<String> checklists = new ArrayList<>();


    /**
     * Specify the minimum violation severity level that will trigger plugin execution failure. Valid options are
     * {@link net.adamcin.oakpal.core.Violation.Severity#MINOR},
     * {@link net.adamcin.oakpal.core.Violation.Severity#MAJOR}, and
     * {@link net.adamcin.oakpal.core.Violation.Severity#SEVERE}.
     * <p>
     * FYI: FileVault Importer errors are reported as MAJOR by default.
     * </p>
     *
     * @since 0.1.0
     */
    @Parameter(defaultValue = "MAJOR")
    protected Violation.Severity failOnSeverity = Violation.Severity.MAJOR;

    protected void reactToReports(List<CheckReport> reports, boolean logPackageId) throws MojoFailureException {
        String errorMessage = String.format("** Violations were reported at or above severity: %s **", failOnSeverity);

        List<CheckReport> nonEmptyReports = reports.stream()
                .filter(r -> !r.getViolations().isEmpty())
                .collect(Collectors.toList());
        boolean shouldFail = nonEmptyReports.stream().anyMatch(r -> !r.getViolations(failOnSeverity).isEmpty());

        if (!nonEmptyReports.isEmpty()) {
            getLog().info("OakPAL Check Reports");
        }
        for (CheckReport r : nonEmptyReports) {
            getLog().info(String.format("  %s", String.valueOf(r.getCheckName())));
            for (Violation v : r.getViolations()) {
                Set<PackageId> packageIds = new LinkedHashSet<>(v.getPackages());
                String violLog = logPackageId && !packageIds.isEmpty()
                        ? String.format("   +- <%s> %s %s", v.getSeverity(), v.getDescription(), packageIds)
                        : String.format("   +- <%s> %s", v.getSeverity(), v.getDescription());
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

    protected PackageScanner.Builder getBuilder() throws MojoExecutionException {
        final ErrorListener errorListener = new DefaultErrorListener();

        final List<PackageCheck> allChecks = new ArrayList<>();
        final ChecklistPlanner checklistPlanner = new ChecklistPlanner(errorListener, checklists);
        checklistPlanner.discoverChecklists();

        for (CheckSpec checkSpec : checklistPlanner.getEffectiveCheckSpecs(checks)) {
            if (StringUtils.isEmpty(checkSpec.getImpl())) {
                throw new MojoExecutionException("Please provide an 'impl' value for " + checkSpec.getName());
            }

            try {
                PackageCheck packageCheck = Locator.loadPackageCheck(checkSpec.getImpl(), checkSpec.getConfig());
                if (StringUtils.isNotEmpty(checkSpec.getName())) {
                    packageCheck = Locator.wrapWithAlias(packageCheck, checkSpec.getName());
                }
                allChecks.add(packageCheck);
            } catch (final Exception e) {
                throw new MojoExecutionException(String.format("Failed to load package check %s. (impl: %s)",
                        Optional.ofNullable(checkSpec.getName()).orElse(""), checkSpec.getImpl()), e);
            }
        }

        if (classPathChecks != null) {
            for (String checkName : classPathChecks) {
                try {
                    PackageCheck packageCheck = Locator.loadPackageCheck(checkName, adhocCheckConfig);
                    allChecks.add(packageCheck);
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

            List<PackageCheck> scriptListeners = scriptPaths.stream().map(s -> {
                try {
                    PackageCheckFactory factory = ScriptPackageCheck.createScriptCheckFactory(s.toURI().toURL());
                    return Optional.of(factory.newInstance(adhocCheckConfig));
                } catch (Exception e) {
                    getLog().error("Failed to read check script " + s.getAbsolutePath(), e);
                    return Optional.<ScriptPackageCheck>empty();
                }
            }).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());

            allChecks.addAll(scriptListeners);
        }

        List<File> preInstall = new ArrayList<>();

        if (preInstallArtifacts != null && !preInstallArtifacts.isEmpty()) {
            List<File> preInstallResolved = resolveDependencies(preInstallArtifacts, false);
            preInstall.addAll(preInstallResolved);
        }

        if (preInstallFiles != null) {
            preInstall.addAll(preInstallFiles);
        }

        InitStage.Builder builder = new InitStage.Builder();

        final Set<URL> unorderedCndUrls = new LinkedHashSet<>();


        if (cndNames != null) {
            try {
                Map<String, URL> pluginNtds = SlingNodetypesScanner.resolveNodeTypeDefinitions(cndNames);
                for (String cndName : cndNames) {
                    if (!pluginNtds.containsKey(cndName)) {
                        throw new MojoExecutionException("Failed to find node type definition on classpath for cndName " + cndName);
                    }
                }
                unorderedCndUrls.addAll(pluginNtds.values());
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to resolve cndNames.", e);
            }
        }

        if (slingNodeTypes) {
            try {
                List<URL> pluginNtds = SlingNodetypesScanner.findNodeTypeDefinitions();
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
                .withErrorListener(errorListener)
                .withPackageListeners(allChecks)
                .withInitStages(checklistPlanner.getInitStages())
                .withInitStage(builder.build())
                .withPreInstallPackages(preInstall);

        return scannerBuilder;
    }
}
