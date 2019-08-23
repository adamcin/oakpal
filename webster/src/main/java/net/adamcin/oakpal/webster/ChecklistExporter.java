/*
 * Copyright 2019 Mark Adamcin
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

package net.adamcin.oakpal.webster;

import static net.adamcin.oakpal.core.Fun.mapValue;
import static net.adamcin.oakpal.core.Fun.testKey;
import static net.adamcin.oakpal.core.Fun.uncheck1;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.stream.JsonCollectors;
import javax.json.stream.JsonGenerator;

import net.adamcin.oakpal.core.Checklist;
import net.adamcin.oakpal.core.ForcedRoot;
import net.adamcin.oakpal.core.Fun;
import net.adamcin.oakpal.core.JavaxJson;
import net.adamcin.oakpal.core.JcrNs;
import net.adamcin.oakpal.core.JsonCnd;
import net.adamcin.oakpal.core.checks.Rule;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.namespace.SessionNamespaceResolver;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exports namespaces, node types, and {@link ForcedRoot}s from a JCR session to assist with project checklist management.
 */
public final class ChecklistExporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChecklistExporter.class);

    /**
     * Builder for a {@link ChecklistExporter}, which is otherwise immutable.
     */
    public static class Builder {
        private List<Op> operations = new ArrayList<>();
        private List<String> exportTypeDefs = new ArrayList<>();
        private List<Rule> pathScopes = new ArrayList<>();
        private List<Rule> nodeTypeFilters = new ArrayList<>();
        private List<JcrNs> nsMapping = new ArrayList<>();

        /**
         * Add an operation to export a set of roots by individual paths. If a specified path does not exist in the
         * repository, a root will not be exported for it.
         *
         * @param paths the list of paths to export as forced roots
         * @return this builder
         */
        public Builder byPath(final String... paths) {
            LOGGER.debug("[byPath] paths={}", Arrays.toString(paths));
            this.operations.add(new Op(SelectorType.PATH, paths));
            return this;
        }

        /**
         * Add an operation to export a set of roots returned by the specified JCR query. This query can be in either
         * JCR-SQL2 or XPath language.
         *
         * @param statement the JCR query statement
         * @return this builder
         */
        public Builder byQuery(final String statement) {
            LOGGER.debug("[byQuery] statement={}", statement);
            this.operations.add(new Op(SelectorType.QUERY, statement));
            return this;
        }

        /**
         * Add an operation to export a set of roots returned by a nodetype query. By default, only nodes which are
         * _explicitly_ typed with one of the specified node types are returned. If you also want to include nodes which
         * are either _explicitly_ or _implicitly_ typed with a particular node type or one of its subtypes, you must
         * prefix the type name with a "+", e.g. "+sling:Folder" instead of "sling:Folder".
         *
         * @param nodeTypes the list of JCR node types to query for
         * @return this builder
         */
        public Builder byNodeType(final String... nodeTypes) {
            LOGGER.debug("[byNodeType] nodeTypes={}", Arrays.toString(nodeTypes));
            this.operations.add(new Op(SelectorType.NODETYPE, nodeTypes));
            this.exportTypeDefs.addAll(Arrays.asList(nodeTypes));
            return this;
        }

        /**
         * Limit the scope of exported forced root paths using a list of {@link Rule} patterns. Paths which last-match
         * an "INCLUDE" pattern are considered to be "in scope", which means they can be returned by the specified
         * operations and/or replaced in existing checklists. Use EXCLUDE patterns to protect existing forced roots from
         * being overwritten by a particular instance of {@link ChecklistExporter}.
         *
         * @param scopePaths the list of include/exclude patterns
         * @return this builder
         * @see Rule#lastMatch(List, String)
         */
        public Builder withScopePaths(final List<Rule> scopePaths) {
            this.pathScopes = Optional.ofNullable(scopePaths).orElse(Collections.emptyList());
            return this;
        }

        /**
         * In situations where a checklist is only concerned with a subset of node types, use this method to
         * exclude irrelevant types from the output. This is useful when trying to avoid introducing a dependency on an
         * JCR namespace or CND that exists in a template repository for other purposes. For example, an EXCLUDE rule
         * for "cq:.*" would exclude any types in the cq namespace.
         *
         * @param nodeTypeFilters the list of include/exclude patterns
         * @return this builder
         * @see Rule#lastMatch(List, String)
         */
        public Builder withNodeTypeFilters(final List<Rule> nodeTypeFilters) {
            this.nodeTypeFilters = Optional.ofNullable(nodeTypeFilters).orElse(Collections.emptyList());
            return this;
        }

        /**
         * Provide a list of JCR namespace prefix mappings to register or remap before performing the operations.
         *
         * @param jcrNamespaces the list of jcr namespace mappings
         * @return this builder
         */
        public Builder withJcrNamespaces(final List<JcrNs> jcrNamespaces) {
            this.nsMapping = jcrNamespaces;
            return this;
        }

        /**
         * To export node types that aren't necessarily referenced in exported forced roots or in a node type selector,
         * list them here in the same format as you would in a node type selector. These are also filtered by
         * {@link #nodeTypeFilters}, which makes sense if you select a broad supertype for export, but want to restrict the
         * resulting exported subtypes to a particular namespace, or to exclude an enumerated list of subtypes.
         *
         * @param exportNodeTypes the list of JCR node types to export
         * @return this builder
         * @see #byNodeType(String...)
         */
        public Builder withExportNodeTypes(final List<String> exportNodeTypes) {
            Optional.ofNullable(exportNodeTypes).ifPresent(exportTypeDefs::addAll);
            return this;
        }

        /**
         * Create the new exporter instance.
         *
         * @return the new {@link ChecklistExporter} instance
         */
        public ChecklistExporter build() {
            return new ChecklistExporter(this.operations, this.exportTypeDefs, this.pathScopes, this.nodeTypeFilters,
                    this.nsMapping);
        }
    }

    public enum SelectorType {
        QUERY, PATH, NODETYPE;

        public static SelectorType byName(final String name) {
            for (SelectorType value : values()) {
                if (value.name().equalsIgnoreCase(name)) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Unknown selector type: " + name);
        }
    }

    /**
     * Represents an atomic export operation.
     */
    private static class Op {
        private final SelectorType selectorType;
        private final List<String> args;

        private Op(final SelectorType selectorType, final String... args) {
            if (args == null) {
                throw new IllegalArgumentException("args cannot be null");
            }
            this.selectorType = selectorType;
            this.args = Arrays.asList(args);
        }

        @Override
        public String toString() {
            return String.format("%s: %s", selectorType.name(), args);
        }
    }

    private final List<Op> operations;
    private final List<String> exportTypeDefs;
    private final List<Rule> pathScopes;
    private final List<Rule> nodeTypeFilters;
    private final List<JcrNs> jcrNamespaces;

    private ChecklistExporter(final List<Op> operations,
                              final List<String> exportTypeDefs,
                              final List<Rule> pathScopes,
                              final List<Rule> nodeTypeFilters,
                              final List<JcrNs> jcrNamespaces) {
        this.operations = operations;
        this.exportTypeDefs = exportTypeDefs;
        this.pathScopes = pathScopes;
        this.nodeTypeFilters = nodeTypeFilters;
        this.jcrNamespaces = jcrNamespaces;
    }

    public enum ForcedRootUpdatePolicy {

        /**
         * Remove all existing forced roots from the checklist before exporting new ones.
         */
        TRUNCATE,

        /**
         * Remove existing forced roots that are included by the pathScopes before exporting new ones.
         */
        REPLACE,

        /**
         * Existing forced roots will not be removed. If exported root paths match existing root paths, the existing
         * entries will be overwritten, such that the primaryType and mixinTypes may be changed by the export.
         */
        MERGE;

        public static ForcedRootUpdatePolicy byName(final String name) {
            for (ForcedRootUpdatePolicy value : values()) {
                if (value.name().equalsIgnoreCase(name)) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Unknown policy name: " + name);
        }
    }

    public static final ForcedRootUpdatePolicy DEFAULT_UPDATE_POLICY = ForcedRootUpdatePolicy.REPLACE;
    public static final String COVARIANT_PREFIX = "+";

    static final Predicate<String> COVARIANT_FILTER = name -> name.startsWith(COVARIANT_PREFIX);
    static final Function<String, String> COVARIANT_FORMAT = name -> name.substring(COVARIANT_PREFIX.length());

    static void ensureNamespaces(final Session session, final NamespaceMapping namespaces) throws RepositoryException {
        NamespaceRegistry registry = session.getWorkspace().getNamespaceRegistry();
        List<String> registered = Arrays.asList(registry.getURIs());
        for (Map.Entry<String, String> entry : namespaces.getURIToPrefixMapping().entrySet()) {
            if (entry.getKey().isEmpty() || entry.getValue().isEmpty()) {
                continue;
            }
            if (registered.contains(entry.getKey())) {
                session.setNamespacePrefix(entry.getValue(), entry.getKey());
            } else {
                registry.registerNamespace(entry.getValue(), entry.getKey());
            }
        }
    }

    static Set<String> findJcrPrefixesInForcedRoot(final @NotNull Set<String> acc, final @NotNull ForcedRoot forcedRoot) {
        acc.addAll(Arrays.asList(forcedRoot.getNamespacePrefixes()));
        return acc;
    }

    static Set<String> findNodeTypesInForcedRoot(final Set<String> acc, final ForcedRoot forcedRoot) {
        Optional.ofNullable(forcedRoot.getPrimaryType()).ifPresent(acc::add);
        Optional.ofNullable(forcedRoot.getMixinTypes()).ifPresent(acc::addAll);
        return acc;
    }

    static Function<String, String> nsRemapName(final NamespaceMapping fromMapping, final NamespaceMapping toMapping) {
        Map<String, String> fromUris = fromMapping.getURIToPrefixMapping();
        Map<String, String> toUris = toMapping.getURIToPrefixMapping();
        Set<String> allUris = new HashSet<>(fromUris.keySet());
        allUris.addAll(toUris.keySet());

        final Map<String, Pattern> prefixToPrefix = new HashMap<>();
        for (String uri : allUris) {
            final String fromPrefix = fromUris.getOrDefault(uri, toUris.get(uri));
            final String toPrefix = toUris.getOrDefault(uri, fromUris.get(uri));
            if (!fromPrefix.equals(toPrefix)) {
                prefixToPrefix.put(toPrefix, Pattern.compile("(?<=^|/)" + Pattern.quote(fromPrefix) + "(?=:)"));
            }
        }

        return value -> prefixToPrefix.entrySet().stream()
                .reduce(
                        value,
                        (input, entry) -> entry.getValue().matcher(input).replaceAll(entry.getKey()),
                        (left, right) -> left.equals(value) ? right : left);
    }

    static Function<ForcedRoot, ForcedRoot> nsRemapForcedRoot(final NamespaceMapping fromMapping, final NamespaceMapping toMapping) {
        final Function<String, String> replacer = nsRemapName(fromMapping, toMapping);
        return orig -> {
            ForcedRoot root = new ForcedRoot();
            Optional.ofNullable(orig.getPath()).map(replacer).ifPresent(root::setPath);
            Optional.ofNullable(orig.getPrimaryType()).map(replacer).ifPresent(root::setPrimaryType);
            Optional.ofNullable(orig.getMixinTypes())
                    .map(mixins -> mixins.stream().map(replacer).collect(Collectors.toList()))
                    .ifPresent(root::setMixinTypes);
            return root;
        };
    }

    /**
     * Function type that provides a Writer.
     */
    @FunctionalInterface
    public interface WriterOpener {
        @NotNull Writer open() throws IOException;
    }

    Predicate<ForcedRoot> getRetainFilter(final ForcedRootUpdatePolicy updatePolicy) {
        switch (updatePolicy != null ? updatePolicy : DEFAULT_UPDATE_POLICY) {
            case TRUNCATE:
                // retain nothing
                return root -> false;
            case REPLACE:
                // only retain roots excluded by the path filter
                return root -> Rule.lastMatch(pathScopes, root.getPath()).isExclude();
            case MERGE:
            default:
                // retain everything
                return root -> true;
        }
    }

    BiPredicate<NamePathResolver, NodeType> exportTypeDefSelector() {
        final Set<String> singleTypes = exportTypeDefs.stream()
                .filter(COVARIANT_FILTER.negate())
                .collect(Collectors.toSet());
        final Set<String> superTypes = exportTypeDefs.stream()
                .filter(COVARIANT_FILTER)
                .map(COVARIANT_FORMAT)
                .collect(Collectors.toSet());
        return (resolver, type) -> {
            final String name = type.getName();
            return Rule.lastMatch(nodeTypeFilters, name).isInclude()
                    && (singleTypes.contains(name) || Stream.of(type.getSupertypes())
                    .map(NodeType::getName).anyMatch(superTypes::contains));
        };
    }

    /**
     * Update a checklist (or start a new one) with the forced roots exported from the provided session. Tidied JSON
     * output will be written to the provided writer.
     *
     * @param writerOpener an opener that provides the writer to write the JSON output to
     * @param session      the JCR session to export roots from
     * @param checklist    the checklist to update, or null to start from scratch
     * @param updatePolicy specify behavior for retaining existing forced roots
     * @throws IOException         if an error occurs when writing the checklist
     * @throws RepositoryException if an error occurs when exporting the new forced roots
     */
    public void updateChecklist(final WriterOpener writerOpener,
                                final Session session,
                                final Checklist checklist,
                                final ForcedRootUpdatePolicy updatePolicy)
            throws IOException, RepositoryException {

        final List<JcrNs> chkNs = new ArrayList<>();
        // first attempt to remap JCR namespaces in the session, if necessary.
        if (checklist != null && checklist.getJcrNamespaces() != null) {
            chkNs.addAll(checklist.getJcrNamespaces());
        }

        final NamespaceMapping origMapping = JsonCnd.toNamespaceMapping(chkNs);
        final NamespaceMapping remapping = JsonCnd.toNamespaceMapping(jcrNamespaces);

        ensureNamespaces(session, origMapping);
        ensureNamespaces(session, remapping);

        // try to find the roots. If any error occurs there, we want to fail fast before committing to other
        // potentially expensive, destructive, or error-prone logic.
        final List<ForcedRoot> newRoots = findRoots(session);

        // construct a stream filter for retaining existing forced roots
        Predicate<ForcedRoot> retainFilter = getRetainFilter(updatePolicy);

        final JsonObjectBuilder builder = Json.createObjectBuilder();
        final Map<String, ForcedRoot> existing = new LinkedHashMap<>();
        final List<String> privileges = new ArrayList<>();

        // remap the names of existing jcr definitions to match the new jcr namespaces
        if (checklist != null) {
            checklist.toJson().forEach(builder::add);

            checklist.getJcrPrivilegeNames().stream()
                    .map(nsRemapName(origMapping, remapping))
                    .forEachOrdered(privileges::add);

            checklist.getForcedRoots().stream()
                    .map(nsRemapForcedRoot(origMapping, remapping))
                    .filter(retainFilter)
                    .forEachOrdered(root -> existing.put(root.getPath(), root));
        }

        final Set<String> finalPrefixes = new HashSet<>();
        if (!privileges.isEmpty()) {
            builder.add(Checklist.KEY_JCR_PRIVILEGES, JavaxJson.wrap(privileges));
            privileges.stream().flatMap(JsonCnd::streamNsPrefix).forEach(finalPrefixes::add);
        }

        newRoots.forEach(root -> existing.put(root.getPath(), root));

        final List<ForcedRoot> forcedRoots = new ArrayList<>(existing.values());
        Collections.sort(forcedRoots);

        final JsonArray forcedRootsJson = forcedRoots.stream()
                .map(ForcedRoot::toJson)
                .collect(JsonCollectors.toJsonArray());

        builder.add(Checklist.KEY_FORCED_ROOTS, forcedRootsJson);

        // begin nodetype handling

        final NamePathResolver resolver = new DefaultNamePathResolver(session);
        final Set<Name> builtinNodetypes = CndExporter.BUILTIN_NODETYPES.stream()
                .map(uncheck1(resolver::getQName))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        final List<Name> foundNodeTypes = forcedRoots.stream()
                .reduce(new HashSet<>(),
                        ChecklistExporter::findNodeTypesInForcedRoot,
                        (left, right) -> {
                            left.addAll(right);
                            return left;
                        }).stream()
                .map(uncheck1(resolver::getQName))
                .collect(Collectors.toList());

        final Map<Name, NodeTypeDefinition> exportedNodeTypes =
                CndExporter.retrieveNodeTypes(session, foundNodeTypes, exportTypeDefSelector());
        final List<QNodeTypeDefinition> qNodeTypes = exportedNodeTypes.entrySet().stream()
                .map(mapValue(JsonCnd.adaptToQ(session)))
                .filter(testKey(((Predicate<Name>) builtinNodetypes::contains).negate()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        final NamespaceMapping spm = new NamespaceMapping(new SessionNamespaceResolver(session));

        if (!qNodeTypes.isEmpty()) {
            final Set<String> nsUris = qNodeTypes.stream()
                    .flatMap(JsonCnd::namedBy)
                    .map(Name::getNamespaceURI)
                    .collect(Collectors.toSet());

            final Map<String, String> uriMapping = remapping.getURIToPrefixMapping();
            nsUris.forEach(Fun.uncheckVoid1(uri -> {
                final String prefix = uriMapping.containsKey(uri) ? uriMapping.get(uri) : spm.getPrefix(uri);
                finalPrefixes.add(prefix);
                spm.setMapping(prefix, uri);
            }));

            final JsonObject jcrNodetypes =
                    JsonCnd.toJson(qNodeTypes, new NamespaceMapping(new SessionNamespaceResolver(session)));
            builder.add(Checklist.KEY_JCR_NODETYPES, jcrNodetypes);
        }

        // begin namespace handling
        final Set<String> forcedRootPrefixes = forcedRoots.stream()
                .reduce(new HashSet<>(),
                        ChecklistExporter::findJcrPrefixesInForcedRoot,
                        (left, right) -> {
                            left.addAll(right);
                            return left;
                        });
        finalPrefixes.addAll(forcedRootPrefixes);

        finalPrefixes.removeAll(JsonCnd.BUILTIN_MAPPINGS.getPrefixToURIMapping().keySet());

        if (!finalPrefixes.isEmpty()) {
            final List<JcrNs> exportNamespaces = finalPrefixes.stream()
                    .map(uncheck1(prefix -> JcrNs.create(prefix, spm.getURI(prefix))))
                    .sorted()
                    .collect(Collectors.toList());

            builder.add(Checklist.KEY_JCR_NAMESPACES, exportNamespaces.stream()
                    .map(JcrNs::toJson)
                    .collect(JsonCollectors.toJsonArray()));
        }

        final JsonObject sorted = builder.build().entrySet().stream()
                .sorted(Checklist.comparingJsonKeys(Map.Entry::getKey))
                .collect(JsonCollectors.toJsonObject());

        try (Writer writer = writerOpener.open();
             JsonWriter jsonWriter = Json
                     .createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true))
                     .createWriter(writer)) {
            jsonWriter.writeObject(sorted);
        }
    }

    /**
     * Perform all retrieval operations against the provided session.
     *
     * @param session the session to retrieve nodes from
     * @return the result list of {@link ForcedRoot}s
     * @throws RepositoryException if an error occurs
     */
    public List<ForcedRoot> findRoots(final Session session) throws RepositoryException {
        List<ForcedRoot> roots = new ArrayList<>();
        for (Op op : this.operations) {
            switch (op.selectorType) {
                case PATH:
                    roots.addAll(traverse(session, op.args));
                    break;
                case NODETYPE:
                    roots.addAll(query(session, ntStatement(session, op.args)));
                    break;
                case QUERY:
                default:
                    roots.addAll(query(session, op.args.get(0)));
                    break;
            }
        }
        return roots;
    }

    /**
     * Construct a union SQL2 statement to find nodes of specified types. Prefix a node type name with a '+' to treat
     * that type as a supertype and find nodes which are of that type or any of its subtypes.
     *
     * @param session       the session which will be queried
     * @param nodeTypeNames the list of node type names to query from
     * @return the generated JCR-SQL2 query statement
     * @throws RepositoryException if an error occurs
     */
    String ntStatement(final Session session, final List<String> nodeTypeNames) throws RepositoryException {
        final List<String> subqueries = new ArrayList<>();
        final NodeTypeManager ntManager = session.getWorkspace().getNodeTypeManager();

        // covariants (find nodes of specified types or their subtypes)
        nodeTypeNames.stream()
                .filter(COVARIANT_FILTER)
                .map(COVARIANT_FORMAT)
                .filter(Fun.testOrDefault1(ntManager::hasNodeType, false))
                .map(name -> String.format("SELECT [jcr:path] FROM [%s] AS a", name))
                .forEachOrdered(subqueries::add);

        // invariants (find only nodes which are of specified types)
        nodeTypeNames.stream()
                .filter(COVARIANT_FILTER.negate())
                .filter(Fun.testOrDefault1(ntManager::hasNodeType, false))
                .map(name -> String.format("SELECT [jcr:path] FROM [nt:base] AS a WHERE [a].[jcr:primaryType] = '%s' UNION SELECT [jcr:path] FROM [nt:base] AS a WHERE [a].[jcr:mixinTypes] = '%s'", name, name))
                .forEachOrdered(subqueries::add);

        return String.join(" UNION ", subqueries) + " OPTION(TRAVERSAL OK, INDEX NAME nodetype)";
    }

    /**
     * Retrieve a list of {@link ForcedRoot}s by JCR query.
     *
     * @param session   the session to retrieve nodes from
     * @param statement the query statement
     * @return the result list of ForcedRoots
     * @throws RepositoryException when an error occurs
     */
    List<ForcedRoot> query(final Session session, final String statement) throws RepositoryException {
        final QueryManager qm = session.getWorkspace().getQueryManager();
        final String language =
                statement.toUpperCase().replaceFirst("^\\s*((MEASURE|EXPLAIN)\\s*)*", "")
                        .startsWith("SELECT") ? Query.JCR_SQL2 : Query.XPATH;
        final Query query = qm.createQuery(statement, language);
        final QueryResult result = query.execute();
        final Map<String, ForcedRoot> roots = new LinkedHashMap<>();
        for (NodeIterator nodes = result.getNodes(); nodes.hasNext(); ) {
            nodeToRoot(nodes.nextNode()).ifPresent(root -> roots.put(root.getPath(), root));
        }
        return new ArrayList<>(roots.values());
    }

    /**
     * Gets each node from the session, then adapts to a {@link ForcedRoot} within the current path and node type scopes.
     *
     * @param session the session to retrieve nodes from
     * @param paths   the list of node paths to retrieve
     * @return the result list of ForcedRoots
     * @throws RepositoryException when an error occurs
     */
    List<ForcedRoot> traverse(final Session session, final List<String> paths) throws RepositoryException {
        final List<ForcedRoot> roots = new ArrayList<>();
        for (String path : paths) {
            if (session.nodeExists(path)) {
                nodeToRoot(session.getNode(path)).ifPresent(roots::add);
            }
        }
        return roots;
    }

    /**
     * Construct a forced root object for the given node, within the context of path and type scopes.
     *
     * @param node the node to convert to a {@link ForcedRoot}
     * @return empty when the node is not within path scope
     * @throws RepositoryException when an error occurs
     */
    Optional<ForcedRoot> nodeToRoot(final Node node) throws RepositoryException {
        if (Rule.lastMatch(pathScopes, node.getPath()).isExclude()) {
            return Optional.empty();
        }

        ForcedRoot forcedRoot = new ForcedRoot();
        forcedRoot.setPath(node.getPath());
        final String primaryType = node.getPrimaryNodeType().getName();
        if (Rule.lastMatch(nodeTypeFilters, primaryType).isInclude()) {
            forcedRoot.setPrimaryType(primaryType);
        }
        final List<String> mixinTypes = Stream.of(node.getMixinNodeTypes())
                .map(NodeType::getName)
                .filter(name -> Rule.lastMatch(nodeTypeFilters, name).isInclude())
                .collect(Collectors.toList());
        forcedRoot.setMixinTypes(mixinTypes);
        return Optional.of(forcedRoot);
    }

}
