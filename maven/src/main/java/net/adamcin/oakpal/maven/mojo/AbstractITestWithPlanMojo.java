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
import java.util.ArrayList;
import java.util.List;

import net.adamcin.oakpal.core.CheckSpec;
import net.adamcin.oakpal.core.ForcedRoot;
import net.adamcin.oakpal.core.InstallHookPolicy;
import net.adamcin.oakpal.core.JcrNs;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Base scan class defining scanner parameters.
 */
abstract class AbstractITestWithPlanMojo extends AbstractITestMojo implements PlanBuilderParams, MojoWithPlanParams {

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
    protected List<DependencyFilter> preInstallArtifacts = new ArrayList<>();

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
     * Specify a list of Checks to locate and load as {@link net.adamcin.oakpal.core.ProgressCheck}s.
     * <p>
     * Minimally, there are two ways to define a {@link CheckSpec}. To load a check from the project's test output
     * directory, you must specify the {@code impl} value as a classPath-relative resource name for script checks, or as
     * a fully-qualified class name for Java {@link net.adamcin.oakpal.core.ProgressCheck} implementations.
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
     * also provide a {@code config} element that will be translated to a {@link javax.json.JsonObject} by the
     * {@link net.adamcin.oakpal.maven.component.JavaxJsonObjectConverter} via the
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
     * If this is set to true, InstallHooks in pre-install packages will be enabled.
     *
     * @since 1.4.0
     */
    @Parameter
    protected boolean enablePreInstallHooks;

    /**
     * Specify a policy for InstallHooks in scanned packages. InstallHooks are skipped by default.
     *
     * @since 1.4.0
     */
    @Parameter
    protected InstallHookPolicy installHookPolicy;

    @Parameter(defaultValue = "${project.build.directory}/oakpal-blobs")
    protected String blobStorePath;

    @Override
    public final PlanBuilderParams getPlanBuilderParams() {
        return this;
    }

    @Override
    public List<DependencyFilter> getPreInstallArtifacts() {
        return preInstallArtifacts;
    }

    @Override
    public List<File> getPreInstallFiles() {
        return preInstallFiles;
    }

    @Override
    public List<String> getCndNames() {
        return cndNames;
    }

    @Override
    public boolean isSlingNodeTypes() {
        return slingNodeTypes;
    }

    @Override
    public List<JcrNs> getJcrNamespaces() {
        return jcrNamespaces;
    }

    @Override
    public List<String> getJcrPrivileges() {
        return jcrPrivileges;
    }

    @Override
    public List<ForcedRoot> getForcedRoots() {
        return forcedRoots;
    }

    @Override
    public List<CheckSpec> getChecks() {
        return checks;
    }

    @Override
    public List<String> getChecklists() {
        return checklists;
    }

    @Override
    public boolean isEnablePreInstallHooks() {
        return enablePreInstallHooks;
    }

    @Override
    public InstallHookPolicy getInstallHookPolicy() {
        return installHookPolicy;
    }
}
