/*
 * Copyright 2018 Mark Adamcin
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
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.adamcin.oakpal.core.CheckSpec;
import net.adamcin.oakpal.core.ChecklistPlanner;
import net.adamcin.oakpal.core.DefaultErrorListener;
import net.adamcin.oakpal.core.ErrorListener;
import net.adamcin.oakpal.core.ForcedRoot;
import net.adamcin.oakpal.core.InitStage;
import net.adamcin.oakpal.core.JcrNs;
import net.adamcin.oakpal.core.Locator;
import net.adamcin.oakpal.core.OakMachine;
import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.SlingNodetypesScanner;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

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
     * to import before any packages are installed during the scan. This is usually necessary for proprietary CRX
     * applications like AEM. Use "Tools - Export Node Type" in CRX/de lite to export all nodetypes and save it to a
     * file in your project code base on the test-scope class path (i.e. in {@code src/main/resources} of a common
     * test-scoped dependency, or under {@code src/test/resources} of the referencing module).
     *
     * @since 0.4.0
     */
    @Parameter(name = "cndNames")
    protected List<String> cndNames = new ArrayList<>();

    /**
     * Enable automatic discovery and installation of CND files referenced in {@code Sling-Nodetypes} Manifest headers
     * in the plugin dependencies or the project's {@code test}-scope dependencies.
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
     * Specify a list of Checks to locate and load as {@link ProgressCheck}s.
     * <p>
     * Minimally, there are two ways to define a {@link CheckSpec}. To load a check from the project's test output
     * directory, you must specify the {@code impl} value as a classPath-relative resource name for script checks, or as
     * a fully-qualified class name for Java {@link ProgressCheck} implementations.
     * <p>
     * For example, if your script check source is located at {@code src/test/resources/OAKPAL-INF/scripts/acme-vault-enforcer.js}:
     * <pre>
     * &lt;checks&gt;
     *   &lt;check&gt;
     *     &lt;impl&gt;OAKPAL-INF/scripts/acme-vault-enforcer.js&lt;/impl&gt;
     *   &lt;/check&gt;
     * &lt;/checks&gt;
     * </pre>
     * <p>
     * If your {@code impl} represents a script check or a {@link net.adamcin.oakpal.core.ProgressCheckFactory}, you can
     * also provide a {@code config} element that will be translated to a {@link org.json.JSONObject} by the
     * {@link net.adamcin.oakpal.maven.component.JSONObjectConverter} via the
     * {@link net.adamcin.oakpal.maven.component.OakpalComponentConfigurator}:
     * <pre>
     * &lt;checks&gt;
     *   &lt;check&gt;
     *     &lt;impl&gt;OAKPAL-INF/scripts/acme-vault-enforcer.js&lt;/impl&gt;
     *     &lt;config&gt;
     *       &lt;apples&gt;
     *         &lt;apple&gt;Granny Smith&lt;/apple&gt;
     *         &lt;apple&gt;Red Delicious&lt;/apple&gt;
     *         &lt;apple&gt;IIe&lt;/apple&gt;
     *       &lt;/apples&gt;
     *       &lt;requirePolicyNode&gt;true&lt;/requirePolicyNode&gt;
     *     &lt;/config&gt;
     *   &lt;/check&gt;
     * &lt;/checks&gt;
     * </pre>
     * <p>
     * To reference a check from a specific checklist, available on the plugin or project test-scope classpath, you should
     * not specify the {@code impl} property, but instead reference it by name, in one of the following formats, in order
     * of increasing specificity,
     * <ol>
     * <li>{@code name}</li>
     * <li>{@code checklistName/name}</li>
     * <li>{@code module/checklistName/name}</li>
     * </ol>
     * <p>
     * For example, the oakpal core library provides some useful checks in the {@code basic} checklist, like {@code echo},
     * which prints progress events to System.out:
     * <pre>
     * &lt;checks&gt;
     *   &lt;check&gt;
     *     &lt;name&gt;net.adamcin.oakpal.core/basic/echo&lt;/name&gt;
     *   &lt;/check&gt;
     * &lt;/checks&gt;
     * </pre>
     * <p>
     * It can also be referenced by shorter forms:
     * <pre>
     * &lt;checks&gt;
     *   &lt;check&gt;
     *     &lt;name&gt;basic/echo&lt;/name&gt;
     *   &lt;/check&gt;
     * &lt;/checks&gt;
     * </pre>
     * <pre>
     * &lt;checks&gt;
     *   &lt;check&gt;
     *     &lt;name&gt;echo&lt;/name&gt;
     *   &lt;/check&gt;
     * &lt;/checks&gt;
     * </pre>
     * <p>
     * You can also override the configs defined by a checklist for a given check:
     * <pre>
     * &lt;checks&gt;
     *   &lt;check&gt;
     *     &lt;name&gt;basic/paths&lt;/name&gt;
     *     &lt;config&gt;
     *       &lt;rules&gt;
     *         &lt;rule&gt;
     *           &lt;pattern&gt;/etc/tags(/.*)?&lt;/pattern&gt;
     *           &lt;type&gt;deny&lt;/type&gt;
     *         &lt;/rule&gt;
     *         &lt;rule&gt;
     *           &lt;pattern&gt;/etc/tags/acme(/.*)?&lt;/pattern&gt;
     *           &lt;type&gt;allow&lt;/type&gt;
     *         &lt;/rule&gt;
     *       &lt;/rules&gt;
     *       &lt;denyAllDeletes&gt;true&lt;/denyAllDeletes&gt;
     *     &lt;/config&gt;
     *   &lt;/check&gt;
     * &lt;/checks&gt;
     * </pre>
     *
     * @since 0.5.0
     */
    @Parameter(name = "checks")
    protected List<CheckSpec> checks = new ArrayList<>();

    /**
     * Specify a list of checklist ids {@code [module/]name} to enforce.
     *
     * @since 0.6.0
     */
    @Parameter(name = "checklists")
    protected List<String> checklists = new ArrayList<>();

    /**
     * Defer build failure for a subsequent verify goal.
     *
     * @since 1.1.0
     */
    @Parameter
    protected boolean deferBuildFailure;

    protected OakMachine.Builder getBuilder() throws MojoExecutionException {
        final ErrorListener errorListener = new DefaultErrorListener();

        final List<ProgressCheck> allChecks = new ArrayList<>();
        final ChecklistPlanner checklistPlanner = new ChecklistPlanner(errorListener, checklists);
        checklistPlanner.discoverChecklists();

        for (CheckSpec checkSpec : checklistPlanner.getEffectiveCheckSpecs(checks)) {
            if (StringUtils.isEmpty(checkSpec.getImpl())) {
                throw new MojoExecutionException("Please provide an 'impl' value for " + checkSpec.getName());
            }

            try {
                ProgressCheck progressCheck = Locator.loadProgressCheck(checkSpec.getImpl(), checkSpec.getConfig());
                if (StringUtils.isNotEmpty(checkSpec.getName())) {
                    progressCheck = Locator.wrapWithAlias(progressCheck, checkSpec.getName());
                }
                allChecks.add(progressCheck);
            } catch (final Exception e) {
                throw new MojoExecutionException(String.format("Failed to load package check %s. (impl: %s)",
                        Optional.ofNullable(checkSpec.getName()).orElse(""), checkSpec.getImpl()), e);
            }
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

        OakMachine.Builder machineBuilder = new OakMachine.Builder()
                .withErrorListener(errorListener)
                .withProgressChecks(allChecks)
                .withInitStages(checklistPlanner.getInitStages())
                .withInitStage(builder.build())
                .withPreInstallPackages(preInstall);

        return machineBuilder;
    }
}
