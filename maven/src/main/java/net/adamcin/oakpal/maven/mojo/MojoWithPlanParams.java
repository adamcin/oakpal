package net.adamcin.oakpal.maven.mojo;

import static net.adamcin.oakpal.core.Fun.compose;
import static net.adamcin.oakpal.core.Fun.uncheck1;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.adamcin.oakpal.core.ForcedRoot;
import net.adamcin.oakpal.core.JcrNs;
import net.adamcin.oakpal.core.JsonCnd;
import net.adamcin.oakpal.core.NamespaceMappingRequest;
import net.adamcin.oakpal.core.OakpalPlan;
import net.adamcin.oakpal.core.Result;
import net.adamcin.oakpal.core.SlingNodetypesScanner;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.vault.fs.spi.NodeTypeSet;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

interface MojoWithPlanParams extends MojoWithCommonParams, MojoWithRepositoryParams {

    PlanBuilderParams getPlanBuilderParams();

    default URL getPlanBaseUrl() {
        return uncheck1(File::toURL).apply(getProject().map(MavenProject::getBasedir).orElse(new File(".")));
    }

    default String getPlanName() {
        return null;
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

        planBuilder.withPreInstallUrls(preInstall.stream()
                .map(uncheck1(File::toURL)).collect(Collectors.toList()));
        planBuilder.withJcrPrivileges(params.getJcrPrivileges());
        planBuilder.withForcedRoots(params.getForcedRoots());
        planBuilder.withEnablePreInstallHooks(params.isEnablePreInstallHooks());

        final Set<URL> unorderedCndUrls = new LinkedHashSet<>();

        if (params.getCndNames() != null) {
            try {
                Map<String, URL> pluginNtds = SlingNodetypesScanner.resolveNodeTypeDefinitions(params.getCndNames());
                for (String cndName : params.getCndNames()) {
                    if (!pluginNtds.containsKey(cndName)) {
                        throw new MojoExecutionException("Failed to find node type definition on classpath for cndName "
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
                List<URL> pluginNtds = SlingNodetypesScanner.findNodeTypeDefinitions();
                for (URL ntd : pluginNtds) {
                    if (!unorderedCndUrls.contains(ntd)) {
                        getLog().info(SlingNodetypesScanner.SLING_NODETYPES + ": Discovered node types: "
                                + ntd.toString());
                    }
                }
                unorderedCndUrls.addAll(pluginNtds);
            } catch (IOException e) {
                throw new MojoFailureException("Failed to resolve cndNames.", e);
            }
        }

        // read and aggregate nodetypes from CNDs
        final NamespaceMapping initMapping = JsonCnd.toNamespaceMapping(params.getJcrNamespaces());
        List<NodeTypeSet> readSets = JsonCnd.readNodeTypes(initMapping,
                new ArrayList<>(unorderedCndUrls)).stream()
                .flatMap(Result::stream).collect(Collectors.toList());

        final NodeTypeSet nodeTypeSet = JsonCnd.aggregateNodeTypes(initMapping, readSets);
        final List<QNodeTypeDefinition> jcrNodetypes = new ArrayList<>(nodeTypeSet.getNodeTypes().values());
        planBuilder.withJcrNodetypes(jcrNodetypes);

        // build final namespace mapping
        final NamespaceMappingRequest.Builder nsRequest = new NamespaceMappingRequest.Builder();
        params.getJcrNamespaces().stream().map(JcrNs::getPrefix).forEach(nsRequest::withRetainPrefix);
        params.getJcrPrivileges().stream().flatMap(JsonCnd::streamNsPrefix).forEach(nsRequest::withJCRName);
        params.getForcedRoots().stream().flatMap(compose(ForcedRoot::getNamespacePrefixes, Stream::of))
                .forEach(nsRequest::withJCRName);
        jcrNodetypes.stream().flatMap(JsonCnd::namedBy).forEach(nsRequest::withQName);

        planBuilder.withJcrNamespaces(JsonCnd.toJcrNsList(nodeTypeSet.getNamespaceMapping(), nsRequest.build()));
        return planBuilder.build();
    }
}
