package net.adamcin.oakpal.maven.mojo;

import net.adamcin.oakpal.core.ForcedRoot;
import net.adamcin.oakpal.api.Fun;
import net.adamcin.oakpal.core.JcrNs;
import net.adamcin.oakpal.core.JsonCnd;
import net.adamcin.oakpal.core.NamespaceMappingRequest;
import net.adamcin.oakpal.core.OakpalPlan;
import net.adamcin.oakpal.api.Result;
import net.adamcin.oakpal.core.SlingNodetypesScanner;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.vault.fs.spi.NodeTypeSet;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.adamcin.oakpal.api.Fun.compose1;
import static net.adamcin.oakpal.api.Fun.uncheck1;
import static net.adamcin.oakpal.api.JavaxJson.wrap;

interface MojoWithPlanParams extends MojoWithCommonParams, MojoWithRepositoryParams {

    @NotNull PlanBuilderParams getPlanBuilderParams();

    /**
     * Returns a URL that serves as the plan base URL for resource resolution. Either the maven project's
     * basedir or the current working directory.
     *
     * @return the appropriate base url for the plan
     */
    default @NotNull URL getPlanBaseUrl() {
        return compose1(File::toURI, uncheck1(URI::toURL)).apply(
                getProject()
                        .flatMap(compose1(MavenProject::getBasedir, Optional::ofNullable))
                        .map(File::getAbsoluteFile)
                        .orElse(new File(".").getAbsoluteFile()));
    }

    /**
     * Override to specify a plan name to use as the default in an opear. Otherwise, return null to use the first
     * plan listed in the Oakpal-Plan header by default.
     *
     * @return a specific default plan name or null
     */
    default @Nullable String getPlanName() {
        return null;
    }

    /**
     * Return a list of package files to include as pre-install packages in the plan.
     *
     * @param params the plan builder parameters
     * @return a list of pre-install package files
     * @throws MojoFailureException if an error occurs
     */
    default @NotNull List<File> getPreInstallFiles(final @NotNull PlanBuilderParams params)
            throws MojoFailureException {
        final List<File> preInstall = new ArrayList<>();
        if (params.getPreInstallArtifacts() != null && !params.getPreInstallArtifacts().isEmpty()) {
            List<Dependency> preInstallDeps = new ArrayList<>();
            getProject().ifPresent(project -> {
                for (DependencyFilter depFilter : params.getPreInstallArtifacts()) {
                    Dependency dep = project.getDependencies().stream()
                            .filter(depFilter)
                            .findFirst()
                            .orElseGet(depFilter::toDependency);
                    preInstallDeps.add(dep);
                }
            });
            List<File> preInstallResolved = resolveDependencies(preInstallDeps, false);
            preInstall.addAll(preInstallResolved);
        }

        if (params.getPreInstallFiles() != null) {
            preInstall.addAll(params.getPreInstallFiles());
        }
        return preInstall;
    }

    /**
     * Find and resolve CND resources on the classpath, read them, and aggregate the node type definitions for writing
     * to the {@code jcrNodetypes} property of a plan.
     *
     * @param params         the plan builder parameters
     * @param planMapping    the initial JCR namespace mapping to use
     * @param cndResolver    a throwing function that accepts a list of resource names provided by the params to resolve on
     *                       the classpath that returns a map of names to resolved urls
     * @param slingCndFinder a throwing supplier that discovers CND resources on the classpath referenced in
     *                       {@code Sling-Nodetypes} manifest headers
     * @return an aggregated nodetype set
     * @throws MojoFailureException if an error occurs
     */
    default @NotNull NodeTypeSet aggregateCnds(final @NotNull PlanBuilderParams params,
                                               final @NotNull NamespaceMapping planMapping,
                                               final @NotNull Fun.ThrowingFunction<List<String>, Map<String, URL>> cndResolver,
                                               final @NotNull Fun.ThrowingSupplier<List<URL>> slingCndFinder)
            throws MojoFailureException {

        final Set<URL> unorderedCndUrls = new LinkedHashSet<>();
        if (params.getCndNames() != null) {
            try {
                Map<String, URL> pluginNtds = cndResolver.tryApply(params.getCndNames());
                for (String cndName : params.getCndNames()) {
                    if (!pluginNtds.containsKey(cndName)) {
                        throw new MojoFailureException("Failed to find node type definition on classpath for cndName "
                                + cndName);
                    }
                }
                unorderedCndUrls.addAll(pluginNtds.values());
            } catch (Exception e) {
                throw new MojoFailureException("Failed to resolve cndNames.", e);
            }
        }

        if (params.isSlingNodeTypes()) {
            try {
                List<URL> pluginNtds = slingCndFinder.tryGet();
                for (URL ntd : pluginNtds) {
                    if (!unorderedCndUrls.contains(ntd)) {
                        getLog().info(SlingNodetypesScanner.SLING_NODETYPES + ": Discovered node types: "
                                + ntd.toString());
                    }
                }
                unorderedCndUrls.addAll(pluginNtds);
            } catch (Exception e) {
                throw new MojoFailureException("Failed to resolve cndNames.", e);
            }
        }

        // read and aggregate nodetypes from CNDs
        List<NodeTypeSet> readSets = JsonCnd.readNodeTypes(planMapping,
                new ArrayList<>(unorderedCndUrls)).stream()
                .flatMap(Result::stream).collect(Collectors.toList());

        return JsonCnd.aggregateNodeTypes(planMapping, readSets);
    }

    /**
     * Construct an Oakpal Plan purely from the relevant mojo parameters.
     *
     * @return a complete init stage
     * @throws MojoFailureException if an error occurs
     */
    default OakpalPlan buildPlan() throws MojoFailureException {
        final PlanBuilderParams params = getPlanBuilderParams();
        final OakpalPlan.Builder planBuilder = new OakpalPlan.Builder(getPlanBaseUrl(), getPlanName());

        getLog().debug("building plan: " + params);
        planBuilder.withChecklists(params.getChecklists());
        planBuilder.withChecks(params.getChecks());
        planBuilder.withForcedRoots(params.getForcedRoots());
        planBuilder.withEnablePreInstallHooks(params.isEnablePreInstallHooks());
        planBuilder.withInstallHookPolicy(params.getInstallHookPolicy());
        Optional.ofNullable(params.getRunModes()).ifPresent(planBuilder::withRunModes);

        planBuilder.withRepoInits(params.getRepoInits());
        // get repoinit files
        final List<File> repoInitFiles = params.getRepoInitFiles();
        if (repoInitFiles != null) {
            planBuilder.withRepoInitUrls(repoInitFiles.stream()
                    .map(compose1(File::toURI, uncheck1(URI::toURL))).collect(Collectors.toList()));
        }

        // get pre-install files
        final List<File> preInstall = getPreInstallFiles(params);
        planBuilder.withPreInstallUrls(preInstall.stream()
                .map(compose1(File::toURI, uncheck1(URI::toURL))).collect(Collectors.toList()));

        final NamespaceMapping planMapping = JsonCnd.toNamespaceMapping(params.getJcrNamespaces());
        planBuilder.withJcrPrivileges(JsonCnd.getPrivilegesFromJson(wrap(params.getJcrPrivileges()), planMapping));

        final NodeTypeSet nodeTypeSet = aggregateCnds(params, planMapping,
                SlingNodetypesScanner::resolveNodeTypeDefinitions, SlingNodetypesScanner::findNodeTypeDefinitions);

        final List<QNodeTypeDefinition> jcrNodetypes = new ArrayList<>(nodeTypeSet.getNodeTypes().values());
        planBuilder.withJcrNodetypes(jcrNodetypes);

        // build final namespace mapping
        final NamespaceMappingRequest.Builder nsRequest = new NamespaceMappingRequest.Builder();
        params.getJcrNamespaces().stream().map(JcrNs::getPrefix).forEach(nsRequest::withRetainPrefix);
        params.getJcrPrivileges().stream().flatMap(JsonCnd::streamNsPrefix).forEach(nsRequest::withJCRName);
        params.getForcedRoots().stream().flatMap(compose1(ForcedRoot::getNamespacePrefixes, Stream::of))
                .forEach(nsRequest::withJCRName);
        jcrNodetypes.stream().flatMap(JsonCnd::namedBy).forEach(nsRequest::withQName);

        planBuilder.withJcrNamespaces(JsonCnd.toJcrNsList(nodeTypeSet.getNamespaceMapping(), nsRequest.build()));
        return planBuilder.build();
    }
}
